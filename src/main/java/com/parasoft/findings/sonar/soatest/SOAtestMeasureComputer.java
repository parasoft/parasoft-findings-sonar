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

package com.parasoft.findings.sonar.soatest;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

import static com.parasoft.findings.sonar.soatest.ParasoftMetrics.*;

public class SOAtestMeasureComputer implements MeasureComputer {

    private static final String[] INPUT_METRICS =
            { SOATEST_TESTS_KEY, SOATEST_TEST_ERRORS_KEY, SOATEST_TEST_FAILURES_KEY,
            SKIPPED_SOATEST_TESTS_KEY, SOATEST_TEST_EXECUTION_TIME_KEY };
    private static final String[] OUTPUT_METRICS =
            { SOATEST_TESTS_KEY, SOATEST_TEST_ERRORS_KEY, SOATEST_TEST_FAILURES_KEY,
            SKIPPED_SOATEST_TESTS_KEY, SOATEST_TEST_SUCCESS_DENSITY_KEY, SOATEST_TEST_EXECUTION_TIME_KEY };

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
        return defContext.newDefinitionBuilder()
                .setInputMetrics(INPUT_METRICS)
                .setOutputMetrics(OUTPUT_METRICS).build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
         // measure is already defined on files by XUnitSOAtestParser in scanner stack
        if (context.getComponent().getType() != Component.Type.FILE) {
            int sumTests = createAggregatedIntMeasure(context, SOATEST_TESTS_KEY);
            int sumTestErrors = createAggregatedIntMeasure(context, SOATEST_TEST_ERRORS_KEY);
            int sumTestFailures = createAggregatedIntMeasure(context, SOATEST_TEST_FAILURES_KEY);
            createAggregatedIntMeasure(context, SKIPPED_SOATEST_TESTS_KEY);

            if (sumTests > 0 && sumTestErrors >= 0 && sumTestFailures >= 0) {
                double density = (sumTestErrors + sumTestFailures) * 100D / sumTests;
                context.addMeasure(SOATEST_TEST_SUCCESS_DENSITY_KEY, 100D - density);
            }

            createAggregatedLongMeasure(context, SOATEST_TEST_EXECUTION_TIME_KEY);
        }
    }

    private static int createAggregatedIntMeasure(MeasureComputerContext context, String metric) {
        int sum = 0;
        for (Measure child : context.getChildrenMeasures(metric)) {
            sum += child.getIntValue();
        }
        context.addMeasure(metric, sum);
        return sum;
    }

    private static void createAggregatedLongMeasure(MeasureComputerContext context, String metric) {
        long sum = 0;
        for (Measure child : context.getChildrenMeasures(metric)) {
            sum += child.getLongValue();
        }
        context.addMeasure(metric, sum);
    }
}
