package com.parasoft.findings.sonar.sensor;

import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.exception.CoverageSourceMismatchException;
import com.parasoft.findings.sonar.exception.InvalidReportException;
import com.parasoft.findings.sonar.importer.XSLConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.Configuration;

import java.io.File;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CoverageSensorTest {

    public FileSystem fileSystem;

    public SensorContext sensorContext;

    public NewCoverage newCoverage;

    public Configuration configuration;

    public FilePredicate filePredicate;

    public FilePredicates filePredicates;

    public InputFile inputFile;

    public String[] paths;

    public XSLConverter xslConverter;

    @AfterEach
    private void cleanUp() {
        if (paths != null) {
            for (String path : paths) {
                File file = new File(path.substring(0, path.lastIndexOf(".")) + "-cobertura.xml");
                if (file.exists()) {
                    if (!file.delete()) {
                        fail("Unable to delete remaining file: " + path);
                    }
                }
            }
        }
    }

    public void setUp() {
        fileSystem = mock(FileSystem.class);
        doReturn(new File(System.getProperty("user.dir"))).when(fileSystem).baseDir();

        xslConverter = spy(new XSLConverter(fileSystem));
        configuration = mock(Configuration.class);
        doReturn(paths).when(configuration).getStringArray(any());

        newCoverage = mock(NewCoverage.class);
        doReturn(newCoverage).when(newCoverage).onFile(any());

        sensorContext = mock(SensorContext.class);
        doReturn(configuration).when(sensorContext).config();
        doReturn(newCoverage).when(sensorContext).newCoverage();

        filePredicate = mock(FilePredicate.class);
        filePredicates = mock(FilePredicates.class);
        doReturn(filePredicate).when(filePredicates).hasRelativePath(any());
        doReturn(filePredicates).when(fileSystem).predicates();

        inputFile = mock(InputFile.class);
        doReturn(inputFile).when(fileSystem).inputFile(any());
    }

    @Test
    public void testDescribe() {
        SensorDescriptor sensorDescriptor = mock(SensorDescriptor.class);
        doReturn(sensorDescriptor).when(sensorDescriptor).onlyOnFileType(any());
        doReturn(sensorDescriptor).when(sensorDescriptor).name(any());
        CoverageSensor coverageSensor = new CoverageSensor(fileSystem, xslConverter);

        coverageSensor.describe(sensorDescriptor);

        verify(sensorDescriptor).onlyOnFileType(InputFile.Type.MAIN);
        verify(sensorDescriptor).name(ParasoftConstants.PARASOFT_COVERAGE_IMPORTER);
        verify(sensorDescriptor).onlyWhenConfiguration(any(Predicate.class));
    }

    @Test
    public void testCoverageSensor_noReport() {
        paths = new String[]{};
        setUp();
        CoverageSensor underTest = mock(CoverageSensor.class);
        doCallRealMethod().when(underTest).execute(sensorContext);
        underTest.execute(sensorContext);

        verify(xslConverter, times(0)).transformReports(any(String[].class), any());
    }

    @Test
    public void testCoverageSensor_SourceFileAllFound() {
        paths = new String[]{"src/test/resources/coverageReport/normalCoverageReport.xml"};
        setUp();

        CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
        underTest.execute(sensorContext);

        assertTrue(new File("src/test/resources/coverageReport/normalCoverageReport-cobertura.xml").exists());
        verify(newCoverage, atLeastOnce()).lineHits(anyInt(), anyInt());
        verify(newCoverage, atLeastOnce()).save();
    }

    @Test
    public void testCoverageSensor_SourceFilePartFound() {
        paths = new String[]{"src/test/resources/coverageReport/normalCoverageReport.xml"};
        setUp();
        doAnswer(invocation -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime % 2 == 0) {
                return null;
            } else {
                return inputFile;
            }
        }).when(fileSystem).inputFile(any());

        CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
        underTest.execute(sensorContext);

        assertTrue(new File("src/test/resources/coverageReport/normalCoverageReport-cobertura.xml").exists());
        verify(newCoverage, atLeastOnce()).lineHits(anyInt(), anyInt());
        verify(newCoverage, atLeastOnce()).save();
    }

    @Test
    public void testCoverageSensor_NoMatchedSourceFile() {
        paths = new String[]{"src/test/resources/coverageReport/normalCoverageReport.xml"};
        setUp();
        doReturn(null).when(fileSystem).inputFile(any());

        Exception exception = assertThrows(CoverageSourceMismatchException.class, () -> {
            CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
            underTest.execute(sensorContext);
        });

        assertEquals(Messages.NotMatchedCoverageReportAndProject, exception.getMessage());
        assertTrue(new File("src/test/resources/coverageReport/normalCoverageReport-cobertura.xml").exists());
        verify(newCoverage, times(0)).lineHits(anyInt(), anyInt());
        verify(newCoverage, times(0)).save();
    }

    @Test
    public void testCoverageSensor_BrokenReport() {
        paths = new String[]{"src/test/resources/coverageReport/BrokenReport.xml"};
        setUp();

        Exception exception = assertThrows(InvalidReportException.class, () -> {
            CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
            underTest.execute(sensorContext);
        });

        assertEquals(Messages.NoValidCoberturaReport, exception.getMessage());
        assertFalse(new File(paths[0] + "-cobertura.xml").exists());
        verify(newCoverage, times(0)).lineHits(anyInt(), anyInt());
        verify(newCoverage, times(0)).save();
    }

    @Test
    public void testUploadFileCoverageData_NoPackageCoberturaReport() {
        File file = new File("src/test/resources/coverageReport/NoPackageCoberturaReport.xml");
        setUp();

        CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
        underTest.uploadFileCoverageData(file, sensorContext);

        verify(sensorContext, times(0)).newCoverage();
    }

    @Test
    public void testUploadFileCoverage_NoClassReport() {
        File file = new File("src/test/resources/coverageReport/NoClassCoberturaReport.xml");
        setUp();

        CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
        underTest.uploadFileCoverageData(file, sensorContext);

        verify(sensorContext, times(0)).newCoverage();
    }

    @Test
    public void testUploadFileCoverage_NoLineReport() {
        File file = new File("src/test/resources/coverageReport/NoLineCoberturaReport.xml");
        setUp();

        CoverageSensor underTest = new CoverageSensor(fileSystem, xslConverter);
        underTest.uploadFileCoverageData(file, sensorContext);

        verify(sensorContext, atLeastOnce()).newCoverage();
        verify(newCoverage, atLeastOnce()).save();
        verify(newCoverage, times(0)).lineHits(anyInt(), anyInt());
    }
}
