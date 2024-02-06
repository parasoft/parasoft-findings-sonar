/*
 * (C) Copyright ParaSoft Corporation 2023. All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.exception.InvalidReportException;
import com.parasoft.findings.sonar.exception.CoverageReportAndProjectNotMatchedException;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.utils.common.nls.NLS;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.scanner.sensor.ProjectSensor;

import java.io.File;
import java.util.List;

public class CoverageSensor implements ProjectSensor {

    private final FileSystem fs;

    private int validCoberturaReportsCount = 0;

    private int processedReportsCount = 0;

    public CoverageSensor(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.onlyOnFileType(InputFile.Type.MAIN)
                        .name(ParasoftConstants.PARASOFT_COVERAGE_IMPORTER)
                        .onlyWhenConfiguration((config) -> config.hasKey(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY));
    }

    @Override
    public void execute(SensorContext sensorContext) {
        String[] reportPaths = sensorContext.config().getStringArray(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY);

        if (reportPaths == null || reportPaths.length == 0) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParasoftReportNotSpecified, Messages.Coverage));
            return;
        }

        Logger.getLogger().info(Messages.ConvertingCoverageReportsToCoberturaReports);

        List<File> coberturaReports = new XSLConverter(fs, XSLConverter.COBERTURA_XSL_NAME_SUFFIX,
                XSLConverter.COBERTURA_TARGET_REPORT_NAME_SUFFIX).transformReports(reportPaths);
        for (File coberturaReport : coberturaReports) {
            Logger.getLogger().info(NLS.getFormatted(Messages.UploadCodeCoverageData, coberturaReport.getName()));
            uploadFileCoverageData(coberturaReport, sensorContext);
        }
        if (validCoberturaReportsCount == 0) {
            throw new InvalidReportException(Messages.NoValidCoberturaReport);
        }
        if (processedReportsCount == 0) {
            throw new CoverageReportAndProjectNotMatchedException(Messages.NotMatchedCoverageReportAndProject);
        }
    }

    public void uploadFileCoverageData(File report, SensorContext context) {

        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(report);

            Element root = document.getRootElement();
            Element packagesElement = root.element("packages");
            List<Element> packageElements;
            if (packagesElement == null || (packageElements = packagesElement.elements("package")) == null || packageElements.isEmpty()) {
                Logger.getLogger().error(NLS.getFormatted(Messages.InvalidCoberturaCoverageReport, report.getAbsolutePath()));
                return;
            }
            validCoberturaReportsCount++;

            for (Element packageElement : packageElements) {
                Element classesElement = packageElement.element("classes");
                List<Element> classElements;
                if (classesElement == null || (classElements = classesElement.elements("class")) == null || classElements.isEmpty()) {
                    continue;
                }
                for (Element classElement : classElements) {
                    String filename = classElement.attributeValue("filename");
                    InputFile file = fs.inputFile(fs.predicates().hasRelativePath(filename));
                    if (file == null) {
                        Logger.getLogger().warn(NLS.getFormatted(Messages.FileNotFoundInProject, filename));
                        continue;
                    }
                    NewCoverage coverage = context.newCoverage().onFile(file);

                    Element linesElement = classElement.element("lines");
                    List<Element> lineElements;
                    if (linesElement == null || (lineElements = linesElement.elements("line")) == null || lineElements.isEmpty()) {
                        coverage.save();
                        continue;
                    }

                    for (Element line : lineElements) {
                        int lineNumber = Integer.parseInt(line.attributeValue("number"));
                        int hits = Integer.parseInt(line.attributeValue("hits"));
                        coverage.lineHits(lineNumber, hits);
                    }
                    coverage.save();
                    processedReportsCount++;
                }
            }
        } catch (Exception e) {
            Logger.getLogger().error(NLS.getFormatted(Messages.FailedToLoadCoberturaReport, report.getAbsolutePath()), e);
        }
    }

}
