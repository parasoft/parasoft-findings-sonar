/*
 * Copyright 2018 Parasoft Corporation
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

package com.parasoft.findings.sonar.sensor;

import java.io.File;
import java.util.*;

import com.parasoft.findings.sonar.*;
import com.parasoft.findings.sonar.importer.ParasoftDottestAndCpptestTestsParser;
import com.parasoft.findings.sonar.importer.TestSummary;
import com.parasoft.findings.utils.common.nls.NLS;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.scanner.sensor.ProjectSensor;

import com.parasoft.findings.sonar.importer.ParasoftIssuesParser;

/**
 * Global sensor that is executed as a part of sonar runtime.
 * In final version it should read report location, import and create sonar issues
 */
@ScannerSide
public abstract class AbstractParasoftFindingsSensor
    implements ProjectSensor
{
    private final ParasoftProduct _product;
    protected AbstractParasoftFindingsSensor(ParasoftProduct product)
    {
        _product = product;
    }

    @Override
    public void describe(final SensorDescriptor descriptor)
    {
        descriptor.name(NLS.getFormatted(Messages.PluginName, _product.profileName));
        descriptor.onlyOnLanguages(_product.languages.toArray(new String[0]));
    }

    @Override
    public void execute(final SensorContext context)
    {
        String[] reportLocationPath = context.config().getStringArray(_product.reportPathKey);
        if (reportLocationPath == null) {
            reportLocationPath = new String[0];
        }

        ParasoftIssuesParser findingsParser = new ParasoftIssuesParser(new Properties());
        Map<File, Element> unitTestReports = new HashMap<>();
        List<File> staticAnalysisReports = new ArrayList<>();
        for (var path : reportLocationPath) {
            File reportFile = getFile(path, context);
            if (reportFile != null) {
                try {
                    SAXReader reader = new SAXReader();
                    Document document = reader.read(reportFile);
                    Element rootElement = document.getRootElement();
                    ReportType reportType = determineReportType(rootElement);
                    switch (reportType) {
                        case XML_STATIC_AND_TESTS:
                            staticAnalysisReports.add(reportFile);
                            unitTestReports.put(reportFile, rootElement);
                            break;
                        case XML_STATIC:
                            staticAnalysisReports.add(reportFile);
                            break;
                        case XML_TESTS:
                            unitTestReports.put(reportFile, rootElement);
                            break;
                        default:
                            Logger.getLogger().warn(NLS.getFormatted(Messages.SkippedInvalidReport, reportFile.getAbsolutePath()));
                    }
                } catch (DocumentException e) {
                    Logger.getLogger().error(NLS.getFormatted(Messages.SkippedInvalidReport, reportFile.getAbsolutePath()), e);
                }
            }
        }

        processUnitTestReports(unitTestReports, context);
        processStaticAnalysisReports(staticAnalysisReports, findingsParser, context);
    }

    /**
     * The default implementation of this method processes the unit test reports of dottest and cpptest.
     * For jtest reports it is overridden in the {@link JtestFindingsSensor} class.
     */
    protected void processUnitTestReports(Map<File, Element> reportFiles, SensorContext context ) {
        if (isUnitTestReportsEmpty(reportFiles)) {
            return;
        }
        ParasoftDottestAndCpptestTestsParser testsParser = new ParasoftDottestAndCpptestTestsParser();
        TestSummary unitTestSummaryForProject = new TestSummary();
        for (var file : reportFiles.keySet()) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParsingReportFile, Messages.UnitTest, file));

            TestSummary unitTestSummaryForReport = testsParser.loadTestResults(reportFiles.get(file));

            Logger.getLogger().info(unitTestSummaryForReport.toString());

            unitTestSummaryForProject.mergeFrom(unitTestSummaryForReport);
        }
        testsParser.saveMeasuresOnProject(context, unitTestSummaryForProject);
    }

    protected boolean isUnitTestReportsEmpty(Map<File, Element> reportFiles) {
        if (reportFiles.isEmpty()) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParasoftReportNotSpecified, Messages.UnitTest));
            return true;
        }
        return false;
    }

    private void processStaticAnalysisReports(List<File> reportFiles, ParasoftIssuesParser findingsParser, SensorContext context) {
        if (reportFiles.isEmpty()) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParasoftReportNotSpecified, Messages.StaticAnalysis));
            return;
        }
        for (var file : reportFiles) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParsingReportFile, Messages.StaticAnalysis, file));
            loadFindings(file, findingsParser, context);
        }
    }

    private File getFile(String reportPath, SensorContext context) {
        var fs = context.fileSystem();
        File reportFile = new File(reportPath);
        if (!reportFile.isAbsolute()) {
            reportFile = new File(fs.baseDir(), reportPath);
        }

        if (!reportFile.exists()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.NoReportFile, reportFile.getAbsolutePath()));
            return null;
        }
        return reportFile;
    }

    private ReportType determineReportType(Element rootElement) {
        Element codingStandardsElement = rootElement.element("CodingStandards"); //$NON-NLS-1$
        Element execElement = rootElement.element("Exec"); //$NON-NLS-1$

        if (codingStandardsElement != null && execElement != null ) {
            return ReportType.XML_STATIC_AND_TESTS;
        } else if (codingStandardsElement != null) { // The CodingStandards node exists
            return ReportType.XML_STATIC;
        } else if (execElement != null) { // The Exec node exists
            return ReportType.XML_TESTS;
        } else {
            return ReportType.UNKNOWN;
        }
    }

    private void loadFindings(File reportFile, ParasoftIssuesParser findingsParser, SensorContext context)
    {
        var fs = context.fileSystem();
        var findingsCount = findingsParser.loadFindings(reportFile);
        Logger.getLogger().info(NLS.getFormatted(Messages.FindingsImported, findingsCount));

        var files = fs.inputFiles(fs.predicates().hasLanguages(_product.languages));
        if (!files.iterator().hasNext()) {
            Logger.getLogger().error(Messages.NoSourcesFound);
            return;
        }
        for (var file : files) {
            var count = findingsParser.createNewIssues(file, _product, context);
            if (count > 0) {
                Logger.getLogger().info(NLS.getFormatted(Messages.CreatedIssues, count, new File(file.uri().getPath()).getAbsoluteFile().toString()));
            }
        }
    }

    enum ReportType {
        XML_STATIC_AND_TESTS, XML_STATIC, XML_TESTS, UNKNOWN
    }
}
