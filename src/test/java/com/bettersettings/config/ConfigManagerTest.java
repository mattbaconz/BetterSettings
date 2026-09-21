package com.bettersettings.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigManagerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void strictLoaderRejectsMalformedYaml() throws Exception {
        Path valid = temporaryDirectory.resolve("valid.yml");
        Path invalid = temporaryDirectory.resolve("invalid.yml");
        Files.writeString(valid, "preset:\n  active: classic\n");
        Files.writeString(invalid, "broken: [\n");

        assertEquals("classic", ConfigManager.loadStrict(valid.toFile()).getString("preset.active"));
        assertThrows(InvalidConfigurationException.class,
            () -> ConfigManager.loadStrict(invalid.toFile()));
    }
}
