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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SOAtestReportMapper {

    private final Map<String, SOAtestReport> reportMapper;

    public SOAtestReportMapper() {
        this.reportMapper = new HashMap<>();
    }

    public Map<String, SOAtestReport> getReportMapper() {
        return reportMapper;
    }

    public SOAtestReport getOrCreateTestReport(String classname) {
        return reportMapper.computeIfAbsent(classname, name -> new SOAtestReport());
    }

    public Set<String> getClassnames() {
        return new HashSet<>(reportMapper.keySet());
    }

    public void mergeInto(String classname, String intoClassname) {
        SOAtestReport from = reportMapper.get(classname);
        if (from != null) {
            SOAtestReport to = getOrCreateTestReport(intoClassname);
            to.addReportDataFrom(from);
            reportMapper.remove(classname);
        }
    }

    public void normalizeReports() {
        // update class report with results from inner classes
        for (String classname : getClassnames()) {
            if (StringUtils.contains(classname, "$")) {
                // XUnit reports classes whereas sonar supports files
                String parentClassName = StringUtils.substringBefore(classname, "$");
                mergeInto(classname, parentClassName);
            }
        }
    }
}
