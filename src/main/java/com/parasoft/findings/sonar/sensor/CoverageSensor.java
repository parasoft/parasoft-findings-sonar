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
import com.parasoft.xtest.common.nls.NLS;
import net.sf.saxon.s9api.*;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CoverageSensor implements ProjectSensor {

    private FileSystem fs;

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
        if (reportPaths != null) {
            List<File> coberturaReports = transformToCoberturaReports(reportPaths);
            for (File coberturaReport : coberturaReports) {
                Logger.getLogger().info(NLS.bind(Messages.UploadCodeCoverageData, coberturaReport.getName()));
            }
        }
    }

    public List<File> transformToCoberturaReports (String[] reportPaths) {
        List<File> coberturaReports = new ArrayList<>();

        for (String reportPath : reportPaths) {
            File reportFile = new File(fs.baseDir(), reportPath);
            if (!reportFile.isFile() || !reportFile.exists() || !reportFile.canRead()) {
                Logger.getLogger().warn(NLS.bind(Messages.InvalidReportFile, reportFile.getAbsolutePath()));
                continue;
            } else {
                Logger.getLogger().info(NLS.bind(Messages.ParsingReportFile, reportFile.getName()));
                File resultFile = transformToCoberturaFormat(reportFile);
                if (resultFile != null) {
                    coberturaReports.add(resultFile);
                }
            }
        }
        return coberturaReports;
    }

    public File transformToCoberturaFormat(File report) {
        try {
            File result = new File(report.getAbsolutePath() + "-cobertura.xml");

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
            Logger.getLogger().error(e.getMessage(), e);
            return null;
        }
    }
}
