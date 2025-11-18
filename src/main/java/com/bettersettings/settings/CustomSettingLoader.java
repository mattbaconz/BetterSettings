package com.bettersettings.settings;

import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingsRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * Loads custom settings from YAML configuration files.
 * Allows server owners to create settings without coding.
 */
public class CustomSettingLoader {
    
    private final Plugin plugin;
    
    public CustomSettingLoader(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Loads all custom settings from configuration files.
     */
    public void loadCustomSettings(Map<String, FileConfiguration> customConfigs) {
        SettingsRegistry registry = SettingsRegistry.getInstance();
        
        for (Map.Entry<String, FileConfiguration> entry : customConfigs.entrySet()) {
            String configName = entry.getKey().replace("custom_", "");
            FileConfiguration config = entry.getValue();
            
            if (!config.getBoolean("enabled", false)) {
                if (plugin.getConfig().getBoolean("logging.debug", false)) {
                    plugin.getLogger().info("Skipping disabled custom setting: " + configName);
                }
                continue;
            }
            
            try {
                Setting setting = createSettingFromConfig(config);
                registry.registerSetting(setting);
                
                if (plugin.getConfig().getBoolean("logging.registrations", true)) {
                    plugin.getLogger().info("Loaded custom setting: " + setting.getId());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load custom setting from " + configName + ".yml: " + e.getMessage());
            }
        }
    }
    
    /**
     * Creates a Setting instance from a configuration.
     */
    private Setting createSettingFromConfig(FileConfiguration config) {
        String id = config.getString("id", "custom_unknown");
        String description = config.getString("description", "Custom Setting");
        String iconMaterial = config.getString("icon", "PAPER");
        boolean defaultState = config.getBoolean("default-state", true);
        String permission = config.getString("permission");
        String category = config.getString("category", null);
        int priority = config.getInt("priority", 100);
        
        // Validate material
        Material material;
        try {
            material = Material.valueOf(iconMaterial.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + iconMaterial + "' for setting " + id + ", using PAPER");
            material = Material.PAPER;
        }
        
        final Material finalMaterial = material;
        final String finalPermission = (permission == null || permission.equalsIgnoreCase("null")) ? null : permission;
        final String finalCategory = (category == null || category.equalsIgnoreCase("null")) ? null : category;
        
        return new Setting() {
            @Override
            public String getId() {
                return id;
            }
            
            @Override
            public String getDescription() {
                return description;
            }
            
            @Override
            public ItemStack getIcon(Player player, boolean state) {
                return new ItemStack(finalMaterial);
            }
            
            @Override
            public boolean getDefaultState() {
                return defaultState;
            }
            
            @Override
            public String getPermission() {
                return finalPermission;
            }
            
            @Override
            public com.bettersettings.api.SettingCategory getCategory() {
                if (finalCategory != null) {
                    return SettingsRegistry.getInstance().getCategory(finalCategory);
                }
                return null;
            }
            
            @Override
            public int getPriority() {
                return priority;
            }
            
            @Override
            public boolean onToggle(Player player, boolean newState) {
                // Custom settings don't have built-in behavior
                // Third-party plugins should listen to setting changes via API
                return true;
            }
        };
    }
}
