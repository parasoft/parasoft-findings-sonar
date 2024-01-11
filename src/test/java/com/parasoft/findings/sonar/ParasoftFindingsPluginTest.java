/*
 * (C) Copyright Parasoft Corporation 2020.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

/**
 * 
 */
package com.parasoft.findings.sonar;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin.Context;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

/**
 * Parasoft Jtest UTA: Test class for ParasoftFindingsPlugin
 *
 * @see ParasoftFindingsPlugin
 * @author bmcglau
 */
public class ParasoftFindingsPluginTest {

    /**
     * Parasoft Jtest UTA: Test for define(Context)
     *
     * @see ParasoftFindingsPlugin#define(Context)
     * @author bmcglau
     */
    @Test
    public void testDefine()
        throws Throwable {
        // Given
        ParasoftFindingsPlugin underTest = new ParasoftFindingsPlugin();

        // When
        Context context = mock(Context.class);
        SonarRuntime sonarRuntime = mock(SonarRuntime.class);
        Version version = mock(Version.class);
        when(sonarRuntime.getApiVersion()).thenReturn(version);
        when(context.getRuntime()).thenReturn(sonarRuntime);
        underTest.define(context);

    }
}