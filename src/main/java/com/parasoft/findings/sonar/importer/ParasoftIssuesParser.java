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

import com.parasoft.findings.sonar.*;
import com.parasoft.findings.sonar.utils.MapperUtil;
import com.parasoft.findings.sonar.utils.SonarAPIVersionUtil;
import com.parasoft.findings.utils.common.nls.NLS;
import com.parasoft.findings.utils.common.util.CollectionUtil;
import com.parasoft.findings.utils.common.util.IOUtils;
import com.parasoft.findings.utils.common.logging.FindingsLogger;
import com.parasoft.findings.utils.results.testableinput.IFileTestableInput;
import com.parasoft.findings.utils.results.testableinput.ITestableInput;
import com.parasoft.findings.utils.results.violations.*;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * A parser for Parasoft files containing xml report.
 */
public class ParasoftIssuesParser
{
    private final Properties _properties;

    private XmlReportViolationsImporter _importer = null;

    private final Map<File, Set<IRuleViolation>> _violations = Collections.synchronizedMap(new HashMap<>());

    /**
     * Creates a new instance of {@link ParasoftIssuesParser}.
     */
    public ParasoftIssuesParser(Properties properties)
    {
        _properties = properties;
        FindingsLogger.setCurrentFactory(new SonarLoggerHandlerFactory());
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
                    Logger.getLogger().error(NLS.getFormatted(Messages.ResultIsNotInstanceOfIRuleViolation));
                    continue;
                }
                ResultLocation location = violation.getResultLocation();
                ITestableInput testableInput = location != null ? location.getTestableInput() : null;
                File inputFile = null;
                if (testableInput != null) {
                    if (testableInput instanceof IFileTestableInput) {
                        inputFile = new File(((IFileTestableInput) testableInput).getFileLocation().toURI().getPath()).getAbsoluteFile();
                    } else {
                        Logger.getLogger().error(NLS.getFormatted(Messages.InputIsNotInstanceOfIFileTestableInput, testableInput.getClass().getSimpleName()));
                    }
                }
                if (inputFile == null) {
                    continue;
                }
                var violations = _violations.get(inputFile);
                if (violations == null) {
                    violations = new HashSet<>();
                    _violations.put(inputFile, violations);
                    Logger.getLogger().info(NLS.getFormatted(Messages.AddedLocationWithFinding, inputFile));
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

    public Set<IRuleViolation> getFindings(File inputFile)
    {
        return _violations.get(inputFile);
    }

    public int createNewIssues(InputFile sourceFile, ParasoftProduct product, SensorContext context)
    {
        ActiveRules activeRules = context.activeRules();
        File source;
        if (sourceFile.isFile()) {
            source = new File(sourceFile.uri().getPath()).getAbsoluteFile();
        } else {
            return 0;
        }
        var findings = getFindings(source);
        if (CollectionUtil.isEmpty(findings)) {
            Logger.getLogger().info(NLS.getFormatted(Messages.NoFindingsFor, source.toString()));
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
}