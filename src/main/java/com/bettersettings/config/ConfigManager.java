package com.bettersettings.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

/** Loads every owner file strictly and swaps the complete configuration set only on success. */
public final class ConfigManager {

    private final Plugin plugin;
    private volatile Map<String, FileConfiguration> configs = Map.of();
    private volatile Map<String, File> configFiles = Map.of();
    private volatile FileConfiguration packagedSettings;
    private volatile FileConfiguration donutPreset;
    private volatile FileConfiguration effectiveSettings;
    private volatile String lastLoadError;
    private ConfigState previousState;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        ensureFiles();
        if (!reloadAll()) {
            throw new IllegalStateException("Unable to load BetterSettings configuration: " + lastLoadError);
        }
    }

    private void ensureFiles() {
        plugin.saveDefaultConfig();
        for (String name : new String[]{ "settings", "messages", "performance", "ui" }) {
            File file = new File(plugin.getDataFolder(), name + ".yml");
            if (!file.exists()) plugin.saveResource(name + ".yml", false);
        }
        File settingsFolder = new File(plugin.getDataFolder(), "settings");
        if (!settingsFolder.exists() && settingsFolder.mkdirs()) {
            createExampleCustomSetting(settingsFolder);
        }
    }

    private void createExampleCustomSetting(File settingsFolder) {
        File exampleFile = new File(settingsFolder, "example.yml");
        if (exampleFile.exists()) return;
        YamlConfiguration example = new YamlConfiguration();
        example.set("enabled", false);
        example.set("id", "myplugin_example");
        example.set("description", "&aExample Custom Setting");
        example.set("icon", "DIAMOND");
        example.set("default-state", true);
        example.set("permission", null);
        example.set("category", "gameplay");
        example.set("priority", 50);
        try {
            example.save(exampleFile);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create settings/example.yml", exception);
        }
    }

    /**
     * Strictly parses all files into a candidate and publishes them together.
     * A malformed file leaves every previously loaded configuration object live.
     */
    public synchronized boolean reloadAll() {
        try {
            Map<String, FileConfiguration> candidate = new LinkedHashMap<>();
            Map<String, File> files = new LinkedHashMap<>();
            loadOwner(candidate, files, "config");
            loadOwner(candidate, files, "settings");
            loadOwner(candidate, files, "messages");
            loadOwner(candidate, files, "performance");
            loadOwner(candidate, files, "ui");

            File settingsFolder = new File(plugin.getDataFolder(), "settings");
            File[] customFiles = settingsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (customFiles != null) {
                java.util.Arrays.sort(customFiles, java.util.Comparator.comparing(File::getName));
                for (File file : customFiles) {
                    String name = file.getName().substring(0, file.getName().length() - 4);
                    candidate.put("custom_" + name, loadStrict(file));
                    files.put("custom_" + name, file);
                }
            }

            FileConfiguration packaged = loadResourceStrict("settings.yml");
            FileConfiguration preset = loadResourceStrict("presets/donutsmp.yml");
            String activePreset = normalizePreset(candidate.get("config").getString("preset.active", "classic"));
            FileConfiguration presetOverrides = "donutsmp".equals(activePreset)
                ? sectionCopy(preset.getConfigurationSection("overrides"))
                : new YamlConfiguration();
            FileConfiguration effective = ConfigurationMerger.mergePreset(
                packaged, presetOverrides, candidate.get("settings")
            );

            if (!configs.isEmpty()) {
                previousState = new ConfigState(
                    configs, configFiles, packagedSettings, donutPreset, effectiveSettings
                );
            }
            configs = Map.copyOf(candidate);
            configFiles = Map.copyOf(files);
            packagedSettings = packaged;
            donutPreset = preset;
            effectiveSettings = effective;
            lastLoadError = null;
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            lastLoadError = exception.getMessage();
            plugin.getLogger().log(Level.SEVERE,
                "Configuration reload rejected; the current live settings remain active. " + exception.getMessage(), exception);
            return false;
        }
    }

    private void loadOwner(Map<String, FileConfiguration> target, Map<String, File> files, String name)
        throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        target.put(name, loadStrict(file));
        files.put(name, file);
    }

    static YamlConfiguration loadStrict(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        return yaml;
    }

    private YamlConfiguration loadResourceStrict(String path) throws IOException, InvalidConfigurationException {
        InputStream stream = plugin.getResource(path);
        if (stream == null) throw new IOException("Missing packaged resource " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        }
    }

    private static YamlConfiguration sectionCopy(org.bukkit.configuration.ConfigurationSection section) {
        return section == null ? new YamlConfiguration() : ConfigurationMerger.merge(section);
    }

    public static String normalizePreset(String value) {
        return value != null && value.equalsIgnoreCase("donutsmp") ? "donutsmp" : "classic";
    }

    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, configs.get("config"));
    }

    public FileConfiguration getMainConfig() { return configs.get("config"); }
    public FileConfiguration getSettingsConfig() { return configs.get("settings"); }
    public FileConfiguration getPackagedSettingsConfig() { return packagedSettings; }
    public FileConfiguration getEffectiveSettingsConfig() { return effectiveSettings; }
    public FileConfiguration getMessagesConfig() { return configs.get("messages"); }
    public FileConfiguration getPerformanceConfig() { return configs.get("performance"); }
    public FileConfiguration getUIConfig() { return configs.get("ui"); }
    public FileConfiguration getDonutPresetConfig() { return donutPreset; }
    public String getActivePreset() {
        return normalizePreset(getMainConfig().getString("preset.active", "classic"));
    }
    public boolean hideUnavailable() {
        return getMainConfig().getBoolean("preset.hide-unavailable", false);
    }
    public String getLastLoadError() { return lastLoadError; }

    /** Marks the parsed candidate as the accepted live configuration. */
    public synchronized void acceptReload() {
        previousState = null;
    }

    /** Restores the last accepted configuration after semantic registry validation fails. */
    public synchronized void rollbackReload() {
        if (previousState == null) return;
        configs = previousState.configs();
        configFiles = previousState.configFiles();
        packagedSettings = previousState.packagedSettings();
        donutPreset = previousState.donutPreset();
        effectiveSettings = previousState.effectiveSettings();
        previousState = null;
    }

    public Map<String, FileConfiguration> getCustomSettingConfigs() {
        Map<String, FileConfiguration> result = new LinkedHashMap<>();
        configs.forEach((name, config) -> {
            if (name.startsWith("custom_")) result.put(name, config);
        });
        return Map.copyOf(result);
    }

    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        if (config == null || file == null) return;
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + name + ".yml", exception);
        }
    }

    private record ConfigState(
        Map<String, FileConfiguration> configs,
        Map<String, File> configFiles,
        FileConfiguration packagedSettings,
        FileConfiguration donutPreset,
        FileConfiguration effectiveSettings
    ) {}
}
