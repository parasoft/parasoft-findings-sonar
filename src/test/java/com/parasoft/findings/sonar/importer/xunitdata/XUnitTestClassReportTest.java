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
package com.parasoft.findings.sonar.importer.xunitdata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XUnitTestClassReportTest {

  @Test
  public void shouldIncrementCounters() {
    XUnitTestClassReport report = new XUnitTestClassReport();
    report.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    report.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_OK).setDurationMilliseconds(200L));
    //Some negative duration can occur due to bug in xUnit.
    report.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_OK).setDurationMilliseconds(-200L));
    report.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_SKIPPED));

    assertThat(report.getResults()).hasSize(4);
    assertThat(report.getSkipped()).isEqualTo(1);
    assertThat(report.getTests()).isEqualTo(4);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L);
    assertThat(report.getErrors()).isEqualTo(1);
    assertThat(report.getFailures()).isZero();
    assertThat(report.getNegativeTimeTestNumber()).isEqualTo(1L);
  }

  @Test
  public void shouldHaveEmptyReport() {
    XUnitTestClassReport report = new XUnitTestClassReport();
    assertThat(report.getResults()).isEmpty();
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getTests()).isZero();
    assertThat(report.getDurationMilliseconds()).isZero();
    assertThat(report.getErrors()).isZero();
    assertThat(report.getFailures()).isZero();
  }
}
