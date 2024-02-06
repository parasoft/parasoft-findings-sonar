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

package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.sonar.importer.SOAtestTestsParser;
import com.parasoft.findings.utils.common.nls.NLS;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_SOATEST_IMPORTER;
import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_SOATEST_REPORT_PATHS_KEY;

public class SOAtestSensor implements Sensor {

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(PARASOFT_SOATEST_IMPORTER)
                .onlyWhenConfiguration((config) -> config.hasKey(PARASOFT_SOATEST_REPORT_PATHS_KEY));
    }

    @Override
    public void execute(SensorContext context) {
        List<File> xUnitFiles = convert(context);
        collect(context, xUnitFiles);
    }

    private List<File> convert(SensorContext context) {
        String[] reportPaths = context.config().getStringArray(PARASOFT_SOATEST_REPORT_PATHS_KEY);

        if (reportPaths == null || reportPaths.length == 0) {
            Logger.getLogger().info(NLS.getFormatted(Messages.ParasoftReportNotSpecified, Messages.SOAtest));
            return Collections.emptyList();
        }

        Logger.getLogger().info(NLS.getFormatted(Messages.ConvertingParasoftReportsToXUnitReports, Messages.SOAtest));

        return new XSLConverter(context.fileSystem(), XSLConverter.SOA_XUNIT_XSL_NAME_SUFFIX,
                XSLConverter.XUNIT_TARGET_REPORT_NAME_SUFFIX).transformReports(reportPaths);
    }

    private void collect(SensorContext context, List<File> xUnitFiles) {
        new SOAtestTestsParser().collect(context, xUnitFiles);
    }

}
