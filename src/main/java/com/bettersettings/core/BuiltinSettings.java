package com.bettersettings.core;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingsRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Registers all built-in settings provided by BetterSettings.
 * Settings are now fully configurable via settings.yml.
 */
public class BuiltinSettings {

    public static void registerAll(BetterSettings plugin) {
        SettingsRegistry registry = SettingsRegistry.getInstance();
        FileConfiguration config = plugin.getConfigManager().getSettingsConfig();
        
        // Communication settings
        if (config.getBoolean("chat.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "chat", "bettersettings_chat", null));
        }
        if (config.getBoolean("private-messages.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "private-messages", "bettersettings_pm", null));
        }
        if (config.getBoolean("death-messages.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "death-messages", "bettersettings_deathmsg", null));
        }
        if (config.getBoolean("join-leave-messages.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "join-leave-messages", "bettersettings_joinleave", null));
        }
        if (config.getBoolean("teleport-requests.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "teleport-requests", "bettersettings_tprequest", null));
        }
        if (config.getBoolean("trade-requests.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "trade-requests", "bettersettings_traderequest", null));
        }
        if (config.getBoolean("friend-requests.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "friend-requests", "bettersettings_friendrequest", null));
        }
        
        // Display settings
        if (config.getBoolean("scoreboard.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "scoreboard", "bettersettings_scoreboard", BuiltinSettings::handleScoreboardToggle));
        }
        if (config.getBoolean("player-visibility.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "player-visibility", "bettersettings_visibility", BuiltinSettings::handleVisibilityToggle));
        }
        if (config.getBoolean("particle-effects.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "particle-effects", "bettersettings_particles", null));
        }
        if (config.getBoolean("sound-effects.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "sound-effects", "bettersettings_sounds", null));
        }
        if (config.getBoolean("weather.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "weather", "bettersettings_weather", BuiltinSettings::handleWeatherToggle));
        }
        if (config.getBoolean("time.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "time", "bettersettings_time", BuiltinSettings::handleTimeToggle));
        }
        if (config.getBoolean("damage-indicators.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "damage-indicators", "bettersettings_damageindicators", null));
        }
        if (config.getBoolean("night-vision.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "night-vision", "bettersettings_nightvision", BuiltinSettings::handleNightVisionToggle));
        }
        
        // Gameplay settings
        if (config.getBoolean("auto-pickup.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "auto-pickup", "bettersettings_autopickup", null));
        }
        if (config.getBoolean("flight.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "flight", "bettersettings_flight", BuiltinSettings::handleFlightToggle));
        }
        if (config.getBoolean("pvp.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "pvp", "bettersettings_pvp", null));
        }
        if (config.getBoolean("auto-sprint.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "auto-sprint", "bettersettings_autosprint", null));
        }
        
        // Protection settings
        if (config.getBoolean("drop-protection.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "drop-protection", "bettersettings_dropprotect", null));
        }
        if (config.getBoolean("inventory-protection.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "inventory-protection", "bettersettings_invprotect", null));
        }
        
        // Notification settings
        if (config.getBoolean("mob-spawn-notifications.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "mob-spawn-notifications", "bettersettings_mobspawn", null));
        }
        if (config.getBoolean("achievement-notifications.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "achievement-notifications", "bettersettings_achievements", null));
        }
        if (config.getBoolean("action-bar-messages.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "action-bar-messages", "bettersettings_actionbar", null));
        }
        if (config.getBoolean("boss-bar-notifications.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "boss-bar-notifications", "bettersettings_bossbar", null));
        }
        if (config.getBoolean("title-messages.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "title-messages", "bettersettings_titles", null));
        }
        if (config.getBoolean("keep-inventory-reminder.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "keep-inventory-reminder", "bettersettings_keepinvreminder", null));
        }
        if (config.getBoolean("mob-griefing-notifications.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "mob-griefing-notifications", "bettersettings_mobgriefing", null));
        }
        if (config.getBoolean("fire-spread-warnings.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "fire-spread-warnings", "bettersettings_firespread", null));
        }
        if (config.getBoolean("explosion-warnings.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "explosion-warnings", "bettersettings_explosions", null));
        }
        if (config.getBoolean("command-spy.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "command-spy", "bettersettings_commandspy", null));
        }
        if (config.getBoolean("social-spy.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "social-spy", "bettersettings_socialspy", null));
        }
        
        // Communication settings (additional)
        if (config.getBoolean("chat-mentions.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "chat-mentions", "bettersettings_mentions", null));
        }
        if (config.getBoolean("dm-sound.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "dm-sound", "bettersettings_dmsound", null));
        }
        
        // Gameplay settings (additional)
        if (config.getBoolean("auto-respawn.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "auto-respawn", "bettersettings_autorespawn", null));
        }
        if (config.getBoolean("god-mode.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "god-mode", "bettersettings_godmode", handleGodModeToggle));
        }
        if (config.getBoolean("speed-boost.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "speed-boost", "bettersettings_speed", handleSpeedToggle));
        }
        if (config.getBoolean("jump-boost.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "jump-boost", "bettersettings_jump", handleJumpToggle));
        }
        if (config.getBoolean("water-breathing.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "water-breathing", "bettersettings_waterbreathing", handleWaterBreathingToggle));
        }
        if (config.getBoolean("fire-resistance.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "fire-resistance", "bettersettings_fireresistance", handleFireResistanceToggle));
        }
        if (config.getBoolean("hunger-loss.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "hunger-loss", "bettersettings_hunger", null));
        }
        if (config.getBoolean("item-pickup.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "item-pickup", "bettersettings_itempickup", null));
        }
        if (config.getBoolean("entity-collision.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "entity-collision", "bettersettings_collision", handleCollisionToggle));
        }
        if (config.getBoolean("teleport-cooldown-bypass.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "teleport-cooldown-bypass", "bettersettings_tpcooldownbypass", null));
        }
        if (config.getBoolean("build-mode.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "build-mode", "bettersettings_buildmode", null));
        }
        
        // Display settings (additional)
        if (config.getBoolean("afk-status.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "afk-status", "bettersettings_afk", null));
        }
        if (config.getBoolean("vanish.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "vanish", "bettersettings_vanish", handleVanishToggle));
        }
        if (config.getBoolean("block-break-particles.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "block-break-particles", "bettersettings_breakparticles", null));
        }
        if (config.getBoolean("scoreboard-numbers.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "scoreboard-numbers", "bettersettings_scoreboardnumbers", null));
        }
        if (config.getBoolean("tab-list.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "tab-list", "bettersettings_tablist", handleTabListToggle));
        }
        if (config.getBoolean("coordinates-display.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "coordinates-display", "bettersettings_coords", null));
        }
        if (config.getBoolean("biome-display.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "biome-display", "bettersettings_biome", null));
        }
        
        // Protection settings (additional)
        if (config.getBoolean("fall-damage.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "fall-damage", "bettersettings_falldamage", null));
        }
        if (config.getBoolean("mob-targeting.enabled", true)) {
            registry.registerSetting(createSetting(plugin, "mob-targeting", "bettersettings_mobtargeting", null));
        }
    }
    
    private static Setting createSetting(BetterSettings plugin, String configKey, String id, ToggleHandler handler) {
        FileConfiguration config = plugin.getConfigManager().getSettingsConfig();
        
        return new Setting() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getDescription() {
                return config.getString(configKey + ".description", "Setting");
            }

            @Override
            public ItemStack getIcon(Player player, boolean state) {
                try {
                    String materialName = config.getString(configKey + ".icon", "PAPER");
                    return new ItemStack(Material.valueOf(materialName));
                } catch (IllegalArgumentException e) {
                    return new ItemStack(Material.PAPER);
                }
            }

            @Override
            public boolean getDefaultState() {
                return config.getBoolean(configKey + ".default-state", true);
            }

            @Override
            public String getPermission() {
                String perm = config.getString(configKey + ".permission");
                return (perm == null || perm.equalsIgnoreCase("null")) ? null : perm;
            }
            
            @Override
            public com.bettersettings.api.SettingCategory getCategory() {
                String categoryId = config.getString(configKey + ".category", null);
                if (categoryId != null) {
                    return SettingsRegistry.getInstance().getCategory(categoryId);
                }
                return null;
            }
            
            @Override
            public int getPriority() {
                return config.getInt(configKey + ".priority", 100);
            }

            @Override
            public boolean onToggle(Player player, boolean newState) {
                if (handler != null) {
                    return handler.handle(player, newState);
                }
                return true;
            }
        };
    }
    
    private static boolean handleScoreboardToggle(Player player, boolean newState) {
        if (newState) {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            player.setScoreboard(mainScoreboard);
        } else {
            Scoreboard emptyScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(emptyScoreboard);
        }
        return true;
    }
    
    private static boolean handleVisibilityToggle(Player player, boolean newState) {
        BetterSettings plugin = BetterSettings.getInstance();
        if (plugin == null) return false;
        
        if (newState) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    player.showPlayer(plugin, onlinePlayer);
                }
            }
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    player.hidePlayer(plugin, onlinePlayer);
                }
            }
        }
        return true;
    }
    
    private static boolean handleWeatherToggle(Player player, boolean newState) {
        // Weather toggle - sets player's weather
        if (newState) {
            player.resetPlayerWeather();
        } else {
            player.setPlayerWeather(org.bukkit.WeatherType.CLEAR);
        }
        return true;
    }
    
    private static boolean handleTimeToggle(Player player, boolean newState) {
        // Time toggle - sets player's time
        if (newState) {
            player.resetPlayerTime();
        } else {
            player.setPlayerTime(6000, false); // Set to noon
        }
        return true;
    }
    
    private static boolean handleNightVisionToggle(Player player, boolean newState) {
        // Night vision toggle
        if (newState) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                false
            ));
        } else {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        }
        return true;
    }
    
    private static boolean handleFlightToggle(Player player, boolean newState) {
        // Flight toggle
        player.setAllowFlight(newState);
        if (!newState && player.isFlying()) {
            player.setFlying(false);
        }
        return true;
    }
    
    private static final ToggleHandler handleGodModeToggle = (player, newState) -> {
        player.setInvulnerable(newState);
        return true;
    };
    
    private static final ToggleHandler handleSpeedToggle = (player, newState) -> {
        if (newState) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED,
                Integer.MAX_VALUE,
                1,
                false,
                false,
                false
            ));
        } else {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        }
        return true;
    };
    
    private static final ToggleHandler handleJumpToggle = (player, newState) -> {
        if (newState) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                Integer.MAX_VALUE,
                1,
                false,
                false,
                false
            ));
        } else {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
        }
        return true;
    };
    
    private static final ToggleHandler handleWaterBreathingToggle = (player, newState) -> {
        if (newState) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.WATER_BREATHING,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                false
            ));
        } else {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING);
        }
        return true;
    };
    
    private static final ToggleHandler handleFireResistanceToggle = (player, newState) -> {
        if (newState) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                false
            ));
        } else {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
        }
        return true;
    };
    
    private static final ToggleHandler handleCollisionToggle = (player, newState) -> {
        player.setCollidable(newState);
        return true;
    };
    
    private static final ToggleHandler handleVanishToggle = (player, newState) -> {
        BetterSettings plugin = BetterSettings.getInstance();
        if (plugin == null) return false;
        
        if (newState) {
            // Hide player from others
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.hidePlayer(plugin, player);
                }
            }
        } else {
            // Show player to others
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.showPlayer(plugin, player);
                }
            }
        }
        return true;
    };
    
    private static final ToggleHandler handleTabListToggle = (player, newState) -> {
        if (newState) {
            // Show in tab list - reset to default
            player.playerListName(null);
        } else {
            // Hide from tab list by setting name to empty
            player.playerListName(Component.empty());
        }
        return true;
    };
    
    @FunctionalInterface
    private interface ToggleHandler {
        boolean handle(Player player, boolean newState);
    }
}
