/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.rules;

import java.util.Set;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.ParasoftProduct;

public abstract class AbstractRuleProfile implements BuiltInQualityProfilesDefinition
{
    final ParasoftProduct _product;
    private Set<LanguageRules> _rules;

    protected AbstractRuleProfile(ParasoftProduct product)
    {
        _product = product;
    }

    @Override
    public void define(Context context)
    {
        Logger.getLogger().info("Initializing " + getClass().getSimpleName()); //$NON-NLS-1$
        if (_rules == null) {
            _rules = AbstractRulesDefinition.findLanguageRules(_product.profileName);
        }
        if (_rules != null) {
            Logger.getLogger().info("  " + _rules.size() + " LanguageRules found for " + _product.profileName); //$NON-NLS-1$ //$NON-NLS-2$
            for (LanguageRules lr : _rules) {
                var profile = context.createBuiltInQualityProfile(_product.profileName, lr.language);
                Logger.getLogger().info("  Activating " + lr.getRuleIds().size() + " rules for " + lr.language); //$NON-NLS-1$ //$NON-NLS-2$
                for (String ruleId : lr.getRuleIds()) {
                    profile.activateRule(lr.repositoryId, ruleId);
                }
                profile.done();
            }
        } else {
            Logger.getLogger().warn("  LanguageRules not found for " + _product.profileName); //$NON-NLS-1$
        }
    }
}
