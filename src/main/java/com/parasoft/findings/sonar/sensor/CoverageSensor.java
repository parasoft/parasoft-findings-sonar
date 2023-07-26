/*
 * (C) Copyright ParaSoft Corporation 2023. All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.ParasoftConstants;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.sensor.ProjectSensor;


public class CoverageSensor implements ProjectSensor {

    @Override
    public void describe(SensorDescriptor sensorDescriptor) {
        sensorDescriptor.onlyOnFileType(InputFile.Type.MAIN).name(ParasoftConstants.PARASOFT_COVERAGE_IMPORTER);
    }

    @Override
    public void execute(SensorContext sensorContext) {
        String[] reportPaths = sensorContext.config().getStringArray(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY);
        if (reportPaths != null) {
            for (String reportPath : reportPaths) {
                Logger.getLogger().info("ReportPath = " + reportPath);
            }
        }
    }
}
