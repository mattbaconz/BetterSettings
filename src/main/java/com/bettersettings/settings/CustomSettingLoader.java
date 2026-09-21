package com.bettersettings.settings;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.config.ConfigurationMerger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Builds YAML settings into the current managed-registration candidate. */
public final class CustomSettingLoader {

    private final BetterSettings plugin;

    public CustomSettingLoader(BetterSettings plugin) {
        this.plugin = plugin;
    }

    public ValidationReport loadManagedSettings() {
        SettingsRegistry registry = SettingsRegistry.getInstance();
        List<ValidationIssue> issues = new ArrayList<>();
        List<SettingDefinition> definitions = new ArrayList<>();
        int skipped = 0;

        String configuredPreset = plugin.getConfigManager().getMainConfig().getString("preset.active", "classic");
        String activePreset = plugin.getConfigManager().getActivePreset();
        if (configuredPreset != null
            && !configuredPreset.equalsIgnoreCase("classic")
            && !configuredPreset.equalsIgnoreCase("donutsmp")) {
            issues.add(new ValidationIssue(
                ValidationIssue.Severity.WARNING, "unknown-preset", "config.yml", "preset.active",
                "Unknown preset '" + configuredPreset + "'; classic is active."
            ));
        }

        Set<String> categoryIds = new HashSet<>(registry.getCapturedCategoryIds());
        categoryIds.add("uncategorized");
        CustomSettingParser parser = new CustomSettingParser(categoryIds);
        Set<String> packagedKeys = plugin.getConfigManager().getPackagedSettingsConfig().getKeys(false);
        Set<String> reservedKeys = new HashSet<>(packagedKeys);

        if ("donutsmp".equals(activePreset)) {
            ConfigurationSection presetSettings = plugin.getConfigManager().getDonutPresetConfig()
                .getConfigurationSection("settings");
            if (presetSettings != null) {
                Set<String> presetKeys = presetSettings.getKeys(false);
                reservedKeys.addAll(presetKeys);
                YamlConfiguration ownerOverrides = ownerSubset(
                    plugin.getConfigManager().getSettingsConfig(), presetKeys
                );
                YamlConfiguration effectivePreset = ConfigurationMerger.merge(presetSettings, ownerOverrides);
                CustomSettingParser.Result result = parser.parse("preset:donutsmp", effectivePreset, Set.of());
                definitions.addAll(result.definitions());
                issues.addAll(result.issues());
                skipped += result.skippedCount();
            }
        }

        CustomSettingParser.Result ownerResult = parser.parse(
            "settings.yml", plugin.getConfigManager().getSettingsConfig(), reservedKeys
        );
        definitions.addAll(ownerResult.definitions());
        issues.addAll(ownerResult.issues());
        skipped += ownerResult.skippedCount();

        for (Map.Entry<String, FileConfiguration> entry
            : plugin.getConfigManager().getCustomSettingConfigs().entrySet()) {
            String source = "settings/" + entry.getKey().substring("custom_".length()) + ".yml";
            CustomSettingParser.Result result = parser.parse(source, entry.getValue(), Set.of());
            definitions.addAll(result.definitions());
            issues.addAll(result.issues());
            skipped += result.skippedCount();
        }

        validateBuiltinMetadata(categoryIds, packagedKeys, issues);

        DefinitionCatalog catalog = DefinitionCatalog.build(definitions);
        issues.addAll(catalog.issues());
        int duplicates = catalog.duplicateCount();

        Set<String> builtinIds = registry.getCapturedSettingIds();
        for (SettingDefinition definition : catalog.settings().values()) {
            if (builtinIds.contains(definition.id())) {
                duplicates++;
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.ERROR, "duplicate-id", definition.source(), definition.id(),
                    "Custom setting ID '" + definition.id() + "' conflicts with a packaged setting."
                ));
            }
        }

        int invalid = (int) issues.stream()
            .filter(issue -> issue.severity() == ValidationIssue.Severity.ERROR
                && !issue.code().equals("duplicate-id"))
            .count();
        if (invalid == 0 && duplicates == 0) {
            catalog.settings().values().forEach(definition ->
                registry.registerValueSetting(new ConfiguredValueSetting(plugin, definition))
            );
        }

        int loaded = registry.getCapturedSettingCount();
        return new ValidationReport(
            activePreset, loaded, 0, skipped, duplicates, invalid, false, List.copyOf(issues)
        );
    }

    static YamlConfiguration ownerSubset(ConfigurationSection source, Set<String> rootKeys) {
        YamlConfiguration subset = new YamlConfiguration();
        for (String path : source.getKeys(true)) {
            String root = path.contains(".") ? path.substring(0, path.indexOf('.')) : path;
            Object value = source.get(path);
            if (rootKeys.contains(root) && !(value instanceof ConfigurationSection)) {
                subset.set(path, value);
            }
        }
        return subset;
    }

    private void validateBuiltinMetadata(Set<String> categoryIds, Set<String> packagedKeys,
                                         List<ValidationIssue> issues) {
        FileConfiguration effective = plugin.getConfigManager().getEffectiveSettingsConfig();
        for (String key : packagedKeys) {
            ConfigurationSection section = effective.getConfigurationSection(key);
            if (section == null || !section.getBoolean("enabled", true)) continue;
            String materialName = section.getString("icon", "PAPER");
            Material material = Material.matchMaterial(materialName == null ? "PAPER" : materialName);
            if (material == null || material == Material.AIR) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING, "invalid-material", "settings.yml", key + ".icon",
                    "Unknown material '" + materialName + "'; PAPER will be used."
                ));
            }
            String category = section.getString("category");
            if (category == null || !categoryIds.contains(category.toLowerCase(Locale.ROOT))) {
                issues.add(new ValidationIssue(
                    ValidationIssue.Severity.WARNING, "unknown-category", "settings.yml", key + ".category",
                    "Unknown category '" + category + "'; uncategorized will be used."
                ));
            }
        }
    }
}
