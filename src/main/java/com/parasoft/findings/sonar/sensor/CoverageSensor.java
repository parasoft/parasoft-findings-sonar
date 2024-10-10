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
import com.parasoft.findings.sonar.exception.CoverageSourceMismatchException;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.utils.common.IStringConstants;
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
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.util.List;

public class CoverageSensor implements ProjectSensor {

    private final FileSystem fs;

    private final XSLConverter xslConverter;

    private int validCoberturaReportsCount = 0;

    private int processedReportsCount = 0;

    public CoverageSensor(FileSystem fs, XSLConverter xslConverter) {
        this.fs = fs;
        this.xslConverter = xslConverter;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.onlyOnFileType(InputFile.Type.MAIN)
                        .name(ParasoftConstants.PARASOFT_COVERAGE_IMPORTER)
                        .onlyWhenConfiguration((config) -> config.hasKey(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY));
    }

    @Override
    public void execute(SensorContext context) {
        String[] reportPaths = context.config().getStringArray(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY);

        if (reportPaths == null || reportPaths.length == 0) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParasoftReportNotSpecified, Messages.Coverage));
            return;
        }

        Logger.getLogger().info(Messages.TransformingCoverageReportsToCoberturaReports);

        List<File> coberturaReports = this.xslConverter.transformReports(reportPaths, XSLConverter.ReportType.COVERAGE);
        for (File coberturaReport : coberturaReports) {
            uploadFileCoverageData(coberturaReport, context);
        }
        if (validCoberturaReportsCount == 0) {
            throw new InvalidReportException(Messages.NoValidCoberturaReport);
        }
        if (processedReportsCount == 0) {
            throw new CoverageSourceMismatchException(Messages.NotMatchedCoverageReportAndProject);
        }
    }

    public void uploadFileCoverageData(File report, SensorContext context) {
        Logger.getLogger().info(NLS.getFormatted(Messages.UploadCodeCoverageData, report.getAbsoluteFile()));
        try {
            SAXReader reader = new SAXReader();
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
            reader.setEntityResolver(new EmptyEntityResolver());
            Document document = reader.read(report);

            Element root = document.getRootElement();
            Element packagesElement = root.element("packages"); //$NON-NLS-1$
            List<Element> packageElements;
            if (packagesElement == null || (packageElements = packagesElement.elements("package")) == null || packageElements.isEmpty()) {  //$NON-NLS-1$
                Logger.getLogger().error(NLS.getFormatted(Messages.InvalidCoberturaCoverageReport, report.getAbsolutePath()));
                return;
            }
            validCoberturaReportsCount++;

            for (Element packageElement : packageElements) {
                Element classesElement = packageElement.element("classes"); //$NON-NLS-1$
                List<Element> classElements;
                if (classesElement == null || (classElements = classesElement.elements("class")) == null || classElements.isEmpty()) { //$NON-NLS-1$
                    continue;
                }
                for (Element classElement : classElements) {
                    String filename = classElement.attributeValue("filename"); //$NON-NLS-1$
                    InputFile file = fs.inputFile(fs.predicates().hasRelativePath(filename));
                    if (file == null) {
                        Logger.getLogger().warn(NLS.getFormatted(Messages.FileNotFoundInProject, filename));
                        continue;
                    }
                    NewCoverage coverage = context.newCoverage().onFile(file);

                    Element linesElement = classElement.element("lines"); //$NON-NLS-1$
                    List<Element> lineElements;
                    if (linesElement == null || (lineElements = linesElement.elements("line")) == null || lineElements.isEmpty()) { //$NON-NLS-1$
                        coverage.save();
                        continue;
                    }

                    for (Element line : lineElements) {
                        int lineNumber = Integer.parseInt(line.attributeValue("number")); //$NON-NLS-1$
                        int hits = Integer.parseInt(line.attributeValue("hits")); //$NON-NLS-1$
                        coverage.lineHits(lineNumber, hits);
                    }
                    coverage.save();
                    processedReportsCount++;
                }
            }
            Logger.getLogger().info(NLS.getFormatted(Messages.UploadedCodeCoverageData));
        } catch (Exception e) { // parasoft-suppress OWASP2021.A5.NCE "This is intentionally designed to ensure exceptions during cobertura report loading don't cause the process to fail."
            Logger.getLogger().error(NLS.getFormatted(Messages.FailedToLoadCoberturaReport, report.getAbsolutePath()), e);
        }
    }

    // This would prevent making any calls to resolve URL references to external DTD
    private static class EmptyEntityResolver
            implements EntityResolver {
        @Override
        public InputSource resolveEntity(String publicID, String systemID) {
            return new InputSource(new StringReader(IStringConstants.EMPTY));
        }
    }
}
