package com.bettersettings.settings;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingCategory;
import com.bettersettings.api.SettingOption;
import com.bettersettings.api.SettingType;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.api.ValueSetting;
import com.bettersettings.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Runtime setting backed by one immutable, validated YAML definition. */
final class ConfiguredValueSetting implements ValueSetting {
    private final BetterSettings plugin;
    private final SettingDefinition definition;

    ConfiguredValueSetting(BetterSettings plugin, SettingDefinition definition) {
        this.plugin = plugin;
        this.definition = definition;
    }

    @Override public String getId() { return definition.id(); }
    @Override public String getDescription() { return definition.description(); }
    @Override public ItemStack getIcon(Player player, String value) { return new ItemStack(definition.material()); }
    @Override public SettingType getType() { return definition.type(); }
    @Override public String getDefaultValue() { return definition.defaultValue(); }
    @Override public List<SettingOption> getOptions() { return definition.options(); }
    @Override public String getPermission() { return definition.permission(); }
    @Override public int getPriority() { return definition.priority(); }
    @Override public String getBehaviorId() { return definition.behaviorId(); }

    @Override
    public SettingCategory getCategory() {
        SettingCategory category = SettingsRegistry.getInstance().getCategory(definition.categoryId());
        return category != null ? category : SettingsRegistry.getInstance().getCategory("uncategorized");
    }

    @Override
    public void onValueChange(Player player, String oldValue, String newValue) {
        for (SettingAction action : definition.actions()) {
            if (!action.matches(newValue)) continue;
            String value = replaceVariables(action.value(), player, oldValue, newValue);
            try {
                switch (action.type()) {
                    case MESSAGE -> player.sendMessage(ColorUtils.toComponent(value));
                    case PLAYER_COMMAND -> player.performCommand(stripSlash(value));
                    case CONSOLE_COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(value));
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Best-effort action failed for setting " + getId()
                    + " and player " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    private static String replaceVariables(String value, Player player, String oldValue, String newValue) {
        return value.replace("{player}", player.getName())
            .replace("{uuid}", player.getUniqueId().toString())
            .replace("{old-value}", oldValue)
            .replace("{value}", newValue);
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }
}
