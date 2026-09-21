package com.bettersettings.settings;

import com.bettersettings.api.SettingOption;
import com.bettersettings.api.SettingType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Parses all supported custom-setting YAML shapes without mutating the live registry. */
public final class CustomSettingParser {

    private final Set<String> categoryIds;

    public CustomSettingParser(Set<String> categoryIds) {
        this.categoryIds = Set.copyOf(categoryIds);
    }

    public Result parse(String source, ConfigurationSection root, Set<String> knownSettingKeys) {
        List<SettingDefinition> definitions = new ArrayList<>();
        List<ValidationIssue> issues = new ArrayList<>();
        int skipped = 0;

        if (looksLikeDefinition(root)) {
            ParseOutcome outcome = parseDefinition(source, sourceKey(source), root);
            issues.addAll(outcome.issues());
            if (outcome.definition() != null) {
                definitions.add(outcome.definition());
            } else if (outcome.disabled()) {
                skipped++;
            }
        } else {
            for (String key : root.getKeys(false)) {
                if (knownSettingKeys.contains(key)) {
                    continue;
                }
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null || !looksLikeDefinition(section)) {
                    continue;
                }
                ParseOutcome outcome = parseDefinition(source, key, section);
                issues.addAll(outcome.issues());
                if (outcome.definition() != null) {
                    definitions.add(outcome.definition());
                } else if (outcome.disabled()) {
                    skipped++;
                }
            }
        }

        return new Result(List.copyOf(definitions), List.copyOf(issues), skipped);
    }

    private ParseOutcome parseDefinition(String source, String key, ConfigurationSection section) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (!section.getBoolean("enabled", true)) {
            return new ParseOutcome(null, issues, true);
        }

        String id = normalizeId(section.getString("id", key));
        if (id.isBlank()) {
            issues.add(error(source, key, "invalid-id", "Setting ID cannot be empty."));
        }

        String typeName = section.getString("type", "toggle");
        SettingType type;
        try {
            type = SettingType.valueOf(typeName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            type = SettingType.TOGGLE;
            issues.add(error(source, key + ".type", "invalid-type", "Type must be toggle or choice."));
        }

        String iconName = section.getString("icon", "PAPER");
        Material material = Material.matchMaterial(iconName == null ? "PAPER" : iconName);
        if (material == null || material == Material.AIR) {
            material = Material.PAPER;
            issues.add(warning(source, key + ".icon", "invalid-material",
                "Unknown material '" + iconName + "'; PAPER will be used."));
        }

        String category = normalizeNullable(section.getString("category"));
        if (category == null || !categoryIds.contains(category)) {
            if (category != null) {
                issues.add(warning(source, key + ".category", "unknown-category",
                    "Unknown category '" + category + "'; uncategorized will be used."));
            }
            category = "uncategorized";
        }

        List<SettingOption> options = parseOptions(section, key, source, issues);
        String defaultValue;
        if (type == SettingType.TOGGLE) {
            defaultValue = Boolean.toString(section.getBoolean("default-state",
                section.getBoolean("default", true)));
        } else {
            defaultValue = section.getString("default", section.getString("default-value"));
            if (options.size() < 2) {
                issues.add(error(source, key + ".options", "invalid-options",
                    "Choice settings require at least two unique options."));
            }
            String candidate = defaultValue;
            if (candidate == null || options.stream().noneMatch(option -> option.value().equals(candidate))) {
                issues.add(error(source, key + ".default", "invalid-choice-default",
                    "Choice default must match one of the configured option values."));
            }
        }

        List<SettingAction> actions = parseActions(section, key, source, issues, type, options);
        boolean hasErrors = issues.stream().anyMatch(issue -> issue.severity() == ValidationIssue.Severity.ERROR);
        if (hasErrors) {
            return new ParseOutcome(null, issues, false);
        }

        String permission = normalizeNullable(section.getString("permission"));
        String behavior = normalizeNullable(section.getString("behavior",
            section.getString("capability")));
        SettingDefinition definition = new SettingDefinition(
            id,
            section.getString("description", humanize(key)),
            material,
            type,
            defaultValue,
            options,
            permission,
            category,
            section.getInt("priority", 100),
            behavior,
            actions,
            source + ":" + key
        );
        return new ParseOutcome(definition, issues, false);
    }

    private List<SettingOption> parseOptions(ConfigurationSection section, String key, String source,
                                              List<ValidationIssue> issues) {
        ConfigurationSection optionsSection = section.getConfigurationSection("options");
        if (optionsSection == null) {
            return List.of();
        }
        Map<String, SettingOption> options = new LinkedHashMap<>();
        for (String value : optionsSection.getKeys(false)) {
            String normalized = value.trim();
            if (normalized.isEmpty() || options.containsKey(normalized)) {
                issues.add(error(source, key + ".options", "invalid-options", "Choice option values must be unique and non-empty."));
                continue;
            }
            options.put(normalized, new SettingOption(normalized, optionsSection.getString(value, humanize(value))));
        }
        return List.copyOf(options.values());
    }

    private List<SettingAction> parseActions(ConfigurationSection section, String key, String source,
                                              List<ValidationIssue> issues, SettingType settingType,
                                              List<SettingOption> options) {
        List<SettingAction> actions = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("actions")) {
            String typeName = raw.get("type") == null ? "" : raw.get("type").toString();
            String value = raw.get("value") == null ? "" : raw.get("value").toString().trim();
            String when = raw.get("when") == null ? "any" : raw.get("when").toString().trim();
            SettingAction.Type type = switch (typeName.toLowerCase(Locale.ROOT)) {
                case "message" -> SettingAction.Type.MESSAGE;
                case "player-command" -> SettingAction.Type.PLAYER_COMMAND;
                case "console-command" -> SettingAction.Type.CONSOLE_COMMAND;
                default -> null;
            };
            boolean validWhen = when.equalsIgnoreCase("any")
                || (settingType == SettingType.TOGGLE
                    && (when.equalsIgnoreCase("true") || when.equalsIgnoreCase("false")))
                || (settingType == SettingType.CHOICE
                    && options.stream().anyMatch(option -> option.value().equals(when)));
            if (type == null || value.isEmpty() || !validWhen) {
                issues.add(error(source, key + ".actions", "invalid-action",
                    "Actions need a supported type, non-empty value, and when matching any or a valid setting value."));
            } else {
                actions.add(new SettingAction(type, value, when));
            }
        }
        ConfigurationSection legacy = section.getConfigurationSection("actions");
        if (legacy != null) {
            parseLegacyActions(legacy.getStringList("enable"), "true", source, key, issues, actions);
            parseLegacyActions(legacy.getStringList("disable"), "false", source, key, issues, actions);
            for (String actionKey : legacy.getKeys(false)) {
                if (!actionKey.equals("enable") && !actionKey.equals("disable")) {
                    issues.add(error(source, key + ".actions." + actionKey, "invalid-action",
                        "Only enable and disable are valid legacy action groups."));
                }
            }
        } else if (section.contains("actions") && section.get("actions") != null
            && !(section.get("actions") instanceof List<?>)) {
            issues.add(error(source, key + ".actions", "invalid-action",
                "Actions must be a list of typed actions or enable/disable groups."));
        }
        return List.copyOf(actions);
    }

    private void parseLegacyActions(List<String> configured, String when, String source, String key,
                                    List<ValidationIssue> issues, List<SettingAction> actions) {
        for (String entry : configured) {
            int separator = entry.indexOf(':');
            String name = separator < 0 ? "" : entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = separator < 0 ? "" : entry.substring(separator + 1).trim();
            SettingAction.Type type = switch (name) {
                case "message" -> SettingAction.Type.MESSAGE;
                case "command", "console-command" -> SettingAction.Type.CONSOLE_COMMAND;
                case "player-command" -> SettingAction.Type.PLAYER_COMMAND;
                default -> null;
            };
            if (type == null || value.isEmpty()) {
                issues.add(error(source, key + ".actions", "invalid-action",
                    "Legacy actions must begin with message:, command:, or player-command:."));
            } else {
                actions.add(new SettingAction(type, value, when));
            }
        }
    }

    private static boolean looksLikeDefinition(ConfigurationSection section) {
        return section.contains("enabled") || section.contains("id") || section.contains("description")
            || section.contains("default-state") || section.contains("type") || section.contains("options")
            || section.contains("actions");
    }

    private static String sourceKey(String source) {
        int slash = Math.max(source.lastIndexOf('/'), source.lastIndexOf('\\'));
        String fileName = slash >= 0 ? source.substring(slash + 1) : source;
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String humanize(String value) {
        String text = value.replace('-', ' ').replace('_', ' ');
        return text.isEmpty() ? "Setting" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static ValidationIssue warning(String source, String path, String code, String message) {
        return new ValidationIssue(ValidationIssue.Severity.WARNING, code, source, path, message);
    }

    private static ValidationIssue error(String source, String path, String code, String message) {
        return new ValidationIssue(ValidationIssue.Severity.ERROR, code, source, path, message);
    }

    private record ParseOutcome(SettingDefinition definition, List<ValidationIssue> issues, boolean disabled) {}

    public record Result(List<SettingDefinition> definitions, List<ValidationIssue> issues, int skippedCount) {
        public boolean hasErrors() {
            return issues.stream().anyMatch(issue -> issue.severity() == ValidationIssue.Severity.ERROR);
        }
    }
}
