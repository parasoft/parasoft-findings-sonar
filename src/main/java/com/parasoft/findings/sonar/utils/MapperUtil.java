package com.parasoft.findings.sonar.utils;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.utils.results.violations.ViolationRuleUtil;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;

import java.util.HashMap;
import java.util.Map;

public class MapperUtil {

    /**
     * This method should only be called when using sonar-plugin-api version 10.1 and above.
     * since the {@link org.sonar.api.issue.impact.Severity} enum was introduced in version 10.1.
     */
    public static Severity mapToSonarImpactSeverity(int severity) {
        switch (severity) {
            case ViolationRuleUtil.SEVERITY_HIGHEST:
            case ViolationRuleUtil.SEVERITY_HIGH:
                return Severity.HIGH;
            case ViolationRuleUtil.SEVERITY_MEDIUM:
                return Severity.MEDIUM;
            case ViolationRuleUtil.SEVERITY_LOWEST:
            case ViolationRuleUtil.SEVERITY_LOW:
            default:
                return Severity.LOW;
        }
    }

    public static String categoryToTag(String category) {
        return category.toLowerCase().replace('_', '-');
    }

    public static boolean isSecurityHotspot(String category, boolean isSev1) {
        String tag = categoryToTag(category);
        return !isSev1 && (tag.contains("security") || //$NON-NLS-1$
                tag.startsWith("owasp") || //$NON-NLS-1$
                tag.startsWith("cwe") || //$NON-NLS-1$
                tag.startsWith("pcidss") || //$NON-NLS-1$
                tag.startsWith("apsc") || //$NON-NLS-1$
                tag.startsWith("cert")); //$NON-NLS-1$
    }

    /**
     * This method should only be called when using sonar-plugin-api version 10.1 and above.
     * since the {@link org.sonar.api.issue.impact.SoftwareQuality} enum was introduced in version 10.1.
     */
    public static SoftwareQuality mapToSonarImpactSoftwareQuality(String category, boolean isSev1) {
        String tag = categoryToTag(category);
        if (isSev1 && (tag.contains("security") || //$NON-NLS-1$
                tag.startsWith("owasp") || //$NON-NLS-1$
                tag.startsWith("cwe") || //$NON-NLS-1$
                tag.startsWith("pcidss") || //$NON-NLS-1$
                tag.startsWith("apsc") || //$NON-NLS-1$
                tag.startsWith("cert"))) { //$NON-NLS-1$
            return SoftwareQuality.SECURITY;
        } else if (isSev1 ||
                tag.startsWith("bd") || //$NON-NLS-1$
                tag.startsWith("pb") || //$NON-NLS-1$
                tag.startsWith("cs.pb") || //$NON-NLS-1$
                tag.startsWith("vb.pb")) { //$NON-NLS-1$
            return SoftwareQuality.RELIABILITY;
        }
        return SoftwareQuality.MAINTAINABILITY;
    }

    /**
     * This method should only be called when using sonar-plugin-api version 10.1 and above.
     * since the {@link org.sonar.api.rules.CleanCodeAttribute} enum was introduced in version 10.1.
     */
    public static CleanCodeAttribute mapToSonarCleanCodeAttribute(String category) {
        String tag = categoryToTag(category);
        String[] splitTag = tag.split("\\.");
        if (TagPrefixMap.TAG_PREFIX_MAP.containsKey(splitTag[0])) {
            return TagPrefixMap.TAG_PREFIX_MAP.get(splitTag[0]);
        } else {
            Logger.getLogger().debug("The category '" + category + "' has been mapped to 'CONVENTIONAL' because it does not match any existing categories in the plugin."); //$NON-NLS-1$  //$NON-NLS-2$
            return CleanCodeAttribute.CONVENTIONAL;
        }
    }

    /**
     * @deprecated use {@link MapperUtil#mapToSonarImpactSeverity(int)} instead
     */
    @Deprecated
    public static org.sonar.api.batch.rule.Severity mapToSonarSeverity(int severity) {
        switch (severity) {
            case ViolationRuleUtil.SEVERITY_HIGHEST:
                return org.sonar.api.batch.rule.Severity.BLOCKER;
            case ViolationRuleUtil.SEVERITY_HIGH:
                return org.sonar.api.batch.rule.Severity.CRITICAL;
            case ViolationRuleUtil.SEVERITY_MEDIUM:
                return org.sonar.api.batch.rule.Severity.MAJOR;
            case ViolationRuleUtil.SEVERITY_LOWEST:
                return org.sonar.api.batch.rule.Severity.INFO;
            case ViolationRuleUtil.SEVERITY_LOW:
            default:
                return org.sonar.api.batch.rule.Severity.MINOR;
        }
    }

    /**
     * @deprecated use {@link MapperUtil#mapToSonarImpactSoftwareQuality(String, boolean)} (int)} instead
     */
    @Deprecated
    @SuppressWarnings("nls")
    public static RuleType mapToSonarRuleType(String category, boolean isSev1) {
        String tag = categoryToTag(category);
        if (tag.contains("security") ||
                tag.startsWith("owasp") ||
                tag.startsWith("cwe") ||
                tag.startsWith("pcidss") ||
                tag.startsWith("apsc") ||
                tag.startsWith("cert")) {
            return isSev1 ? RuleType.VULNERABILITY : RuleType.SECURITY_HOTSPOT;
        } else if (isSev1 ||
                tag.startsWith("bd") ||
                tag.startsWith("pb") ||
                tag.startsWith("cs.pb") ||
                tag.startsWith("vb.pb")) {
            return RuleType.BUG;
        }
        return RuleType.CODE_SMELL;
    }

    /**
     * This class should only be used when using sonar-plugin-api version 10.1 and above,
     * since the {@link org.sonar.api.rules.CleanCodeAttribute} enum was introduced in version 10.1.
     */
    private static class TagPrefixMap {
        private static final Map<String, CleanCodeAttribute> TAG_PREFIX_MAP = new HashMap<>();

        static {
            //The following three attributes belong to the CONSISTENT category
            //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#consistent
            //FORMATTED
            TAG_PREFIX_MAP.put("format", CleanCodeAttribute.FORMATTED); //$NON-NLS-1$

            //CONVENTIONAL
            TAG_PREFIX_MAP.put("apsc-dv", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("arru", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("autosar", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("bean", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cert", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cert-c", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cert-cpp", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cls", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cmug", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("codsta", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("com", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("comment", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cs", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("ct", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("cwe", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("dbc", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("eclipse", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("ejb", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("gbj5369", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("gjb8114", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("global", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("hibernate", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("hicpp", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("inter", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("javadoc", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("jdbc", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("jsf", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("junit", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misra", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misra2004", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misra2008", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misra2012", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misrac2012", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misracpp2023", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("misracpp202x", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("opu", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("owasp2017", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("owasp2021", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("owasp2019", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("owasp2023", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("owasp-asvs-403", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("parser", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("pcidss32", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("pcidss40", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("pfo", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("port", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("props", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("qt", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("roslyn", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("sec", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("security", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("serial", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("servlet", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("spr", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("spring", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("stl", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("struts", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("templ", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("tug", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("vb", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("vvsg", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("wpf", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("xml", CleanCodeAttribute.CONVENTIONAL); //$NON-NLS-1$

            //IDENTIFIABLE
            TAG_PREFIX_MAP.put("naming", CleanCodeAttribute.IDENTIFIABLE); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("ng", CleanCodeAttribute.IDENTIFIABLE); //$NON-NLS-1$


            //The following four attributes belong to the INTENTIONAL category
            //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#intentional
            //CLEAR
            TAG_PREFIX_MAP.put("brm", CleanCodeAttribute.CLEAR); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("preproc", CleanCodeAttribute.CLEAR); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("uc", CleanCodeAttribute.CLEAR); //$NON-NLS-1$

            //LOGICAL
            TAG_PREFIX_MAP.put("bd", CleanCodeAttribute.LOGICAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("err", CleanCodeAttribute.LOGICAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("except", CleanCodeAttribute.LOGICAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("pb", CleanCodeAttribute.LOGICAL); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("trs", CleanCodeAttribute.LOGICAL); //$NON-NLS-1$

            //COMPLETE
            TAG_PREFIX_MAP.put("init", CleanCodeAttribute.COMPLETE); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("ifd", CleanCodeAttribute.COMPLETE); //$NON-NLS-1$

            //EFFICIENT
            TAG_PREFIX_MAP.put("gc", CleanCodeAttribute.EFFICIENT); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("mobile", CleanCodeAttribute.EFFICIENT); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("mrm", CleanCodeAttribute.EFFICIENT); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("opt", CleanCodeAttribute.EFFICIENT); //$NON-NLS-1$


            //The following three attributes belong to the ADAPTABLE category
            //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#adaptable
            //FOCUSED
            TAG_PREFIX_MAP.put("metrics", CleanCodeAttribute.FOCUSED); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("metric", CleanCodeAttribute.FOCUSED); //$NON-NLS-1$

            //DISTINCT
            TAG_PREFIX_MAP.put("cdd", CleanCodeAttribute.DISTINCT); //$NON-NLS-1$

            //MODULAR
            TAG_PREFIX_MAP.put("oom", CleanCodeAttribute.MODULAR); //$NON-NLS-1$
            TAG_PREFIX_MAP.put("oop", CleanCodeAttribute.MODULAR); //$NON-NLS-1$
        }
    }
}
