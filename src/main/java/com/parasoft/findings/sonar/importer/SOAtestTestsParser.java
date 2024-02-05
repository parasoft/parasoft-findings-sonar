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
import com.parasoft.findings.sonar.importer.xunitdata.XUnitTestClassReport;
import com.parasoft.findings.utils.common.nls.NLS;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;

public class SOAtestTestsParser extends AbstractSOAtestAndJtestTestsParser {

    @Override
    protected UnitTestSummary saveMeasuresOnFile(XUnitTestClassReport report, InputFile inputFile, SensorContext context) {
        int testsCount = report.getTests();
        int failureTestsCount = report.getFailures();
        long duration = report.getDurationMilliseconds();
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TESTS, testsCount);
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_FAILURES, failureTestsCount);
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_EXECUTION_TIME, report.getDurationMilliseconds());

        double successDensity = 0;
        if (testsCount > 0 && report.getFailures() >= 0) {
            double density = report.getFailures() * 100D / testsCount;
            successDensity = 100D - density;
        }
        saveMeasureOnFile(context, inputFile, SOAtestMetrics.SOATEST_TEST_SUCCESS_DENSITY, successDensity);

        UnitTestSummary unitTestSummaryForFile = new UnitTestSummary(testsCount, failureTestsCount, duration);

        Logger.getLogger().info(NLS.getFormatted(Messages.CollectedUnitTestsForFile, inputFile));
        Logger.getLogger().info(unitTestSummaryForFile);

        return unitTestSummaryForFile;
    }
}
