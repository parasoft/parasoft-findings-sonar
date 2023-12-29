package com.parasoft.findings.sonar;

import com.parasoft.findings.utils.results.violations.ViolationRuleUtil;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;

import java.util.Map;

public class MapperUtil {

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
}
