package com.parasoft.findings.sonar.exception;

public class CoverageSourceMismatchException extends RuntimeException {

    public CoverageSourceMismatchException(String message) {
        super(message);
    }
}
