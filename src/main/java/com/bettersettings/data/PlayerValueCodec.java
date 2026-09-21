package com.bettersettings.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Converts legacy boolean YAML and typed string values to one canonical in-memory format. */
public final class PlayerValueCodec {
    private PlayerValueCodec() {}

    public static Map<String, String> read(ConfigurationSection yaml) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : yaml.getKeys(false)) {
            Object raw = yaml.get(key);
            if (raw instanceof Boolean bool) {
                values.put(key, Boolean.toString(bool));
            } else if (raw instanceof String string) {
                values.put(key, string);
            }
        }
        return values;
    }

    public static YamlConfiguration write(Map<String, String> values) {
        YamlConfiguration yaml = new YamlConfiguration();
        values.forEach((key, value) -> {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                yaml.set(key, Boolean.parseBoolean(value));
            } else {
                yaml.set(key, value);
            }
        });
        return yaml;
    }

    public static String resolveChoice(String stored, String defaultValue, Set<String> allowedValues) {
        return stored != null && allowedValues.contains(stored) ? stored : defaultValue;
    }
}
