package com.parasoft.findings.sonar.importer.xunit.data;

public class XUnitTestCase {

    public enum Status {
        OK, ERROR, FAILURE
    }
    private final String name;
    private final String filePath;
    private final String className;
    private final long time;
    private Status status;
    private String stackTrace;

    public XUnitTestCase(String name, String filePath, String className, long time) {
        this.name = name;
        this.filePath = filePath;
        this.className = className;
        this.time = time;
        this.status = Status.OK;
    }

    public String getFilePath() {
        return filePath;
    }

    public Status getStatus() {
        return status;
    }

    public long getTime() {
        return time;
    }

    public void setFailure(Status status) {
        this.status = status;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
