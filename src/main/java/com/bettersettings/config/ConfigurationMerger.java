package com.bettersettings.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;

/** Deep-merges YAML layers in argument order; later scalar and list values win. */
public final class ConfigurationMerger {
    private ConfigurationMerger() {}

    public static YamlConfiguration merge(ConfigurationSection... layers) {
        YamlConfiguration result = new YamlConfiguration();
        for (ConfigurationSection layer : layers) {
            if (layer == null) continue;
            layer.getValues(true).forEach((path, value) -> {
                if (!(value instanceof ConfigurationSection)) result.set(path, value);
            });
        }
        return result;
    }

    /**
     * Applies a preset without letting untouched owner blocks copied from the packaged
     * defaults cancel it. Once any value in an owner block is edited, that complete
     * block wins so values intentionally restored to the packaged value remain explicit.
     */
    public static YamlConfiguration mergePreset(ConfigurationSection packaged,
                                                ConfigurationSection preset,
                                                ConfigurationSection owner) {
        YamlConfiguration ownerOverrides = new YamlConfiguration();
        if (owner != null) {
            for (String root : owner.getKeys(false)) {
                ConfigurationSection ownerBlock = owner.getConfigurationSection(root);
                ConfigurationSection packagedBlock = packaged == null ? null : packaged.getConfigurationSection(root);
                if (ownerBlock == null) {
                    Object value = owner.get(root);
                    Object packagedValue = packaged == null ? null : packaged.get(root);
                    if (!Objects.equals(value, packagedValue)) ownerOverrides.set(root, value);
                    continue;
                }
                if (packagedBlock != null && Objects.equals(
                    leafValues(ownerBlock), leafValues(packagedBlock))) continue;
                ownerBlock.getValues(true).forEach((path, value) -> {
                    if (!(value instanceof ConfigurationSection)) ownerOverrides.set(root + "." + path, value);
                });
            }
        }
        return merge(packaged, preset, ownerOverrides);
    }

    private static java.util.Map<String, Object> leafValues(ConfigurationSection section) {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        section.getValues(true).forEach((path, value) -> {
            if (!(value instanceof ConfigurationSection)) values.put(path, value);
        });
        return values;
    }
}
