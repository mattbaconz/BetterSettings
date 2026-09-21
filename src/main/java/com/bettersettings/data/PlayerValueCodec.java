package com.bettersettings.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    public static Set<String> readBooleanIds(ConfigurationSection yaml) {
        Set<String> ids = new LinkedHashSet<>();
        for (String key : yaml.getKeys(false)) {
            if (yaml.get(key) instanceof Boolean) ids.add(key);
        }
        return Set.copyOf(ids);
    }

    public static Set<String> booleanIdsForSave(Set<String> persistedBooleanIds,
                                                Set<String> currentToggleIds,
                                                Set<String> currentChoiceIds) {
        Set<String> ids = new LinkedHashSet<>(persistedBooleanIds);
        ids.addAll(currentToggleIds);
        ids.removeAll(currentChoiceIds);
        return Set.copyOf(ids);
    }

    public static YamlConfiguration write(Map<String, String> values) {
        Set<String> legacyBooleanIds = values.entrySet().stream()
            .filter(entry -> isBoolean(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return write(values, legacyBooleanIds);
    }

    public static YamlConfiguration write(Map<String, String> values, Set<String> toggleIds) {
        YamlConfiguration yaml = new YamlConfiguration();
        values.forEach((key, value) -> {
            if (toggleIds.contains(key) && isBoolean(value)) {
                yaml.set(key, Boolean.parseBoolean(value));
            } else {
                yaml.set(key, value);
            }
        });
        return yaml;
    }

    private static boolean isBoolean(String value) {
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
    }

    public static String resolveChoice(String stored, String defaultValue, Set<String> allowedValues) {
        return stored != null && allowedValues.contains(stored) ? stored : defaultValue;
    }
}
