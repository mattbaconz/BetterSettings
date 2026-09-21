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
     * Applies a preset without letting the untouched owner copy of the packaged
     * defaults cancel it. Only owner values that differ from the packaged value,
     * plus owner-only additions, are treated as explicit overrides.
     */
    public static YamlConfiguration mergePreset(ConfigurationSection packaged,
                                                ConfigurationSection preset,
                                                ConfigurationSection owner) {
        YamlConfiguration ownerOverrides = new YamlConfiguration();
        if (owner != null) {
            owner.getValues(true).forEach((path, value) -> {
                if (value instanceof ConfigurationSection) return;
                Object packagedValue = packaged == null ? null : packaged.get(path);
                if (!Objects.equals(value, packagedValue)) ownerOverrides.set(path, value);
            });
        }
        return merge(packaged, preset, ownerOverrides);
    }
}
