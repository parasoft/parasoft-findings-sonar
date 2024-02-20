package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.importer.SOAtestTestsParser;
import com.parasoft.findings.sonar.importer.XSLConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import java.io.File;

import static com.parasoft.findings.sonar.ParasoftConstants.PARASOFT_SOATEST_REPORT_PATHS_KEY;
import static com.parasoft.findings.sonar.importer.XSLConverter.XUNIT_TARGET_REPORT_NAME_SUFFIX;
import static com.parasoft.findings.sonar.importer.soatest.SOAtestMetrics.SOATEST_TESTS;
import static com.parasoft.findings.sonar.importer.soatest.SOAtestMetrics.SOATEST_TEST_FAILURES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class SOAtestSensorTest {


    private static final File BASE_DIR = new File("src/test/resources/soatest");

    @TempDir
    public File tempDir;

    @RegisterExtension
    public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

    @AfterEach
    public void cleanUp() {
        File[] tempFiles = BASE_DIR.listFiles((dir, name) ->
                name.endsWith(XUNIT_TARGET_REPORT_NAME_SUFFIX) || name.contentEquals(".sonar"));
        if (ArrayUtils.isNotEmpty(tempFiles)) {
            for (File tempFile : tempFiles) {
                if (!FileUtils.deleteQuietly(tempFile)) {
                    fail("Unable to delete temp file/directory: " + tempFile);
                }
            }
        }
    }

    @Test
    public void testDescribeExecuteOnlyWhenKeyPresent() {
        Configuration configWithKey = mock(Configuration.class);
        when(configWithKey.hasKey(PARASOFT_SOATEST_REPORT_PATHS_KEY)).thenReturn(true);
        Configuration configWithoutKey = mock(Configuration.class);
        DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

        new SOAtestSensor(mock(XSLConverter.class), mock(SOAtestTestsParser.class)).describe(descriptor);

        assertThat(descriptor.configurationPredicate()).accepts(configWithKey);
        assertThat(descriptor.configurationPredicate()).rejects(configWithoutKey);
    }

    @Test
    public void testExecuteWarnsWhenKeyIsEmpty() {
        Configuration configWithEmptyKey = mock(Configuration.class);
        when(configWithEmptyKey.getStringArray(PARASOFT_SOATEST_REPORT_PATHS_KEY)).thenReturn(new String[0]);
        SensorContextTester context = SensorContextTester.create(tempDir);
        XSLConverter mockXSLConverter = mock(XSLConverter.class);
        SOAtestTestsParser mockSOAtestTestsParser = mock(SOAtestTestsParser.class);

        new SOAtestSensor(mockXSLConverter, mockSOAtestTestsParser).execute(context);

        assertThat(logTester.logs(Level.INFO)).contains("No Parasoft SOAtest report(s) specified");
        verify(mockXSLConverter, never()).transformReports(any(String[].class), any(XSLConverter.ReportType.class));
        verify(mockSOAtestTestsParser, never()).collect(any(), any());
    }

    @Test
    public void testExecuteShouldNotFailIfReportsNotFound() {
        SensorContextTester context = SensorContextTester.create(tempDir);
        MapSettings settings = new MapSettings();
        settings.setProperty(PARASOFT_SOATEST_REPORT_PATHS_KEY, "unknown");
        context.setSettings(settings);

        assertDoesNotThrow(() -> new SOAtestSensor(new XSLConverter(context.fileSystem()),
                new SOAtestTestsParser()).execute(context));
    }

    @Test
    public void testConvertMultipleReports() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty(PARASOFT_SOATEST_REPORT_PATHS_KEY,
                "parabank_soatest-2022.2.0-report.xml,multiple-resources-and-directory-levels_soatest-2022.2.0-report.xml," +
                        new File(BASE_DIR,"parabank_soatest-2022.2.0-report_copy.xml").getAbsolutePath());
        context.setSettings(settings);

        new SOAtestSensor(new XSLConverter(context.fileSystem()), mock(SOAtestTestsParser.class)).execute(context);

        assertThat(new File(BASE_DIR, "parabank_soatest-2022.2.0-report" + XUNIT_TARGET_REPORT_NAME_SUFFIX))
                .isFile().size().isGreaterThan(0);
        assertThat(new File(BASE_DIR,
                "multiple-resources-and-directory-levels_soatest-2022.2.0-report" + XUNIT_TARGET_REPORT_NAME_SUFFIX))
                .isFile().size().isGreaterThan(0);
        assertThat(new File(BASE_DIR, "parabank_soatest-2022.2.0-report_copy" + XUNIT_TARGET_REPORT_NAME_SUFFIX))
                .isFile().size().isGreaterThan(0);
    }

    @Test
    public void testExecuteShouldSaveMeasures() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty(PARASOFT_SOATEST_REPORT_PATHS_KEY,
                new File(BASE_DIR,"parabank_soatest-2022.2.0-report.xml").getAbsolutePath());
        context.setSettings(settings);
        context.fileSystem()
                .add(resource("TestAssets/PB-ParabankServices-v3.tst"))
                .add(resource("TestAssets/PB-WebUI-v3.tst"))
                .add(resource("TestAssets/Smoke.tst"));
        FileSystem spyFileSystem = spy(context.fileSystem());
        doReturn(new File("D:/Projects/GitHub/parasoft/parabank/soatest")).when(spyFileSystem).baseDir();

        new SOAtestSensor(new XSLConverter(spyFileSystem), new SOAtestTestsParser()).execute(context);

        assertThat(context.measures(":TestAssets/PB-ParabankServices-v3.tst")).hasSize(4);
        assertThat(context.measures(":TestAssets/PB-WebUI-v3.tst")).hasSize(4);
        assertThat(context.measures(":TestAssets/Smoke.tst")).hasSize(4);

        assertThat(context.measure(":TestAssets/PB-ParabankServices-v3.tst", SOATEST_TESTS).value()).isEqualTo(30);
        assertThat(context.measure(":TestAssets/PB-ParabankServices-v3.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(30);

        assertThat(context.measure(":TestAssets/PB-WebUI-v3.tst", SOATEST_TESTS).value()).isEqualTo(5);
        assertThat(context.measure(":TestAssets/PB-WebUI-v3.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(5);

        assertThat(context.measure(":TestAssets/Smoke.tst", SOATEST_TESTS).value()).isEqualTo(51);
        assertThat(context.measure(":TestAssets/Smoke.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(42);
    }

    @Test
    public void testExecuteShouldAggregateResultsFromSameReports() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty(PARASOFT_SOATEST_REPORT_PATHS_KEY,
                new File(BASE_DIR,"parabank_soatest-2022.2.0-report.xml").getAbsolutePath() + "," +
                        new File(BASE_DIR,"parabank_soatest-2022.2.0-report_copy.xml").getAbsolutePath());
        context.setSettings(settings);
        SOAtestTestsParser spySOAtestTestsParser = spy(new SOAtestTestsParser());
        doAnswer(invocation -> resource((String) invocation.getArguments()[0])).when(spySOAtestTestsParser)
                .findResourceByFilePath(anyString(), any());
        FileSystem spyFileSystem = spy(context.fileSystem());
        doReturn(new File("D:/Projects/GitHub/parasoft/parabank/soatest")).when(spyFileSystem).baseDir();

        new SOAtestSensor(new XSLConverter(spyFileSystem), spySOAtestTestsParser).execute(context);

        assertThat(context.measures(":TestAssets/PB-ParabankServices-v3.tst")).hasSize(4);
        assertThat(context.measures(":TestAssets/PB-WebUI-v3.tst")).hasSize(4);
        assertThat(context.measures(":TestAssets/Smoke.tst")).hasSize(4);

        assertThat(context.measure(":TestAssets/PB-ParabankServices-v3.tst", SOATEST_TESTS).value()).isEqualTo(60);
        assertThat(context.measure(":TestAssets/PB-ParabankServices-v3.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(60);

        assertThat(context.measure(":TestAssets/PB-WebUI-v3.tst", SOATEST_TESTS).value()).isEqualTo(10);
        assertThat(context.measure(":TestAssets/PB-WebUI-v3.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(10);

        assertThat(context.measure(":TestAssets/Smoke.tst", SOATEST_TESTS).value()).isEqualTo(102);
        assertThat(context.measure(":TestAssets/Smoke.tst", SOATEST_TEST_FAILURES).value()).isEqualTo(84);
    }

    private DefaultInputFile resource(String key) {
        return new TestInputFileBuilder("", key).setType(InputFile.Type.TEST).build();
    }
}