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

import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_CPPTEST_REPORT_PATHS_KEY;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.*;

public class CpptestFindingsSensorTest {

    private static final File BASE_DIR = new File("src/test/resources/cpptest");

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
        context.settings().setProperty(PARASOFT_CPPTEST_REPORT_PATHS_KEY, new File(BASE_DIR,"Cpptest-std-2023.2.0-unitTests-report.xml").getAbsolutePath());

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new CpptestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR, "Cpptest-std-2023.2.0-unitTests-report.xml").getAbsolutePath());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 162, Errors: 0, Failures: 36, Duration: 45,263ms");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }

    @Test
    public void testExecute_staticReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_CPPTEST_REPORT_PATHS_KEY, "Cpptest-std-2023.1.1-static-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new CpptestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR, "Cpptest-std-2023.1.1-static-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("6 findings imported");
            verify(logger, times(1)).error("No source files found");
        }
    }

    @Test
    public void testExecute_staticAndUnitTestReport()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_CPPTEST_REPORT_PATHS_KEY, "Cpptest-pro-2023.2.0-static-and-unitTests-report.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new CpptestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).info("Parsing Parasoft unit test XML report: " + new File(BASE_DIR, "Cpptest-pro-2023.2.0-static-and-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("Added Parasoft unit test results for project: Total: 162, Errors: 0, Failures: 36, Duration: 45,203ms");
            verify(logger, times(1)).info("Parsing Parasoft static analysis XML report: " + new File(BASE_DIR, "Cpptest-pro-2023.2.0-static-and-unitTests-report.xml").getAbsoluteFile());
            verify(logger, times(1)).info("1,781 findings imported");
            verify(logger, times(1)).error("No source files found");
        }
    }

    @Test
    public void testExecute_reportNotExists()
    {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        context.settings().setProperty(PARASOFT_CPPTEST_REPORT_PATHS_KEY, "wrongReport.xml");

        try (MockedStatic<Logger> mockedStatic = Mockito.mockStatic(Logger.class)) {
            FindingsLogger logger = mock(FindingsLogger.class);
            mockedStatic.when(Logger::getLogger).thenReturn(logger);

            new CpptestFindingsSensor(new ParasoftDottestAndCpptestTestsParser()).execute(context);

            verify(logger, times(1)).warn("XML report file not found: "+ new File(BASE_DIR, "wrongReport.xml").getAbsolutePath());
            verify(logger, times(1)).info("No Parasoft unit test report(s) specified");
            verify(logger, times(1)).info("No Parasoft static analysis report(s) specified");
        }
    }
}
