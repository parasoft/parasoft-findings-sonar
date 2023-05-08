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

import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin.Context;

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
        underTest.define(context);

    }
}