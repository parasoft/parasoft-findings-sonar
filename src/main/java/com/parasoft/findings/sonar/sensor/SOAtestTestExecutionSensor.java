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
import com.parasoft.findings.sonar.soatest.XUnitSOAtestParser;
import com.parasoft.findings.sonar.soatest.TestExecutionReportConverter;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.io.File;
import java.util.List;

import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_SOATEST_TEST_EXECUTION_IMPORTER;
import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_SOATEST_TEST_EXECUTION_REPORT_PATHS_KEY;

public class SOAtestTestExecutionSensor implements Sensor {

    private final FileSystem fs;

    public SOAtestTestExecutionSensor(FileSystem fs) {
        this.fs = fs;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(PARASOFT_SOATEST_TEST_EXECUTION_IMPORTER)
                .onlyWhenConfiguration((config) -> config.hasKey(PARASOFT_SOATEST_TEST_EXECUTION_REPORT_PATHS_KEY));
    }

    @Override
    public void execute(SensorContext context) {
        List<File> xUnitFiles = convert(context);
        collect(context, xUnitFiles);
        clean(xUnitFiles);
    }

    private List<File> convert(SensorContext context) {
        Logger.getLogger().info(Messages.ConvertingSOAtestTestExecutionReportsToXUnitReports);
        return new TestExecutionReportConverter(fs).convert(context);
    }

    private void collect(SensorContext context, List<File> xUnitFiles) {
        Logger.getLogger().info(Messages.ParsingXUnitReports);
        new XUnitSOAtestParser(fs).collect(context, xUnitFiles);
    }

    private void clean(List<File> xUnitFiles) {
        Logger.getLogger().info(Messages.DeleteIntermediateXUnitFiles);
        xUnitFiles.forEach(file -> {
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        });
    }
}
