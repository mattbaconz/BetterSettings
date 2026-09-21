package com.bettersettings.api;

import org.bukkit.entity.Player;

/** A provider-owned capability that applies a typed setting after its value is committed. */
public interface SettingBehavior {
    String getId();
    default boolean isAvailable() { return true; }
    void apply(Player player, ValueSetting setting, String oldValue, String newValue) throws Exception;
}
