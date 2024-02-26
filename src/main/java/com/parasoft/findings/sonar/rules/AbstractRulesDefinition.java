package com.parasoft.findings.sonar.rules;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.*;

import com.parasoft.findings.sonar.*;
import com.parasoft.findings.sonar.utils.MapperUtil;
import com.parasoft.findings.utils.common.IStringConstants;
import com.parasoft.findings.utils.common.nls.NLS;
import com.parasoft.findings.utils.common.util.FileUtil;
import com.parasoft.findings.utils.common.util.StringUtil;
import com.parasoft.findings.utils.results.violations.ViolationRuleUtil;
import com.parasoft.findings.utils.rules.RuleDescription;
import com.parasoft.findings.utils.rules.RuleDescriptionImporter;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

public abstract class AbstractRulesDefinition
    implements RulesDefinition
{
    public static final String BUILTIN_RULES_PATH = "com/parasoft/findings/sonar/res/builtinRules/"; //$NON-NLS-1$
    public static final String BUILTIN_RULES_DIR_NAME = "builtinRules"; //$NON-NLS-1$

    protected final Configuration _config;
    protected final AbstractRuleProfile _profile;
    protected final ParasoftProduct _product;
    private static Map<String, Set<LanguageRules>> _rulesMap = new HashMap<>();

    private static Object Y2021_VERSION = null;
    private static Method ADD_OWASP_METHOD = null;

    private static ZipFile _pluginJarFile;
    private static String _tempPath;
    private String localizedRuleFolder = "";

    static {
        try {
            Class<?> versionClass = Class.forName("org.sonar.api.server.rule.RulesDefinition$OwaspTop10Version"); //$NON-NLS-1$
            ADD_OWASP_METHOD = NewRule.class.getMethod("addOwaspTop10", versionClass, OwaspTop10[].class); //$NON-NLS-1$
            Y2021_VERSION = getY2021Version(versionClass);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            // Ignore - OWASP 2021 is not supported for this version of SonarQube.
        }
    }

    public AbstractRulesDefinition(Configuration config, AbstractRuleProfile profile)
    {
        _config = config;
        _profile = profile;
        _product = profile._product;

        _rulesMap.put(_product.profileName, _product.getLanguageRules());

        String localeLanguage = Locale.getDefault().getLanguage();
        if (Locale.CHINESE.getLanguage().equals(localeLanguage)) {
            localizedRuleFolder = ParasoftConstants.CHINESE_FOLDER_NAME;
        } else if (Locale.JAPANESE.getLanguage().equals(localeLanguage)) {
            localizedRuleFolder = ParasoftConstants.JAPANESE_FOLDER_NAME;
        }
    }

    public static Set<LanguageRules> findLanguageRules(String productProfileName)
    {
        if (!_rulesMap.containsKey(productProfileName)) {
            Logger.getLogger().error(NLS.getFormatted(Messages.ProfileDoesNotExist, productProfileName));
            return null;
        }
        return _rulesMap.get(productProfileName);
    }

    abstract String getLanguageFor(RuleDescription rule, String fileName);

    @Override
    public void define(Context context)
    {
        boolean isSuccess = extractBuiltinRulesIntoTempDirIfNotExist();
        if(!isSuccess) {
            return;
        }

        Logger.getLogger().info(NLS.getFormatted(Messages.Initializing, getClass().getSimpleName()));
        var builtinRulesDir = getBuiltinRulesDir();
        if (builtinRulesDir != null) {
            var rulesDirs = getRulesDirs();
            if (rulesDirs.isEmpty()) {
                Logger.getLogger().error(NLS.bind(Messages.NotLoadingRules, _product.profileName, builtinRulesDir.getAbsolutePath() + "/" + _product.builtinRulesPath)); //$NON-NLS-1$
                return;
            }
            for (File rulesDir : rulesDirs) {
                for (var rulesFile : Objects.requireNonNull(rulesDir.listFiles())) {
                    if (!isRulesFile(rulesFile)) {
                        continue;
                    }
                    try {
                        Logger.getLogger().info(NLS.getFormatted(Messages.LoadingRules, rulesFile.getName()));
                        var importer = new RuleDescriptionImporter();
                        addRules(rulesFile.getName(), importer.performImport(rulesFile));
                    } catch (Exception e) {
                        Logger.getLogger().error(NLS.getFormatted(Messages.ErrorReadingRulesFile, rulesFile.getName()), e);
                    }
                }
            }
        } else {
            Logger.getLogger().warn(NLS.getFormatted(Messages.rulesRootDirectoryNotFound, _tempPath, _product.builtinRulesPath));
        }

        for (LanguageRules lr : _rulesMap.get(_product.profileName)) {
            NewRepository repository = context.createRepository(lr.repositoryId, lr.language);
            repository.setName(ParasoftConstants.RULES_REPOSITORY_NAME);
            addRulesToRepository(lr, repository);

            addUnknownRule(repository);
            repository.done();
        }
    }

    private synchronized boolean extractBuiltinRulesIntoTempDirIfNotExist()
    {
        if(_tempPath == null) {
            try {
                _tempPath = Files.createTempDirectory("parasoft_findings_sonar_").toFile().getAbsolutePath(); //$NON-NLS-1$
                Logger.getLogger().info(NLS.getFormatted(Messages.TemporaryFolderIsCreated, _tempPath));
            } catch (IOException e) {
                Logger.getLogger().error(NLS.getFormatted(Messages.FailedToCreateTemporaryFolder), e);
                return false;
            }
        }
        if(_pluginJarFile == null) {
            CodeSource src = AbstractRulesDefinition.class.getProtectionDomain().getCodeSource();
            if( src != null ) {
                try {
                    URI jar = src.getLocation().toURI();
                    _pluginJarFile = new ZipFile(jar.getPath());
                    String expectedBuiltinRulesStoragePath = new File(_tempPath, BUILTIN_RULES_DIR_NAME).getAbsolutePath();
                    _pluginJarFile.extractFile(BUILTIN_RULES_PATH, _tempPath, BUILTIN_RULES_DIR_NAME);
                    Logger.getLogger().info(NLS.getFormatted(Messages.RuleFilesHaveBeenExtracted, expectedBuiltinRulesStoragePath));
                } catch (ZipException | URISyntaxException e) {
                    Logger.getLogger().error(NLS.getFormatted(Messages.FailedToExtractRuleFiles, _pluginJarFile), e);
                    return false;
                }
                replaceCpptestRuleFilesWithLocalizedIfNeeded();
            } else {
                // Generally, this code block will never be accessed.
                Logger.getLogger().error(NLS.getFormatted(Messages.NoPluginJARFileIsFound));
                return false;
            }
        }
        return true;
    }

    /**
     * Utils can not recognize the localized rule files('ja' and 'zh_CN' folders under {cpptest_root_path}/integration/dtpserver/cpptest/model/rules/ path).
     * We need to override the English rule files with localized files according to the language of the server which SonarQube is installed.
    * */
    private void replaceCpptestRuleFilesWithLocalizedIfNeeded()
    {
        File cpptestRulesDirectory = new File(_tempPath + "/" + ParasoftProduct.CPPTEST.builtinRulesPath, ParasoftProduct.CPPTEST.rulesPath + "/rules"); //$NON-NLS-1$ //$NON-NLS-2$
        if(!cpptestRulesDirectory.exists()) {
            return;
        }
        File japaneseRulesDirectory = new File(cpptestRulesDirectory, ParasoftConstants.JAPANESE_FOLDER_NAME);
        File chineseRulesDirectory = new File(cpptestRulesDirectory, ParasoftConstants.CHINESE_FOLDER_NAME);
        try {
            if (ParasoftConstants.JAPANESE_FOLDER_NAME.equals(localizedRuleFolder) && japaneseRulesDirectory.exists()) {
                FileUtil.recursiveCopy(japaneseRulesDirectory, cpptestRulesDirectory);
            } else if (ParasoftConstants.CHINESE_FOLDER_NAME.equals(localizedRuleFolder) && chineseRulesDirectory.exists()) {
                FileUtil.recursiveCopy(chineseRulesDirectory, cpptestRulesDirectory);
            }
        } catch (IOException e) {
            Logger.getLogger().error(NLS.getFormatted(Messages.ErrorHandlingLocalizedRuleFiles), e);
        }

        FileUtil.recursiveDelete(japaneseRulesDirectory);
        FileUtil.recursiveDelete(chineseRulesDirectory);
    }

    protected boolean isRulesFile(File file)
    {
        return file.isFile() && file.getName().endsWith(".xml"); //$NON-NLS-1$
    }

    protected File getBuiltinRulesDir()
    {
        if (_tempPath == null) {
            return null;
        }
        var builtinRulesDir = new File(_tempPath, _product.builtinRulesPath);
        return builtinRulesDir.isDirectory() ? builtinRulesDir : null;
    }

    protected Set<File> getRulesDirs()
    {
        var builtinRulesDir = getBuiltinRulesDir();
        Set<File> dirs = new HashSet<>();
        var rulesDir = new File(builtinRulesDir, _product.rulesPath + "/rules"); //$NON-NLS-1$
        if (rulesDir.isDirectory()) {
            dirs.add(rulesDir);
        }
        var metricsDir = new File(builtinRulesDir, _product.rulesPath + "/metrics"); //$NON-NLS-1$
        if (metricsDir.isDirectory()) {
            dirs.add(metricsDir);
        }
        return dirs;
    }

    protected void addRules(String fileName, List<RuleDescription> rulesList)
    {
        for (RuleDescription rule : rulesList) {
            String lang = getLanguageFor(rule, fileName);
            boolean added = false;
            for (LanguageRules lr : _rulesMap.get(_product.profileName)) {
                if ("*".equals(lang)) {
                    lr.addRule(rule);
                    added = true;
                } else if (lang.equals(lr.language)) {
                    lr.addRule(rule);
                    added = true;
                    break;
                }
            }
            if (!added) {
                Logger.getLogger().warn(NLS.getFormatted(Messages.RuleNotAdded, rule.getRuleId() ,lang));
            }
        }
    }

    protected void addRulesToRepository(LanguageRules rules, NewRepository repository)
    {
        for (RuleDescription ruleDescription : rules.getRules()) {
            var category = ruleDescription.getCategoryId();
            var tag = MapperUtil.categoryToTag(category);
            var name = getValidRuleName(ruleDescription);
            var ruleKey = RuleKey.of(rules.repositoryId, ruleDescription.getRuleId());
            var desc = getRuleDescription(_tempPath + "/" + _product.builtinRulesPath, ruleDescription.getRuleId()); //$NON-NLS-1$
            var severity = ruleDescription.getSeverity();
            var sev = MapperUtil.mapToSonarSeverity(severity).name();
            var isSev1 = severity == ViolationRuleUtil.SEVERITY_HIGHEST;

            NewRule newRule = repository.createRule(ruleKey.rule()).setName(name)
                .setHtmlDescription(desc)
                .setTags(ParasoftConstants.PARASOFT_REPOSITORY_TAG, tag);

            // When the runtime SONAR API version is lower than 10.1 or the rule is a security hotspot, use deprecated setType(RuleType) and setSeverity(String)
            if (!ParasoftFindingsPlugin.isAPIVersionAtLeast10_1 || MapperUtil.isSecurityHotspot(category, isSev1)) {
                RuleType type = MapperUtil.mapToSonarRuleType(category, isSev1);
                newRule.setType(type)
                        .setSeverity(sev);
            } else {
                var cleanCodeAttribute = MapperUtil.mapToSonarCleanCodeAttribute(category);
                var sonarImpactSoftwareQuality = MapperUtil.mapToSonarImpactSoftwareQuality(category, isSev1);
                var sonarImpactSeverity = MapperUtil.mapToSonarImpactSeverity(severity);
                newRule.setCleanCodeAttribute(cleanCodeAttribute)
                        .addDefaultImpact(sonarImpactSoftwareQuality, sonarImpactSeverity);
            }

            addOwasp(newRule, ruleDescription);
            addCwe(newRule, ruleDescription);
        }
        Logger.getLogger().info(NLS.getFormatted(Messages.loadedIntoRepository, rules.getRules().size(), rules.language, rules.repositoryId));
    }

    @SuppressWarnings("nls")
    private void addOwasp(NewRule newRule, RuleDescription ruleDescription)
    {
        String[] ruleIdParts = ruleDescription.getRuleId().split("[\\.\\-]");
        if (ruleIdParts.length < 2 || !ruleIdParts[0].toUpperCase().startsWith("OWASP")) {
            return;
        }
        OwaspTop10 value = getOwaspStandard(ruleIdParts);
        if (value != null) {
            if (ruleIdParts[0].contains("2017")) {
                newRule.addOwaspTop10(value);
            } else if (ruleIdParts[0].contains("2021")) {
                if (Y2021_VERSION != null && ADD_OWASP_METHOD != null) {
                    try {
                        ADD_OWASP_METHOD.invoke(newRule, Y2021_VERSION, new OwaspTop10[] {value});
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        Logger.getLogger().info(NLS.getFormatted(Messages.UnableToSetOWASPStandardForRule, ruleDescription.getRuleId()), e);
                    }
                }
            } else {
                Logger.getLogger().warn(NLS.getFormatted(Messages.UnsupportedOWASPVersion, ruleIdParts[0], ruleDescription.getRuleId()));
            }
        } else {
            Logger.getLogger().info(NLS.getFormatted(Messages.DoesNotHaveAStandardInItsRuleId, ruleDescription.getRuleId()));
        }
    }

    private static Object getY2021Version(Class<?> versionClass) {
        for (Object o : versionClass.getEnumConstants()) {
            if (o.toString().equals("Y2021")) { //$NON-NLS-1$
                return o;
            }
        }
        return null;
    }

    private static OwaspTop10 getOwaspStandard(String[] ruleIdParts) {
        for (int i = 1; i < ruleIdParts.length-1; i++) {
            for (OwaspTop10 value : OwaspTop10.values()) {
                if (value.name().equals(ruleIdParts[i])) {
                    return value;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("nls")
    private void addCwe(NewRule newRule, RuleDescription ruleDescription)
    {
        String[] ruleIdParts = ruleDescription.getRuleId().split("[\\.\\-]");
        if (ruleIdParts.length < 2 || !ruleIdParts[0].equals("CWE")) {
            return;
        }
        for (int i = 1; i < ruleIdParts.length-1; i++) {
            try {
                newRule.addCwe(Integer.parseInt(ruleIdParts[i]));
                return;
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        Logger.getLogger().warn(NLS.getFormatted(Messages.DoesNotHaveACWEIDInItsRuleId, ruleDescription.getRuleId()));
    }

    protected void addUnknownRule(NewRepository repository)
    {
        NewRule newRule = repository.createRule(ParasoftConstants.UNKNOWN_RULE_ID)
                .setName(Messages.UnknownRuleName)
                .setHtmlDescription(Messages.NoDescription)
                .setTags(ParasoftConstants.PARASOFT_REPOSITORY_TAG);
        if (ParasoftFindingsPlugin.isAPIVersionAtLeast10_1) {
            newRule.addDefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW);
        } else {
            newRule.setSeverity(org.sonar.api.rule.Severity.INFO);
        }
    }

    private String getValidRuleName(RuleDescription rule)
    {
        var name = "[" + rule.getCategoryId() + "] " + rule.getHeader();
        if (name.length() > 197) {
            name = name.substring(0, 197) + "..."; //$NON-NLS-1$
        }
        return name;
    }


    private String getRuleDescription(String root, String ruleId)
    {
        if (StringUtil.isNonEmptyTrimmed(root)) {
            var localizedRuleDocsPath = "".equals(localizedRuleFolder) ? "" : "/" + localizedRuleFolder;
            var docRoot = new File(root, _product.docPath + localizedRuleDocsPath);
            var docFile = guessRuleFile(docRoot, ruleId);
            if (docFile != null) {
                try {
                    return FileUtil.readFile(docFile, IStringConstants.UTF_8);
                } catch (IOException e) {
                    Logger.getLogger().error(NLS.getFormatted(Messages.UnableToReadDocsFile, docFile.getAbsolutePath()), e);
                }
            } else {
                docFile = new File(docRoot, ruleId + ".html"); //$NON-NLS-1$
                Logger.getLogger().error(NLS.bind(Messages.FailedToDetermineLocalFile, docFile.getAbsolutePath()));
            }
            return NLS.bind(Messages.RuleDocNotFoundAt, docFile.getAbsolutePath());
        }
        return Messages.RuleDocNotFound;
    }

    private File guessRuleFile(File root, String ruleId)
    {
        var docFile = ruleId.replace('-', '.') + ".html"; //$NON-NLS-1$
        var files = root.listFiles((FilenameFilter)(dir, name) -> name.replace('-', '.').equals(docFile));
        return files != null && files.length > 0 ? files[0] : null;
    }
}
