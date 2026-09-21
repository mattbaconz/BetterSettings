package com.bettersettings.api.events;

import com.bettersettings.api.SettingType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired after any typed setting value has been committed. */
public final class PlayerSettingValueChangeEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String settingId;
    private final SettingType settingType;
    private final String oldValue;
    private final String newValue;

    public PlayerSettingValueChangeEvent(@NotNull Player player, @NotNull String settingId,
                                         @NotNull SettingType settingType,
                                         @NotNull String oldValue, @NotNull String newValue) {
        super(player);
        this.settingId = settingId;
        this.settingType = settingType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getSettingId() { return settingId; }
    public SettingType getSettingType() { return settingType; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
