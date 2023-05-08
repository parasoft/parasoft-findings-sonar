package com.parasoft.findings.sonar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.parasoft.xtest.common.api.variables.IVariable;
import com.parasoft.xtest.common.api.variables.IVariablesProvider;
import com.parasoft.xtest.common.text.UString;
import com.parasoft.xtest.common.variables.StaticVariable;
import com.parasoft.xtest.common.variables.VariablesResolver;

public class SonarVariablesResolver
    extends VariablesResolver
{

    SonarVariablesProvider _provider;

    public SonarVariablesResolver()
    {
        super(new SonarVariablesProvider());
        _provider = (SonarVariablesProvider) getVariablesProvider();
    }

    public void addVariable(String key, String value)
    {
        if (UString.isNonEmptyTrimmed(key)) {
            _provider.addVariable(key, value);
        }
    }

    private static class SonarVariablesProvider
        implements IVariablesProvider
    {
        private final Map<String, String> _variables = new HashMap<>();

        @Override
        public IVariable getVariable(String sName)
        {
            String sValue = _variables.get(sName);
            return sValue == null ? null : new StaticVariable(sName, sValue);
        }

        public void addVariable(String key, String value)
        {
            _variables.put(key, value);
        }

        @Override
        public String[] getVariableNames()
        {
            Set<String> keys = _variables.keySet();
            return keys.toArray(new String[keys.size()]);
        }

    }

}
