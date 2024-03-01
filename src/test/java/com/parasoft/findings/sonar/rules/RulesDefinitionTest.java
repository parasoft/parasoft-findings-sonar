/*
* (C) Copyright ParaSoft Corporation 2022. All rights reserved.
* THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
* The copyright notice above does not evidence any
* actual or intended publication of such source code.
*/


package com.parasoft.findings.sonar.rules;

import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.ParasoftFindingsPlugin;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.utils.common.util.IOUtils;
import com.parasoft.findings.utils.rules.RuleDescription;
import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.NewRule;
import org.sonar.api.utils.Version;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static com.parasoft.findings.sonar.rules.AbstractRulesDefinition.BUILTIN_RULES_DIR_NAME;
import static com.parasoft.findings.sonar.rules.AbstractRulesDefinition.BUILTIN_RULES_PATH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        RuleDescription rule = mock(RuleDescription.class);
        when(rule.getRuleId()).thenReturn("VB.");
        assertEquals("vbnet", def.getLanguageFor(rule, null));
        rule = mock(RuleDescription.class);
        when(rule.getRuleId()).thenReturn("Something Else");
        assertEquals("cs", def.getLanguageFor(rule, null));

        def = new CpptestRulesDefinition(config);
        rule = mock(RuleDescription.class);
        when(rule.getCategoryId()).thenReturn("cdd");
        assertEquals("*", def.getLanguageFor(rule, null));
        rule = mock(RuleDescription.class);
        when(rule.getCategoryId()).thenReturn("OOP");
        assertEquals("cpp", def.getLanguageFor(rule, null));
    }

    @ParameterizedTest
    @MethodSource("testDefine_Params")
    void testDefine(AbstractRulesDefinition def) throws IOException {
        try(MockedStatic<Files> filesClassMock = mockStatic(Files.class)) {
            Path targetPath = Paths.get("target");
            filesClassMock.when(() -> Files.createTempDirectory("parasoft_findings_sonar_")).thenReturn(targetPath);
            try(MockedConstruction<ZipFile> mockedZipFileConstruction = Mockito.mockConstruction(ZipFile.class)) {
                Context context = mock(Context.class);
                NewRule rule = mock(NewRule.class);
                NewRepository repo = mockRepository(rule);
                Plugin.Context pluginContext = mock(Plugin.Context.class);
                SonarRuntime sonarRuntime = mock(SonarRuntime.class);
                Version version = mock(Version.class);
                when(sonarRuntime.getApiVersion()).thenReturn(version);
                when(pluginContext.getRuntime()).thenReturn(sonarRuntime);
                when(version.isGreaterThanOrEqual(any())).thenReturn(true);
                ParasoftFindingsPlugin plugin = new ParasoftFindingsPlugin();

                String ruleFile = null;
                switch (def._product) {
                    case JTEST:
                        ruleFile = "jtest_corerules.xml";
                        break;
                    case CPPTEST:
                        ruleFile = "rules_cpp_zh_CN.xml";
                        break;
                    default:
                    case DOTTEST:
                        ruleFile = "rules_dot_wizard.xml";
                        break;
                }
                copyRuleFile(ruleFile, def._product);

                when(context.createRepository(any(), any())).thenReturn(repo);

                when(version.isGreaterThanOrEqual(any())).thenReturn(true);
                plugin.define(pluginContext);
                def.define(context);

                int zipFileConstructionCalledTimes = mockedZipFileConstruction.constructed().size();
                switch (def._product) {
                    case CPPTEST:
                        assertEquals(1, zipFileConstructionCalledTimes);
                        filesClassMock.verify(() -> Files.createTempDirectory("parasoft_findings_sonar_"), times(1));
                        verify(mockedZipFileConstruction.constructed().get(0), times(1))
                                .extractFile(BUILTIN_RULES_PATH, targetPath.toFile().getAbsolutePath(), BUILTIN_RULES_DIR_NAME);
                        break;
                    case JTEST:
                    case DOTTEST:
                    default:
                        assertEquals(0, zipFileConstructionCalledTimes);
                        filesClassMock.verify(() -> Files.createTempDirectory("parasoft_findings_sonar_"), times(0));
                }

                var profileContext = mock(org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context.class);
                NewBuiltInQualityProfile newProfile = mock(NewBuiltInQualityProfile.class);
                when(profileContext.createBuiltInQualityProfile(any(), any())).thenReturn(newProfile);

                when(version.isGreaterThanOrEqual(any())).thenReturn(false);
                plugin.define(pluginContext);
                def._profile.define(profileContext);

                String cpptestRulesDirectoryPath = "target" + "/" + ParasoftProduct.CPPTEST.builtinRulesPath + "/" + ParasoftProduct.CPPTEST.rulesPath + "/rules";
                assertFalse(new File(cpptestRulesDirectoryPath, "zh_CN/rules_cpp_zh_CN.xml").exists());
                assertTrue(new File(cpptestRulesDirectoryPath, "rules_cpp_zh_CN.xml").exists());
            }
        }
    }

    static Stream<Arguments> testDefine_Params() {
        Configuration config = mock(Configuration.class);

        Locale.setDefault(Locale.CHINESE);
        return Stream.of(
            Arguments.of(new CpptestRulesDefinition(config)),
            Arguments.of(new JtestRulesDefinition(config)),
            Arguments.of(new DottestRulesDefinition(config))
        );
    }

    void copyRuleFile(String filename, ParasoftProduct product) throws FileNotFoundException, IOException
    {
        var targetPath = "target" + "/" + product.builtinRulesPath + "/" + product.rulesPath + "/rules";
        if (product == ParasoftProduct.CPPTEST) {
            //
            targetPath += "/zh_CN";
        }
        var rulesPath = new File(targetPath);
        rulesPath.mkdirs();

        IOUtils.copy(new FileInputStream(new File("src/test/resources/rules", filename)),
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
        when(rule.setCleanCodeAttribute(any())).thenReturn(rule);
        when(rule.addDefaultImpact(any(), any())).thenReturn(rule);

        return repo;
    }

    @Test
    void testFindLanguageRules_notFound() throws Exception {
        Set<LanguageRules> result = AbstractRulesDefinition.findLanguageRules("notFound");
        assertNull(result);
    }
}
