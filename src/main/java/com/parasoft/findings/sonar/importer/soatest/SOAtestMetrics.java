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

package com.parasoft.findings.sonar.importer.soatest;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public class SOAtestMetrics implements Metrics {

    public static String DOMAIN_PARASOFT = "Parasoft"; //$NON-NLS-1$

    public static final String SOATEST_TESTS_KEY = "parasoft_soatest_tests"; //$NON-NLS-1$

    public static final Metric<Integer> SOATEST_TESTS =
            new Metric.Builder(SOATEST_TESTS_KEY, "SOAtest Tests", Metric.ValueType.INT) //$NON-NLS-1$
                    .setDescription("Number of SOAtest tests") //$NON-NLS-1$
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(false)
                    .setDomain(DOMAIN_PARASOFT)
                    .create();

    public static final String SOATEST_TEST_FAILURES_KEY = "parasoft_soatest_test_failures"; //$NON-NLS-1$
    public static final Metric<Integer> SOATEST_TEST_FAILURES =
            new Metric.Builder(SOATEST_TEST_FAILURES_KEY, "SOAtest Test Failures", Metric.ValueType.INT) //$NON-NLS-1$
                    .setDescription("Number of SOAtest test failures") //$NON-NLS-1$
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setBestValue(0.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String SOATEST_TEST_SUCCESS_DENSITY_KEY = "parasoft_soatest_test_success_density"; //$NON-NLS-1$
    public static final Metric<Double> SOATEST_TEST_SUCCESS_DENSITY =
            new Metric.Builder(SOATEST_TEST_SUCCESS_DENSITY_KEY, "SOAtest Test Success", Metric.ValueType.PERCENT) //$NON-NLS-1$
                    .setDescription("Density of successful SOAtest tests") //$NON-NLS-1$
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setWorstValue(0.0)
                    .setBestValue(100.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String SOATEST_TEST_EXECUTION_TIME_KEY = "parasoft_soatest_test_execution_time"; //$NON-NLS-1$
    public static final Metric<Long> SOATEST_TEST_EXECUTION_TIME =
            new Metric.Builder(SOATEST_TEST_EXECUTION_TIME_KEY, "SOAtest Test Duration", Metric.ValueType.MILLISEC) //$NON-NLS-1$
                    .setDescription("Execution duration of SOAtest tests") //$NON-NLS-1$
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(false)
                    .setDomain(DOMAIN_PARASOFT)
                    .create();

    @Override
    public List<Metric> getMetrics() {
        return Arrays.asList(SOATEST_TESTS, SOATEST_TEST_FAILURES, SOATEST_TEST_SUCCESS_DENSITY, SOATEST_TEST_EXECUTION_TIME);
    }
}
