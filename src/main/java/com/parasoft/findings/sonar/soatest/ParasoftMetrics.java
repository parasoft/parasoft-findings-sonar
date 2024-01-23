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

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public class ParasoftMetrics implements Metrics {

    public static String DOMAIN_PARASOFT = "Parasoft";

    public static final String FUNCTIONAL_TESTS_KEY = "parasoft_functional_tests";

    public static final Metric<Integer> FUNCTIONAL_TESTS =
            new Metric.Builder(FUNCTIONAL_TESTS_KEY, "Functional Tests", Metric.ValueType.INT)
                    .setDescription("Number of functional tests")
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(false)
                    .setDomain(DOMAIN_PARASOFT)
                    .create();

    public static final String FUNCTIONAL_TEST_ERRORS_KEY = "parasoft_functional_test_errors";
    public static final Metric<Integer> FUNCTIONAL_TEST_ERRORS =
            new Metric.Builder(FUNCTIONAL_TEST_ERRORS_KEY, "Functional Test Errors", Metric.ValueType.INT)
                    .setDescription("Number of functional test errors")
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setBestValue(0.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String FUNCTIONAL_TEST_FAILURES_KEY = "parasoft_functional_test_failures";
    public static final Metric<Integer> FUNCTIONAL_TEST_FAILURES =
            new Metric.Builder(FUNCTIONAL_TEST_FAILURES_KEY, "Functional Test Failures", Metric.ValueType.INT)
                    .setDescription("Number of functional test failures")
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setBestValue(0.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String SKIPPED_FUNCTIONAL_TESTS_KEY = "parasoft_skipped_functional_tests";
    public static final Metric<Integer> SKIPPED_FUNCTIONAL_TESTS =
            new Metric.Builder(SKIPPED_FUNCTIONAL_TESTS_KEY, "Skipped Functional Tests", Metric.ValueType.INT)
                    .setDescription("Number of skipped functional tests")
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setBestValue(0.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String FUNCTIONAL_TEST_SUCCESS_DENSITY_KEY = "parasoft_functional_test_success_density";
    public static final Metric<Double> FUNCTIONAL_TEST_SUCCESS_DENSITY =
            new Metric.Builder(FUNCTIONAL_TEST_SUCCESS_DENSITY_KEY, "Functional Test Success (%)", Metric.ValueType.PERCENT)
                    .setDescription("Density of successful functional tests")
                    .setDirection(Metric.DIRECTION_BETTER)
                    .setQualitative(true)
                    .setDomain(DOMAIN_PARASOFT)
                    .setWorstValue(0.0)
                    .setBestValue(100.0)
                    .setOptimizedBestValue(true)
                    .create();

    public static final String FUNCTIONAL_TEST_EXECUTION_TIME_KEY = "parasoft_functional_test_execution_time";
    public static final Metric<Long> FUNCTIONAL_TEST_EXECUTION_TIME =
            new Metric.Builder(FUNCTIONAL_TEST_EXECUTION_TIME_KEY, "Functional Test Duration", Metric.ValueType.MILLISEC)
                    .setDescription("Execution duration of functional tests")
                    .setDirection(Metric.DIRECTION_WORST)
                    .setQualitative(false)
                    .setDomain(DOMAIN_PARASOFT)
                    .create();
    
    @Override
    public List<Metric> getMetrics() {
        return Arrays.asList(FUNCTIONAL_TESTS, FUNCTIONAL_TEST_ERRORS, FUNCTIONAL_TEST_FAILURES,
                SKIPPED_FUNCTIONAL_TESTS, FUNCTIONAL_TEST_SUCCESS_DENSITY, FUNCTIONAL_TEST_EXECUTION_TIME);
    }
}
