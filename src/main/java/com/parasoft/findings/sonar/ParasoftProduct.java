/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar;

import com.parasoft.findings.sonar.rules.LanguageRules;
import com.parasoft.xtest.results.api.IRuleViolation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.parasoft.findings.sonar.rules.AbstractRulesDefinition.BUILTIN_RULES_DIR_NAME;

@SuppressWarnings("nls")
public enum ParasoftProduct {
    JTEST("Parasoft Jtest", "Java", Arrays.asList("java", "xml", "text"),
        Arrays.asList("parasoft-jtest-java", "parasoft-jtest-xml", "parasoft-jtest-text"), BUILTIN_RULES_DIR_NAME + "/jtest",
        "sonar.parasoft.jtest.reportPaths", Messages.JtestReportPathName, Messages.JtestReportPathDesc,
        "integration/dtpserver/jtest/model", "rules/doc",
        "com.parasoft.jtest.flowanalyzer.rules.builtin", "com.parasoft.jtest.flowanalyzer"),

    DOTTEST("Parasoft dotTest", "C#", Arrays.asList("cs", "vbnet"),
        Arrays.asList("parasoft-dottest-cs", "parasoft-dottest-vbnet"), BUILTIN_RULES_DIR_NAME + "/dottest",
        "sonar.parasoft.dottest.reportPaths", Messages.dotTestReportPathName, Messages.dotTestReportPathDesc,
        "integration/DTPServer/dotTEST/model", "rules/doc",
        "com.parasoft.dottest.flowanalyzer.rules.builtin", "com.parasoft.dottest.flowanalyzer"),

    CPPTEST("Parasoft C/C++Test", "C++", Arrays.asList("c", "cpp"),
        Arrays.asList("parasoft-cpptest-c", "parasoft-cpptest-cpp"), BUILTIN_RULES_DIR_NAME + "/cpptest",
        "sonar.parasoft.cpptest.reportPaths", Messages.CppTestReportPathName, Messages.CppTestReportPathDesc,
        "integration/dtpserver/cpptest/model", "rules/docs",
        "com.parasoft.cpptest.flowanalyzer.rules.builtin", "com.parasoft.cpptest.flowanalyzer");

    public final String profileName;
    public final String subcategory;
    public final List<String> languages;
    public final List<String> ruleRepoIds;
    public final String builtinRulesPath;
    public final String reportPathKey;
    public final String reportPathName;
    public final String reportPathDesc;
    public final String rulesPath;
    public final String docPath;
    public final String ruleProvider;
    public final String analyzerId;

    private ParasoftProduct(String profileName, String subcategory, List<String> languages, List<String> ruleRepoIds,
        String builtinRulesPath, String reportPathKey, String reportPathName,
        String reportPathDesc, String rulesPath, String docPath, String ruleProvider, String analyzerId)
    {
        this.profileName = profileName;
        this.subcategory = subcategory;
        this.languages = languages;
        this.ruleRepoIds = ruleRepoIds;
        this.builtinRulesPath = builtinRulesPath;
        this.reportPathKey = reportPathKey;
        this.reportPathName = reportPathName;
        this.reportPathDesc = reportPathDesc;
        this.rulesPath = rulesPath;
        this.docPath = docPath;
        this.ruleProvider = ruleProvider;
        this.analyzerId = analyzerId;
    }

    public Set<LanguageRules> getLanguageRules()
    {
        Set<LanguageRules> rules = new HashSet<>();
        for (int i = 0; i < languages.size(); i++) {
            rules.add(new LanguageRules(languages.get(i), ruleRepoIds.get(i)));
        }
        return rules;
    }

    public int getLanguageIndex(IRuleViolation finding)
    {
        String langId = finding.getLanguageId();
        if (languages.contains(langId)) {
            return languages.indexOf(langId);
        }
        String filename = finding.getResultLocation().getTestableInput().getName();
        String extn = filename.substring(filename.lastIndexOf('.')+1);
        if (languages.contains(extn)) {
            return languages.indexOf(extn);
        }
        if ("dotnet".equals(langId) && "vb".equals(extn)) {
            // Note: if langId is 'dotnet', languages contains 'cs' above
            return languages.indexOf("vbnet");
        }
        return -1;
    }
}
