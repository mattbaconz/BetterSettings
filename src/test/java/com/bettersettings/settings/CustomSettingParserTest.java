package com.bettersettings.settings;

import com.bettersettings.api.SettingType;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomSettingParserTest {

    private final CustomSettingParser parser = new CustomSettingParser(
        Set.of("communication", "uncategorized")
    );

    @Test
    void parsesCustomerNestedTestBlockAndFallsBackToUncategorized() throws Exception {
        YamlConfiguration yaml = yaml("""
            Test:
              enabled: true
              default-state: true
              permission: null
              icon: ENDER_PEARL
              description: "&eToggle private messages"
              category: "&7Uncategorized"
              priority: 1
            """);

        CustomSettingParser.Result result = parser.parse("settings.yml", yaml, Set.of());

        assertFalse(result.hasErrors(), () -> result.issues().toString());
        assertEquals(1, result.definitions().size());
        SettingDefinition definition = result.definitions().getFirst();
        assertEquals("test", definition.id());
        assertEquals("uncategorized", definition.categoryId());
        assertEquals(Material.ENDER_PEARL, definition.material());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("unknown-category")));
    }

    @Test
    void parsesLegacySingleRootAndMultipleNamedSettings() throws Exception {
        YamlConfiguration single = yaml("""
            enabled: true
            id: example_single
            description: Single
            icon: PAPER
            default-state: false
            """);
        YamlConfiguration multiple = yaml("""
            first:
              enabled: true
              id: example_first
              description: First
              icon: STONE
              default-state: true
            second:
              enabled: true
              id: example_second
              type: choice
              default: friends
              description: Second
              icon: BOOK
              options:
                everyone: Everyone
                friends: Friends
                nobody: Nobody
            """);

        assertEquals("example_single", parser.parse("single.yml", single, Set.of()).definitions().getFirst().id());
        CustomSettingParser.Result result = parser.parse("multiple.yml", multiple, Set.of());
        assertEquals(2, result.definitions().size());
        assertEquals(SettingType.CHOICE, result.definitions().get(1).type());
        assertEquals("friends", result.definitions().get(1).defaultValue());
    }

    @Test
    void reportsInvalidMaterialAndUsesSafeFallback() throws Exception {
        YamlConfiguration yaml = yaml("""
            bad-icon:
              enabled: true
              description: Bad icon
              icon: DEFINITELY_NOT_A_MATERIAL
              default-state: true
            """);

        CustomSettingParser.Result result = parser.parse("bad.yml", yaml, Set.of());

        assertEquals(Material.PAPER, result.definitions().getFirst().material());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("invalid-material")));
        assertFalse(result.hasErrors());
    }

    @Test
    void rejectsInvalidActionAndInvalidChoiceDefault() throws Exception {
        YamlConfiguration yaml = yaml("""
            invalid:
              enabled: true
              type: choice
              default: missing
              description: Invalid
              icon: PAPER
              options:
                yes: Yes
                no: No
              actions:
                - type: shell-command
                  value: rm everything
            """);

        CustomSettingParser.Result result = parser.parse("invalid.yml", yaml, Set.of());

        assertTrue(result.hasErrors());
        assertEquals(0, result.definitions().size());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("invalid-choice-default")));
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("invalid-action")));
    }

    @Test
    void parsesValidatedTypedAndLegacyActions() throws Exception {
        YamlConfiguration typed = yaml("""
            alerts:
              enabled: true
              default-state: true
              description: Alerts
              icon: BELL
              actions:
                - type: message
                  when: "true"
                  value: "&aEnabled {value}"
                - type: player-command
                  value: "alerts sync"
                - type: console-command
                  when: "false"
                  value: "audit {uuid} {old-value} {value}"
            """);
        YamlConfiguration legacy = yaml("""
            enabled: true
            id: legacy_actions
            description: Legacy actions
            icon: PAPER
            actions:
              enable:
                - "message: enabled"
              disable:
                - "player-command: alerts off"
            """);

        CustomSettingParser.Result typedResult = parser.parse("typed.yml", typed, Set.of());
        CustomSettingParser.Result legacyResult = parser.parse("legacy.yml", legacy, Set.of());

        assertFalse(typedResult.hasErrors(), () -> typedResult.issues().toString());
        assertEquals(3, typedResult.definitions().getFirst().actions().size());
        assertEquals("true", typedResult.definitions().getFirst().actions().getFirst().when());
        assertFalse(legacyResult.hasErrors(), () -> legacyResult.issues().toString());
        assertEquals("true", legacyResult.definitions().getFirst().actions().getFirst().when());
        assertEquals("false", legacyResult.definitions().getFirst().actions().get(1).when());
    }

    @Test
    void rejectsActionWhenThatCannotMatchTheSetting() throws Exception {
        YamlConfiguration yaml = yaml("""
            alerts:
              enabled: true
              default-state: true
              description: Alerts
              icon: BELL
              actions:
                - type: message
                  when: sometimes
                  value: nope
            """);

        CustomSettingParser.Result result = parser.parse("bad-when.yml", yaml, Set.of());

        assertTrue(result.hasErrors());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.code().equals("invalid-action")));
    }

    private static YamlConfiguration yaml(String value) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(value);
        return yaml;
    }
}
