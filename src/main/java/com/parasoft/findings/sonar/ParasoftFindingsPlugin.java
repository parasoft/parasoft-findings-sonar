package com.parasoft.findings.sonar;

import com.parasoft.findings.sonar.importer.JtestTestsParser;
import com.parasoft.findings.sonar.importer.SOAtestTestsParser;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.sonar.importer.soatest.SOAtestMeasureComputer;
import com.parasoft.findings.sonar.importer.soatest.SOAtestMetrics;
import com.parasoft.findings.sonar.sensor.CoverageSensor;
import com.parasoft.findings.sonar.sensor.CpptestFindingsSensor;
import com.parasoft.findings.sonar.sensor.DottestFindingsSensor;
import com.parasoft.findings.sonar.sensor.JtestFindingsSensor;
import com.parasoft.findings.sonar.sensor.SOAtestSensor;
import com.parasoft.findings.sonar.utils.SonarAPIVersionUtil;
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
    public static boolean isAPIVersionAtLeast10_1 = false;
    @Override
    public void define(Context context)
    {
        isAPIVersionAtLeast10_1 = SonarAPIVersionUtil.isAPIVersionAtLeast10_1(context.getRuntime());
        // register example rules and sensor
        context.addExtensions(
            JtestRulesDefinition.class, DottestRulesDefinition.class, CpptestRulesDefinition.class,
            JtestProfile.class,         DottestProfile.class,         CpptestProfile.class,
            JtestFindingsSensor.class,  DottestFindingsSensor.class,  CpptestFindingsSensor.class,
            CoverageSensor.class, JtestTestsParser.class
        );
        // register custom metric and sensor for SOAtest test execution
        context.addExtensions(SOAtestMetrics.class, SOAtestMeasureComputer.class, XSLConverter.class,
                SOAtestTestsParser.class, SOAtestSensor.class);
        // register properties
        context.addExtensions(ParasoftConfiguration.getProperties());
    }
}
