package com.parasoft.findings.sonar.rules;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.parasoft.findings.sonar.SonarServicesProvider;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import com.parasoft.xtest.common.ISeverityConsts;
import com.parasoft.xtest.common.IStringConstants;
import com.parasoft.xtest.common.io.FileUtil;
import com.parasoft.xtest.common.nls.NLS;
import com.parasoft.xtest.common.text.UString;
import com.parasoft.xtest.configuration.api.rules.IRuleDescription;
import com.parasoft.xtest.configuration.internal.rules.RuleDescriptionProvider;
import com.parasoft.xtest.configuration.rules.RuleDescriptionParser;
import com.parasoft.findings.sonar.Logger;
import com.parasoft.findings.sonar.Messages;
import com.parasoft.findings.sonar.ParasoftConstants;
import com.parasoft.findings.sonar.ParasoftProduct;
import com.parasoft.findings.sonar.importer.ParasoftFindingsParser;

public abstract class AbstractRulesDefinition
    implements RulesDefinition
{
    protected final Configuration _config;
    protected final AbstractRuleProfile _profile;
    protected final ParasoftProduct _product;
    private static Map<String, Set<LanguageRules>> _rulesMap = new HashMap<>();

    private static Object Y2021_VERSION = null;
    private static Method ADD_OWASP_METHOD = null;
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
        SonarServicesProvider.getInstance();
        initializeVariables();

        _rulesMap.put(_product.profileName, _product.getLanguageRules());
    }

    public static Set<LanguageRules> findLanguageRules(String productProfileName)
    {
        if (!_rulesMap.containsKey(productProfileName)) {
            Logger.getLogger().error("Profile '" + productProfileName + "' does not exist!"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        return _rulesMap.get(productProfileName);
    }

    abstract void initializeVariables();
    abstract String getLanguageFor(IRuleDescription rule, String fileName);

    @Override
    public void define(Context context)
    {
        Logger.getLogger().info("Initializing " + getClass().getSimpleName()); //$NON-NLS-1$
        var productRoot = getRootDir();
        if (productRoot != null) {
            var rulesDirs = getRulesDirs();
            if (rulesDirs.isEmpty()) {
                Logger.getLogger().error(NLS.bind(Messages.NotLoadingRules, _product.profileName, _product.rootPathKey));
                return;
            }
            for (File rulesDir : rulesDirs) {
                for (var rulesFile : rulesDir.listFiles()) {
                    if (!isRulesFile(rulesFile)) {
                        continue;
                    }
                    try {
                        Logger.getLogger().info("Loading rules from " + rulesFile.getName()); //$NON-NLS-1$
                        var url = rulesFile.toURI().toURL();
                        var provider = new RuleDescriptionProvider(_product.ruleProvider, _product.analyzerId, url);
                        var parser = new RuleDescriptionParser(provider);
                        parser.parseFile(url);
                        addRules(rulesFile.getName(), parser.getRules());
                    } catch (Exception e) {
                        Logger.getLogger().error("Error reading rules file " + rulesFile.getName(), e); //$NON-NLS-1$
                        continue;
                    }
                }
            }
        } else {
            Logger.getLogger().warn("Product Root not found: " + _config.get(_product.rootPathKey)); //$NON-NLS-1$
        }

        for (LanguageRules lr : _rulesMap.get(_product.profileName)) {
            NewRepository repository = context.createRepository(lr.repositoryId, lr.language);
            repository.setName(ParasoftConstants.RULES_REPOSITORY_NAME);
            addRulesToRepository(lr, repository);

            addUnknownRule(repository);
            repository.done();
        }
    }

    protected boolean isRulesFile(File file)
    {
        return file.isFile() && file.getName().endsWith(".xml"); //$NON-NLS-1$
    }

    protected File getRootDir()
    {
        var rootPath = _config.get(_product.rootPathKey).orElse("");
        if (!UString.isNonEmptyTrimmed(rootPath)) {
            return null;
        }
        var rootDir = new File(rootPath);
        return rootDir.isDirectory() ? rootDir : null;
    }

    protected Set<File> getRulesDirs()
    {
        var rootDir = getRootDir();
        Set<File> dirs = new HashSet<>();
        var rulesDir = new File(rootDir, _product.rulesPath + "/rules"); //$NON-NLS-1$
        if (rulesDir.isDirectory()) {
            dirs.add(rulesDir);
        }
        var metricsDir = new File(rootDir, _product.rulesPath + "/metrics"); //$NON-NLS-1$
        if (metricsDir.isDirectory()) {
            dirs.add(metricsDir);
        }
        return dirs;
    }

    protected void addRules(String fileName, List<IRuleDescription> rulesList)
    {
        for (IRuleDescription rule : rulesList) {
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
                Logger.getLogger().warn("Rule " + rule.getRuleId() + "not added - no LanguageRules for language " + lang); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    protected void addRulesToRepository(LanguageRules rules, NewRepository repository)
    {
        for (IRuleDescription ruleDescription : rules.getRules()) {
            var tag = ruleDescription.getCategoryId().toLowerCase().replace('_', '-');
            var name = getValidRuleName(ruleDescription);
            var ruleKey = RuleKey.of(rules.repositoryId, ruleDescription.getRuleId());
            var desc = getRuleDescription(_product.rootPathKey, ruleDescription.getRuleId());
            var type = getType(tag, ruleDescription.getSeverity() == ISeverityConsts.SEVERITY_HIGHEST);
            var sev = ParasoftFindingsParser.mapToSonarSeverity(ruleDescription.getSeverity()).name();

            NewRule newRule = repository.createRule(ruleKey.rule()).setName(name)
                .setHtmlDescription(desc)
                .setType(type)
                .setSeverity(sev)
                .setTags(ParasoftConstants.PARASOFT_REPOSITORY_TAG, tag);
            addOwasp(newRule, ruleDescription);
            addCwe(newRule, ruleDescription);
        }
        Logger.getLogger().info(rules.getRules().size() + " Rules for " + rules.language + " loaded into repository " + rules.repositoryId); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("nls")
    private void addOwasp(NewRule newRule, IRuleDescription ruleDescription)
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
                        Logger.getLogger().info("Unable to set OWASP 2021 standard for rule " + ruleDescription.getRuleId(), e);
                    }
                }
            } else {
                Logger.getLogger().warn("Unsupported OWASP version " + ruleIdParts[0] + " for rule " + ruleDescription.getRuleId());
            }
        } else {
            Logger.getLogger().info("OWASP rule " + ruleDescription.getRuleId() + " does not have a standard in its ruleId");
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
    private void addCwe(NewRule newRule, IRuleDescription ruleDescription)
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
        Logger.getLogger().warn("CWE Rule " + ruleDescription.getRuleId() + " does not have a CWE ID in its ruleId");
    }

    protected void addUnknownRule(NewRepository repository)
    {
        repository.createRule(ParasoftConstants.UNKNOWN_RULE_ID).setName(Messages.UnknownRuleName)
            .setHtmlDescription(Messages.NoDescription)
            .setSeverity(Severity.INFO)
            .setTags(ParasoftConstants.PARASOFT_REPOSITORY_TAG);
    }

    private String getValidRuleName(IRuleDescription rule)
    {
        var name = "[" + rule.getCategoryId() + "] " + rule.getHeader();
        if (name.length() > 197) {
            name = name.substring(0, 197) + "..."; //$NON-NLS-1$
        }
        return name;
    }

    @SuppressWarnings("nls")
    private RuleType getType(String tag, boolean isSev1)
    {
        if (tag.contains("security") || tag.startsWith("owasp") || tag.startsWith("cwe")
                || tag.startsWith("pcidss") || tag.startsWith("apsc") || tag.startsWith("cert")) {
            return isSev1 ? RuleType.VULNERABILITY : RuleType.SECURITY_HOTSPOT;
        } else if (isSev1 || tag.startsWith("bd") || tag.startsWith("pb") || tag.startsWith("cs.pb") || tag.startsWith("vb.pb")) {
            return RuleType.BUG;
        }

        return RuleType.CODE_SMELL;
    }

    private String getRuleDescription(String pathKey, String ruleId)
    {
        String root = _config.get(pathKey).orElse("");
        if (UString.isNonEmptyTrimmed(root)) {
            var docRoot = new File(root, _product.docPath);
            var docFile = guessRuleFile(docRoot, ruleId);
            if (docFile != null) {
                try {
                    return FileUtil.readFile(docFile, IStringConstants.UTF_8);
                } catch (IOException e) {
                    Logger.getLogger().error("Unable to read docs file: " + docFile.getAbsolutePath(), e); //$NON-NLS-1$
                }
            } else {
                docFile = new File(docRoot, ruleId + ".html"); //$NON-NLS-1$
                Logger.getLogger().error("Failed to determine local file for rule doc location: " + docFile.getAbsolutePath()); //$NON-NLS-1$
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
