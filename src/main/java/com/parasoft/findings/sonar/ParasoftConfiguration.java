package com.parasoft.findings.sonar;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

/**
 * Helper class to generate configuration visible in UI of sonar.
 */
public class ParasoftConfiguration
{

    private ParasoftConfiguration()
    {}

    public static List<PropertyDefinition> getProperties()
    {
        List<PropertyDefinition> items = new ArrayList<>();
        for (var product : ParasoftProduct.values()) {
            items.add(PropertyDefinition.builder(product.rootPathKey)
                .name(product.rootPathName)
                .description(product.rootPathDesc)
                .category(ParasoftConstants.EXTERNAL_ANALYZERS_CATEGORY)
                .subCategory(product.subcategory)
                .build());
            items.add(PropertyDefinition.builder(product.reportPathKey)
                .name(product.reportPathName)
                .description(product.reportPathDesc)
                .onQualifiers(Qualifiers.PROJECT)
                .category(ParasoftConstants.EXTERNAL_ANALYZERS_CATEGORY)
                .subCategory(product.subcategory)
                .multiValues(true)
                .build());
        }
        return items;
    }
}
