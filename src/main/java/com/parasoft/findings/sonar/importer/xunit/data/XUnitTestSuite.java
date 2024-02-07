package com.parasoft.findings.sonar.importer.xunit.data;

import com.parasoft.findings.sonar.importer.TestSummary;

import java.util.ArrayList;
import java.util.List;

public class XUnitTestSuite {

    private final TestSummary testSummary = new TestSummary();
    private final List<XUnitTestCase> testCases = new ArrayList<>();
    private String filePath;

    public void addTestCase(XUnitTestCase testCase) {
        this.testCases.add(testCase);
        testSummary.add(testCase);
    }

    public void mergeFrom(XUnitTestSuite testSuite) {
        testSuite.getTestCases().forEach(this::addTestCase);
    }

    public TestSummary getTestSummary() {
        return testSummary;
    }

    public List<XUnitTestCase> getTestCases() {
        return testCases;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
