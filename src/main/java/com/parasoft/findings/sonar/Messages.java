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
    public static String JtestReportPathDesc;
    public static String JtestReportPathName;
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
    public static String UploadedCodeCoverageData;
    public static String SkippedInvalidReportFile;
    public static String TransformedReport;
    public static String FailedToTransformReport;
    public static String FailedToLoadCoberturaReport;
    public static String FileNotFoundInProject;
    public static String NoValidReportsFound;
    public static String InvalidCoberturaCoverageReport;
    public static String SkippedInvalidReport;
    public static String NotMatchedCoverageReportAndProject;
    public static String NoValidCoberturaReport;
    public static String SOAtestReportPathName;
    public static String SOAtestReportPathDesc;
    public static String TransformingParasoftReportsToXUnitReports;
    public static String TransformingCoverageReportsToCoberturaReports;
    public static String TransformingReport;
    public static String ParsingXUnitReport;
    public static String FailedToParseXUnitReport;
    public static String ResourceNotFound;
    public static String ParasoftReportNotSpecified;
    public static String UnitTestResults;
    public static String SkipAddingUnitTestResultsForFile;
    public static String AddedUnitTestsForProjectSummary;
    public static String AddedSOAtestTestsForProjectSummary;
    public static String AddedUnitTestResultsForFile;
    public static String AddedSOAtestTestResultsForFile;
    public static String UnitTest;
    public static String SOAtest;
    public static String Coverage;
    public static String StaticAnalysis;

    static {
        // initialize resource bundle
        NLS.initMessages(Messages.class);
    }

    private Messages()
    {}
}
