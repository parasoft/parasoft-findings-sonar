package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.importer.ParasoftDottestAndCpptestTestsParser;
import com.parasoft.findings.utils.common.logging.FindingsLogger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;

import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_DOTTEST_REPORT_PATHS_KEY;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.*;

public class DottestFindingsSensorTest {

    private static final File BASE_DIR = new File("src/test/resources/dottest");

    @AfterEach
    public void cleanUp() {
        File[] tempFiles = BASE_DIR.listFiles((dir, name) -> name.contentEquals(".sonar"));
        if (ArrayUtils.isNotEmpty(tempFiles)) {
            for (File tempFile : tempFiles) {
                if (!FileUtils.deleteQuietly(tempFile)) {
                    fail("Unable to delete temp file/directory: " + tempFile);
                }
            }
        }
    }

    @Test
    public void testExecute_unitTestReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-unitTests-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 30, Errors: 0, Failures: 11, Duration: 2,702ms");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }

    @Test
    public void testExecute_multipleUnitTestReports()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-unitTests-report.xml,DotTest-2023.1.1-unitTests-report-copy.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(2)).info("Total: 30, Errors: 0, Failures: 11, Duration: 2,702ms");
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 60, Errors: 0, Failures: 22, Duration: 5,404ms");
        }
    }

    @Test
    public void testExecute_unitTestReport_noTimeAttribute()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-unitTests-report-no-time-attr.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-unitTests-report-no-time-attr.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 30, Errors: 0, Failures: 11, Duration: 0ms");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }

    @Test
    public void testExecute_unitTestReport_noTotalElement()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-unitTests-report-no-totalElement.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-unitTests-report-no-totalElement.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 0, Errors: 0, Failures: 0, Duration: 0ms");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }

    @Test
    public void testExecute_staticReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-static-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-static-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("1,973 findings imported");
            verify(logger, times(1)).error("No source files found");
        }
    }

    @Test
    public void testExecute_staticAndUnitTestReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-static-and-unitTests-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-static-and-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 30, Errors: 0, Failures: 11, Duration: 2,702ms");
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-static-and-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("1,973 findings imported");
            verify(logger, times(1)).error("No source files found");
        }
    }

    @Test
    public void testExecute_multipleStaticReports()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "DotTest-2023.1.1-static-report.xml,DotTest-2023.1.1-static-report-copy.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-static-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR,"DotTest-2023.1.1-static-report-copy.xml").getAbsoluteFile());
            verify(logger, times(2)).info("1,973 findings imported");
            verify(logger, times(2)).error("No source files found");
        }
    }

    @Test
    public void testExecute_reportNotExists()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "wrongReport.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).warn("XML report file not found: " + new File(BASE_DIR,"wrongReport.xml").getAbsolutePath());
            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }

    @Test
    public void testExecute_invalidReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_DOTTEST_REPORT_PATHS_KEY, "invalid-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new DottestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).warn("Skipped invalid report: " + new File(BASE_DIR,"invalid-report.xml").getAbsolutePath() + ".");
            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }
}
