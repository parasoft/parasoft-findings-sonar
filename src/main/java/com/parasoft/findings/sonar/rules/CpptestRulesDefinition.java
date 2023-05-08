package com.parasoft.findings.sonar.rules;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sonar.api.config.Configuration;

import com.parasoft.xtest.common.api.variables.IVariablesResolver;
import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.SonarVariablesResolver;
import com.parasoft.xtest.services.api.ServiceUtil;

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
    protected void initializeVariables()
    {
        // get resolver service and set variables
        var resolver = ServiceUtil.getService(IVariablesResolver.class);
        if (resolver instanceof SonarVariablesResolver) {
            var dotTestRoot = _config.get(_product.rootPathKey).orElse("");
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.CPPTEST_HOME_KEY, dotTestRoot);
        } else {
            Logger.getLogger().error("Expected SonarVariablesResolver but was " + resolver); //$NON-NLS-1$
        }
    }

    @Override
    String getLanguageFor(IRuleDescription rule, String fileName) {
        String category = rule.getCategoryId().toLowerCase().replace('_', '-');
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
