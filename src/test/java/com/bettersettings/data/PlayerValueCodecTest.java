package com.bettersettings.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlayerValueCodecTest {

    @Test
    void readsLegacyBooleansAndPersistsTypedValues() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("toggle: true\nprivacy: friends\n");

        Map<String, String> values = PlayerValueCodec.read(yaml);

        assertEquals("true", values.get("toggle"));
        assertEquals("friends", values.get("privacy"));
        YamlConfiguration saved = PlayerValueCodec.write(values, Set.of("toggle"));
        assertInstanceOf(Boolean.class, saved.get("toggle"));
        assertEquals("friends", saved.getString("privacy"));
    }

    @Test
    void persistsBooleanLookingChoiceAsAString() {
        YamlConfiguration saved = PlayerValueCodec.write(
            Map.of("toggle", "true", "privacy", "true"),
            Set.of("toggle")
        );

        assertInstanceOf(Boolean.class, saved.get("toggle"));
        assertInstanceOf(String.class, saved.get("privacy"));
        assertEquals("true", saved.getString("privacy"));
    }

    @Test
    void retainsBooleanTypeForDormantLegacyTogglesButNotCurrentChoices() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("legacy-toggle: true\nstring-choice: 'true'\n");

        Set<String> booleanIds = PlayerValueCodec.booleanIdsForSave(
            PlayerValueCodec.readBooleanIds(yaml),
            Set.of("current-toggle"),
            Set.of("string-choice", "former-toggle-now-choice")
        );

        assertEquals(Set.of("legacy-toggle", "current-toggle"), booleanIds);
    }

    @Test
    void invalidStoredChoiceFallsBackWithoutDestroyingStoredData() {
        String resolved = PlayerValueCodec.resolveChoice("old-option", "friends", Set.of("everyone", "friends", "nobody"));

        assertEquals("friends", resolved);
    }

    @Test
    void choiceValueSurvivesSerializedRestartRoundTrip() throws Exception {
        YamlConfiguration saved = PlayerValueCodec.write(Map.of("chat_scope", "nobody"));
        YamlConfiguration restarted = new YamlConfiguration();
        restarted.loadFromString(saved.saveToString());

        assertEquals("nobody", PlayerValueCodec.read(restarted).get("chat_scope"));
        assertEquals("nobody", PlayerValueCodec.resolveChoice(
            PlayerValueCodec.read(restarted).get("chat_scope"),
            "friends",
            Set.of("everyone", "friends", "nobody")
        ));
    }
}
