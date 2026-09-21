package com.bettersettings.listeners;

import com.bettersettings.BetterSettings;
import com.bettersettings.core.BuiltinSettings;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player connection events to manage data persistence.
 * <p>
 * This listener loads data during asynchronous pre-login, restores stateful native
 * settings after join, and saves a snapshot when the player quits.
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

    /** Loads persisted values before synchronous join handlers need them. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.getDataManager().loadDataNow(event.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().run(plugin,
            task -> BuiltinSettings.restorePlayerState(plugin, event.getPlayer()), null);
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
