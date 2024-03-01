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
import com.parasoft.findings.utils.common.nls.NLS;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.scanner.ScannerSide;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ScannerSide
public class XSLConverter {

    public static final String XUNIT_XSL_NAME_SUFFIX = "xunit.xsl"; //$NON-NLS-1$
    public static final String SOA_XUNIT_XSL_NAME_SUFFIX = "soatest-xunit.xsl"; //$NON-NLS-1$
    public static final String COBERTURA_XSL_NAME_SUFFIX = "cobertura.xsl"; //$NON-NLS-1$
    public static final String XUNIT_TARGET_REPORT_NAME_SUFFIX = "-xunit_converted-from-xml-report.xml"; //$NON-NLS-1$
    public static final String COBERTURA_TARGET_REPORT_NAME_SUFFIX =  "-cobertura.xml"; //$NON-NLS-1$

    private static final String XSL_RESOURCE_DIR = "/com/parasoft/findings/sonar/res/xsl/"; //$NON-NLS-1$

    private final FileSystem fs;

    public XSLConverter(FileSystem fs) {
        this.fs = fs;
    }

    public List<File> transformReports(String[] reportPaths, ReportType reportType) {
        Set<File> reportFiles = new LinkedHashSet<>();
        for (String reportPath : reportPaths) {
            File reportFile = new File(reportPath);
            if (!reportFile.isAbsolute()) {
                reportFile = new File(fs.baseDir(), reportPath);
            }
            reportFiles.add(reportFile);
        }

        return transformReports(reportFiles, reportType);
    }

    public List<File> transformReports(Set<File> reportFiles, ReportType reportType) {
        List<File> targetReports = new ArrayList<>();

        for (File reportFile : reportFiles) {
            transformReportFile(reportFile.getAbsoluteFile(), targetReports, reportType);
        }
        if (targetReports.isEmpty()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.NoValidReportsFound, getReportTypeMessage(reportType)));
        }
        return targetReports;
    }

    private void transformReportFile(File reportFile, List<File> targetReports, ReportType reportType) {
        if (!reportFile.isFile() || !reportFile.exists() || !reportFile.canRead()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.SkippedInvalidReportFile, getReportTypeMessage(reportType), reportFile.getAbsolutePath()));
        } else {
            Logger.getLogger().info(NLS.getFormatted(Messages.TransformingReport, reportFile.getAbsolutePath()));
            File resultFile = transformReport(reportFile, reportType);
            if (resultFile != null) {
                Logger.getLogger().info(NLS.getFormatted(Messages.TransformedReport, resultFile.getAbsolutePath()));
                targetReports.add(resultFile);
            }
        }
    }

    private File transformReport(File report, ReportType reportType) {
        try {
            File result = new File(getTargetReportFilePath(report, reportType));

            Source xsltFile = new StreamSource(getClass().getResourceAsStream(getXsl(reportType)));
            Processor processor = new Processor(false);
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable stylesheet = compiler.compile(xsltFile);
            Xslt30Transformer transformer = stylesheet.load30();

            Serializer out = processor.newSerializer(result);
            out.setOutputProperty(Serializer.Property.METHOD, "xml"); //$NON-NLS-1$
            out.setOutputProperty(Serializer.Property.INDENT, "yes"); //$NON-NLS-1$

            Map<QName, XdmValue> paramsMap = new LinkedHashMap<>();
            QName paramName = new QName("pipelineBuildWorkingDirectory"); //$NON-NLS-1$
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

    private String getTargetReportFilePath(File reportFile, ReportType reportType) {
        String fileName = reportFile.getName();
        String filePath = reportFile.getAbsolutePath();
        int dotIndex = fileName.lastIndexOf(".");
        String fileNameWithoutExt = dotIndex > -1 ? fileName.substring(0, dotIndex) : fileName;
        return filePath.replace(fileName, fileNameWithoutExt + getTargetReportNameSuffix(reportType));
    }

    private String getTargetReportNameSuffix(ReportType reportType) {
        switch (reportType) {
            case UNIT_TEST:
            case SOATEST:
                return XUNIT_TARGET_REPORT_NAME_SUFFIX;
            case COVERAGE:
                return COBERTURA_TARGET_REPORT_NAME_SUFFIX;
            default:
                throw new UnsupportedOperationException("Unsupported report type"); // should never happen
        }
    }

    private String getXsl(ReportType reportType) {
        switch (reportType) {
            case UNIT_TEST:
                return XSL_RESOURCE_DIR + XUNIT_XSL_NAME_SUFFIX;
            case SOATEST:
                return XSL_RESOURCE_DIR + SOA_XUNIT_XSL_NAME_SUFFIX;
            case COVERAGE:
                return XSL_RESOURCE_DIR + COBERTURA_XSL_NAME_SUFFIX;
            default:
                throw new UnsupportedOperationException("Unsupported report type"); // should never happen
        }
    }

    private String getReportTypeMessage(ReportType reportType) {
        switch (reportType) {
            case UNIT_TEST:
                return Messages.UnitTest;
            case SOATEST:
                return Messages.SOAtest;
            case COVERAGE:
                return Messages.Coverage;
            default:
                throw new UnsupportedOperationException("Unsupported report type"); // should never happen
        }
    }

    public enum ReportType {
        UNIT_TEST, SOATEST, COVERAGE
    }
}
