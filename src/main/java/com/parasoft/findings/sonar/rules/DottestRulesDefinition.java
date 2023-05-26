package com.parasoft.findings.sonar.rules;

import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import org.sonar.api.config.Configuration;

import java.io.File;

public class DottestRulesDefinition
    extends AbstractRulesDefinition
{
    public DottestRulesDefinition(Configuration config)
    {
        super(config, new DottestProfile());
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
