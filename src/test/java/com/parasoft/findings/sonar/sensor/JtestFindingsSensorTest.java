package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.importer.JtestTestsParser;
import com.parasoft.findings.sonar.importer.XSLConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;

import java.io.File;

import static com.parasoft.findings.sonar.importer.XSLConverter.XUNIT_TARGET_REPORT_NAME_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class JtestFindingsSensorTest {
    private static final File BASE_DIR = new File("src/test/resources/jtest");

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
    public void testExecute_staticAnalysisReport() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.parasoft.jtest.reportPaths", "Jtest-2022.2.0-static-report.xml");
        context.setSettings(settings);

        new JtestFindingsSensor(new XSLConverter(context.fileSystem()), new JtestTestsParser()).execute(context);

        assertThat(logTester.logs(Level.INFO)).contains("4 findings imported");
        assertThat(logTester.logs(Level.INFO)).contains("Added location with finding(s): /D:/RWorkspaces/project-workspace/CICD/jtestdemo/src/main/java/parasoft/ForViolations.java");
        assertThat(logTester.logs(Level.ERROR   )).contains("No source files found");
    }

    @Test
    public void testExecute_UnitTestReport() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.parasoft.jtest.reportPaths", new File(BASE_DIR,"Jtest-2022.2.0-unitTests-report.xml").getAbsolutePath());
        context.setSettings(settings);
        context.fileSystem()
                .add(resource("src/main/java/parasoft/ForUnit.java", InputFile.Type.MAIN))
                .add(resource("src/test/java/parasoft/ForUnitTest.java", InputFile.Type.TEST));

        FileSystem spyFileSystem = spy(context.fileSystem());
        doReturn(new File("D:/RWorkspaces/project-workspace/CICD/jtestdemo")).when(spyFileSystem).baseDir();
        new JtestFindingsSensor(new XSLConverter(spyFileSystem), new JtestTestsParser()).execute(context);

        assertThat(new File(BASE_DIR, "Jtest-2022.2.0-unitTests-report" + XUNIT_TARGET_REPORT_NAME_SUFFIX)).isFile().size().isGreaterThan(0);

        assertThat(logTester.logs(Level.INFO)).contains("Added Parasoft unit test results for project: Total: 3, Errors: 1, Failures: 1, Duration: 51ms");
        assertThat(context.measures(":src/test/java/parasoft/ForUnitTest.java")).hasSize(4);
        assertThat(context.measure(":src/test/java/parasoft/ForUnitTest.java", CoreMetrics.TESTS).value()).isEqualTo(3);
        assertThat(context.measure(":src/test/java/parasoft/ForUnitTest.java", CoreMetrics.TEST_ERRORS).value()).isEqualTo(1);
        assertThat(context.measure(":src/test/java/parasoft/ForUnitTest.java", CoreMetrics.TEST_FAILURES).value()).isEqualTo(1);
        assertThat(context.measure(":src/test/java/parasoft/ForUnitTest.java", CoreMetrics.TEST_EXECUTION_TIME).value()).isEqualTo(51L);
    }

    @Test
    public void testExecute_unitTestReport_sourceNotFound() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        // TODO 添加constants
        settings.setProperty("sonar.parasoft.jtest.reportPaths", new File(BASE_DIR,"Jtest-2022.2.0-unitTests-report.xml").getAbsolutePath());
        context.setSettings(settings);
        context.fileSystem()
                .add(resource("src/main/java/parasoft/ForUnit.java", InputFile.Type.MAIN));

        FileSystem spyFileSystem = spy(context.fileSystem());
        doReturn(new File("D:/RWorkspaces/project-workspace/CICD/jtestdemo")).when(spyFileSystem).baseDir();
        new JtestFindingsSensor(new XSLConverter(spyFileSystem), new JtestTestsParser()).execute(context);


        assertThat(logTester.logs(Level.DEBUG)).contains("Resource not found: ./src/test/java/parasoft/ForUnitTest.java");
        assertThat(logTester.logs(Level.INFO)).contains("Added Parasoft unit test results for project: Total: 0, Errors: 0, Failures: 0, Duration: 0ms");
    }

    @Test
    public void testExecute_reportNotExists() {
        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        MapSettings settings = new MapSettings();
        settings.setProperty("sonar.parasoft.jtest.reportPaths", "/wrongReport.xml");
        context.setSettings(settings);

        new JtestFindingsSensor(new XSLConverter(context.fileSystem()), new JtestTestsParser()).execute(context);

        assertThat(logTester.logs(Level.WARN)).contains("XML report file not found: " + new File(BASE_DIR, "wrongReport.xml").getAbsolutePath());
        assertThat(logTester.logs(Level.INFO)).contains("No Parasoft unit test report(s) specified");
        assertThat(logTester.logs(Level.INFO)).contains("No Parasoft static analysis report(s) specified");
    }

    private DefaultInputFile resource(String key, InputFile.Type type) {
        return new TestInputFileBuilder("", key).setType(type).setLanguage("java").setModuleBaseDir(new File("D:/RWorkspaces/project-workspace/CICD/jtestdemo").toPath()).build();
    }
}