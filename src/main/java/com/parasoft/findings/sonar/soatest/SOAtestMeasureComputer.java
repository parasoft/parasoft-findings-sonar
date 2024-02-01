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

import static com.parasoft.findings.sonar.soatest.SOAtestMetrics.*;

public class SOAtestMeasureComputer implements MeasureComputer {

    private static final String[] INPUT_METRICS =
            { SOATEST_TESTS_KEY, SOATEST_TEST_FAILURES_KEY, SOATEST_TEST_EXECUTION_TIME_KEY };
    private static final String[] OUTPUT_METRICS =
            { SOATEST_TESTS_KEY, SOATEST_TEST_FAILURES_KEY, SOATEST_TEST_SUCCESS_DENSITY_KEY,
                    SOATEST_TEST_EXECUTION_TIME_KEY };

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
        return defContext.newDefinitionBuilder()
                .setInputMetrics(INPUT_METRICS)
                .setOutputMetrics(OUTPUT_METRICS).build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        // measures are calculated and saved in files by SOAtestXUnitParser in scanner stack
        if (context.getComponent().getType() != Component.Type.FILE) {
            int totalTests = sumAndAddMeasure(context, SOATEST_TESTS_KEY);
            int totalFailures = sumAndAddMeasure(context, SOATEST_TEST_FAILURES_KEY);

            if (totalTests > 0 && totalFailures >= 0) {
                double density = totalFailures * 100D / totalTests;
                context.addMeasure(SOATEST_TEST_SUCCESS_DENSITY_KEY, 100D - density);
            }

            sumAndAddExecutionTimeMeasure(context);
        }
    }

    private static int sumAndAddMeasure(MeasureComputerContext context, String metric) {
        int sum = 0;
        for (Measure child : context.getChildrenMeasures(metric)) {
            sum += child.getIntValue();
        }
        context.addMeasure(metric, sum);
        return sum;
    }

    private static void sumAndAddExecutionTimeMeasure(MeasureComputerContext context) {
        long sum = 0;
        for (Measure child : context.getChildrenMeasures(SOATEST_TEST_EXECUTION_TIME_KEY)) {
            sum += child.getLongValue();
        }
        context.addMeasure(SOATEST_TEST_EXECUTION_TIME_KEY, sum);
    }
}
