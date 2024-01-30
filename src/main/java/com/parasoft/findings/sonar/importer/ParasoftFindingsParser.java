/*
 * Copyright 2018 Parasoft Corporation
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.parasoft.findings.sonar.*;
import com.parasoft.findings.utils.common.nls.NLS;
import com.parasoft.findings.utils.common.util.CollectionUtil;
import com.parasoft.findings.utils.common.util.IOUtils;
import com.parasoft.findings.utils.common.logging.FindingsLogger;
import com.parasoft.findings.utils.results.testableinput.IFileTestableInput;
import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import com.parasoft.findings.utils.results.violations.*;

import org.dom4j.Element;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;

import java.io.Serializable;

/**
 * A parser for Parasoft files containing xml report.
 */
public class ParasoftFindingsParser
{
    private final Properties _properties;

    private XmlReportViolationsImporter _importer = null;

    private final Map<String, Set<IRuleViolation>> _violations = Collections.synchronizedMap(new HashMap<>());

    /**
     * Creates a new instance of {@link ParasoftFindingsParser}.
     */
    public ParasoftFindingsParser(Properties properties)
    {
        _properties = properties;
        FindingsLogger.setCurrentFactory(new SonarLoggerHandlerFactory());
        Logger.getLogger().info("Service initialization"); //$NON-NLS-1$
    }

    public int loadFindings(File file)
    {
        FileInputStream input = null;
        int loadedFindings = 0;
        try {
            input = new FileInputStream(file);

            XmlReportViolations importedData = getImporter().performImport(file);
            while (importedData.hasNext()) {
                IViolation result = importedData.next();
                IRuleViolation violation;
                if (result instanceof IRuleViolation) {
                    violation = (IRuleViolation)result;
                } else {
                    Logger.getLogger().error("Result is not instance of IRuleViolation"); //$NON-NLS-1$
                    continue;
                }
                ResultLocation location = violation.getResultLocation();
                ITestableInput testableInput = location != null ? location.getTestableInput() : null;
                String inputPath = null;
                if (testableInput != null) {
                    if (testableInput instanceof IFileTestableInput) {
                        inputPath = ((IFileTestableInput) testableInput).getFileLocation().toURI().getPath();
                    } else {
                        Logger.getLogger().error("Input is not instance of IFileTestableInput but " + testableInput.getClass().getSimpleName()); //$NON-NLS-1$
                    }
                }
                if (inputPath == null) {
                    continue;
                }
                var violations = _violations.get(inputPath);
                if (violations == null) {
                    violations = new HashSet<>();
                    _violations.put(inputPath, violations);
                    Logger.getLogger().info("Collected location with finding(s): " + inputPath); //$NON-NLS-1$
                }
                violations.add(violation);
                loadedFindings++;
            }

        } catch (FileNotFoundException exception) {
            Logger.getLogger().error(exception.getMessage(), exception);
        } finally {
            IOUtils.close(input);
        }
        return loadedFindings;
    }

    public Set<IRuleViolation> getFindings(String inputFilePath)
    {
        return _violations.get(inputFilePath);
    }

    public int createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
    {
        ActiveRules activeRules = context.activeRules();
        String inputFilePath = sourceFile.uri().getPath();
        var findings = getFindings(inputFilePath);
        if (CollectionUtil.isEmpty(findings)) {
            Logger.getLogger().info(NLS.getFormatted(Messages.NoFindingsFor, sourceFile.toString()));
            return 0;
        }
        int findingsCount = 0;
        for (IRuleViolation finding : findings) {
            var languageIdx = product.getLanguageIndex(finding);
            if (languageIdx < 0) {
                Logger.getLogger().warn(NLS.getFormatted(Messages.UnsupportedFindingLanguage, finding.getRuleId(), finding.getLanguageId()));
                continue;
            }
            var ruleRepoId = product.ruleRepoIds.get(languageIdx);
            ResultAdditionalAttributes attributes = new ResultAdditionalAttributes(finding);
            if (attributes.isSuppressed()) {
                continue;
            }

            String ruleId = finding.getRuleId();
            RuleKey ruleKey = RuleKey.of(ruleRepoId, ruleId);
            if (activeRules.find(ruleKey) == null) {
                ruleId = ruleId.replace('-', '.');
                ruleKey = RuleKey.of(ruleRepoId, ruleId);
                if (activeRules.find(ruleKey) == null) {
                    ruleKey = RuleKey.of(ruleRepoId, ParasoftConstants.UNKNOWN_RULE_ID);
                    if (activeRules.find(ruleKey) == null) {
                        ruleKey = null;
                    }
                }
            }
            if (ruleKey != null) {
                NewIssue newIssue = context.newIssue().forRule(ruleKey);
                NewIssueLocation primaryLocation = newIssue.newLocation().on(sourceFile)
                    .at(sourceFile.selectLine(finding.getResultLocation().getSourceRange().getStartLine())).message(finding.getMessage());
                newIssue.at(primaryLocation);

                var severity = attributes.getSeverity();
                var ruleCategory = attributes.getRuleCategory();
                var isSev1 = severity == ViolationRuleUtil.SEVERITY_HIGHEST;
                var isSecurityHotspot = MapperUtil.isSecurityHotspot(ruleCategory, isSev1);
                // When the runtime SONAR API version is lower than 10.1 or the rule is a security hotspot, use deprecated overrideSeverity(Severity)
                if (!SonarAPIVersionUtil.isAPIVersionAtLeast10_1(context.runtime()) || isSecurityHotspot) {
                    newIssue = newIssue.overrideSeverity(MapperUtil.mapToSonarSeverity(severity));
                } else {
                    var impactSoftwareQuality = MapperUtil.mapToSonarImpactSoftwareQuality(ruleCategory, isSev1);
                    var impactSeverity = MapperUtil.mapToSonarImpactSeverity(severity);
                    newIssue = newIssue.overrideImpact(impactSoftwareQuality, impactSeverity);
                }
                new PathBuilder(context, newIssue).setPath(finding);
                newIssue.save();
                findingsCount++;
            } else {
                Logger.getLogger().warn(NLS.getFormatted(Messages.RuleNotFound, ruleId, ruleRepoId));
            }
        }
        return findingsCount;
    }

    private synchronized XmlReportViolationsImporter getImporter()
    {
        if (_importer == null) {
            _importer = new XmlReportViolationsImporter(_properties);
        }
        return _importer;
    }

    public void loadTestResults(Element rootElement, UnitTestResult unitTestResult) {
        Element executedTestsDetailsElement = findExecutedTestsDetailsElement(rootElement);
        Element totalElement = (executedTestsDetailsElement != null) ? executedTestsDetailsElement.element("Total") : null;
        if (totalElement != null) {
            unitTestResult.setTotalTests(parseIntOrDefault(totalElement.attributeValue("total")));
            unitTestResult.setErrors(parseIntOrDefault(totalElement.attributeValue("err")));
            unitTestResult.setFailures(parseIntOrDefault(totalElement.attributeValue("fail")));
            unitTestResult.setDuration(getTimeAttributeInMS(totalElement.attributeValue("time")));
        }
    }

    public void saveMeasures(SensorContext context, UnitTestResult unitTestResult) {
        if (unitTestResult.getTotalTests() > 0) {
            saveMeasure(context, CoreMetrics.TESTS, unitTestResult.getTotalTests());
            saveMeasure(context, CoreMetrics.TEST_ERRORS, unitTestResult.getErrors());
            saveMeasure(context, CoreMetrics.TEST_FAILURES, unitTestResult.getFailures());
            if (unitTestResult.getDuration() > 0) {
                saveMeasure(context, CoreMetrics.TEST_EXECUTION_TIME, unitTestResult.getDuration());
            }
        }
    }

    // For cppTest professional report, "ExecutedTestsDetails" node is under root element.
    private Element findExecutedTestsDetailsElement(Element rootElement) {
        return (rootElement.element("ExecutedTestsDetails") != null) ? rootElement.element("ExecutedTestsDetails") : rootElement.element("Exec").element("ExecutedTestsDetails");
    }

    private int parseIntOrDefault(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    //  Get a time attribute in milliseconds
    private long getTimeAttributeInMS(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm:ss.SSS");
        LocalTime localTime = LocalTime.parse(value, formatter);
        return localTime.toNanoOfDay() / 1_000_000;
    }

    // Create a new measure for your specified metric through using SonarQube's Measure API
    private <T extends Serializable> void saveMeasure(SensorContext context, Metric<T> metric, T value) {
        context.<T>newMeasure().forMetric(metric).on(context.project()).withValue(value).save();
    }
}