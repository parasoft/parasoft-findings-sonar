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

public class XUnitTestResultTest {

  @Test
  public void shouldBeError() {
    XUnitTestResult result = new XUnitTestResult().setStatus(XUnitTestResult.STATUS_ERROR);
    assertThat(result.getStatus()).isEqualTo(XUnitTestResult.STATUS_ERROR);
    assertThat(result.isError()).isTrue();
    assertThat(result.isErrorOrFailure()).isTrue();
  }

  @Test
  public void shouldBeFailure() {
    XUnitTestResult result = new XUnitTestResult().setStatus(XUnitTestResult.STATUS_FAILURE);
    assertThat(result.getStatus()).isEqualTo(XUnitTestResult.STATUS_FAILURE);
    assertThat(result.isError()).isFalse();
    assertThat(result.isErrorOrFailure()).isTrue();
  }

  @Test
  public void shouldBeSuccess() {
    XUnitTestResult result = new XUnitTestResult().setStatus(XUnitTestResult.STATUS_OK);
    assertThat(result.getStatus()).isEqualTo(XUnitTestResult.STATUS_OK);
    assertThat(result.isError()).isFalse();
    assertThat(result.isErrorOrFailure()).isFalse();
  }
}
