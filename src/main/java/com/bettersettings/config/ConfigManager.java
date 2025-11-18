package com.bettersettings.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages multiple configuration files for better organization.
 * Supports main config, settings config, messages config, and custom setting configs.
 */
public class ConfigManager {
    
    private final Plugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        
        loadConfigs();
    }
    
    /**
     * Loads all configuration files.
     */
    private void loadConfigs() {
        // Main config
        plugin.saveDefaultConfig();
        configs.put("config", plugin.getConfig());
        
        // Settings config
        loadConfig("settings");
        
        // Messages config
        loadConfig("messages");
        
        // Performance config
        loadConfig("performance");
        
        // UI config
        loadConfig("ui");
        
        // Load custom settings from settings folder
        loadCustomSettings();
    }
    
    /**
     * Loads a specific config file.
     */
    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        if (!file.exists()) {
            plugin.saveResource(name + ".yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        configFiles.put(name, file);
    }
    
    /**
     * Loads custom setting configurations from the settings folder.
     */
    private void loadCustomSettings() {
        File settingsFolder = new File(plugin.getDataFolder(), "settings");
        if (!settingsFolder.exists()) {
            settingsFolder.mkdirs();
            createExampleCustomSetting();
        }
        
        File[] files = settingsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                configs.put("custom_" + name, config);
                configFiles.put("custom_" + name, file);
            }
        }
    }
    
    /**
     * Creates an example custom setting file.
     */
    private void createExampleCustomSetting() {
        File exampleFile = new File(plugin.getDataFolder(), "settings/example.yml");
        FileConfiguration example = new YamlConfiguration();
        
        example.set("enabled", false);
        example.set("id", "myplugin_example");
        example.set("description", "&aExample Custom Setting");
        example.set("icon", "DIAMOND");
        example.set("default-state", true);
        example.set("permission", null);
        example.set("category", "gameplay");
        example.set("priority", 50);
        
        example.setComments("enabled", java.util.Arrays.asList(
            "Example custom setting configuration",
            "Copy this file to create your own custom settings",
            "Set enabled to true to activate this setting"
        ));
        
        example.setComments("category", java.util.Arrays.asList(
            "Category ID (see ui.yml for available categories)",
            "Options: communication, display, gameplay, protection, or custom category ID",
            "Leave as null for uncategorized"
        ));
        
        example.setComments("priority", java.util.Arrays.asList(
            "Sort priority within category (lower = first)",
            "Default: 100"
        ));
        
        try {
            example.save(exampleFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create example setting", e);
        }
    }
    
    /**
     * Gets a configuration by name.
     */
    public FileConfiguration getConfig(String name) {
        return configs.getOrDefault(name, plugin.getConfig());
    }
    
    /**
     * Gets the main config.
     */
    public FileConfiguration getMainConfig() {
        return plugin.getConfig();
    }
    
    /**
     * Gets the settings config.
     */
    public FileConfiguration getSettingsConfig() {
        return configs.get("settings");
    }
    
    /**
     * Gets the messages config.
     */
    public FileConfiguration getMessagesConfig() {
        return configs.get("messages");
    }
    
    /**
     * Gets the performance config.
     */
    public FileConfiguration getPerformanceConfig() {
        return configs.get("performance");
    }
    
    /**
     * Gets the UI config.
     */
    public FileConfiguration getUIConfig() {
        return configs.get("ui");
    }
    
    /**
     * Gets all custom setting configs.
     */
    public Map<String, FileConfiguration> getCustomSettingConfigs() {
        Map<String, FileConfiguration> customConfigs = new HashMap<>();
        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            if (entry.getKey().startsWith("custom_")) {
                customConfigs.put(entry.getKey(), entry.getValue());
            }
        }
        return customConfigs;
    }
    
    /**
     * Saves a specific config file.
     */
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save " + name + ".yml", e);
            }
        }
    }
    
    /**
     * Reloads all configuration files.
     */
    public void reloadAll() {
        plugin.reloadConfig();
        configs.clear();
        configFiles.clear();
        loadConfigs();
    }
}
