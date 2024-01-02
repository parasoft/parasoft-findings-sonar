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
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.soatest.XUnitSOAtestParser;
import com.parasoft.findings.sonar.soatest.TestExecutionReportConverter;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.io.File;
import java.util.List;

public class SOAtestTestExecutionSensor implements Sensor {

    private final TestExecutionReportConverter testExecutionReportConverter;

    private final XUnitSOAtestParser xUnitSOAtestParser;

    public SOAtestTestExecutionSensor(TestExecutionReportConverter testExecutionReportConverter, XUnitSOAtestParser xUnitSOAtestParser, FileSystem fs) {
        this.testExecutionReportConverter = testExecutionReportConverter;
        this.xUnitSOAtestParser = xUnitSOAtestParser;
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name(ParasoftConstants.PARASOFT_SOATEST_TEST_EXECUTION_IMPORTER);
    }

    @Override
    public void execute(SensorContext context) {
        List<File> xUnitFiles = convert(context);
        collect(context, xUnitFiles);
    }

    private List<File> convert(SensorContext context) {
        Logger.getLogger().info(Messages.ConvertingSOAtestTestExecutionReportsToXUnitReports);
        return testExecutionReportConverter.convert(context);
    }

    private void collect(SensorContext context, List<File> xUnitFiles) {
        Logger.getLogger().info(Messages.ParsingXUnitReports);
        xUnitSOAtestParser.collect(context, xUnitFiles);
    }
}
