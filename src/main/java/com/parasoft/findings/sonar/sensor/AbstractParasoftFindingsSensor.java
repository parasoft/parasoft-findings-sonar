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
        UnitTestResult unitTestResult = new UnitTestResult();
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
                            loadFindings(reportFile, findingsParser, context);
                            findingsParser.loadTestResults(rootElement, unitTestResult);
                            break;
                        case XML_STATIC:
                            loadFindings(reportFile, findingsParser, context);
                            break;
                        case XML_TESTS:
                            findingsParser.loadTestResults(rootElement, unitTestResult);
                            break;
                        default:
                            Logger.getLogger().warn(NLS.getFormatted(Messages.InvalidReport, reportFile.getAbsolutePath()));
                    }
                } catch (DocumentException e) {
                    Logger.getLogger().error(NLS.getFormatted(Messages.FailedToLoadReport, reportFile.getAbsolutePath()), e);
                }
            }
        }
        findingsParser.saveMeasures(context, unitTestResult);
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
