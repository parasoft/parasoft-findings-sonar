/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.rule.Severity;

import com.parasoft.xtest.common.ISeverityConsts;

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
            Arguments.of(ISeverityConsts.SEVERITY_HIGHEST, Severity.BLOCKER),
            Arguments.of(ISeverityConsts.SEVERITY_HIGH,    Severity.CRITICAL),
            Arguments.of(ISeverityConsts.SEVERITY_MEDIUM,  Severity.MAJOR),
            Arguments.of(ISeverityConsts.SEVERITY_LOW,     Severity.MINOR),
            Arguments.of(ISeverityConsts.SEVERITY_LOWEST,  Severity.INFO),
            Arguments.of(ISeverityConsts.INVALID_SEVERITY, Severity.MINOR)
        );
    }
}
