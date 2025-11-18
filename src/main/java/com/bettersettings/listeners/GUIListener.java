package com.bettersettings.listeners;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.api.events.PlayerSettingChangeEvent;
import com.bettersettings.api.events.PlayerToggleSettingEvent;
import com.bettersettings.data.PlayerDataManager;
import com.bettersettings.gui.SettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles GUI interaction events for the settings menu.
 * <p>
 * This listener processes clicks on setting items in the GUI, validates permissions,
 * executes toggle logic on the player's scheduler, refreshes the GUI display,
 * and handles pagination navigation.
 * </p>
 */
public class GUIListener implements Listener {

    private final BetterSettings plugin;
    private final NamespacedKey settingIdKey;

    /**
     * Creates a new GUIListener instance.
     * 
     * @param plugin The plugin instance
     */
    public GUIListener(BetterSettings plugin) {
        this.plugin = plugin;
        this.settingIdKey = new NamespacedKey(plugin, "setting_id");
    }
    
    /**
     * Handles inventory close events to clean up pagination data.
     * 
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SettingsGUI) {
            if (event.getPlayer() instanceof Player) {
                SettingsGUI.cleanup((Player) event.getPlayer());
            }
        }
    }
    
    /**
     * Handles category selection clicks.
     * 
     * @param event The inventory click event
     */
    @EventHandler
    public void onCategoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof com.bettersettings.gui.CategoryGUI)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }
        
        // Extract category ID
        String categoryId = meta.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "category"),
            PersistentDataType.STRING
        );
        
        if (categoryId != null) {
            playSound(player, "navigate");
            SettingsGUI.open(player, 0, categoryId);
        }
    }

    /**
     * Handles inventory click events in the settings GUI.
     * <p>
     * This method:
     * <ul>
     *   <li>Validates that the clicked inventory is a SettingsGUI</li>
     *   <li>Cancels the event to prevent item theft</li>
     *   <li>Handles navigation buttons (previous/next page)</li>
     *   <li>Extracts the setting ID from the clicked item</li>
     *   <li>Retrieves the Setting instance from the registry</li>
     *   <li>Checks player permission for the setting</li>
     *   <li>Executes toggle logic on the player's scheduler</li>
     *   <li>Updates the PlayerDataManager cache</li>
     *   <li>Refreshes the GUI to reflect the new state</li>
     * </ul>
     * </p>
     * 
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Validate that clicked inventory holder is SettingsGUI instance
        if (!(event.getInventory().getHolder() instanceof SettingsGUI)) {
            return;
        }

        // Cancel event to prevent item theft
        event.setCancelled(true);

        // Ensure clicker is a player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SettingsGUI gui = (SettingsGUI) event.getInventory().getHolder();
        ItemStack clickedItem = event.getCurrentItem();

        // Validate clicked item exists
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Check for navigation actions
        String action = meta.getPersistentDataContainer().get(
            new NamespacedKey(plugin, "action"),
            PersistentDataType.STRING
        );
        
        if (action != null) {
            handleNavigation(player, gui, action);
            return;
        }

        // Extract setting ID from PersistentDataContainer
        String settingId = meta.getPersistentDataContainer().get(
            settingIdKey,
            PersistentDataType.STRING
        );

        if (settingId == null) {
            return;
        }

        // Retrieve Setting instance from SettingsRegistry
        Setting setting = SettingsRegistry.getInstance().getSetting(settingId);
        if (setting == null) {
            plugin.getLogger().warning(
                "Player " + player.getName() + " clicked on unknown setting: " + settingId
            );
            return;
        }

        // Check player permission for the setting
        String permission = setting.getPermission();
        if (permission != null && !player.hasPermission(permission)) {
            // Player doesn't have permission, silently ignore
            return;
        }

        // Execute toggle logic on player's scheduler
        player.getScheduler().run(plugin, scheduledTask -> {
            PlayerDataManager dataManager = plugin.getDataManager();
            
            // Get current state
            boolean currentState = dataManager.getSetting(
                player.getUniqueId(),
                setting.getId(),
                setting.getDefaultState()
            );
            
            // Calculate new state (toggle)
            boolean newState = !currentState;
            
            // Fire pre-toggle event (cancellable)
            PlayerToggleSettingEvent preEvent = new PlayerToggleSettingEvent(player, setting.getId(), currentState, newState);
            Bukkit.getPluginManager().callEvent(preEvent);
            
            if (preEvent.isCancelled()) {
                playSound(player, "deny");
                return;
            }
            
            // Call setting's onToggle callback
            boolean allowed;
            try {
                allowed = setting.onToggle(player, newState);
            } catch (Exception e) {
                plugin.getLogger().warning(
                    "Error toggling setting " + setting.getId() + " for player " + player.getName() + ": " + e.getMessage()
                );
                e.printStackTrace();
                playSound(player, "deny");
                return;
            }
            
            if (allowed) {
                // Update PlayerDataManager cache
                dataManager.setSetting(player.getUniqueId(), setting.getId(), newState);
                
                // Fire post-toggle event
                PlayerSettingChangeEvent postEvent = new PlayerSettingChangeEvent(player, setting.getId(), currentState, newState);
                Bukkit.getPluginManager().callEvent(postEvent);
                
                // Play toggle sound
                playSound(player, "toggle");
                
                // Log toggle if enabled
                if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.toggles", false)) {
                    plugin.getLogger().info(
                        player.getName() + " toggled " + setting.getId() + " to " + newState
                    );
                }
                
                // Refresh GUI to show updated state (if configured)
                var uiConfig = plugin.getConfigManager().getUIConfig();
                if (uiConfig.getBoolean("performance.refresh-on-toggle", true)) {
                    SettingsGUI.open(player, gui.getCurrentPage(), gui.getCurrentCategory());
                }
            } else {
                // Play deny sound if toggle was not allowed
                playSound(player, "deny");
            }
            
        }, null);
    }
    
    /**
     * Handles navigation button clicks.
     * 
     * @param player The player who clicked
     * @param gui The current GUI instance
     * @param action The navigation action
     */
    private void handleNavigation(Player player, SettingsGUI gui, String action) {
        int currentPage = gui.getCurrentPage();
        int totalPages = gui.getTotalPages();
        String currentCategory = gui.getCurrentCategory();
        
        switch (action) {
            case "prev_page":
                if (currentPage > 0) {
                    playSound(player, "navigate");
                    SettingsGUI.open(player, currentPage - 1, currentCategory);
                }
                break;
            case "next_page":
                if (currentPage < totalPages - 1) {
                    playSound(player, "navigate");
                    SettingsGUI.open(player, currentPage + 1, currentCategory);
                }
                break;
            case "back":
                playSound(player, "navigate");
                SettingsGUI.open(player, 0, null);
                break;
        }
    }
    
    /**
     * Plays a sound effect for the player if enabled in config.
     * 
     * @param player The player
     * @param soundType The type of sound (toggle, navigate, deny)
     */
    private void playSound(Player player, String soundType) {
        var uiConfig = plugin.getConfigManager().getUIConfig();
        if (!uiConfig.getBoolean("sounds." + soundType + ".enabled", false)) {
            return;
        }
        
        try {
            String soundName = uiConfig.getString("sounds." + soundType + ".sound");
            float volume = (float) uiConfig.getDouble("sounds." + soundType + ".volume", 1.0);
            float pitch = (float) uiConfig.getDouble("sounds." + soundType + ".pitch", 1.0);
            
            if (soundName != null) {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            // Invalid sound name, silently ignore
        }
    }
}

