/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.sonar.importer.JtestTestsParser;
import org.dom4j.Element;
import org.sonar.api.batch.sensor.SensorContext;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JtestFindingsSensor extends AbstractParasoftFindingsSensor
{

    public JtestFindingsSensor()
    {
        super(ParasoftProduct.JTEST);
    }

    @Override
    protected void processUnitTestReports(Map<File, Element> reportFiles, SensorContext context) {
        if (isUnitTestReportsEmpty(reportFiles)) {
            return;
        }

        List<File> transformedReports = new XSLConverter(context.fileSystem(), "xunit.xsl", "-xunit_converted-from-xml-report.xml")
                                            .transformReports(reportFiles.keySet());
        new JtestTestsParser().collect(context, transformedReports);
    }
}
