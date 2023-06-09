package com.parasoft.findings.sonar;

import com.parasoft.findings.sonar.sensor.CpptestFindingsSensor;
import com.parasoft.findings.sonar.sensor.DottestFindingsSensor;
import com.parasoft.findings.sonar.sensor.JtestFindingsSensor;
import org.sonar.api.Plugin;

import com.parasoft.findings.sonar.rules.CpptestProfile;
import com.parasoft.findings.sonar.rules.CpptestRulesDefinition;
import com.parasoft.findings.sonar.rules.DottestProfile;
import com.parasoft.findings.sonar.rules.DottestRulesDefinition;
import com.parasoft.findings.sonar.rules.JtestProfile;
import com.parasoft.findings.sonar.rules.JtestRulesDefinition;

/**
 * Parasoft Plugin for Sonar. Registers Parasoft extensions for Sonar runtime and web application.
 */
public class ParasoftFindingsPlugin
    implements Plugin
{
    @Override
    public void define(Context context)
    {
        System.setProperty("com.parasoft.xtest.stats.disable", "true"); //$NON-NLS-1$ //$NON-NLS-2$

        // register example rules and sensor
        context.addExtensions(
            JtestRulesDefinition.class, DottestRulesDefinition.class, CpptestRulesDefinition.class,
            JtestProfile.class,         DottestProfile.class,         CpptestProfile.class,
            JtestFindingsSensor.class,  DottestFindingsSensor.class,  CpptestFindingsSensor.class
        );
        // register properties
        context.addExtensions(ParasoftConfiguration.getProperties());
    }
}
