/*
 * Copyright 2024 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parasoft.findings.sonar.importer;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.importer.xunitdata.XUnitTestClassReport;
import com.parasoft.findings.utils.common.nls.NLS;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;

public class JtestTestsParser extends AbstractSOAtestAndJtestTestsParser {

    @Override
    protected TestSummary saveMeasuresOnFile(XUnitTestClassReport report, InputFile inputFile, SensorContext context) {
        try {
            int skippedTestsCount = report.getSkipped();
            int testsCount = report.getTests() - skippedTestsCount;
            int errorTestsCount = report.getErrors();
            int failureTestsCount = report.getFailures();
            long duration = report.getDurationMilliseconds();

            saveMeasureOnFile(context, inputFile, CoreMetrics.SKIPPED_TESTS, skippedTestsCount);
            saveMeasureOnFile(context, inputFile, CoreMetrics.TESTS, testsCount);
            saveMeasureOnFile(context, inputFile, CoreMetrics.TEST_ERRORS, errorTestsCount);
            saveMeasureOnFile(context, inputFile, CoreMetrics.TEST_FAILURES, failureTestsCount);
            saveMeasureOnFile(context, inputFile, CoreMetrics.TEST_EXECUTION_TIME, duration);
            TestSummary unitTestSummaryForFile = new TestSummary(testsCount, errorTestsCount, failureTestsCount, duration);

            Logger.getLogger().info(NLS.getFormatted(Messages.CollectedUnitTestsForFile, inputFile));
            Logger.getLogger().info(unitTestSummaryForFile);

            return unitTestSummaryForFile;
        } catch (UnsupportedOperationException e) {
            Logger.getLogger().warn(NLS.getFormatted(Messages.SkipAddingUnitTestMeasuresForFile, inputFile));
            Logger.getLogger().debug(e.getMessage());
        }

        return new TestSummary();
    }

    @Override
    protected void logTestSummaryForProject(TestSummary testSummaryForProject) {
        Logger.getLogger().info(NLS.getFormatted(Messages.AddedUnitTestsForProjectSummary, testSummaryForProject));
    }
}
