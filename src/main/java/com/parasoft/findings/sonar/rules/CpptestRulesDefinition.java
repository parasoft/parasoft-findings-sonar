package com.parasoft.findings.sonar.rules;

import com.parasoft.findings.sonar.MapperUtil;
import com.parasoft.findings.utils.rules.RuleDescription;
import org.sonar.api.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CpptestRulesDefinition
    extends AbstractRulesDefinition
{
    @SuppressWarnings("nls")
    private static final Set<String> CPP_CATEGORIES = new HashSet<>(Arrays.asList(
        "except", "oop", "qt", "stl", "templ"
    ));

    public CpptestRulesDefinition(Configuration config)
    {
        super(config, new CpptestProfile());
    }

    @Override
    String getLanguageFor(RuleDescription rule, String fileName) {
        String category = MapperUtil.categoryToTag(rule.getCategoryId());
        for (String prefix : CPP_CATEGORIES) {
            if (category.startsWith(prefix)) {
                return "cpp"; //$NON-NLS-1$
            }
        }
        return "*";
    }

    @Override
    protected boolean isRulesFile(File file)
    {
        return super.isRulesFile(file) &&
                (file.getName().startsWith("rules") || file.getName().equals("metrics.xml")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
