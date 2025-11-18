package com.bettersettings.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a player toggles a setting.
 * <p>
 * This event is cancellable. If cancelled, the setting will not be toggled
 * and the player's preference will remain unchanged.
 * </p>
 * <p>
 * This event is called synchronously on the player's region scheduler,
 * making it safe to perform player-specific operations.
 * </p>
 *
 * @since 1.0.0
 */
public class PlayerToggleSettingEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final String settingId;
    private final boolean oldState;
    private final boolean newState;

    /**
     * Constructs a new PlayerToggleSettingEvent.
     *
     * @param player the player toggling the setting
     * @param settingId the unique identifier of the setting being toggled
     * @param oldState the current state before toggle
     * @param newState the new state after toggle
     */
    public PlayerToggleSettingEvent(@NotNull Player player, @NotNull String settingId, boolean oldState, boolean newState) {
        super(player);
        this.settingId = settingId;
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * Gets the unique identifier of the setting being toggled.
     *
     * @return the setting ID
     */
    @NotNull
    public String getSettingId() {
        return settingId;
    }

    /**
     * Gets the current state of the setting before the toggle.
     *
     * @return the old state
     */
    public boolean getOldState() {
        return oldState;
    }

    /**
     * Gets the new state of the setting after the toggle.
     *
     * @return the new state
     */
    public boolean getNewState() {
        return newState;
    }

    /**
     * Checks if the setting is being enabled.
     *
     * @return true if the setting is being enabled, false if being disabled
     */
    public boolean isEnabling() {
        return newState;
    }

    /**
     * Checks if the setting is being disabled.
     *
     * @return true if the setting is being disabled, false if being enabled
     */
    public boolean isDisabling() {
        return !newState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
