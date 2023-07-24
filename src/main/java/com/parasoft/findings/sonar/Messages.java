/*
 * (C) Copyright ParaSoft Corporation 2022. All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft The copyright notice above
 * does not evidence any actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar;

import com.parasoft.xtest.common.nls.NLS;

public final class Messages
    extends NLS
{
    public static String CppTestReportPathDesc;
    public static String CppTestReportPathName;
    public static String CreatedIssues;
    public static String JtestName;
    public static String JtestReportPathDesc;
    public static String JtestReportPathName;
    public static String dotTestName;
    public static String dotTestReportPathDesc;
    public static String dotTestReportPathName;
    public static String FindingsImported;
    public static String NoDescription;
    public static String NoFindingsFor;
    public static String NoReportFile;
    public static String NoSourcesFound;
    public static String NotLoadingRules;
    public static String ParsingReportFile;
    public static String PluginName;
    public static String RuleDocNotFound;
    public static String RuleDocNotFoundAt;
    public static String RuleNotFound;
    public static String UnknownRuleName;
    public static String UnsupportedFindingLanguage;
    public static String CoverageReportPathName;
    public static String CoverageReportPathDesc;
    public static String CoverageSubCategory;

    static {
        // initialize resource bundle
        NLS.initMessages(Messages.class);
    }

    private Messages()
    {}
}
