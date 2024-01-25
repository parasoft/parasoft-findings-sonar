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
package com.parasoft.findings.sonar.soatest.data;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class XUnitTestClassReport {
  private int errors = 0;
  private int failures = 0;
  private int skipped = 0;
  private int tests = 0;
  private long durationMilliseconds = 0L;


  private long negativeTimeTestNumber = 0L;
  private List<XUnitTestResult> results = null;

  public XUnitTestClassReport add(XUnitTestClassReport other) {
    for (XUnitTestResult otherResult : other.getResults()) {
      add(otherResult);
    }
    return this;
  }

  public XUnitTestClassReport add(XUnitTestResult result) {
    initResults();
    boolean hasName = results.stream().map(XUnitTestResult::getName).anyMatch(result.getName()::equals);
    if (hasName && StringUtils.contains(result.getName(), "$")) {
      return this;
    }
    results.add(result);
    if (result.getStatus().equals(XUnitTestResult.STATUS_SKIPPED)) {
      skipped += 1;

    } else if (result.getStatus().equals(XUnitTestResult.STATUS_FAILURE)) {
      failures += 1;

    } else if (result.getStatus().equals(XUnitTestResult.STATUS_ERROR)) {
      errors += 1;
    }
    tests += 1;
    if (result.getDurationMilliseconds() < 0) {
      negativeTimeTestNumber += 1;
    } else {
      durationMilliseconds += result.getDurationMilliseconds();
    }
    return this;
  }

  private void initResults() {
    if (results == null) { // parasoft-suppress TRS.ILI-3 "no thread safety problem here since its running single-threaded on the scanner side"
      results = new ArrayList<>();
    }
  }

  public int getErrors() {
    return errors;
  }

  public int getFailures() {
    return failures;
  }

  public int getSkipped() {
    return skipped;
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

  public List<XUnitTestResult> getResults() {
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }
}
