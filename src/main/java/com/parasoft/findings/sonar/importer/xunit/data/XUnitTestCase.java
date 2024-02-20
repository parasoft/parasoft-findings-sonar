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
