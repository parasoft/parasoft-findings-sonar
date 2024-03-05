/*
 * Copyright 2024 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public String getFilePath() {
        return filePath;
    }
}
