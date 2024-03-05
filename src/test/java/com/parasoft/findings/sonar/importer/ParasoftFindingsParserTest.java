/*
 * (C) Copyright Parasoft Corporation 2020.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

/**
 *
 */
package com.parasoft.findings.sonar.importer;

import com.parasoft.findings.sonar.ParasoftProduct;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Parasoft Jtest UTA: Test class for ParasoftIssuesParser
 *
 * @see ParasoftIssuesParser
 * @author bmcglau
 */
public class ParasoftFindingsParserTest {

    @Test
    public void testParasoftFindingsParser1() throws URISyntaxException {
        // Test loadFindings(File file)
        ParasoftIssuesParser parasoftFindingsParser = new ParasoftIssuesParser(new Properties());
        int result = parasoftFindingsParser.loadFindings(new File("src/test/resources/cpptest/Cpptest-std-2023.1.1-static-report.xml"));
        assertEquals(6, result);

        SensorContext context = mock(SensorContext.class);
        ActiveRules activeRulesResult = mock(ActiveRules.class);
        when(context.activeRules()).thenReturn(activeRulesResult);
        InputFile inputFile = mock(InputFile.class);

        // Test createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
        when(inputFile.uri()).thenReturn(new URI("file:///D:/PARASOFT/src/sc3/parasoft-96505-repro/app/main.c"));
        result = parasoftFindingsParser.createNewIssues(inputFile, ParasoftProduct.CPPTEST, context);
        assertEquals(0, result);

        // Test createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
        when(inputFile.uri()).thenReturn(new URI("file:/D:/PARASOFT/src/sc3/parasoft-96505-repro/bootloader/main.c"));
        result = parasoftFindingsParser.createNewIssues(inputFile, ParasoftProduct.CPPTEST, context);
        assertEquals(0, result);
    }

    @Test
    public void testParasoftFindingsParser2() throws URISyntaxException {
        // Test loadFindings(File file)
        ParasoftIssuesParser parasoftFindingsParser = new ParasoftIssuesParser(new Properties());
        int result = parasoftFindingsParser.loadFindings(new File("src/test/resources/cpptest/Cpptest-std-2023.1.1-static-report.xml"));
        assertEquals(6, result);

        SensorContext context = mock(SensorContext.class);
        ActiveRules activeRulesResult = mock(ActiveRules.class);
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        Version version = mock(Version.class);
        when(sonarRuntime.getApiVersion()).thenReturn(version);
        when(context.runtime()).thenReturn(sonarRuntime);
        when(context.activeRules()).thenReturn(activeRulesResult);
        InputFile inputFile = mock(InputFile.class);

        NewIssue newIssueResult = mock(NewIssue.class);
        when(newIssueResult.overrideSeverity(nullable(org.sonar.api.batch.rule.Severity.class))).thenReturn(newIssueResult);
        when(newIssueResult.overrideImpact(nullable(SoftwareQuality.class), nullable(Severity.class))).thenReturn(newIssueResult);
        NewIssueLocation newLocationResult = mock(NewIssueLocation.class);
        when(newLocationResult.message(nullable(String.class))).thenReturn(newLocationResult);
        when(newLocationResult.at(nullable(TextRange.class))).thenReturn(newLocationResult);
        when(newLocationResult.on(nullable(InputComponent.class))).thenReturn(newLocationResult);
        when(newIssueResult.newLocation()).thenReturn(newLocationResult);
        when(newIssueResult.forRule(nullable(RuleKey.class))).thenReturn(newIssueResult);
        when(context.newIssue()).thenReturn(newIssueResult);
        ActiveRule findResult = mock(ActiveRule.class);
        when(activeRulesResult.find(nullable(RuleKey.class))).thenReturn(findResult);
        when(context.activeRules()).thenReturn(activeRulesResult);

        // Test createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
        when(version.isGreaterThanOrEqual(nullable(Version.class))).thenReturn(true);
        when(inputFile.uri()).thenReturn(new URI("file:///D:/PARASOFT/src/sc3/parasoft-96505-repro/app/main.c"));
        result = parasoftFindingsParser.createNewIssues(inputFile, ParasoftProduct.CPPTEST, context);
        assertEquals(3, result);

        // Test createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
        when(version.isGreaterThanOrEqual(nullable(Version.class))).thenReturn(false);
        when(inputFile.uri()).thenReturn(new URI("file:/D:/PARASOFT/src/sc3/parasoft-96505-repro/bootloader/main.c"));
        result = parasoftFindingsParser.createNewIssues(inputFile, ParasoftProduct.CPPTEST, context);
        assertEquals(3, result);
    }
}