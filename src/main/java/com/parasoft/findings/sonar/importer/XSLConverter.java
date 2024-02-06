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

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XSLConverter {

    public static final String XUNIT_XSL_NAME_SUFFIX = "xunit.xsl";
    public static final String SOA_XUNIT_XSL_NAME_SUFFIX = "soatest-xunit.xsl";
    public static final String COBERTURA_XSL_NAME_SUFFIX = "cobertura.xsl";
    public static final String XUNIT_TARGET_REPORT_NAME_SUFFIX = "-xunit_converted-from-xml-report.xml";
    public static final String COBERTURA_TARGET_REPORT_NAME_SUFFIX =  "-cobertura.xml";

    private static final String XSL_RESOURCE_DIR = "/com/parasoft/findings/sonar/res/xsl/";

    private final FileSystem fs;

    private final String xsl;

    private final String targetReportNameSuffix;

    public XSLConverter(FileSystem fs, String xslName, String targetReportNameSuffix) {
        this.fs = fs;
        this.xsl = XSL_RESOURCE_DIR + xslName;
        this.targetReportNameSuffix = targetReportNameSuffix;
    }

    public List<File> transformReports(String[] reportPaths) {
        List<File> targetReports = new ArrayList<>();

        for (String reportPath : reportPaths) {
            File reportFile = new File(reportPath);
            if (!reportFile.isAbsolute()) {
                reportFile = new File(fs.baseDir(), reportPath);
            }
            transformReportFile(reportFile, targetReports);
        }
        if (targetReports.isEmpty()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.NoValidReportsFound, getReportType()));
        }
        return targetReports;
    }

    public List<File> transformReports(Set<File> reportFiles) {
        List<File> targetReports = new ArrayList<>();

        for (File reportFile : reportFiles) {
            transformReportFile(reportFile.getAbsoluteFile(), targetReports);
        }
        if (targetReports.isEmpty()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.NoValidReportsFound, getReportType()));
        }
        return targetReports;
    }

    private void transformReportFile(File reportFile, List<File> targetReports) {
        if (!reportFile.isFile() || !reportFile.exists() || !reportFile.canRead()) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.SkippedInvalidReportFile, getReportType(), reportFile.getAbsolutePath()));
        } else {
            Logger.getLogger().info(NLS.getFormatted(Messages.ConvertingReport, reportFile.getAbsolutePath()));
            File resultFile = transformReport(reportFile);
            if (resultFile != null) {
                Logger.getLogger().info(NLS.getFormatted(Messages.TransformedReport, reportFile.getAbsolutePath(), resultFile.getAbsolutePath()));
                targetReports.add(resultFile);
            }
        }
    }

    private File transformReport(File report) {
        try {
            File result = new File(getTargetReportFilePath(report));

            Source xsltFile = new StreamSource(getClass().getResourceAsStream(xsl));
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

    private String getTargetReportFilePath(File reportFile) {
        String fileName = reportFile.getName();
        String filePath = reportFile.getAbsolutePath();
        int dotIndex = fileName.lastIndexOf(".");
        String fileNameWithoutExt = dotIndex > -1 ? fileName.substring(0, dotIndex) : fileName;
        return filePath.replace(fileName, fileNameWithoutExt + targetReportNameSuffix);
    }

    private String getReportType() {
        if (xsl.endsWith(XUNIT_XSL_NAME_SUFFIX)) {
            return Messages.UnitTest;
        } else if (xsl.endsWith(SOA_XUNIT_XSL_NAME_SUFFIX)) {
            return Messages.SOAtest;
        } else if (xsl.endsWith(COBERTURA_XSL_NAME_SUFFIX)) {
            return Messages.Coverage;
        } else {
            throw new UnsupportedOperationException("Unsupported report type"); // should never happen
        }
    }
}
