package com.bettersettings.listeners;

import com.bettersettings.BetterSettings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player connection events to manage data persistence.
 * <p>
 * This listener ensures that player setting data is loaded when players join
 * and saved when players quit, using async I/O operations to prevent blocking.
 * </p>
 *
 * @since 1.0.0
 */
public class PlayerConnectionListener implements Listener {

    private final BetterSettings plugin;

    /**
     * Creates a new PlayerConnectionListener.
     *
     * @param plugin The plugin instance
     */
    public PlayerConnectionListener(BetterSettings plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events.
     * <p>
     * Triggers async loading of player setting data from disk.
     * </p>
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getDataManager().loadData(event.getPlayer().getUniqueId());
    }

    /**
     * Handles player quit events.
     * <p>
     * Triggers async saving of player setting data to disk.
     * </p>
     *
     * @param event The player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().saveData(event.getPlayer().getUniqueId());
    }
}
