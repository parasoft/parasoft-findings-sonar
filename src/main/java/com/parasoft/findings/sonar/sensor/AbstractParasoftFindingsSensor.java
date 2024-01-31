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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.parasoft.findings.sonar.*;
import com.parasoft.findings.utils.common.nls.NLS;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.scanner.sensor.ProjectSensor;

import com.parasoft.findings.sonar.importer.ParasoftFindingsParser;

/**
 * Global sensor that is executed as a part of sonar runtime.
 * In final version it should reads report location, import and create sonar issues
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
        var reportLocationPath = context.config().getStringArray(_product.reportPathKey);
        if (reportLocationPath == null) {
            reportLocationPath = new String[0];
        }

        var findingsParser = new ParasoftFindingsParser(new Properties());
        List<Element> unitTestReportRootElements = new ArrayList<>();
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
                            unitTestReportRootElements.add(rootElement);
                            break;
                        case XML_STATIC:
                            staticAnalysisReports.add(reportFile);
                            break;
                        case XML_TESTS:
                            unitTestReportRootElements.add(rootElement);
                            break;
                        default:
                            Logger.getLogger().warn(NLS.getFormatted(Messages.InvalidReport, reportFile.getAbsolutePath()));
                    }
                } catch (DocumentException e) {
                    Logger.getLogger().error(NLS.getFormatted(Messages.InvalidReport, reportFile.getAbsolutePath()), e);
                }
            }
        }

        processUnitTestReports(unitTestReportRootElements, findingsParser, context);
        processStaticAnalysisReports(staticAnalysisReports, findingsParser, context);
    }

    private void processUnitTestReports(List<Element> rootElements, ParasoftFindingsParser findingsParser, SensorContext context ) {
        if (rootElements.isEmpty()) {
            // TODO: Logger
            return;
        }
        UnitTestResult unitTestResult = new UnitTestResult(0, 0, 0, 0L);
        for (var root : rootElements) {
            unitTestResult.mergeFrom(findingsParser.loadTestResults(root));
        }
        findingsParser.saveMeasures(context, unitTestResult);
    }

    private void processStaticAnalysisReports(List<File> reportFiles, ParasoftFindingsParser findingsParser, SensorContext context) {
        if (reportFiles.isEmpty()) {
            // TODO: Logger
            return;
        }
        for (var file : reportFiles) {
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
        Logger.getLogger().info(NLS.getFormatted(Messages.ParsingReportFile, reportFile));
        return reportFile;
    }

    private ReportType determineReportType(Element rootElement) {
        Element codingStandardsElement = rootElement.element("CodingStandards");
        Element execElement = rootElement.element("Exec");

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

    private void loadFindings(File reportFile, ParasoftFindingsParser findingsParser, SensorContext context)
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
                Logger.getLogger().info(NLS.getFormatted(Messages.CreatedIssues, count, file.toString()));
            }
        }
    }

    enum ReportType {
        XML_STATIC_AND_TESTS, XML_STATIC, XML_TESTS, UNKNOWN
    }
}
