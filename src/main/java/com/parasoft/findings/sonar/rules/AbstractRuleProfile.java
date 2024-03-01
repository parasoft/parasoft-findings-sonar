/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.rules;

import java.util.Set;

import com.parasoft.findings.utils.common.nls.NLS;
import com.parasoft.findings.sonar.Messages;
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
        Logger.getLogger().info(NLS.getFormatted(Messages.Initializing, getClass().getSimpleName()));
        if (_rules == null) {
            _rules = AbstractRulesDefinition.findLanguageRules(_product.profileName);
        }
        if (_rules != null) {
            Logger.getLogger().info(NLS.getFormatted(Messages.LanguageRulesFound, _rules.size(), _product.profileName));
            for (LanguageRules lr : _rules) {
                var profile = context.createBuiltInQualityProfile(_product.profileName, lr.language);
                Logger.getLogger().info(NLS.getFormatted(Messages.ActivatingRules, lr.getRuleIds().size(), lr.language));
                for (String ruleId : lr.getRuleIds()) {
                    profile.activateRule(lr.repositoryId, ruleId);
                }
                profile.done();
            }
        } else {
            Logger.getLogger().warn(NLS.getFormatted(Messages.LanguageRulesNotFound,  _product.profileName));
        }
    }
}
