/*
 * Copyright 2024 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.sonar.api.scanner.ScannerSide;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ScannerSide
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
            InputFile resource = findResourceByFilePath(filePath, fs);
            if (resource != null) {
                result.put(resource, testSuite);
            } else {
                Logger.getLogger().debug(NLS.getFormatted(Messages.ResourceNotFound, filePath));
            }
        });
        return result;
    }

    public InputFile findResourceByFilePath(String filePath, FileSystem fs) {
        return fs.inputFile(fs.predicates().hasPath(filePath));
    }

    protected abstract boolean saveMeasuresOnFile(TestSummary testSummaryOnFile, InputFile inputFile, SensorContext context);

    protected abstract void logTestSummaryForProject(TestSummary testSummaryForProject);

    protected <T extends Serializable> void saveMeasureOnFile(SensorContext context, InputFile inputFile, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(inputFile).withValue(value).save();
    }
}
