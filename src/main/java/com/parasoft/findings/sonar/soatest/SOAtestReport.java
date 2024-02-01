/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.parasoft.findings.sonar.soatest;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SOAtestReport {
    private int tests = 0;
    private int failures = 0;
    private long durationMilliseconds = 0L;
    private long negativeTimeTestNumber = 0L;
    private List<SOAtestTestCase> results = null;

    public void addReportDataFrom(SOAtestReport other) {
        for (SOAtestTestCase result : other.getResults()) {
            addTestCaseDataFrom(result);
        }
    }

    public void addTestCaseDataFrom(SOAtestTestCase result) {
        if (results == null) { // parasoft-suppress TRS.ILI-3 "no thread safety problem here since its running single-threaded on the scanner side"
            results = new ArrayList<>();
        }
        boolean hasName = results.stream().map(SOAtestTestCase::getName).anyMatch(result.getName()::equals);
        if (hasName && StringUtils.contains(result.getName(), "$")) {
            return;
        }
        results.add(result);
        tests += 1;
        if (result.isFailure()) {
            failures += 1;
        }
        if (result.getDurationMilliseconds() < 0) {
            negativeTimeTestNumber += 1;
        } else {
            durationMilliseconds += result.getDurationMilliseconds();
        }
    }

    public int getFailures() {
        return failures;
    }

    public int getTests() {
        return tests;
    }

    public long getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public long getNegativeTimeTestNumber() {
        return negativeTimeTestNumber;
    }

    public List<SOAtestTestCase> getResults() {
        if (results == null) {
            return Collections.emptyList();
        }
        return results;
    }
}
