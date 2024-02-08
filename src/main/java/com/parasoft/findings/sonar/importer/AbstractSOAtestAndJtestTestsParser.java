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
import com.parasoft.findings.sonar.importer.xunit.XUnitSAXParser;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestSuite;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestsContainer;
import com.parasoft.findings.utils.common.nls.NLS;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.Metric;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractSOAtestAndJtestTestsParser {

    public void collect(SensorContext context, List<File> xunitFiles) {
        parseFiles(context, xunitFiles);
    }

    private void parseFiles(SensorContext context, List<File> reports) {
        XUnitTestsContainer xUnitTestsContainerOnProject = parseFiles(reports);
        saveMeasuresOnProject(xUnitTestsContainerOnProject, context);
    }

    private XUnitTestsContainer parseFiles(List<File> reports) {

        XUnitSAXParser xUnitSAXParser = new XUnitSAXParser();
        XUnitTestsContainer xUnitTestsContainerOnProject = new XUnitTestsContainer();
        for (File report : reports) {
            try {
                Logger.getLogger().info(NLS.getFormatted(Messages.ParsingXUnitReport, report));
                xUnitTestsContainerOnProject.mergeFrom(xUnitSAXParser.parse(report));
            } catch (ParserConfigurationException | IOException | SAXException e) {
                throw new InvalidReportException(NLS.getFormatted(Messages.FailedToParseXUnitReport, report), e);
            }
        }
        return xUnitTestsContainerOnProject;
    }

    private void saveMeasuresOnProject(XUnitTestsContainer xUnitTestsContainerOnProject, SensorContext context) {
        Map<InputFile, XUnitTestSuite> inputFileAndTestSuitePairs = mapToInputFile(xUnitTestsContainerOnProject.getTestSuites(), context.fileSystem());
        TestSummary testSummaryOfSavedTests = new TestSummary();
        for (Map.Entry<InputFile, XUnitTestSuite> entry : inputFileAndTestSuitePairs.entrySet()) {
            XUnitTestSuite testSuite = entry.getValue();
            if (testSuite.getTestSummary().getTotalTests() > 0) {
                if (saveMeasuresOnFile(testSuite.getTestSummary(), entry.getKey(), context)) {
                    testSummaryOfSavedTests.mergeFrom(testSuite.getTestSummary());
                }
            }
        }
        logTestSummaryForProject(testSummaryOfSavedTests);
    }

    private Map<InputFile, XUnitTestSuite> mapToInputFile(Map<String, XUnitTestSuite> testSuites, FileSystem fs) {
        Map<InputFile, XUnitTestSuite> result = new HashMap<>();
        testSuites.forEach((filePath, testSuite) -> {
            InputFile resource = fs.inputFile(fs.predicates().hasPath(filePath));
            if (resource != null) {
                result.put(resource, testSuite);
            } else {
                Logger.getLogger().debug(NLS.getFormatted(Messages.ResourceNotFound, filePath));
            }
        });
        return result;
    }

    protected abstract boolean saveMeasuresOnFile(TestSummary testSummaryOnFile, InputFile inputFile, SensorContext context);

    protected abstract void logTestSummaryForProject(TestSummary testSummaryForProject);

    protected <T extends Serializable> void saveMeasureOnFile(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
    }
}
