package com.bettersettings.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationMergerTest {

    @Test
    void appliesPackagedThenPresetThenOwnerPrecedence() throws Exception {
        YamlConfiguration packaged = yaml("chat:\n  description: Packaged\n  icon: PAPER\n");
        YamlConfiguration preset = yaml("chat:\n  description: Preset\n");
        YamlConfiguration owner = yaml("chat:\n  description: Owner\n");

        YamlConfiguration merged = ConfigurationMerger.merge(packaged, preset, owner);

        assertEquals("Owner", merged.getString("chat.description"));
        assertEquals("PAPER", merged.getString("chat.icon"));
    }

    @Test
    void cleanPackagedOwnerCopyDoesNotUndoPresetWhileOwnerEditsStillWin() throws Exception {
        YamlConfiguration packaged = yaml("chat:\n  description: Packaged\n  icon: PAPER\n  category: communication\n");
        YamlConfiguration preset = yaml("chat:\n  description: Preset\n  category: donut_chat\n");
        YamlConfiguration owner = yaml("chat:\n  description: Packaged\n  icon: DIAMOND\n  category: communication\n");

        YamlConfiguration merged = ConfigurationMerger.mergePreset(packaged, preset, owner);

        assertEquals("Preset", merged.getString("chat.description"));
        assertEquals("donut_chat", merged.getString("chat.category"));
        assertEquals("DIAMOND", merged.getString("chat.icon"));
    }

    private static YamlConfiguration yaml(String input) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(input);
        return yaml;
    }
}
