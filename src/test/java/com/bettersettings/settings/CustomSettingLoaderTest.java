package com.bettersettings.settings;

import com.bettersettings.config.ConfigurationMerger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CustomSettingLoaderTest {

    @Test
    void ownerCanOverridePresetDefinitionWithoutCopyingEveryField() throws Exception {
        YamlConfiguration preset = yaml("""
            auction_alerts:
              id: auction_alerts
              enabled: true
              description: Preset label
              behavior: auction-alerts
              icon: GOLD_INGOT
            """);
        YamlConfiguration owner = yaml("""
            auction_alerts:
              enabled: false
              description: Owner label
            unrelated:
              id: unrelated
              enabled: true
            """);

        YamlConfiguration overrides = CustomSettingLoader.ownerSubset(owner, Set.of("auction_alerts"));
        YamlConfiguration merged = ConfigurationMerger.merge(preset, overrides);

        assertFalse(merged.getBoolean("auction_alerts.enabled"));
        assertEquals("Owner label", merged.getString("auction_alerts.description"));
        assertEquals("auction-alerts", merged.getString("auction_alerts.behavior"));
        assertEquals("GOLD_INGOT", merged.getString("auction_alerts.icon"));
        assertFalse(merged.contains("unrelated"));
    }

    private static YamlConfiguration yaml(String input) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(input);
        return yaml;
    }
}
