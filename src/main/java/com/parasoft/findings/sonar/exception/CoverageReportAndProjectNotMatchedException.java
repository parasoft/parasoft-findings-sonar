package com.parasoft.findings.sonar.exception;

public class CoverageReportAndProjectNotMatchedException extends RuntimeException {

    public CoverageReportAndProjectNotMatchedException(String message) {
        super(message);
    }
}
