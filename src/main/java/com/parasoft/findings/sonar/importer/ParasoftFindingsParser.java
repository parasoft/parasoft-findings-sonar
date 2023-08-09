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

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

import com.parasoft.xtest.common.ISeverityConsts;
import com.parasoft.xtest.common.api.ITestableInput;
import com.parasoft.xtest.common.collections.UCollection;
import com.parasoft.xtest.common.io.IOUtils;
import com.parasoft.xtest.common.nls.NLS;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.SonarServicesProvider;
import com.parasoft.xtest.results.api.IResultLocation;
import com.parasoft.xtest.results.api.IRuleViolation;
import com.parasoft.xtest.results.api.IViolation;
import com.parasoft.xtest.results.api.importer.IImportedData;

/**
 * A parser for Parasoft files containing xml report.
 */
public class ParasoftFindingsParser
{
    private final Properties _properties;

    private SonarResultsImporter _importer = null;

    private final Map<String, Set<IRuleViolation>> _violations = Collections.synchronizedMap(new HashMap<String, Set<IRuleViolation>>());

    /**
     * Creates a new instance of {@link ParasoftFindingsParser}.
     */
    public ParasoftFindingsParser(Properties properties)
    {
        _properties = properties;
        SonarServicesProvider.getInstance();
        Logger.getLogger().info("Service initialization"); //$NON-NLS-1$
    }

    public int loadFindings(File file)
    {
        FileInputStream input = null;
        int loadedFindings = 0;
        try {
            input = new FileInputStream(file);

            IImportedData importedData = getImporter().performImport(file);
            while (importedData.hasNext()) {
                IViolation result = importedData.next();
                IRuleViolation violation;
                if (result instanceof IRuleViolation) {
                    violation = (IRuleViolation)result;
                } else {
                    Logger.getLogger().error("Result is not instance of IRuleViolation"); //$NON-NLS-1$
                    continue;
                }
                IResultLocation location = violation.getResultLocation();
                ITestableInput testableInput = location != null ? location.getTestableInput() : null;
                String inputName = testableInput != null ? testableInput.getName() : null;
                if (inputName == null) {
                    continue;
                }
                var violations = _violations.get(inputName);
                if (violations == null) {
                    violations = new HashSet<>();
                    _violations.put(inputName, violations);
                    Logger.getLogger().info("Collected location with finding(s): " + inputName); //$NON-NLS-1$
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

    public Set<IRuleViolation> getFindings(String name)
    {
        return _violations.get(name);
    }

    public int createNewIssues(InputFile javaFile, ParasoftProduct product, SensorContext context)
    {
        ActiveRules activeRules = context.activeRules();
        String fileName = javaFile.filename();
        var findings = getFindings(fileName);
        if (UCollection.isEmpty(findings)) {
            Logger.getLogger().info(NLS.bind(Messages.NoFindingsFor, fileName));
            return 0;
        }
        int findingsCount = 0;
        for (IRuleViolation finding : findings) {
            var languageIdx = product.getLanguageIndex(finding);
            if (languageIdx < 0) {
                Logger.getLogger().warn(NLS.bind(Messages.UnsupportedFindingLanguage, finding.getRuleId(), finding.getLanguageId()));
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
                NewIssueLocation primaryLocation = newIssue.newLocation().on(javaFile)
                    .at(javaFile.selectLine(finding.getResultLocation().getSourceRange().getStartLine())).message(finding.getMessage());
                newIssue.at(primaryLocation);
                if (severity != null) {
                    newIssue = newIssue.overrideSeverity(severity);
                }
                new PathBuilder(context, newIssue).setPath(finding);
                newIssue.save();
                findingsCount++;
            } else {
                Logger.getLogger().warn(NLS.bind(Messages.RuleNotFound, ruleId, ruleRepoId));
            }
        }
        return findingsCount;
    }

    public static Severity mapToSonarSeverity(int severity)
    {
        switch (severity) {
            case ISeverityConsts.SEVERITY_HIGHEST:
                return Severity.BLOCKER;
            case ISeverityConsts.SEVERITY_HIGH:
                return Severity.CRITICAL;
            case ISeverityConsts.SEVERITY_MEDIUM:
                return Severity.MAJOR;
            case ISeverityConsts.SEVERITY_LOW:
                return Severity.MINOR;
            case ISeverityConsts.SEVERITY_LOWEST:
                return Severity.INFO;
            default:
                return Severity.MINOR;
        }
    }

    private synchronized SonarResultsImporter getImporter()
    {
        if (_importer == null) {
            _importer = new SonarResultsImporter(_properties);
        }
        return _importer;
    }
}