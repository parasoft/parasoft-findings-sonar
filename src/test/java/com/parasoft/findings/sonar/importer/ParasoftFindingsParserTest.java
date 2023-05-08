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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.parasoft.findings.sonar.ParasoftProduct;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import com.parasoft.xtest.common.api.ISourceRange;
import com.parasoft.xtest.common.api.ITestableInput;
import com.parasoft.xtest.results.api.IResultLocation;
import com.parasoft.xtest.results.api.IRuleViolation;

/**
 * Parasoft Jtest UTA: Test class for ParasoftFindingsParser
 *
 * @see ParasoftFindingsParser
 * @author bmcglau
 */
public class ParasoftFindingsParserTest {

    /**
     * Parasoft Jtest UTA: Test for createNewIssues(InputFile, ParasoftProduct, SensorContext)
     *
     * @see ParasoftFindingsParser#createNewIssues(InputFile, ParasoftProduct, SensorContext)
     * @author bmcglau
     */
    @Test
    public void testCreateNewIssues()
        throws Throwable {
        // Given
        Properties properties = new Properties(); // UTA: default value
        ParasoftFindingsParser underTest = new ParasoftFindingsParser(properties);

        // When
        InputFile javaFile = mock(InputFile.class);
        ParasoftProduct product = ParasoftProduct.JTEST; // UTA: default value
        SensorContext context = mock(SensorContext.class);
        int result = underTest.createNewIssues(javaFile, product, context);
        // Then - assertions for result of method createNewIssues(InputFile, ParasoftProduct, SensorContext)
        assertEquals(0, result);

    }

    /**
     * Parasoft Jtest UTA: Test for createNewIssues(InputFile, ParasoftProduct, SensorContext)
     *
     * @see ParasoftFindingsParser#createNewIssues(InputFile, ParasoftProduct, SensorContext)
     * @author bmcglau
     */
    @Test
    public void testCreateNewIssues2()
        throws Throwable {
        // Given
        Properties properties = new Properties(); // UTA: default value
        Map<String, Set<IRuleViolation>> violations = new HashMap<>();
        Set<IRuleViolation> ruleViolations = new HashSet<>();
        violations.put("report.xml", ruleViolations);
        ruleViolations.add(mockRuleViolation());
        ParasoftFindingsParser underTest = new UnderTest(properties, violations);

        // When
        InputFile javaFile = mock(InputFile.class);
        when(javaFile.filename()).thenReturn("report.xml");
        ParasoftProduct product = ParasoftProduct.JTEST; // UTA: default value
        SensorContext context = mock(SensorContext.class);
        ActiveRules activeRulesResult = mock(ActiveRules.class);
        when(context.activeRules()).thenReturn(activeRulesResult);
        int result = underTest.createNewIssues(javaFile, product, context);
        // Then - assertions for result of method createNewIssues(InputFile, ParasoftProduct, SensorContext)
        assertEquals(0, result);

    }

    private IRuleViolation mockRuleViolation() {
        IRuleViolation violation = mock(IRuleViolation.class);
        String getRuleIdResult = "getRuleIdResult"; // UTA: default value
        when(violation.getRuleId()).thenReturn(getRuleIdResult);
        IResultLocation getResultLocationResult = mock(IResultLocation.class);
        when(violation.getResultLocation()).thenReturn(getResultLocationResult);
        ITestableInput input = mock(ITestableInput.class);
        when(getResultLocationResult.getTestableInput()).thenReturn(input);
        when(input.getName()).thenReturn("foo.java");
        return violation;
    }

    private class UnderTest
        extends ParasoftFindingsParser {
        private final Map<String, Set<IRuleViolation>> violations;

        public UnderTest(Properties properties, Map<String, Set<IRuleViolation>> violations) {
            super(properties);
            this.violations = violations;
        }

        @Override
        public Set<IRuleViolation> getFindings(String name) {
            return violations.get(name);
        }
    }

    /**
     * Parasoft Jtest UTA: Test cloned from
     * com.parasoft.findings.reports.sonar.importer.ParasoftFindingsParserTest#testCreateNewIssues2()
     *
     * @see ParasoftFindingsParser#createNewIssues(InputFile, ParasoftProduct, SensorContext)
     * @author bmcglau
     */
    @Test
    public void testCreateNewIssues3()
        throws Throwable {
        // Given
        Properties properties = new Properties(); // UTA: default value
        Map<String, Set<IRuleViolation>> violations = new HashMap<>();
        Set<IRuleViolation> ruleViolations = new HashSet<>();
        violations.put("report.xml", ruleViolations);
        ruleViolations.add(mockRuleViolation2());
        ParasoftFindingsParser underTest = new UnderTest(properties, violations);

        // When
        InputFile javaFile = mock(InputFile.class);
        TextRange selectLineResult = mock(TextRange.class);
        when(javaFile.selectLine(anyInt())).thenReturn(selectLineResult);
        when(javaFile.filename()).thenReturn("report.xml");
        ParasoftProduct product = ParasoftProduct.JTEST; // UTA: default value
        SensorContext context = mock(SensorContext.class);
        NewIssue newIssueResult = mock(NewIssue.class);
        when(newIssueResult.overrideSeverity(nullable(Severity.class))).thenReturn(newIssueResult);
        NewIssueLocation newLocationResult = mock(NewIssueLocation.class);
        when(newLocationResult.message(nullable(String.class))).thenReturn(newLocationResult);
        when(newLocationResult.at(nullable(TextRange.class))).thenReturn(newLocationResult);
        when(newLocationResult.on(nullable(InputComponent.class))).thenReturn(newLocationResult);
        when(newIssueResult.newLocation()).thenReturn(newLocationResult);
        when(newIssueResult.forRule(nullable(RuleKey.class))).thenReturn(newIssueResult);
        when(context.newIssue()).thenReturn(newIssueResult);
        ActiveRules activeRulesResult = mock(ActiveRules.class);
        ActiveRule findResult = mock(ActiveRule.class);
        when(activeRulesResult.find(nullable(RuleKey.class))).thenReturn(findResult);
        when(context.activeRules()).thenReturn(activeRulesResult);
        int result = underTest.createNewIssues(javaFile, product, context);
        // Then - assertions for result of method createNewIssues(InputFile, ParasoftProduct, SensorContext)
        assertEquals(1, result);

    }

    private IRuleViolation mockRuleViolation2() {
        IRuleViolation violation = mock(IRuleViolation.class);
        String getRuleIdResult = "getRuleIdResult"; // UTA: default value
        when(violation.getRuleId()).thenReturn(getRuleIdResult);
        IResultLocation getResultLocationResult = mock(IResultLocation.class);
        ISourceRange getSourceRangeResult = mock(ISourceRange.class);
        int getStartLineResult = 2; // UTA: default value
        when(getSourceRangeResult.getStartLine()).thenReturn(getStartLineResult);
        when(getResultLocationResult.getSourceRange()).thenReturn(getSourceRangeResult);
        when(violation.getResultLocation()).thenReturn(getResultLocationResult);
        ITestableInput input = mock(ITestableInput.class);
        when(getResultLocationResult.getTestableInput()).thenReturn(input);
        when(input.getName()).thenReturn("foo.java");
        return violation;
    }

}