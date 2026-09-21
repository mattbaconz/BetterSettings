package com.bettersettings.core;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingCategory;
import com.bettersettings.api.SettingsRegistry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

/** Registers immutable category definitions into the current managed candidate. */
public final class BuiltinCategories {
    private BuiltinCategories() {}

    public static void registerAll(BetterSettings plugin) {
        registerSection(plugin, plugin.getConfigManager().getUIConfig().getConfigurationSection("categories"));
        if ("donutsmp".equals(plugin.getConfigManager().getActivePreset())) {
            registerSection(plugin,
                plugin.getConfigManager().getDonutPresetConfig().getConfigurationSection("categories"));
        }
        if (!SettingsRegistry.getInstance().getCapturedCategoryIds().contains("uncategorized")) {
            SettingsRegistry.getInstance().registerCategory(new SettingCategory(
                "uncategorized", "&7Uncategorized", Material.CHEST, 999, null,
                List.of("&7Settings without a known category")
            ));
        }
    }

    private static void registerSection(BetterSettings plugin, ConfigurationSection categories) {
        if (categories == null) return;
        for (String key : categories.getKeys(false)) {
            ConfigurationSection section = categories.getConfigurationSection(key);
            if (section == null) continue;
            String id = section.getString("id");
            if (id == null || id.isBlank()) {
                plugin.getLogger().warning("Category " + key + " has no ID and was skipped");
                continue;
            }
            String permission = section.getString("permission");
            if (permission != null && (permission.isBlank() || permission.equalsIgnoreCase("null"))) {
                permission = null;
            }
            String iconName = section.getString("icon", "CHEST");
            Material icon = Material.matchMaterial(iconName == null ? "CHEST" : iconName.toUpperCase(Locale.ROOT));
            if (icon == null || icon == Material.AIR) {
                plugin.getLogger().warning("Invalid category material '" + iconName + "' for " + id + "; using CHEST");
                icon = Material.CHEST;
            }
            SettingsRegistry.getInstance().registerCategory(new SettingCategory(
                id,
                section.getString("name", key),
                icon,
                section.getInt("priority", 100),
                permission,
                section.getStringList("description")
            ));
        }
    }
}
