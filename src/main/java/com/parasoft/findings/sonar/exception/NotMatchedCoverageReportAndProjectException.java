package com.parasoft.findings.sonar.exception;

public class NotMatchedCoverageReportAndProjectException extends RuntimeException {

    public NotMatchedCoverageReportAndProjectException(String message) {
        super(message);
    }
}
