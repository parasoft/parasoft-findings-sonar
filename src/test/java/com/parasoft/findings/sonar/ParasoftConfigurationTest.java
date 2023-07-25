/*
 * (C) Copyright ParaSoft Corporation 2022. All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft The copyright notice above
 * does not evidence any actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;

public class ParasoftConfigurationTest
{
    @Test
    public void testGetProperties()
        throws Throwable
    {
        var result = ParasoftConfiguration.getProperties();

        assertEquals(4, result.size());

        var def = result.get(0);
        assertEquals("External Analyzers / Java\n"
                + "Parasoft Jtest Report Files\n"
                + "Path (absolute or relative) to Jtest xml report files.\n"
                + "sonar.parasoft.jtest.reportPaths\n"
                + "Default: ", getDefinitionString(def));

        def = result.get(1);
        assertEquals("External Analyzers / C#\n"
                + "Parasoft dotTest Report Files\n"
                + "Path (absolute or relative) to dotTest xml report files.\n"
                + "sonar.parasoft.dottest.reportPaths\n"
                + "Default: ", getDefinitionString(def));

        def = result.get(2);
        assertEquals("External Analyzers / C++\n"
                + "Parasoft C/C++Test Report Files\n"
                + "Path (absolute or relative) to C/C++Test xml report files.\n"
                + "sonar.parasoft.cpptest.reportPaths\n"
                + "Default: ", getDefinitionString(def));

        def = result.get(3);
        assertEquals("codeCoverage / Parasoft Code Coverage\n"
                + "Parasoft Coverage Report Files\n"
                + "Path (absolute or relative) to parasoft coverage xml report files.\n"
                + "sonar.parasoft.coverage.reportPaths\n"
                + "Default: ", getDefinitionString(def));
    }

    private String getDefinitionString(PropertyDefinition def)
    {
        var buff = new StringBuilder();
        buff
            .append(def.category()).append(" / ").append(def.subCategory()).append("\n")
            .append(def.name()).append("\n")
            .append(def.description()).append("\n")
            .append(def.key()).append("\n")
            .append("Default: ").append(def.defaultValue());
        return buff.toString();
    }
}
