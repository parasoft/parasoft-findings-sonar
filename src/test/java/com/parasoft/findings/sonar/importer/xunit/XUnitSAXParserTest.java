package com.parasoft.findings.sonar.importer.xunit;

import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestSuite;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestsContainer;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class XUnitSAXParserTest {

    private static final File BASE_DIR = new File("src/test/resources/jtest");

    @Test
    public void parse() {
        XUnitSAXParser xUnitSAXParser = new XUnitSAXParser();
        try {
            XUnitTestsContainer xUnitTestsContainer = xUnitSAXParser.parse(new File(BASE_DIR,"Jtest-2022.2.0-unitTests-report-converted-to-xunit-report.xml"));

            assertEquals(1, xUnitTestsContainer.getTestSuites().size());
            String testSuiteFilePath = "./src/test/java/parasoft/ForUnitTest.java";
            XUnitTestSuite testSuite = xUnitTestsContainer.getTestSuites().get(testSuiteFilePath);
            assertNotNull(testSuite);
            assertEquals(testSuiteFilePath, testSuite.getFilePath());
            assertEquals(3, testSuite.getTestCases().size());
            assertTrue(testSuite.getTestCases().get(1).getStackTrace().startsWith("java.lang.ArithmeticException: java.lang.ArithmeticException: / by zero"));
            assertTrue(testSuite.getTestCases().get(2).getStackTrace().startsWith("org.opentest4j.AssertionFailedError: org.opentest4j.AssertionFailedError: expected: <1> but was: <2>"));
        } catch (Exception e) {
            fail("Exception occurred while parsing the file: " + e.getMessage());
        }
    }
}