package com.bettersettings;

import com.bettersettings.commands.SettingsCommand;
import com.bettersettings.config.ConfigManager;
import com.bettersettings.core.BuiltinSettings;
import com.bettersettings.data.PlayerDataManager;
import com.bettersettings.listeners.CoreListener;
import com.bettersettings.listeners.GUIListener;
import com.bettersettings.listeners.PlayerConnectionListener;
import com.bettersettings.settings.CustomSettingLoader;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for BetterSettings.
 * <p>
 * BetterSettings is a centralized, API-first player settings management system
 * designed for Paper/Purpur/Folia servers (1.20.5+). It provides a thread-safe
 * settings registry and GUI for players to manage their preferences.
 * </p>
 * <p>
 * This plugin is fully compatible with Folia's regionized threading model and
 * uses appropriate schedulers for all operations.
 * </p>
 *
 * @since 1.0.0
 */
public class BetterSettings extends JavaPlugin {

    private static BetterSettings instance;
    private ConfigManager configManager;
    private PlayerDataManager dataManager;
    private CustomSettingLoader customSettingLoader;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask autoSaveTask;
    private io.papermc.paper.threadedregions.scheduler.ScheduledTask cacheCleanupTask;

    /**
     * Called when the plugin is enabled.
     * <p>
     * Initialization order:
     * <ol>
     *   <li>Save default configuration</li>
     *   <li>Initialize PlayerDataManager</li>
     *   <li>Register built-in settings</li>
     *   <li>Register commands</li>
     *   <li>Register event listeners</li>
     *   <li>Start scheduled tasks</li>
     * </ol>
     * </p>
     */
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize ConfigManager
        configManager = new ConfigManager(this);
        getLogger().info("Configuration loaded");
        
        // Initialize PlayerDataManager
        dataManager = new PlayerDataManager(this);
        if (configManager.getPerformanceConfig().getBoolean("logging.registrations", true)) {
            getLogger().info("PlayerDataManager initialized");
        }
        
        // Register built-in settings
        BuiltinSettings.registerAll(this);
        if (configManager.getPerformanceConfig().getBoolean("logging.registrations", true)) {
            getLogger().info("Built-in settings registered");
        }
        
        // Load custom settings
        customSettingLoader = new CustomSettingLoader(this);
        customSettingLoader.loadCustomSettings(configManager.getCustomSettingConfigs());
        
        // Register commands
        var settingsCommand = getCommand("settings");
        if (settingsCommand != null) {
            SettingsCommand cmdExecutor = new SettingsCommand(this);
            settingsCommand.setExecutor(cmdExecutor);
            settingsCommand.setTabCompleter(cmdExecutor);
            if (configManager.getPerformanceConfig().getBoolean("logging.registrations", true)) {
                getLogger().info("SettingsCommand registered");
            }
        } else {
            getLogger().warning("Failed to register /settings command - command not found in plugin.yml");
        }
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new CoreListener(this), this);
        
        if (configManager.getPerformanceConfig().getBoolean("logging.registrations", true)) {
            getLogger().info("Event listeners registered");
        }
        
        // Start scheduled tasks
        startScheduledTasks();
        
        // Initialize bStats
        int pluginId = 28034;
        new Metrics(this, pluginId);
        
        getLogger().info("BetterSettings v" + getDescription().getVersion() + " enabled!");
    }

    /**
     * Called when the plugin is disabled.
     * <p>
     * Saves all cached player data to disk before shutdown.
     * </p>
     */
    @Override
    public void onDisable() {
        // Cancel scheduled tasks
        cancelScheduledTasks();
        
        // Save all cached player data
        if (dataManager != null) {
            getLogger().info("Saving all player data...");
            dataManager.saveAllData();
            getLogger().info("All player data saved");
        }
        
        getLogger().info("BetterSettings disabled");
    }

    /**
     * Starts scheduled tasks for auto-save and cache cleanup.
     */
    private void startScheduledTasks() {
        // Auto-save task
        int autoSaveInterval = configManager.getPerformanceConfig().getInt("auto-save.interval", 6000);
        if (autoSaveInterval > 0) {
            autoSaveTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> dataManager.saveAllData(),
                autoSaveInterval,
                autoSaveInterval
            );
            if (configManager.getPerformanceConfig().getBoolean("logging.debug", false)) {
                getLogger().info("Auto-save task started (interval: " + autoSaveInterval + " ticks)");
            }
        }
        
        // Cache cleanup task
        int cleanupInterval = configManager.getPerformanceConfig().getInt("cache.cleanup-interval", 12000);
        if (cleanupInterval > 0) {
            cacheCleanupTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this,
                task -> dataManager.cleanupCache(),
                cleanupInterval,
                cleanupInterval
            );
            if (configManager.getPerformanceConfig().getBoolean("logging.debug", false)) {
                getLogger().info("Cache cleanup task started (interval: " + cleanupInterval + " ticks)");
            }
        }
    }

    /**
     * Cancels all scheduled tasks.
     */
    private void cancelScheduledTasks() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (cacheCleanupTask != null) {
            cacheCleanupTask.cancel();
            cacheCleanupTask = null;
        }
    }

    /**
     * Reloads the plugin configuration and restarts scheduled tasks.
     */
    public void reload() {
        configManager.reloadAll();
        cancelScheduledTasks();
        startScheduledTasks();
    }
    
    /**
     * Returns the ConfigManager instance.
     *
     * @return the ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Returns the singleton instance of BetterSettings.
     *
     * @return the plugin instance
     */
    public static BetterSettings getInstance() {
        return instance;
    }

    /**
     * Returns the PlayerDataManager instance.
     * <p>
     * This accessor is used by other components (commands, listeners, GUI)
     * to access player setting data.
     * </p>
     *
     * @return the PlayerDataManager instance
     */
    public PlayerDataManager getDataManager() {
        return dataManager;
    }
}
