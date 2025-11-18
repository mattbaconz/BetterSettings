package com.bettersettings.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a player successfully toggles a setting.
 * <p>
 * This event is not cancellable as the setting has already been changed.
 * Use {@link PlayerToggleSettingEvent} to prevent the toggle.
 * </p>
 * <p>
 * This event is called synchronously on the player's region scheduler,
 * making it safe to perform player-specific operations.
 * </p>
 *
 * @since 1.0.0
 */
public class PlayerSettingChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String settingId;
    private final boolean oldState;
    private final boolean newState;

    /**
     * Constructs a new PlayerSettingChangeEvent.
     *
     * @param player the player who toggled the setting
     * @param settingId the unique identifier of the setting that was toggled
     * @param oldState the state before the toggle
     * @param newState the state after the toggle
     */
    public PlayerSettingChangeEvent(@NotNull Player player, @NotNull String settingId, boolean oldState, boolean newState) {
        super(player);
        this.settingId = settingId;
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * Gets the unique identifier of the setting that was toggled.
     *
     * @return the setting ID
     */
    @NotNull
    public String getSettingId() {
        return settingId;
    }

    /**
     * Gets the state of the setting before the toggle.
     *
     * @return the old state
     */
    public boolean getOldState() {
        return oldState;
    }

    /**
     * Gets the state of the setting after the toggle.
     *
     * @return the new state
     */
    public boolean getNewState() {
        return newState;
    }

    /**
     * Checks if the setting was enabled.
     *
     * @return true if the setting was enabled, false if disabled
     */
    public boolean wasEnabled() {
        return newState;
    }

    /**
     * Checks if the setting was disabled.
     *
     * @return true if the setting was disabled, false if enabled
     */
    public boolean wasDisabled() {
        return !newState;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
