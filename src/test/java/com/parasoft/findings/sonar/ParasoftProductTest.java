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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import com.parasoft.findings.utils.results.violations.IRuleViolation;
import com.parasoft.findings.utils.results.violations.ResultLocation;
import org.junit.jupiter.api.Test;

/**
 * Parasoft Jtest UTA: Test class for ParasoftProduct
 *
 * @see ParasoftProduct
 * @author bmcglau
 */
public class ParasoftProductTest {

    /**
     * Parasoft Jtest UTA: Test for getLanguageIndex(IRuleViolation)
     *
     * @see ParasoftProduct#getLanguageIndex(IRuleViolation)
     * @author bmcglau
     */
    @Test
    public void testGetLanguageIndex()
        throws Throwable {
        // Given
        ParasoftProduct underTest = ParasoftProduct.JTEST;

        // When
        IRuleViolation finding = mock(IRuleViolation.class);
        String getLanguageIdResult = "java"; // UTA: provided value
        when(finding.getLanguageId()).thenReturn(getLanguageIdResult);
        ResultLocation getResultLocationResult = mock(ResultLocation.class);
        ITestableInput getTestableInputResult = mock(ITestableInput.class);
        String getNameResult = "getNameResult.java"; // UTA: default value
        when(getTestableInputResult.getName()).thenReturn(getNameResult);
        when(getResultLocationResult.getTestableInput()).thenReturn(getTestableInputResult);
        when(finding.getResultLocation()).thenReturn(getResultLocationResult);
        int result = underTest.getLanguageIndex(finding);
        // Then - assertions for result of method getLanguageIndex(IRuleViolation)
        assertEquals(0, result);
    }

    /**
     * Parasoft Jtest UTA: Test for getLanguageIndex(IRuleViolation)
     *
     * @see ParasoftProduct#getLanguageIndex(IRuleViolation)
     * @author bmcglau
     */
    @Test
    public void testGetLanguageIndex2()
        throws Throwable {
        // Given
        ParasoftProduct underTest = ParasoftProduct.DOTTEST;

        // When
        IRuleViolation finding = mock(IRuleViolation.class);
        String getLanguageIdResult = "dotnet"; // UTA: provided value
        when(finding.getLanguageId()).thenReturn(getLanguageIdResult);

        ResultLocation getResultLocationResult = mock(ResultLocation.class);
        ITestableInput getTestableInputResult = mock(ITestableInput.class);
        String getNameResult = "foo.vb"; // UTA: provided value
        when(getTestableInputResult.getName()).thenReturn(getNameResult);
        when(getResultLocationResult.getTestableInput()).thenReturn(getTestableInputResult);
        when(finding.getResultLocation()).thenReturn(getResultLocationResult);
        int result = underTest.getLanguageIndex(finding);
        // Then - assertions for result of method getLanguageIndex(IRuleViolation)
        assertEquals(1, result);
    }

    /**
     * Parasoft Jtest UTA: Test for getLanguageIndex(IRuleViolation)
     *
     * @see ParasoftProduct#getLanguageIndex(IRuleViolation)
     * @author bmcglau
     */
    @Test
    public void testGetLanguageIndex3()
        throws Throwable {
        // Given
        ParasoftProduct underTest = ParasoftProduct.DOTTEST;

        // When
        IRuleViolation finding = mock(IRuleViolation.class);
        String getLanguageIdResult = "dotnet"; // UTA: provided value
        when(finding.getLanguageId()).thenReturn(getLanguageIdResult);

        ResultLocation getResultLocationResult = mock(ResultLocation.class);
        ITestableInput getTestableInputResult = mock(ITestableInput.class);
        String getNameResult = "foo.cs"; // UTA: provided value
        when(getTestableInputResult.getName()).thenReturn(getNameResult);
        when(getResultLocationResult.getTestableInput()).thenReturn(getTestableInputResult);
        when(finding.getResultLocation()).thenReturn(getResultLocationResult);
        int result = underTest.getLanguageIndex(finding);
        // Then - assertions for result of method getLanguageIndex(IRuleViolation)
        assertEquals(0, result);
    }

    /**
     * Parasoft Jtest UTA: Test for getLanguageIndex(IRuleViolation)
     *
     * @see ParasoftProduct#getLanguageIndex(IRuleViolation)
     * @author bmcglau
     */
    @Test
    public void testGetLanguageIndex4()
        throws Throwable {
        // Given
        ParasoftProduct underTest = ParasoftProduct.JTEST;

        // When
        IRuleViolation finding = mock(IRuleViolation.class);

        ResultLocation getResultLocationResult = mock(ResultLocation.class);
        ITestableInput getTestableInputResult = mock(ITestableInput.class);
        String getNameResult = "foo"; // UTA: provided value
        when(getTestableInputResult.getName()).thenReturn(getNameResult);
        when(getResultLocationResult.getTestableInput()).thenReturn(getTestableInputResult);
        when(finding.getResultLocation()).thenReturn(getResultLocationResult);
        int result = underTest.getLanguageIndex(finding);
        // Then - assertions for result of method getLanguageIndex(IRuleViolation)
        assertEquals(-1, result);
    }

}