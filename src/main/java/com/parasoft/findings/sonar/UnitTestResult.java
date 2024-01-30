package com.parasoft.findings.sonar;

public class UnitTestResult {
    private int totalTests;
    private int failures;
    private int errors;
    private long duration;

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests += totalTests;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures += failures;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors += errors;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration += duration;
    }
}
