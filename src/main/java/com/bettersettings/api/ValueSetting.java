package com.bettersettings.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A typed player setting. Toggle values are persisted as booleans and choices as strings.
 * Existing {@link Setting} implementations remain supported through a registry adapter.
 *
 * @since 1.1.0
 */
public interface ValueSetting extends Setting {
    SettingType getType();
    String getDefaultValue();
    List<SettingOption> getOptions();
    ItemStack getIcon(Player player, String value);

    default String getBehaviorId() { return null; }

    /** Called after commit. Failures are best-effort and do not roll back storage. */
    default void onValueChange(Player player, String oldValue, String newValue) {}

    @Override
    default ItemStack getIcon(Player player, boolean state) {
        return getIcon(player, Boolean.toString(state));
    }

    @Override
    default boolean getDefaultState() { return Boolean.parseBoolean(getDefaultValue()); }

    @Override
    default boolean onToggle(Player player, boolean newState) { return true; }
}
