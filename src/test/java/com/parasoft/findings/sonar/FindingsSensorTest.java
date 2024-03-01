/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import com.parasoft.findings.sonar.importer.ParasoftDottestAndCpptestTestsParser;
import com.parasoft.findings.sonar.importer.JtestTestsParser;
import com.parasoft.findings.sonar.importer.XSLConverter;
import com.parasoft.findings.sonar.sensor.AbstractParasoftFindingsSensor;
import com.parasoft.findings.sonar.sensor.CpptestFindingsSensor;
import com.parasoft.findings.sonar.sensor.DottestFindingsSensor;
import com.parasoft.findings.sonar.sensor.JtestFindingsSensor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;

class FindingsSensorTest
{
    @ParameterizedTest
    @MethodSource("testBasic_Params")
    void testBasic(AbstractParasoftFindingsSensor sensor)
    {
        SensorDescriptor desc = mock(SensorDescriptor.class);
        SensorContext context = mock(SensorContext.class);
        Configuration config = mock(Configuration.class);
        FileSystem filesys = mock(FileSystem.class);
        FilePredicates pred = mock(FilePredicates.class);

        when(context.config()).thenReturn(config);
        when(config.getStringArray(any())).thenReturn(new String[] {"src/test/resources/jtest/Jtest-2022.2.0-static-report.xml"});
        when(context.fileSystem()).thenReturn(filesys);
        when(filesys.baseDir()).thenReturn(new File("."));
        when(filesys.predicates()).thenReturn(pred);

        sensor.describe(desc);
        sensor.execute(context);
    }

    static Object[][] testBasic_Params() {
        return new Object[][] {
            {new JtestFindingsSensor(mock(XSLConverter.class), mock(JtestTestsParser.class))},
            {new DottestFindingsSensor(mock(ParasoftDottestAndCpptestTestsParser.class))},
            {new CpptestFindingsSensor(mock(ParasoftDottestAndCpptestTestsParser.class))}
        };
    }
}
