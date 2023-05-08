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

package com.parasoft.findings.sonar.importer;

import java.io.File;
import java.util.Properties;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.xtest.common.api.progress.EmptyProgressMonitor;
import com.parasoft.xtest.common.services.RawServiceContext;
import com.parasoft.xtest.reports.preferences.FileImportPreferences;
import com.parasoft.xtest.results.api.importer.IImportPreferences;
import com.parasoft.xtest.results.api.importer.IImportedData;
import com.parasoft.xtest.results.api.importer.IViolationImportResult;
import com.parasoft.xtest.results.api.importer.IViolationImporterService;
import com.parasoft.xtest.results.xapi.IMatchingViolationImporter;
import com.parasoft.xtest.services.api.IServicesProvider;
import com.parasoft.xtest.services.api.ServiceUtil;

/**
 * Loads results from report xml file.
 */
public class SonarResultsImporter
{
    private final Properties _properties;

    /**
     * @param properties non-default settings to use to load results.
     */
    public SonarResultsImporter(Properties properties)
    {
        _properties = properties;
    }

    /**
     * Imports results from given xml file.
     * 
     * @param file source xml file
     * @return import result or null if import cannot be performed.
     */
    public IImportedData performImport(File file)
    {
        IViolationImporterService service = ServiceUtil.getService(IViolationImporterService.class, new RawServiceContext(_properties));
        if (service == null) {
            Logger.getLogger().error("Report importer service is null"); //$NON-NLS-1$
            IServicesProvider servicesProvider = ServiceUtil.getServicesProvider();
            if (servicesProvider == null) {
                Logger.getLogger().error("Services provider not registered"); //$NON-NLS-1$
            }
            return null;
        }

        IImportPreferences prefs = new FileImportPreferences(file);
        IViolationImportResult importResult = null;
        if (service instanceof IMatchingViolationImporter) {
            var matcher = new SonarLocationMatcher();
            importResult = ((IMatchingViolationImporter)service).importViolations(prefs, matcher, EmptyProgressMonitor.getInstance());
        } else {
            importResult = service.importViolations(prefs, EmptyProgressMonitor.getInstance());
        }
        return (IImportedData)importResult;
    }

}
