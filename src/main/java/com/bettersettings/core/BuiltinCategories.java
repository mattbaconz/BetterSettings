package com.bettersettings.core;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingCategory;
import com.bettersettings.api.SettingsRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Registers all built-in categories from ui.yml.
 */
public class BuiltinCategories {

    public static void registerAll(BetterSettings plugin) {
        SettingsRegistry registry = SettingsRegistry.getInstance();
        FileConfiguration uiConfig = plugin.getConfigManager().getUIConfig();
        
        ConfigurationSection categoriesSection = uiConfig.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("No categories section found in ui.yml");
            return;
        }
        
        for (String key : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(key);
            if (categorySection == null) continue;
            
            String id = categorySection.getString("id");
            if (id == null || id.isEmpty()) {
                plugin.getLogger().warning("Category " + key + " has no ID, skipping");
                continue;
            }
            
            String name = categorySection.getString("name", key);
            String iconName = categorySection.getString("icon", "CHEST");
            int priority = categorySection.getInt("priority", 100);
            String permission = categorySection.getString("permission");
            if (permission != null && permission.equalsIgnoreCase("null")) {
                permission = null;
            }
            List<String> description = categorySection.getStringList("description");
            
            Material icon;
            try {
                icon = Material.valueOf(iconName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid icon material '" + iconName + "' for category " + key + ", using CHEST");
                icon = Material.CHEST;
            }
            
            SettingCategory category = new SettingCategory(id, name, icon, priority, permission, description);
            registry.registerCategory(category);
            
            if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.registrations", true)) {
                plugin.getLogger().info("Registered category: " + id);
            }
        }
    }
}
