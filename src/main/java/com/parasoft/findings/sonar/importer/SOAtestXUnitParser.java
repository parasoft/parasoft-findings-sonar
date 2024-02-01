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
package com.parasoft.findings.sonar.importer;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.soatest.SOAtestMetrics;
import com.parasoft.findings.sonar.soatest.SOAtestReport;
import com.parasoft.findings.sonar.soatest.SOAtestReportMapper;
import com.parasoft.findings.utils.common.nls.NLS;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.Metric;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SOAtestXUnitParser {

    private final FileSystem fs;

    public SOAtestXUnitParser(FileSystem fs) {
        this.fs = fs;
    }

    public void parse(SensorContext context, List<File> xunitFiles) {
        SOAtestReportMapper reportMapper = new SOAtestReportMapper();
        parseFiles(xunitFiles, reportMapper);
        reportMapper.normalizeReports();
        save(reportMapper, context);
    }

    private static void parseFiles(List<File> reports, SOAtestReportMapper reportMapper) {
        SOAtestSAXParser parser = new SOAtestSAXParser(reportMapper);
        for (File report : reports) {
            try {
                parser.parse(report);
            } catch (DocumentException | XMLStreamException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void save(SOAtestReportMapper index, SensorContext context) {
        long negativeTimeTestNumber = 0;
        Map<InputFile, SOAtestReport> indexByInputFile = mapToInputFile(index.getReportMapper());
        for (Map.Entry<InputFile, SOAtestReport> entry : indexByInputFile.entrySet()) {
            SOAtestReport report = entry.getValue();
            if (report.getTests() > 0) {
                negativeTimeTestNumber += report.getNegativeTimeTestNumber();
                save(report, entry.getKey(), context);
            }
        }
        if (negativeTimeTestNumber > 0) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.TotalDurationNotAccurateWithNegativeTimeTests, negativeTimeTestNumber));
        }
    }

    private Map<InputFile, SOAtestReport> mapToInputFile(Map<String, SOAtestReport> indexByClassname) {
        Map<InputFile, SOAtestReport> result = new HashMap<>();
        indexByClassname.forEach((className, index) -> {
            InputFile resource = getUnitTestResource(className, index);
            if (resource != null) {
                SOAtestReport report = result.computeIfAbsent(resource, r -> new SOAtestReport());
                // in case of repeated/parameterized tests (JUnit 5.x) we may end up with tests having the same name
                index.getResults().forEach(report::addTestCaseDataFrom);
            } else {
                Logger.getLogger().debug(NLS.getFormatted(Messages.ResourceNotFound, className));
            }
        });
        return result;
    }

    private static void save(SOAtestReport report, InputFile inputFile, SensorContext context) {
        int testsCount = report.getTests();
        saveMeasure(context, inputFile, SOAtestMetrics.SOATEST_TESTS, testsCount);
        saveMeasure(context, inputFile, SOAtestMetrics.SOATEST_TEST_FAILURES, report.getFailures());

        double successDensity = 0;
        if (testsCount > 0 && report.getFailures() >= 0) {
            double density = report.getFailures() * 100D / testsCount;
            successDensity = 100D - density;
        }
        saveMeasure(context, inputFile, SOAtestMetrics.SOATEST_TEST_SUCCESS_DENSITY, successDensity);

        saveMeasure(context, inputFile, SOAtestMetrics.SOATEST_TEST_EXECUTION_TIME, report.getDurationMilliseconds());
    }

//    @CheckForNull TODO need work?
    private InputFile getUnitTestResource(String className, SOAtestReport SOAtestReport) {
        InputFile resource = findResourceByClassName(className);
        if (resource == null) {
            // fall back on testSuite class name (repeated and parameterized tests from JUnit 5.0 are using test name as classname)
            // Was fixed in JUnit 5.0.3 (see: https://github.com/junit-team/junit5/issues/1182)
            return SOAtestReport.getResults().stream()
                    .map(r -> findResourceByClassName(r.getTestSuiteName()))
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
