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
import com.parasoft.findings.sonar.importer.soatest.SOAtestMetrics;
import com.parasoft.findings.utils.common.nls.NLS;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;

public class SOAtestTestsParser extends AbstractSOAtestAndJtestTestsParser {

    @Override
    protected boolean saveMeasuresOnFile(TestSummary testSummaryOnFile, InputFile inputFile, SensorContext context) {
        int testsCount = testSummaryOnFile.getTotalTests();
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TESTS, testsCount);
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_FAILURES, testSummaryOnFile.getFailures());
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_EXECUTION_TIME, testSummaryOnFile.getDuration());

        double successDensity = 0;
        if (testsCount > 0 && testSummaryOnFile.getFailures() >= 0) {
            double density = testSummaryOnFile.getFailures() * 100D / testsCount;
            successDensity = 100D - density;
        }
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_SUCCESS_DENSITY, successDensity);

        Logger.getLogger().info(NLS.getFormatted(Messages.CollectedSOAtestTestsForFile, inputFile));
        Logger.getLogger().info(testSummaryOnFile);

        return true;
    }

    @Override
    protected void logTestSummaryForProject(TestSummary testSummaryForProject) {
        Logger.getLogger().info(NLS.getFormatted(Messages.AddedSOAtestTestsForProjectSummary, testSummaryForProject));
    }
}
