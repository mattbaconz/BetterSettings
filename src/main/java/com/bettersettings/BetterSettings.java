package com.bettersettings;

import com.bettersettings.commands.SettingsCommand;
import com.bettersettings.config.ConfigManager;
import com.bettersettings.core.BuiltinSettings;
import com.bettersettings.data.PlayerDataManager;
import com.bettersettings.listeners.CoreListener;
import com.bettersettings.listeners.GUIListener;
import com.bettersettings.listeners.PlayerConnectionListener;
import com.bettersettings.settings.CustomSettingLoader;
import com.bettersettings.settings.ValidationIssue;
import com.bettersettings.settings.ValidationReport;
import com.bettersettings.api.SettingsRegistry;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for BetterSettings.
 * <p>
 * BetterSettings is a centralized, API-first player settings management system
 * designed for Paper and compatible Paper-derived servers (1.20.5+). It provides a thread-safe
 * settings registry and GUI for players to manage their preferences.
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
        
        customSettingLoader = new CustomSettingLoader(this);
        ValidationReport initialReport = buildAndApplyRegistry();
        if (!initialReport.applied()) {
            getLogger().severe("Initial settings configuration is invalid; BetterSettings cannot start safely.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        configManager.acceptReload();
        
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
            boolean flushed = dataManager.saveAllDataAndWait(10_000L);
            getLogger().info(flushed ? "All player data saved" : "Player data flush timed out; check earlier errors");
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
        ValidationReport report = reloadSettings();
        if (!report.applied()) {
            throw new IllegalStateException("Reload rejected; the previous settings registry remains active");
        }
    }

    public ValidationReport reloadSettings() {
        if (!configManager.reloadAll()) {
            ValidationReport report = new ValidationReport(
                configManager.getActivePreset(), SettingsRegistry.getInstance().getSettings().size(),
                SettingsRegistry.getInstance().getValidationReport().unavailableCount(), 0, 0, 1, false,
                List.of(new ValidationIssue(
                    ValidationIssue.Severity.ERROR, "invalid-yaml", "configuration", "",
                    configManager.getLastLoadError() == null ? "Configuration could not be parsed." : configManager.getLastLoadError()
                ))
            );
            SettingsRegistry.getInstance().cancelManagedRegistration(report);
            return report;
        }

        ValidationReport report = buildAndApplyRegistry();
        if (report.applied()) {
            configManager.acceptReload();
            cancelScheduledTasks();
            startScheduledTasks();
        } else {
            configManager.rollbackReload();
        }
        return SettingsRegistry.getInstance().getValidationReport();
    }

    private ValidationReport buildAndApplyRegistry() {
        SettingsRegistry registry = SettingsRegistry.getInstance();
        registry.beginManagedRegistration();
        try {
            com.bettersettings.core.BuiltinCategories.registerAll(this);
            BuiltinSettings.registerAll(this);
            ValidationReport report = customSettingLoader.loadManagedSettings();
            if (!report.valid()) {
                registry.cancelManagedRegistration(report);
                logValidation(report);
                return report;
            }
            boolean applied = registry.commitManagedRegistration(report);
            ValidationReport finalReport = registry.getValidationReport();
            logValidation(finalReport);
            return applied ? finalReport : report.withApplied(false);
        } catch (RuntimeException exception) {
            ValidationReport report = new ValidationReport(
                configManager.getActivePreset(), registry.getSettings().size(), 0, 0, 0, 1, false,
                List.of(new ValidationIssue(
                    ValidationIssue.Severity.ERROR, "registry-build", "configuration", "", exception.getMessage()
                ))
            );
            registry.cancelManagedRegistration(report);
            getLogger().log(java.util.logging.Level.SEVERE, "Settings registry build failed; live snapshot was kept", exception);
            return report;
        }
    }

    private void logValidation(ValidationReport report) {
        getLogger().info("Settings registry: loaded=" + report.loadedCount()
            + ", unavailable=" + report.unavailableCount()
            + ", skipped=" + report.skippedCount()
            + ", duplicate=" + report.duplicateCount()
            + ", invalid=" + report.invalidCount()
            + ", preset=" + report.activePreset());
        report.issues().forEach(issue -> {
            String message = issue.source() + (issue.path().isBlank() ? "" : " [" + issue.path() + "]")
                + ": " + issue.message();
            if (issue.severity() == ValidationIssue.Severity.ERROR) getLogger().severe(message);
            else getLogger().warning(message);
        });
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
