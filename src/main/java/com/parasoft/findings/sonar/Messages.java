/*
 * (C) Copyright ParaSoft Corporation 2022. All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft The copyright notice above
 * does not evidence any actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar;

import com.parasoft.findings.utils.common.nls.NLS;

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
    public static String UploadCodeCoverageData;
    public static String InvalidReportFile;
    public static String TransformReportToCoberturaFormat;
    public static String TransformReportToXUnitFormat;
    public static String FailedToTransformReport;
    public static String FailedToLoadCoberturaReport;
    public static String FailedToLoadReport;
    public static String FileNotFoundInProject;
    public static String NoValidCoverageReportsFound;
    public static String NoValidSOAtestReportsFound;
    public static String InvalidCoberturaCoverageReport;
    public static String InvalidReport;
    public static String NotMatchedCoverageReportAndProject;
    public static String NoValidCoberturaReport;
    public static String SOAtestReportPathName;
    public static String SOAtestReportPathDesc;
    public static String ConvertingSOAtestReportsToXUnitReports;
    public static String ParsingXUnitReports;
    public static String FailedToParseXUnitReport;
    public static String TotalDurationNotAccurateWithNegativeTimeTests;
    public static String ResourceNotFound;

    static {
        // initialize resource bundle
        NLS.initMessages(Messages.class);
    }

    private Messages()
    {}
}
