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
import com.parasoft.findings.utils.common.nls.NLS;
import net.sf.saxon.s9api.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.scanner.sensor.ProjectSensor;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CoverageSensor implements ProjectSensor {

    private final FileSystem fs;

    private int validCoberturaReportsCount = 0;

    private int processedReportsCount = 0;

    public CoverageSensor(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.onlyOnFileType(InputFile.Type.MAIN).name(ParasoftConstants.PARASOFT_COVERAGE_IMPORTER);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        String[] reportPaths = sensorContext.config().getStringArray(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY);
        if (reportPaths != null && reportPaths.length > 0) {
            List<File> coberturaReports = transformToCoberturaReports(reportPaths);
            for (File coberturaReport : coberturaReports) {
                Logger.getLogger().info(NLS.getFormatted(Messages.UploadCodeCoverageData, coberturaReport.getName()));
                uploadFileCoverageData(coberturaReport, sensorContext);
            }
            if (validCoberturaReportsCount == 0) {
                throw new InvalidReportException(Messages.NoValidCoberturaReport);
            }
            if (processedReportsCount == 0) {
                throw new CoverageSourceMismatchException(Messages.NotMatchedCoverageReportAndProject);
            }
        }
    }

    public List<File> transformToCoberturaReports (String[] reportPaths) {
        List<File> coberturaReports = new ArrayList<>();

        for (String reportPath : reportPaths) {
            File reportFile = new File(reportPath);
            if (!reportFile.isAbsolute()) {
                reportFile = new File(fs.baseDir(), reportPath);
            }
            if (!reportFile.isFile() || !reportFile.exists() || !reportFile.canRead()) {
                Logger.getLogger().warn(NLS.getFormatted(Messages.InvalidReportFile, reportFile.getAbsolutePath()));
            } else {
                Logger.getLogger().info(NLS.getFormatted(Messages.ParsingReportFile, reportFile.getName()));
                File resultFile = transformToCoberturaFormat(reportFile);
                if (resultFile != null) {
                    Logger.getLogger().info(NLS.getFormatted(Messages.TransformReportToCoberturaFormat, reportFile.getAbsolutePath(), resultFile.getAbsolutePath()));
                    coberturaReports.add(resultFile);
                }
            }
        }
        if (coberturaReports.size() == 0) {
            throw new InvalidReportException(Messages.NoValidCoverageReportsFound);
        }
        return coberturaReports;
    }

    public File transformToCoberturaFormat(File report) {
        try {
            File result = new File(getCoberturaReportFilePath(report));

            Source xsltFile = new StreamSource(getClass().getResourceAsStream("/com/parasoft/findings/sonar/res/xsl/cobertura.xsl"));
            Processor processor = new Processor(false);
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable stylesheet = compiler.compile(xsltFile);
            Xslt30Transformer transformer = stylesheet.load30();

            Serializer out = processor.newSerializer(result);
            out.setOutputProperty(Serializer.Property.METHOD, "xml");
            out.setOutputProperty(Serializer.Property.INDENT, "yes");

            Map<QName, XdmValue> paramsMap = new LinkedHashMap<>();
            QName paramName = new QName("pipelineBuildWorkingDirectory");
            XdmValue paramValue = new XdmAtomicValue(fs.baseDir().getAbsolutePath());
            paramsMap.put(paramName, paramValue);
            transformer.setStylesheetParameters(paramsMap);

            transformer.transform(new StreamSource(report), out);
            return result;
        } catch (Exception e) {
            Logger.getLogger().error(NLS.getFormatted(Messages.FailedToTransformReport, report.getAbsolutePath()), e);
            return null;
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

    public String getCoberturaReportFilePath(File reportFile) {
        String fileName = reportFile.getName();
        String filePath = reportFile.getAbsolutePath();
        int dotIndex = fileName.lastIndexOf(".");
        String fileNameWithoutExt = dotIndex > -1 ? fileName.substring(0, dotIndex) : fileName;
        return filePath.replace(fileName, fileNameWithoutExt + "-cobertura.xml");
    }
}
