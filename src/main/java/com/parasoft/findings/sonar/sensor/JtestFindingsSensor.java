/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.sonar.importer.JtestTestsParser;
import com.parasoft.findings.utils.common.nls.NLS;
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

        Logger.getLogger().info(NLS.getFormatted(Messages.ConvertingParasoftReportsToXUnitReports, Messages.UnitTest));

        List<File> transformedReports = new XSLConverter(context.fileSystem(), XSLConverter.XUNIT_XSL_NAME_SUFFIX, XSLConverter.XUNIT_TARGET_REPORT_NAME_SUFFIX)
                                            .transformReports(reportFiles.keySet());
        new JtestTestsParser().collect(context, transformedReports);
    }
}
