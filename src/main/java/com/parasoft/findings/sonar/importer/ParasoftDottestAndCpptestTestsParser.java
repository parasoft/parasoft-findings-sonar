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
import com.parasoft.findings.utils.common.nls.NLS;
import org.dom4j.Element;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ParasoftDottestAndCpptestTestsParser {

    public TestSummary loadTestResults(Element rootElement) {
        Element executedTestsDetailsElement = findExecutedTestsDetailsElement(rootElement);
        Element totalElement = (executedTestsDetailsElement != null) ? executedTestsDetailsElement.element("Total") : null;

        if (totalElement != null) {
            return new TestSummary(parseInt(totalElement.attributeValue("total"), 0),
                    parseInt(totalElement.attributeValue("fail"), 0),
                    parseInt(totalElement.attributeValue("err"), 0),
                    getTimeAttributeInMS(totalElement.attributeValue("time"), 0L));
        }
        return new TestSummary();
    }

    public void saveMeasuresOnProject(SensorContext context, TestSummary unitTestSummary) {
        if (unitTestSummary.getTotalTests() > 0) {
            saveMeasureOnProject(context, CoreMetrics.TESTS, unitTestSummary.getTotalTests());
            saveMeasureOnProject(context, CoreMetrics.TEST_ERRORS, unitTestSummary.getErrors());
            saveMeasureOnProject(context, CoreMetrics.TEST_FAILURES, unitTestSummary.getFailures());
            if (unitTestSummary.getDuration() > 0) {
                saveMeasureOnProject(context, CoreMetrics.TEST_EXECUTION_TIME, unitTestSummary.getDuration());
            }
        }
        Logger.getLogger().info(NLS.getFormatted(Messages.AddedUnitTestsForProjectSummary, unitTestSummary));
    }

    // For cppTest professional report, "ExecutedTestsDetails" node is under root element.
    private Element findExecutedTestsDetailsElement(Element rootElement) {
        return (rootElement.element("ExecutedTestsDetails") != null) ?
                rootElement.element("ExecutedTestsDetails") :
                rootElement.element("Exec").element("ExecutedTestsDetails");
    }

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    //  Get a time attribute in milliseconds
    private long getTimeAttributeInMS(String value, long defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm:ss.SSS");
            LocalTime localTime = LocalTime.parse(value, formatter);
            return localTime.toNanoOfDay() / 1_000_000;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Create a new measure for your specified metric through using SonarQube's Measure API
    private <T extends Serializable> void saveMeasureOnProject(SensorContext context, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(context.project()).withValue(value).save();
    }
}
