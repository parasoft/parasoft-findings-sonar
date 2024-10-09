package com.parasoft.findings.sonar.exception;

public class InvalidReportException extends RuntimeException {
    private static final long serialVersionUID = 3854929078880443681L;

    public InvalidReportException(String message) {
        super(message);
    }

    public InvalidReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
