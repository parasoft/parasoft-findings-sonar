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

package com.parasoft.findings.sonar.importer;

import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.importer.xunit.data.XUnitTestCase;
import com.parasoft.findings.utils.common.nls.NLS;

public class TestSummary {
    private int totalTests;
    private int failures;
    private int errors;
    private long duration;

    public TestSummary() {
        this(0, 0, 0, 0L);
    }

    public TestSummary(int totalTests, int failures, int errors, long duration) {
        this.totalTests = totalTests;
        this.failures = failures;
        this.errors = errors;
        this.duration = duration;
    }

    public void add(XUnitTestCase testCase) {
        totalTests++;
        if (testCase.getStatus() == XUnitTestCase.Status.FAILURE) {
            failures++;
        } else if (testCase.getStatus() == XUnitTestCase.Status.ERROR) {
            errors++;
        }
        duration += testCase.getTime();
    }

    public int getTotalTests() {
        return totalTests;
    }

    public int getFailures() {
        return failures;
    }

    public int getErrors() {
        return errors;
    }

    public long getDuration() {
        return duration;
    }

    public void mergeFrom(TestSummary testSummaryToMerge) {
        if (testSummaryToMerge == null) {
            return;
        }
        this.totalTests += testSummaryToMerge.totalTests;
        this.failures += testSummaryToMerge.failures;
        this.errors += testSummaryToMerge.errors;
        this.duration += testSummaryToMerge.duration;
    }

    public String toString() {
        return NLS.getFormatted(Messages.UnitTestResults, this.totalTests, this.errors, this.failures, this.duration);
    }
}
