package com.parasoft.findings.sonar.rules;

import java.io.File;

import org.sonar.api.config.Configuration;

import com.parasoft.xtest.common.api.variables.IVariablesResolver;
import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.SonarVariablesResolver;
import com.parasoft.xtest.services.api.ServiceUtil;

public class DottestRulesDefinition
    extends AbstractRulesDefinition
{
    public DottestRulesDefinition(Configuration config)
    {
        super(config, new DottestProfile());
    }

    @Override
    protected void initializeVariables()
    {
        // get resolver service and set variables
        IVariablesResolver resolver = ServiceUtil.getService(IVariablesResolver.class);
        if (resolver instanceof SonarVariablesResolver) {
            var dotTestRoot = _config.get(_product.rootPathKey).orElse("");
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.DOTTEST_HOME_KEY, dotTestRoot);
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.LOCAL_STORAGE_KEY, ParasoftConstants.DOT_DOTTEST_VALUE);
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.RULE_DIR_KEY, dotTestRoot + ParasoftConstants.RULES_USER_VALUE);
        } else {
            Logger.getLogger().error("Expected SonarVariablesResolver but was " + resolver); //$NON-NLS-1$
        }
    }

    @Override
    String getLanguageFor(IRuleDescription rule, String fileName) {
        return rule.getRuleId().startsWith("VB.") ? "vbnet" : "cs"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    protected boolean isRulesFile(File file)
    {
        return super.isRulesFile(file) && file.getName().startsWith("rules"); //$NON-NLS-1$
    }
}
