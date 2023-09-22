/*
 * Copyright 2018 Parasoft Corporation
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

import java.io.File;
import java.util.Properties;

import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.scanner.sensor.ProjectSensor;

import com.parasoft.xtest.common.nls.NLS;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.importer.ParasoftFindingsParser;

/**
 * Global sensor that is executed as a part of sonar runtime.
 * In final version it should reads report location, import and create sonar issues
 */
@ScannerSide
public abstract class AbstractParasoftFindingsSensor
    implements ProjectSensor
{
    private final ParasoftProduct _product;

    protected AbstractParasoftFindingsSensor(ParasoftProduct product)
    {
        _product = product;
    }

    @Override
    public void describe(final SensorDescriptor descriptor)
    {
        descriptor.name(NLS.bind(Messages.PluginName, _product.profileName));
        descriptor.onlyOnLanguages(_product.languages.toArray(new String[0]));
    }

    @Override
    public void execute(final SensorContext context)
    {
        var reportLocationPath = context.config().getStringArray(_product.reportPathKey);
        if (reportLocationPath == null) {
            reportLocationPath = new String[0];
        }

        var findingsParser = new ParasoftFindingsParser(new Properties());
        for (var path : reportLocationPath) {
            loadFindings(path, findingsParser, context);
        }
    }

    private void loadFindings(String reportPath, ParasoftFindingsParser findingsParser, SensorContext context)
    {
        var fs = context.fileSystem();
        File reportFile = new File(reportPath);
        if (!reportFile.isAbsolute()) {
            reportFile = new File(fs.baseDir(), reportPath);
        }

        if (!reportFile.exists()) {
            Logger.getLogger().warn(NLS.bind(Messages.NoReportFile, reportFile.getAbsolutePath()));
            return;
        }

        Logger.getLogger().info(NLS.bind(Messages.ParsingReportFile, reportFile));
        var findingsCount = findingsParser.loadFindings(reportFile);
        Logger.getLogger().info(NLS.bind(Messages.FindingsImported, findingsCount));

        var files = fs.inputFiles(fs.predicates().hasLanguages(_product.languages));
        if (!files.iterator().hasNext()) {
            Logger.getLogger().error(Messages.NoSourcesFound);
            return;
        }
        for (var file : files) {
            var count = findingsParser.createNewIssues(file, _product, context);
            if (count > 0) {
                Logger.getLogger().info(NLS.bind(Messages.CreatedIssues, count, file.toString()));
            }
        }
    }
}
