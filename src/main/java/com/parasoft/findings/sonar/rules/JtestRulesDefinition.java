package com.parasoft.findings.sonar.rules;

import java.io.File;

import org.sonar.api.config.Configuration;

import com.parasoft.xtest.common.api.variables.IVariablesResolver;
import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.SonarVariablesResolver;
import com.parasoft.xtest.services.api.ServiceUtil;

public class JtestRulesDefinition
    extends AbstractRulesDefinition
{
    public JtestRulesDefinition(Configuration config)
    {
        super(config, new JtestProfile());
    }

    @Override
    protected void initializeVariables()
    {
        // get resolver service and set variables
        IVariablesResolver resolver = ServiceUtil.getService(IVariablesResolver.class);
        if (resolver instanceof SonarVariablesResolver) {
            var jtestRoot = _config.get(_product.rootPathKey).orElse("");
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.JTEST_HOME_KEY, jtestRoot);
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.JTEST_INSTALL_DIR_KEY, jtestRoot);
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.LOCAL_STORAGE_KEY, ParasoftConstants.DOT_JTEST_VALUE);
            ((SonarVariablesResolver) resolver).addVariable(ParasoftConstants.RULE_DIR_KEY, jtestRoot + ParasoftConstants.RULES_USER_VALUE);
        } else {
            Logger.getLogger().error("Expected SonarVariablesResolver but was " + resolver); //$NON-NLS-1$
        }
    }

    @Override
    @SuppressWarnings("nls")
    String getLanguageFor(IRuleDescription rule, String fileName) {
        switch (fileName) {
            case "xmlrules.xml":
                return "xml";
            case "proprules.xml":
                return "text";
            default:
                return "java";
        }
    }

    @Override
    protected boolean isRulesFile(File file)
    {
        return super.isRulesFile(file) &&
                (file.getName().contains("rules") || file.getName().equals("metrics.xml")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
