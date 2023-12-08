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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.parasoft.findings.utils.common.nls.NLS;
import com.parasoft.findings.utils.common.util.CollectionUtil;
import com.parasoft.findings.utils.common.util.IOUtils;
import com.parasoft.findings.utils.common.logging.FindingsLogger;
import com.parasoft.findings.utils.results.testableinput.IFileTestableInput;
import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import com.parasoft.findings.utils.results.violations.*;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.SonarLoggerHandlerFactory;
import com.parasoft.findings.sonar.SonarServicesProvider;

/**
 * A parser for Parasoft files containing xml report.
 */
public class ParasoftFindingsParser
{
    private final Properties _properties;

    private XmlReportViolationsImporter _importer = null;

    private final Map<String, Set<IRuleViolation>> _violations = Collections.synchronizedMap(new HashMap<String, Set<IRuleViolation>>());

    /**
     * Creates a new instance of {@link ParasoftFindingsParser}.
     */
    public ParasoftFindingsParser(Properties properties)
    {
        _properties = properties;
        SonarServicesProvider.getInstance();
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
            Severity severity = mapToSonarSeverity(attributes.getSeverity());
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
                if (severity != null) {
                    newIssue = newIssue.overrideSeverity(severity);
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

    public static Severity mapToSonarSeverity(int severity)
    {
        switch (severity) {
            case ViolationRuleUtil.SEVERITY_HIGHEST:
                return Severity.BLOCKER;
            case ViolationRuleUtil.SEVERITY_HIGH:
                return Severity.CRITICAL;
            case ViolationRuleUtil.SEVERITY_MEDIUM:
                return Severity.MAJOR;
            case ViolationRuleUtil.SEVERITY_LOWEST:
                return Severity.INFO;
            case ViolationRuleUtil.SEVERITY_LOW:
            default:
                return Severity.MINOR;
        }
    }

    private synchronized XmlReportViolationsImporter getImporter()
    {
        if (_importer == null) {
            _importer = new XmlReportViolationsImporter(_properties);
        }
        return _importer;
    }
}