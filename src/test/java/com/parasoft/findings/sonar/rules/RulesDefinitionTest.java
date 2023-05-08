/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.ParasoftProduct;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.NewRule;

import com.parasoft.xtest.common.io.IOUtils;
import com.parasoft.xtest.configuration.api.rules.IRuleDescription;

class RulesDefinitionTest
{
    @ParameterizedTest
    @MethodSource("testBasic_Params")
    void testBasics(AbstractRulesDefinition def, AbstractRuleProfile profile, int langCount)
    {
        assertEquals(profile.getClass(), def._profile.getClass());
        Assertions.assertEquals(profile._product, def._product);

        NewRule rule = mock(NewRule.class);
        NewRepository repo = mockRepository(rule);
        
        def.addUnknownRule(repo);

        verify(rule).setName(Messages.UnknownRuleName);
        verify(rule).setHtmlDescription(Messages.NoDescription);
        verify(rule).setSeverity(Severity.INFO);
        verify(rule).setTags(ParasoftConstants.PARASOFT_REPOSITORY_TAG);

        var rules = AbstractRulesDefinition.findLanguageRules(profile._product.profileName);
        assertEquals(langCount, rules.size());
        for (var r : rules) {
            assertEquals(0, r.getRules().size());
            assertEquals(0, r.getRuleIds().size());
        }
    }

    static Stream<Arguments> testBasic_Params() {
        Configuration config = mock(Configuration.class);
        
        return Stream.of(
            Arguments.of(new JtestRulesDefinition(config), new JtestProfile(), 3),
            Arguments.of(new DottestRulesDefinition(config), new DottestProfile(), 2),
            Arguments.of(new CpptestRulesDefinition(config), new CpptestProfile(), 2)
        );
    }

    @Test
    void testGetLanguageFor() {
        Configuration config = mock(Configuration.class);

        AbstractRulesDefinition def = new JtestRulesDefinition(config);
        assertEquals("xml", def.getLanguageFor(null, "xmlrules.xml"));
        assertEquals("text", def.getLanguageFor(null, "proprules.xml"));
        assertEquals("java", def.getLanguageFor(null, "anything else"));
        
        def = new DottestRulesDefinition(config);
        IRuleDescription rule = mock(IRuleDescription.class);
        when(rule.getRuleId()).thenReturn("VB.");
        assertEquals("vbnet", def.getLanguageFor(rule, null));
        rule = mock(IRuleDescription.class);
        when(rule.getRuleId()).thenReturn("Something Else");
        assertEquals("cs", def.getLanguageFor(rule, null));
        
        def = new CpptestRulesDefinition(config);
        rule = mock(IRuleDescription.class);
        when(rule.getCategoryId()).thenReturn("cdd");
        assertEquals("*", def.getLanguageFor(rule, null));
        rule = mock(IRuleDescription.class);
        when(rule.getCategoryId()).thenReturn("OOP");
        assertEquals("cpp", def.getLanguageFor(rule, null));
    }

    @ParameterizedTest
    @MethodSource("testDefine_Params")
    void testDefine(AbstractRulesDefinition def) throws FileNotFoundException, IOException {
        Context context = mock(Context.class);
        NewRule rule = mock(NewRule.class);
        NewRepository repo = mockRepository(rule);

        String ruleFile = null;
        switch (def._product) {
        case JTEST:
            ruleFile = "jtest_corerules.xml";
            break;
        case CPPTEST:
            ruleFile = "rules_cpp.xml";
            break;
        default:
        case DOTTEST:
            ruleFile = "rules_dot_wizard.xml";
            break;
        }
        var rulesPath = new File("target", def._product.rulesPath + "/rules");
        rulesPath.mkdirs();
        
        copyRuleFile(ruleFile, def._product);
        
        when(context.createRepository(any(), any())).thenReturn(repo);
        when(def._config.get(def._product.rootPathKey)).thenReturn(Optional.of("target"));

        def.define(context);

        var profileContext = mock(org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context.class);
        NewBuiltInQualityProfile newProfile = mock(NewBuiltInQualityProfile.class);
        when(profileContext.createBuiltInQualityProfile(any(), any())).thenReturn(newProfile);

        def._profile.define(profileContext);
    }

    static Stream<Arguments> testDefine_Params() {
        Configuration config = mock(Configuration.class);
        
        return Stream.of(
            Arguments.of(new JtestRulesDefinition(config)),
            Arguments.of(new DottestRulesDefinition(config)),
            Arguments.of(new CpptestRulesDefinition(config))
        );
    }

    void copyRuleFile(String filename, ParasoftProduct product) throws FileNotFoundException, IOException
    {
        var rulesPath = new File("target", product.rulesPath + "/rules");
        rulesPath.mkdirs();
        
        IOUtils.copy(new FileInputStream(new File("src/test/java", filename)),
                new FileOutputStream(new File(rulesPath, filename)));
    }

    NewRepository mockRepository(NewRule rule)
    {
        NewRepository repo = mock(NewRepository.class);

        when(repo.createRule(any())).thenReturn(rule);
        when(rule.setName(any())).thenReturn(rule);
        when(rule.setHtmlDescription(anyString())).thenReturn(rule);
        when(rule.setType(any())).thenReturn(rule);
        when(rule.setSeverity(any())).thenReturn(rule);
        when(rule.setTags(any())).thenReturn(rule);

        return repo;
    }

    @Test
    void testFindLanguageRules_notFound() throws Exception {
        Set<LanguageRules> result = AbstractRulesDefinition.findLanguageRules("notFound");
        assertNull(result);
    }
}
