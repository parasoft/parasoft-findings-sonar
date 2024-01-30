/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.parasoft.findings.sonar.soatest;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.exception.InvalidReportException;
import com.parasoft.findings.sonar.soatest.data.XUnitTestClassReport;
import com.parasoft.findings.sonar.soatest.data.XUnitTestIndex;
import com.parasoft.findings.utils.common.nls.NLS;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.Metric;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class XUnitSOAtestParser {

    private final FileSystem fs;

    public XUnitSOAtestParser(FileSystem fs) {
        this.fs = fs;
    }

    public void collect(SensorContext context, List<File> xunitFiles) {
        parseFiles(context, xunitFiles);
    }

    private void parseFiles(SensorContext context, List<File> reports) {
        XUnitTestIndex index = new XUnitTestIndex();
        parseFiles(reports, index);
        sanitize(index);
        save(index, context);
    }

    private static void parseFiles(List<File> reports, XUnitTestIndex index) {
        StaxParser parser = new StaxParser(index);
        for (File report : reports) {
            try {
                parser.parse(report);
            } catch (XMLStreamException e) {
                throw new InvalidReportException(NLS.getFormatted(Messages.FailedToParseXUnitReport, report), e);
            }
        }
    }

    private static void sanitize(XUnitTestIndex index) {
        for (String classname : index.getClassnames()) {
            if (StringUtils.contains(classname, "$")) {
                // XUnit reports classes whereas sonar supports files
                String parentClassName = StringUtils.substringBefore(classname, "$");
                index.merge(classname, parentClassName);
            }
        }
    }

    private void save(XUnitTestIndex index, SensorContext context) {
        long negativeTimeTestNumber = 0;
        Map<InputFile, XUnitTestClassReport> indexByInputFile = mapToInputFile(index.getIndexByClassname());
        for (Map.Entry<InputFile, XUnitTestClassReport> entry : indexByInputFile.entrySet()) {
            XUnitTestClassReport report = entry.getValue();
            if (report.getTests() > 0) {
                negativeTimeTestNumber += report.getNegativeTimeTestNumber();
                save(report, entry.getKey(), context);
            }
        }
        if (negativeTimeTestNumber > 0) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.TotalDurationNotAccurateWithNegativeTimeTests, negativeTimeTestNumber));
        }
    }

    private Map<InputFile, XUnitTestClassReport> mapToInputFile(Map<String, XUnitTestClassReport> indexByClassname) {
        Map<InputFile, XUnitTestClassReport> result = new HashMap<>();
        indexByClassname.forEach((className, index) -> {
            InputFile resource = getUnitTestResource(className, index);
            if (resource != null) {
                XUnitTestClassReport report = result.computeIfAbsent(resource, r -> new XUnitTestClassReport());
                // in case of repeated/parameterized tests (JUnit 5.x) we may end up with tests having the same name
                index.getResults().forEach(report::add);
            } else {
                Logger.getLogger().debug(NLS.getFormatted(Messages.ResourceNotFound, className));
            }
        });
        return result;
    }

    private static void save(XUnitTestClassReport report, InputFile inputFile, SensorContext context) {
        int testsCount = report.getTests();
        saveMeasure(context, inputFile, ParasoftMetrics.SOATEST_TESTS, testsCount);
        saveMeasure(context, inputFile, ParasoftMetrics.SOATEST_TEST_FAILURES, report.getFailures());

        double successDensity = 0;
        if (testsCount > 0 && report.getFailures() >= 0) {
            double density = report.getFailures() * 100D / testsCount;
            successDensity = 100D - density;
        }
        saveMeasure(context, inputFile, ParasoftMetrics.SOATEST_TEST_SUCCESS_DENSITY, successDensity);

        saveMeasure(context, inputFile, ParasoftMetrics.SOATEST_TEST_EXECUTION_TIME, report.getDurationMilliseconds());
    }

    @CheckForNull
    private InputFile getUnitTestResource(String className, XUnitTestClassReport xUnitTestClassReport) {
        InputFile resource = findResourceByClassName(className);
        if (resource == null) {
            // fall back on testSuite class name (repeated and parameterized tests from JUnit 5.0 are using test name as classname)
            // Was fixed in JUnit 5.0.3 (see: https://github.com/junit-team/junit5/issues/1182)
            return xUnitTestClassReport.getResults().stream()
                    .map(r -> findResourceByClassName(r.getTestSuiteClassName()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        return resource;
    }

    private InputFile findResourceByClassName(String className) {
        // The test files created in SOAtest always end in lowercase .tst suffix.
        String filePath = StringUtils.substringBeforeLast(className, "_tst").replace('.', '/') + ".tst";
        return fs.inputFile(fs.predicates().hasPath(filePath));
    }

    private static <T extends Serializable> void saveMeasure(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
    }
}
