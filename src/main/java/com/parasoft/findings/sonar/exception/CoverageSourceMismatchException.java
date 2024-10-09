package com.parasoft.findings.sonar.exception;

public class CoverageSourceMismatchException extends RuntimeException {
    private static final long serialVersionUID = 3251466861856453327L;

    public CoverageSourceMismatchException(String message) {
        super(message);
    }
}
