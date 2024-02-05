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

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.8
 */
public class XUnitTestIndex {

  private Map<String, XUnitTestClassReport> indexByFilePath;

  public XUnitTestIndex() {
    this.indexByFilePath = new HashMap<>();
  }

  public XUnitTestClassReport index(String filePath) {
    return indexByFilePath.computeIfAbsent(filePath, name -> new XUnitTestClassReport());
  }

  public XUnitTestClassReport get(String filePath) {
    return indexByFilePath.get(filePath);
  }

  public Map<String, XUnitTestClassReport> getIndexByFilePath() {
    return indexByFilePath;
  }

  public int size() {
    return indexByFilePath.size();
  }

  public void remove(String filePath) {
    indexByFilePath.remove(filePath);
  }


}
