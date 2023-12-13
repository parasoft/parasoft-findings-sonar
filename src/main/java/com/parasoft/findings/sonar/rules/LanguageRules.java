/*
 * (C) Copyright Parasoft Corporation 2020.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.findings.sonar.rules;

import com.parasoft.findings.utils.rules.RuleDescription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents and contains the list of rules for a given language.
 */
public class LanguageRules {
    public final String language;
    public final String repositoryId;
    private final List<RuleDescription> _rules = new ArrayList<>();

    public LanguageRules(String language, String repositoryId)
    {
        this.language = language;
        this.repositoryId = repositoryId;
    }

    public List<RuleDescription> getRules()
    {
        return _rules;
    }

    public Set<String> getRuleIds()
    {
        Set<String> result = new HashSet<>();
        for (RuleDescription rule : _rules) {
            result.add(rule.getRuleId());
        }
        return result;
    }

    public void addRule(RuleDescription rule) {
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
