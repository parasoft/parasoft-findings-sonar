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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class XUnitTestIndexTest {

  @Test
  public void shouldIndexNewClassname() {
    XUnitTestIndex index = new XUnitTestIndex();

    XUnitTestClassReport report = index.index("org.sonar.Foo");

    assertThat(report.getTests()).isZero();
    assertThat(index.size()).isEqualTo(1);
    assertThat(report).isSameAs(index.get("org.sonar.Foo"));
  }

  @Test
  public void shouldNotReIndex() {
    XUnitTestIndex index = new XUnitTestIndex();

    XUnitTestClassReport report1 = index.index("org.sonar.Foo");
    XUnitTestClassReport report2 = index.index("org.sonar.Foo");

    assertThat(report1).isSameAs(report2);
    assertThat(report1.getTests()).isZero();
    assertThat(index.size()).isEqualTo(1);
    assertThat(report1).isSameAs(index.get("org.sonar.Foo"));
  }

  @Test
  public void shouldRemoveClassname() {
    XUnitTestIndex index = new XUnitTestIndex();

    index.index("org.sonar.Foo");
    index.remove("org.sonar.Foo");

    assertThat(index.size()).isZero();
    assertThat(index.get("org.sonar.Foo")).isNull();
  }

  @Test
  public void shouldMergeClasses() {
    XUnitTestIndex index = new XUnitTestIndex();
    XUnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_OK).setDurationMilliseconds(200L));
    XUnitTestClassReport publicClass = index.index("org.sonar.Foo");
    publicClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_ERROR).setDurationMilliseconds(1000L));
    publicClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_FAILURE).setDurationMilliseconds(350L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isEqualTo(1);
    XUnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests()).isEqualTo(4);
    assertThat(report.getFailures()).isEqualTo(1);
    assertThat(report.getErrors()).isEqualTo(2);
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getResults()).hasSize(4);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L + 1000L + 350L);
  }

  @Test
  public void shouldRenameClassWhenMergingToNewClass() {
    XUnitTestIndex index = new XUnitTestIndex();
    XUnitTestClassReport innerClass = index.index("org.sonar.Foo$Bar");
    innerClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_ERROR).setDurationMilliseconds(500L));
    innerClass.add(new XUnitTestResult().setStatus(XUnitTestResult.STATUS_OK).setDurationMilliseconds(200L));

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isEqualTo(1);
    XUnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests()).isEqualTo(2);
    assertThat(report.getFailures()).isZero();
    assertThat(report.getErrors()).isEqualTo(1);
    assertThat(report.getSkipped()).isZero();
    assertThat(report.getResults()).hasSize(2);
    assertThat(report.getDurationMilliseconds()).isEqualTo(500L + 200L);
  }

  @Test
  public void shouldNotFailWhenMergingUnknownClass() {
    XUnitTestIndex index = new XUnitTestIndex();

    index.merge("org.sonar.Foo$Bar", "org.sonar.Foo");

    assertThat(index.size()).isZero();
  }
}
