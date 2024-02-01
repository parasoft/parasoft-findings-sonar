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

package com.parasoft.findings.sonar.converter;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.exception.InvalidReportException;
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
import org.sonar.api.batch.sensor.SensorContext;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SOAtestReportConverter {

    private final FileSystem fs;


    public SOAtestReportConverter(FileSystem fs) {
        this.fs = fs;
    }

    public List<File> transform(SensorContext context) {
        String[] reportPaths = context.config().getStringArray(
                ParasoftConstants.PARASOFT_SOATEST_REPORT_PATHS_KEY);
        if (reportPaths != null && reportPaths.length > 0) {
            List<File> xunitReports = new ArrayList<>();
            for (String reportPath : reportPaths) {
                File reportFile = new File(reportPath);
                if (!reportFile.isAbsolute()) {
                    reportFile = new File(fs.baseDir(), reportPath);
                }
                if (!reportFile.isFile() || !reportFile.exists() || !reportFile.canRead()) {
                    Logger.getLogger().warn(NLS.getFormatted(Messages.InvalidReportFile, reportFile.getAbsolutePath()));
                } else {
                    Logger.getLogger().info(NLS.getFormatted(Messages.ParsingReportFile, reportFile.getName()));
                    File resultFile = transformToXUnitFormat(reportFile);
                    if (resultFile != null) {
                        Logger.getLogger().info(NLS.getFormatted(Messages.TransformReportToXUnitFormat, reportFile.getAbsolutePath(), resultFile.getAbsolutePath()));
                        xunitReports.add(resultFile);
                    }
                }
            }
            if (xunitReports.isEmpty()) {
                throw new InvalidReportException(Messages.NoValidSOAtestReportsFound);
            }
            return xunitReports;
        }
        return Collections.emptyList();
    }

    private File transformToXUnitFormat(File report) {
        try {
            File result = new File(getXUnitReportFilePath(report));

            Source xsltFile = new StreamSource(getClass().getResourceAsStream("/com/parasoft/findings/sonar/res/xsl/soatest-xunit.xsl"));
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

    private String getXUnitReportFilePath(File reportFile) {
        String fileName = reportFile.getName();
        String filePath = reportFile.getAbsolutePath();
        int dotIndex = fileName.lastIndexOf(".");
        String fileNameWithoutExt = dotIndex > -1 ? fileName.substring(0, dotIndex) : fileName;
        return filePath.replace(fileName, fileNameWithoutExt + "-xunit_converted-from-xml-report.xml");
    }
}
