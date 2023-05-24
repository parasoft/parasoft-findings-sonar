package com.parasoft.findings.sonar.rules;

import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import org.sonar.api.config.Configuration;

import java.io.File;

public class JtestRulesDefinition
    extends AbstractRulesDefinition
{
    public JtestRulesDefinition(Configuration config)
    {
        super(config, new JtestProfile());
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
