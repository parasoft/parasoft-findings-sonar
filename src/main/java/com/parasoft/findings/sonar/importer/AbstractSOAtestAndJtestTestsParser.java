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
import com.parasoft.findings.sonar.exception.InvalidReportException;
import com.parasoft.findings.sonar.importer.xunitdata.XUnitTestClassReport;
import com.parasoft.findings.sonar.importer.xunitdata.XUnitTestIndex;
import com.parasoft.findings.utils.common.nls.NLS;
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

abstract class AbstractSOAtestAndJtestTestsParser {

    public void collect(SensorContext context, List<File> xunitFiles) {
        parseFiles(context, xunitFiles);
    }

    private void parseFiles(SensorContext context, List<File> reports) {
        XUnitTestIndex index = parseFiles(reports);
        saveMeasures(index, context);
    }

    private XUnitTestIndex parseFiles(List<File> reports) {
        XUnitTestIndex index = new XUnitTestIndex();
        StaxParser parser = new StaxParser(index);
        for (File report : reports) {
            try {
                parser.parse(report);
            } catch (XMLStreamException e) {
                throw new InvalidReportException(NLS.getFormatted(Messages.FailedToParseXUnitReport, report), e);
            }
        }
        return index;
    }

    private void saveMeasures(XUnitTestIndex index, SensorContext context) {
        long negativeTimeTestNumber = 0;
        Map<InputFile, XUnitTestClassReport> indexByInputFile = mapToInputFile(index.getIndexByFilePath(), context.fileSystem());
        UnitTestSummary unitTestSummaryForProject = new UnitTestSummary();
        for (Map.Entry<InputFile, XUnitTestClassReport> entry : indexByInputFile.entrySet()) {
            XUnitTestClassReport report = entry.getValue();
            if (report.getTests() > 0) {
                negativeTimeTestNumber += report.getNegativeTimeTestNumber();
                unitTestSummaryForProject.mergeFrom(saveMeasuresOnFile(report, entry.getKey(), context));
            }
        }
        if (negativeTimeTestNumber > 0) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.TotalDurationNotAccurateWithNegativeTimeTests, negativeTimeTestNumber));
        }

        Logger.getLogger().info(Messages.UnitTestSummaryForProject);
        Logger.getLogger().info(unitTestSummaryForProject);
    }

    private Map<InputFile, XUnitTestClassReport> mapToInputFile(Map<String, XUnitTestClassReport> indexByFilePath, FileSystem fs) {
        Map<InputFile, XUnitTestClassReport> result = new HashMap<>();
        indexByFilePath.forEach((filePath, index) -> {
            InputFile resource = fs.inputFile(fs.predicates().hasPath(filePath));
            if (resource != null) {
                XUnitTestClassReport report = result.computeIfAbsent(resource, r -> new XUnitTestClassReport());
                // in case of repeated/parameterized tests (JUnit 5.x) we may end up with tests having the same name
                index.getResults().forEach(report::add);
            } else {
                Logger.getLogger().debug(NLS.getFormatted(Messages.ResourceNotFound, filePath));
            }
        });
        return result;
    }

    protected abstract UnitTestSummary saveMeasuresOnFile(XUnitTestClassReport report, InputFile inputFile, SensorContext context);

    protected <T extends Serializable> void saveMeasureOnFile(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
    }
}
