package com.bettersettings.settings;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validates unique setting IDs before a candidate can replace the live snapshot. */
public record DefinitionCatalog(Map<String, SettingDefinition> settings, List<ValidationIssue> issues,
                                int duplicateCount, boolean valid) {

    public static DefinitionCatalog build(Collection<SettingDefinition> definitions) {
        Map<String, SettingDefinition> settings = new LinkedHashMap<>();
        java.util.ArrayList<ValidationIssue> issues = new java.util.ArrayList<>();
        int duplicates = 0;
        for (SettingDefinition definition : definitions) {
            SettingDefinition existing = settings.putIfAbsent(definition.id(), definition);
            if (existing != null) {
                duplicates++;
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "duplicate-id",
                    definition.source(),
                    definition.id(),
                    "Duplicate setting ID '" + definition.id() + "' also defined by " + existing.source() + "."
                ));
            }
        }
        if (duplicates > 0) {
            settings.clear();
        }
        return new DefinitionCatalog(Map.copyOf(settings), List.copyOf(issues), duplicates, duplicates == 0);
    }
}
