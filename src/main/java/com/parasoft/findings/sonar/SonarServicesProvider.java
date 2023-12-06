/*
* $Id$
*
* (C) Copyright ParaSoft Corporation 2013. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/

package com.parasoft.findings.sonar;

import java.util.Properties;

import com.parasoft.xtest.common.api.io.IFileInfoService;
import com.parasoft.xtest.common.api.parallel.IParallelRunner;
import com.parasoft.xtest.common.api.variables.IVariablesResolver;
import com.parasoft.xtest.common.application.IApplication;
import com.parasoft.xtest.common.application.OSGiApplication;
import com.parasoft.xtest.common.dtp.DtpAutoconfPreferencesService;
import com.parasoft.xtest.common.dtp.DtpPreferencesFactory;
import com.parasoft.xtest.common.dtp.DtpServiceRegistryFactory;
import com.parasoft.xtest.common.dtp.IDtpPreferences;
import com.parasoft.xtest.common.dtp.IDtpServiceRegistry;
import com.parasoft.xtest.common.io.FileInfoServiceFactory;
import com.parasoft.xtest.common.locations.ILocationAttributes;
import com.parasoft.xtest.common.parallel.ParallelExecutor;
import com.parasoft.xtest.common.preferences.ConfigurationPreferencesFactory;
import com.parasoft.xtest.common.preferences.IConfigurationPreferences;
import com.parasoft.xtest.common.preferences.IPreferences;
import com.parasoft.xtest.common.preferences.PreferencesServiceUtil;
import com.parasoft.xtest.common.services.DefaultServicesProvider;
import com.parasoft.xtest.configuration.api.IPreferencesService;
import com.parasoft.xtest.configuration.api.rules.IRuleDescriptionUpdateService;
import com.parasoft.xtest.configuration.api.rules.IRulesService;
import com.parasoft.xtest.configuration.rules.RuleDescriptionUpdateService;
import com.parasoft.xtest.configuration.rules.RulesServiceFactory;
import com.parasoft.xtest.logging.api.ParasoftLogger;
import com.parasoft.xtest.reports.internal.importers.ViolationImporterServiceFactory;
import com.parasoft.xtest.results.api.IResultPostProcessorService;
import com.parasoft.xtest.results.api.importer.IViolationImporterService;
import com.parasoft.xtest.results.factory.IResultFactory;
import com.parasoft.xtest.results.internal.ResultsInitManager;
import com.parasoft.xtest.results.internal.factory.DefaultCodingStandardsResultFactory;
import com.parasoft.xtest.results.internal.factory.DefaultScopeResultFactory;
import com.parasoft.xtest.results.internal.factory.DefaultSetupProblemsResultFactory;
import com.parasoft.xtest.results.internal.factory.DupcodeViolationStorage;
import com.parasoft.xtest.results.internal.factory.FlowAnalysisResultStorage;
import com.parasoft.xtest.results.internal.factory.MetricsViolationStorage;
import com.parasoft.xtest.results.locations.ResultLocationProcessor;
import com.parasoft.xtest.results.rules.RulesProcessor;
import com.parasoft.xtest.results.sourcecontrol.SourceControlProcessor;
import com.parasoft.xtest.results.suppressions.SuppressionsProcessor;
import com.parasoft.xtest.results.xapi.IResultsInitManager;
import com.parasoft.xtest.results.xapi.xml.IViolationXmlStorage;
import com.parasoft.xtest.results.xml.DefaultCodingStandardsViolationStorage;
import com.parasoft.xtest.services.api.ServiceUtil;
import com.parasoft.xtest.services.api.diagnostics.ServiceDiagnosticCollector;

public final class SonarServicesProvider
    extends DefaultServicesProvider
{
    private static SonarServicesProvider INSTANCE;

    private SonarServicesProvider()
    {}

    public static synchronized SonarServicesProvider getInstance()
    {
        if (INSTANCE == null) {
            INSTANCE = new SonarServicesProvider();
            ServiceUtil.setServicesProvider(INSTANCE);
            INSTANCE.init();
        }
        return INSTANCE;
    }

    private void init()
    {
        System.setProperty(ServiceDiagnosticCollector.DIAGNOSTICS_OFF_PROPERTY, Boolean.TRUE.toString());

        ParasoftLogger.setCurrentFactory(new SonarLoggerHandlerFactory());

        Properties properties;
        registerService(IViolationImporterService.Factory.class, new ViolationImporterServiceFactory());
        registerService(IApplication.class, new OSGiApplication());
        registerService(IParallelRunner.class, new ParallelExecutor(null));
        registerService(IResultFactory.class, new DefaultCodingStandardsResultFactory());
        registerService(IViolationXmlStorage.class, new DefaultCodingStandardsViolationStorage());
        registerService(IViolationXmlStorage.class, new FlowAnalysisResultStorage());
        registerService(IViolationXmlStorage.class, new DupcodeViolationStorage());
        registerService(IViolationXmlStorage.class, new MetricsViolationStorage());
        registerService(IResultPostProcessorService.class, new SourceControlProcessor());
        registerService(IResultPostProcessorService.class, new ResultLocationProcessor());
        properties = new Properties();
        properties.setProperty(IResultPostProcessorService.POST_PROCESSOR_ID_PROPERTY, ILocationAttributes.POST_PROCESSOR_ID);
        registerService(IResultPostProcessorService.class, new ResultLocationProcessor(), properties);
        registerService(IResultFactory.class, new DefaultSetupProblemsResultFactory());
        registerService(IResultFactory.class, new DefaultScopeResultFactory());
        registerService(IResultPostProcessorService.class, new SuppressionsProcessor());
        registerService(IResultPostProcessorService.class, new RulesProcessor());
        registerService(IRuleDescriptionUpdateService.class, new RuleDescriptionUpdateService());
        registerService(IDtpServiceRegistry.Factory.class, new DtpServiceRegistryFactory());
        registerService(IResultsInitManager.class, new ResultsInitManager());
        registerService(IPreferencesService.class, new DtpAutoconfPreferencesService());
        registerService(IFileInfoService.Factory.class, new FileInfoServiceFactory());
        properties = new Properties();
        properties.setProperty(PreferencesServiceUtil.PREFERENCES_ID_PROPERTY, IDtpPreferences.PREFERENCES_ID);
        registerService(IPreferences.Factory.class, new DtpPreferencesFactory(), properties);
        properties = new Properties();
        properties.setProperty(PreferencesServiceUtil.PREFERENCES_ID_PROPERTY, IConfigurationPreferences.PREFERENCES_ID);
        registerService(IPreferences.Factory.class, new ConfigurationPreferencesFactory(), properties);
        registerService(IRulesService.Factory.class, new RulesServiceFactory());
    }
}
