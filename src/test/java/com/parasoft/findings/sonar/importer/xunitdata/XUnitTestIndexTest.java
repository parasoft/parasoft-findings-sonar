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

}
