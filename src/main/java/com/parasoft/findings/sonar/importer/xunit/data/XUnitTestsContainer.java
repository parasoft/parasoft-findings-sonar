package com.parasoft.findings.sonar.importer.xunit.data;

import com.parasoft.findings.sonar.importer.TestSummary;

import java.util.HashMap;
import java.util.Map;

public class XUnitTestsContainer {
    private final TestSummary testSummary = new TestSummary();
    private final Map<String, XUnitTestSuite> testSuites = new HashMap<>();

    public void addTestCase(XUnitTestCase testCase) {
        String filePath = testCase.getFilePath();
        XUnitTestSuite existingTestSuite = testSuites.get(filePath);
        if (existingTestSuite == null) {
            XUnitTestSuite testSuite = new XUnitTestSuite();
            testSuite.setFilePath(filePath);
            testSuite.addTestCase(testCase);
            testSuites.put(filePath, testSuite);
            testSummary.mergeFrom(testSuite.getTestSummary());
        } else {
            existingTestSuite.addTestCase(testCase);
            testSummary.add(testCase);
        }
    }

    public void mergeFrom(XUnitTestsContainer containerToMerge) {
        for (Map.Entry<String, XUnitTestSuite> entry : containerToMerge.testSuites.entrySet()) {
            String filePath = entry.getKey();
            XUnitTestSuite testSuite = entry.getValue();
            XUnitTestSuite existingTestSuite = testSuites.get(filePath);
            if (existingTestSuite == null) {
                testSuites.put(filePath, testSuite);
            } else {
                existingTestSuite.mergeFrom(testSuite);
            }
            testSummary.mergeFrom(testSuite.getTestSummary());
        }
    }

    public Map<String, XUnitTestSuite> getTestSuites() {
        return testSuites;
    }

    public TestSummary getTestSummary() {
        return testSummary;
    }
}
