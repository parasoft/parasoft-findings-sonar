/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import com.parasoft.findings.utils.results.violations.ViolationRuleUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.rule.Severity;

class ParasoftFindingParserTest
{
    @ParameterizedTest
    @MethodSource("testMapToSonarSeverityParams")
    void testMapToSonarSeverity(int parasoftSeverity, String sonarSeverity)
    {
        assertEquals(sonarSeverity, ParasoftFindingsParser.mapToSonarSeverity(parasoftSeverity).toString());
    }

    private static Stream<Arguments> testMapToSonarSeverityParams()
    {
        return Stream.of(
            Arguments.of(ViolationRuleUtil.SEVERITY_HIGHEST, Severity.BLOCKER),
            Arguments.of(ViolationRuleUtil.SEVERITY_HIGH,    Severity.CRITICAL),
            Arguments.of(ViolationRuleUtil.SEVERITY_MEDIUM,  Severity.MAJOR),
            Arguments.of(ViolationRuleUtil.SEVERITY_LOW,     Severity.MINOR),
            Arguments.of(ViolationRuleUtil.SEVERITY_LOWEST,  Severity.INFO),
            Arguments.of(ViolationRuleUtil.INVALID_SEVERITY, Severity.MINOR)
        );
    }
}
