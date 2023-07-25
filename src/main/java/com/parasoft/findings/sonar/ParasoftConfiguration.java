package com.parasoft.findings.sonar;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.CoreProperties;
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
        // Support Static Analysis
        for (var product : ParasoftProduct.values()) {
            items.add(PropertyDefinition.builder(product.reportPathKey)
                .name(product.reportPathName)
                .description(product.reportPathDesc)
                .onQualifiers(Qualifiers.PROJECT)
                .category(ParasoftConstants.EXTERNAL_ANALYZERS_CATEGORY)
                .subCategory(product.subcategory)
                .multiValues(true)
                .build());
        }
        // Support Code Coverage
        items.add(PropertyDefinition.builder(ParasoftConstants.PARASOFT_COVERAGE_REPORT_PATHS_KEY)
                .name(Messages.CoverageReportPathName)
                .description(Messages.CoverageReportPathDesc)
                .onQualifiers(Qualifiers.PROJECT)
                .category(CoreProperties.CATEGORY_CODE_COVERAGE)
                .subCategory(Messages.CoverageSubCategory)
                .multiValues(true)
                .build()
        );
        return items;
    }
}
