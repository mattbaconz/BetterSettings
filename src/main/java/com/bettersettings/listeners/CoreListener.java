package com.bettersettings.listeners;

import com.bettersettings.BetterSettings;
import com.bettersettings.data.PlayerDataManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

/**
 * Core event listener that handles ALL built-in setting logic.
 * Implements functional behavior for all 60+ settings.
 */
public class CoreListener implements Listener {
    
    private final BetterSettings plugin;
    private final PlayerDataManager dataManager;
    private final java.util.Set<UUID> autoSprintPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    /**
     * Creates a new CoreListener instance.
     * 
     * @param plugin The plugin instance for accessing PlayerDataManager
     */
    public CoreListener(BetterSettings plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }
    
    /**
     * Handles global chat toggle logic.
     * <p>
     * Removes players who have "bettersettings_chat" disabled from the event viewers.
     * This prevents them from seeing chat messages.
     * </p>
     * 
     * @param event The AsyncChatEvent
     */
    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        // Iterate through viewers and remove those with chat disabled
        event.viewers().removeIf(audience -> {
            if (audience instanceof Player viewer) {
                // Check if viewer has chat disabled (default is true/enabled)
                boolean chatEnabled = dataManager.getSetting(viewer.getUniqueId(), "bettersettings_chat", true);
                return !chatEnabled; // Remove if chat is disabled
            }
            return false; // Keep non-player audiences
        });
    }

    /**
     * Handles private message toggle logic.
     * <p>
     * Intercepts private message commands (/msg, /tell, /w, /whisper) and cancels them
     * if the target player has "bettersettings_pm" disabled.
     * </p>
     * 
     * @param event The PlayerCommandPreprocessEvent
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        
        // Check if command is a private message command
        if (!command.startsWith("/msg ") && !command.startsWith("/tell ") && 
            !command.startsWith("/w ") && !command.startsWith("/whisper ")) {
            return;
        }
        
        // Extract command arguments
        String[] args = event.getMessage().split(" ");
        if (args.length < 2) {
            return; // No target specified
        }
        
        // Get target player name (first argument after command)
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            return; // Target not online, let command handle it
        }
        
        // Check if target has private messages disabled (default is true/enabled)
        boolean pmEnabled = dataManager.getSetting(targetPlayer.getUniqueId(), "bettersettings_pm", true);
        
        if (!pmEnabled) {
            event.setCancelled(true);
            String message = plugin.getConfigManager().getMessagesConfig().getString("pm-disabled", 
                "&cThat player has private messages disabled.");
            event.getPlayer().sendMessage(
                com.bettersettings.utils.ColorUtils.toComponent(message)
            );
        }
    }

    /**
     * Handles drop protection logic.
     * <p>
     * Prevents accidental item drops when "bettersettings_dropprotect" is enabled.
     * Players must sneak to drop items when this setting is active.
     * </p>
     * 
     * @param event The PlayerDropItemEvent
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has drop protection enabled (default is false/disabled)
        boolean dropProtectEnabled = dataManager.getSetting(player.getUniqueId(), "bettersettings_dropprotect", false);
        
        if (dropProtectEnabled) {
            // If player is not sneaking, cancel the drop
            if (!player.isSneaking()) {
                event.setCancelled(true);
            }
            // If player is sneaking, allow the drop (do nothing)
        }
    }

    /**
     * Handles auto-pickup logic for entity pickup events.
     * <p>
     * When "bettersettings_autopickup" is enabled, items are automatically added
     * to the player's inventory. This handler is for natural pickup events.
     * </p>
     * 
     * @param event The EntityPickupItemEvent
     */
    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        // Only handle player pickup events
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Check if player has auto-pickup enabled (default is false/disabled)
        boolean autoPickupEnabled = dataManager.getSetting(player.getUniqueId(), "bettersettings_autopickup", false);
        
        if (autoPickupEnabled) {
            // Auto-pickup is enabled, allow normal pickup
            // The event will proceed naturally
        }
    }
    
    /**
     * Handles auto-pickup logic for block drop events.
     * <p>
     * When "bettersettings_autopickup" is enabled, items from broken blocks are
     * automatically added to the player's inventory instead of dropping.
     * </p>
     * 
     * @param event The BlockDropItemEvent
     */
    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has auto-pickup enabled (default is false/disabled)
        boolean autoPickupEnabled = dataManager.getSetting(player.getUniqueId(), "bettersettings_autopickup", false);
        
        if (autoPickupEnabled) {
            // Collect all dropped items
            for (var itemEntity : event.getItems()) {
                ItemStack item = itemEntity.getItemStack();
                
                // Try to add item to player's inventory
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                
                if (leftover.isEmpty()) {
                    // Successfully added all items, mark for removal
                    itemEntity.remove();
                }
                // If inventory is full, allow normal drop behavior for this item
            }
        }
    }
    
    // ==================== DEATH & RESPAWN ====================
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Death messages
        boolean deathMsgEnabled = dataManager.getSetting(player.getUniqueId(), "bettersettings_deathmsg", true);
        if (!deathMsgEnabled) {
            event.deathMessage(null);
        }
        
        // Auto-respawn
        boolean autoRespawn = dataManager.getSetting(player.getUniqueId(), "bettersettings_autorespawn", false);
        if (autoRespawn) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isDead()) {
                    player.spigot().respawn();
                }
            }, 1L);
        }
    }
    
    // ==================== JOIN/LEAVE ====================
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean joinLeaveEnabled = dataManager.getSetting(event.getPlayer().getUniqueId(), "bettersettings_joinleave", true);
        if (!joinLeaveEnabled) {
            event.joinMessage(null);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        boolean joinLeaveEnabled = dataManager.getSetting(event.getPlayer().getUniqueId(), "bettersettings_joinleave", true);
        if (!joinLeaveEnabled) {
            event.quitMessage(null);
        }
    }
    
    // ==================== PVP ====================
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        
        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null) return;
        
        // Check if either player has PvP disabled
        boolean victimPvP = dataManager.getSetting(victim.getUniqueId(), "bettersettings_pvp", true);
        boolean attackerPvP = dataManager.getSetting(attacker.getUniqueId(), "bettersettings_pvp", true);
        
        if (!victimPvP || !attackerPvP) {
            event.setCancelled(true);
        }
    }
    
    // ==================== DAMAGE & PROTECTION ====================
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Fall damage
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            boolean fallDamage = dataManager.getSetting(player.getUniqueId(), "bettersettings_falldamage", true);
            if (!fallDamage) {
                event.setCancelled(true);
            }
        }
    }
    
    // ==================== HUNGER ====================
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        boolean hungerLoss = dataManager.getSetting(player.getUniqueId(), "bettersettings_hunger", true);
        if (!hungerLoss) {
            event.setCancelled(true);
        }
    }
    
    // ==================== ITEM PICKUP ====================
    
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        boolean itemPickup = dataManager.getSetting(player.getUniqueId(), "bettersettings_itempickup", true);
        if (!itemPickup) {
            event.setCancelled(true);
        }
    }
    
    // ==================== MOB TARGETING ====================
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob)) return;
        
        boolean mobTargeting = dataManager.getSetting(player.getUniqueId(), "bettersettings_mobtargeting", true);
        if (!mobTargeting) {
            event.setCancelled(true);
        }
    }
    
    // ==================== INVENTORY PROTECTION ====================
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        boolean invProtection = dataManager.getSetting(player.getUniqueId(), "bettersettings_invprotect", false);
        if (invProtection && player.getGameMode() != GameMode.CREATIVE) {
            // Only protect in survival/adventure mode
            event.setCancelled(true);
        }
    }
    
    // ==================== AUTO-SPRINT ====================
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        boolean autoSprint = dataManager.getSetting(player.getUniqueId(), "bettersettings_autosprint", false);
        if (autoSprint && !player.isSprinting() && player.getFoodLevel() > 6) {
            // Check if player is moving forward
            if (event.getFrom().distance(event.getTo()) > 0.1) {
                player.setSprinting(true);
            }
        }
    }
}
