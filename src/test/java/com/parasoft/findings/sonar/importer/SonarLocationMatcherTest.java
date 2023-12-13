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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.parasoft.findings.utils.results.testableinput.IFileTestableInput;
import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import com.parasoft.findings.utils.results.testableinput.PathInput;
import com.parasoft.findings.utils.results.testableinput.ProjectFileTestableInput;
import org.junit.jupiter.api.Test;


/**
 * Parasoft Jtest UTA: Test class for SonarLocationMatcher
 *
 * @see SonarLocationMatcher
 * @author bmcglau
 */
public class SonarLocationMatcherTest {

    /**
     * Parasoft Jtest UTA: Test for getFilePath(IFileTestableInput)
     *
     * @see SonarLocationMatcher#getFilePath(IFileTestableInput)
     * @author bmcglau
     */
    @Test
    public void testGetFilePath()
        throws Throwable {
        // When
        IFileTestableInput input = null; // UTA: provided value
        String result = SonarLocationMatcher.getFilePath(input);
        // Then - assertions for result of method getFilePath(IFileTestableInput)
        assertNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test for getFilePath(IFileTestableInput)
     *
     * @see SonarLocationMatcher#getFilePath(IFileTestableInput)
     * @author bmcglau
     */
    @Test
    public void testGetFilePath2()
        throws Throwable {
        // When
        IFileTestableInput input = mock(IFileTestableInput.class);
        File getFileLocationResult = null; // UTA: provided value
        when(input.getFileLocation()).thenReturn(getFileLocationResult);
        String result = SonarLocationMatcher.getFilePath(input);
        // Then - assertions for result of method getFilePath(IFileTestableInput)
        assertNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test for getFilePath(IFileTestableInput)
     *
     * @see SonarLocationMatcher#getFilePath(IFileTestableInput)
     * @author bmcglau
     */
    @Test
    public void testGetFilePath3()
        throws Throwable {
        // When
        IFileTestableInput input = mock(IFileTestableInput.class);
        File getFileLocationResult = File.createTempFile("getFileLocationResult", null); // UTA: default value
        getFileLocationResult.deleteOnExit();
        when(input.getFileLocation()).thenReturn(getFileLocationResult);
        String result = SonarLocationMatcher.getFilePath(input);
        // Then - assertions for result of method getFilePath(IFileTestableInput)
        assertNotNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test for getFilePath(IFileTestableInput)
     *
     * @see SonarLocationMatcher#getFilePath(IFileTestableInput)
     * @author bmcglau
     */
    @Test
    public void testGetFilePath4()
        throws Throwable {
        // When
        ProjectFileTestableInput input = mock(ProjectFileTestableInput.class);
        File getFileLocationResult = File.createTempFile("getFileLocationResult", null); // UTA: default value
        getFileLocationResult.delete();
        when(input.getFileLocation()).thenReturn(getFileLocationResult);
        String result = SonarLocationMatcher.getFilePath(input);
        // Then - assertions for result of method getFilePath(IFileTestableInput)
        assertEquals("null/null", result);

    }

    /**
     * Parasoft Jtest UTA: Test for matchLocation(Properties, boolean)
     *
     * @see SonarLocationMatcher#matchLocation(Properties, boolean)
     * @author bmcglau
     */
    @Test
    public void testMatchLocation()
        throws Throwable {
        // Given
        SonarLocationMatcher underTest = new SonarLocationMatcher();

        // When
        Properties storedLocation = mock(Properties.class);
        String getPropertyResult = "getPropertyResult"; // UTA: default value
        String getPropertyResult2 = "getPropertyResult2"; // UTA: default value
        String getPropertyResult3 = "getPropertyResult3"; // UTA: default value
        String getPropertyResult4 = "getPropertyResult4"; // UTA: default value
        when(storedLocation.getProperty(nullable(String.class))).thenReturn(getPropertyResult, getPropertyResult2, getPropertyResult3,
            getPropertyResult4);
        boolean bAcceptModified = false; // UTA: default value
        ITestableInput result = underTest.matchLocation(storedLocation, bAcceptModified);
        // Then - assertions for result of method matchLocation(Properties, boolean)
        assertNotNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test for matchLocation(ITestableInput, List, String, String, boolean)
     *
     * @see SonarLocationMatcher#matchLocation(ITestableInput, List, String, String, boolean)
     * @author bmcglau
     */
    @Test
    public void testMatchLocation2()
        throws Throwable {
        // Given
        SonarLocationMatcher underTest = new SonarLocationMatcher();

        // When
        ITestableInput originalInput = null; // UTA: provided value
        List<Long> hashes = new ArrayList<>(); // UTA: default value
        Long item = 0L; // UTA: default value
        hashes.add(item);
        String sRepositoryPath = "sRepositoryPath"; // UTA: default value
        String sBranch = "sBranch"; // UTA: default value
        boolean bAcceptModified = false; // UTA: default value
        ITestableInput result = underTest.matchLocation(originalInput, hashes, sRepositoryPath, sBranch, bAcceptModified);
        // Then - assertions for result of method matchLocation(ITestableInput, List, String, String, boolean)
        assertNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test for matchLocation(ITestableInput, List, String, String, boolean)
     *
     * @see SonarLocationMatcher#matchLocation(ITestableInput, List, String, String, boolean)
     * @author bmcglau
     */
    @Test
    public void testMatchLocation3()
        throws Throwable {
        // Given
        SonarLocationMatcher underTest = new SonarLocationMatcher();

        // When
        PathInput originalInput = mock(PathInput.class);
        List<Long> hashes = new ArrayList<>(); // UTA: default value
        Long item = 0L; // UTA: default value
        hashes.add(item);
        String sRepositoryPath = "sRepositoryPath"; // UTA: default value
        String sBranch = "sBranch"; // UTA: default value
        boolean bAcceptModified = false; // UTA: default value
        ITestableInput result = underTest.matchLocation(originalInput, hashes, sRepositoryPath, sBranch, bAcceptModified);
        // Then - assertions for result of method matchLocation(ITestableInput, List, String, String, boolean)
        assertNotNull(result);

    }

    /**
     * Parasoft Jtest UTA: Test cloned from
     * com.parasoft.findings.reports.sonar.importer.SonarLocationMatcherTest#testMatchLocation4()
     *
     * @see SonarLocationMatcher#matchLocation(ITestableInput, List, String, String, boolean)
     * @author bmcglau
     */
    @Test
    public void testMatchLocation4()
        throws Throwable {
        // Given
        SonarLocationMatcher underTest = new SonarLocationMatcher();

        // When
        PathInput originalInput = mock(PathInput.class);
        when(originalInput.getFileSystemPath()).thenReturn("/some/path");
        when(originalInput.getPath()).thenReturn("/some/path");
        List<Long> hashes = new ArrayList<>(); // UTA: default value
        Long item = 0L; // UTA: default value
        hashes.add(item);
        String sRepositoryPath = "sRepositoryPath"; // UTA: default value
        String sBranch = "sBranch"; // UTA: default value
        boolean bAcceptModified = false; // UTA: default value
        ITestableInput result = underTest.matchLocation(originalInput, hashes, sRepositoryPath, sBranch, bAcceptModified);
        // Then - assertions for result of method matchLocation(ITestableInput, List, String, String, boolean)
        assertNotNull(result);

    }
}