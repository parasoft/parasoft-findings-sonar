package com.parasoft.findings.sonar;

public class UnitTestResult {
    private int totalTests;
    private int failures;
    private int errors;
    private long duration;

    public UnitTestResult(int totalTests, int failures, int errors, long duration) {
        this.totalTests = totalTests;
        this.failures = failures;
        this.errors = errors;
        this.duration = duration;
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

    public void mergeFrom(UnitTestResult unitTestResultToMerge) {
        if (unitTestResultToMerge == null) {
            return;
        }
        this.totalTests += unitTestResultToMerge.totalTests;
        this.failures += unitTestResultToMerge.failures;
        this.errors += unitTestResultToMerge.errors;
        this.duration += unitTestResultToMerge.duration;
    }
}
