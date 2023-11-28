/*
 * (C) Copyright Parasoft Corporation 2020.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.parasoft.xtest.configuration.api.rules.IRuleDescription;

/**
 * Represents and contains the list of rules for a given language.
 */
public class LanguageRules {
    public final String language;
    public final String repositoryId;
    private final List<IRuleDescription> _rules = new ArrayList<>();

    public LanguageRules(String language, String repositoryId)
    {
        this.language = language;
        this.repositoryId = repositoryId;
    }

    public List<IRuleDescription> getRules()
    {
        return _rules;
    }

    public Set<String> getRuleIds()
    {
        Set<String> result = new HashSet<>();
        for (IRuleDescription rule : _rules) {
            result.add(rule.getRuleId());
        }
        return result;
    }

    public void addRule(IRuleDescription rule) {
        _rules.add(rule);
    }

    @Override
    public int hashCode()
    {
        return language.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof LanguageRules)){
            return false;
        }
        return Objects.equals(((LanguageRules) obj).language, language);
    }
}
