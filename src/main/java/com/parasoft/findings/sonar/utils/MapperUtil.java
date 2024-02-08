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

    private static final Map<String, CleanCodeAttribute> TAG_PREFIX_MAP = new HashMap<>();
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
        return !isSev1 && (tag.contains("security") ||
                tag.startsWith("owasp") ||
                tag.startsWith("cwe") ||
                tag.startsWith("pcidss") ||
                tag.startsWith("apsc") ||
                tag.startsWith("cert"));
    }

    public static SoftwareQuality mapToSonarImpactSoftwareQuality(String category, boolean isSev1) {
        String tag = categoryToTag(category);
        if (isSev1 && (tag.contains("security") ||
                tag.startsWith("owasp") ||
                tag.startsWith("cwe") ||
                tag.startsWith("pcidss") ||
                tag.startsWith("apsc") ||
                tag.startsWith("cert"))) {
            return SoftwareQuality.SECURITY;
        } else if (isSev1 ||
                tag.startsWith("bd") ||
                tag.startsWith("pb") ||
                tag.startsWith("cs.pb") ||
                tag.startsWith("vb.pb")) {
            return SoftwareQuality.RELIABILITY;
        }
        return SoftwareQuality.MAINTAINABILITY;
    }

    public static CleanCodeAttribute mapToSonarCleanCodeAttribute(String category) {
        String tag = categoryToTag(category);
        String[] splitTag = tag.split("\\.");
        if (TAG_PREFIX_MAP.containsKey(splitTag[0])) {
            return TAG_PREFIX_MAP.get(splitTag[0]);
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

    static {
        //The following three attributes belong to the CONSISTENT category
        //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#consistent
        //FORMATTED
        TAG_PREFIX_MAP.put("format", CleanCodeAttribute.FORMATTED);

        //CONVENTIONAL
        TAG_PREFIX_MAP.put("apsc-dv", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("arru", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("autosar", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("bean", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cert", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cert-c", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cert-cpp", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cls", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cmug", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("codsta", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("com", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("comment", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cs", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("ct", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("cwe", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("dbc", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("eclipse", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("ejb", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("gbj5369", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("gjb8114", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("global", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("hibernate", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("hicpp", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("inter", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("javadoc", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("jdbc", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("jsf", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("junit", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misra", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misra2004", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misra2008", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misra2012", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misrac2012", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misracpp2023", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("misracpp202x", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("opu", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("owasp2017", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("owasp2021", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("owasp2019", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("owasp2023", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("owasp-asvs-403", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("parser", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("pcidss32", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("pcidss40", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("pfo", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("port", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("props", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("qt", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("roslyn", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("sec", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("security", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("serial", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("servlet", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("spr", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("spring", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("stl", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("struts", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("templ", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("tug", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("vb", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("vvsg", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("wpf", CleanCodeAttribute.CONVENTIONAL);
        TAG_PREFIX_MAP.put("xml", CleanCodeAttribute.CONVENTIONAL);

        //IDENTIFIABLE
        TAG_PREFIX_MAP.put("naming", CleanCodeAttribute.IDENTIFIABLE);
        TAG_PREFIX_MAP.put("ng", CleanCodeAttribute.IDENTIFIABLE);


        //The following four attributes belong to the INTENTIONAL category
        //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#intentional
        //CLEAR
        TAG_PREFIX_MAP.put("brm", CleanCodeAttribute.CLEAR);
        TAG_PREFIX_MAP.put("preproc", CleanCodeAttribute.CLEAR);
        TAG_PREFIX_MAP.put("uc", CleanCodeAttribute.CLEAR);

        //LOGICAL
        TAG_PREFIX_MAP.put("bd", CleanCodeAttribute.LOGICAL);
        TAG_PREFIX_MAP.put("err", CleanCodeAttribute.LOGICAL);
        TAG_PREFIX_MAP.put("except", CleanCodeAttribute.LOGICAL);
        TAG_PREFIX_MAP.put("pb", CleanCodeAttribute.LOGICAL);
        TAG_PREFIX_MAP.put("trs", CleanCodeAttribute.LOGICAL);

        //COMPLETE
        TAG_PREFIX_MAP.put("init", CleanCodeAttribute.COMPLETE);
        TAG_PREFIX_MAP.put("ifd", CleanCodeAttribute.COMPLETE);

        //EFFICIENT
        TAG_PREFIX_MAP.put("gc", CleanCodeAttribute.EFFICIENT);
        TAG_PREFIX_MAP.put("mobile", CleanCodeAttribute.EFFICIENT);
        TAG_PREFIX_MAP.put("mrm", CleanCodeAttribute.EFFICIENT);
        TAG_PREFIX_MAP.put("opt", CleanCodeAttribute.EFFICIENT);


        //The following three attributes belong to the ADAPTABLE category
        //The official documentation is https://docs.sonarsource.com/sonarqube/latest/user-guide/clean-code/#adaptable
        //FOCUSED
        TAG_PREFIX_MAP.put("metrics", CleanCodeAttribute.FOCUSED);
        TAG_PREFIX_MAP.put("metric", CleanCodeAttribute.FOCUSED);

        //DISTINCT
        TAG_PREFIX_MAP.put("cdd", CleanCodeAttribute.DISTINCT);

        //MODULAR
        TAG_PREFIX_MAP.put("oom", CleanCodeAttribute.MODULAR);
        TAG_PREFIX_MAP.put("oop", CleanCodeAttribute.MODULAR);
    }
}
