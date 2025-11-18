package com.bettersettings.commands;

import com.bettersettings.BetterSettings;
import com.bettersettings.gui.SettingsGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command executor for the /settings command.
 * <p>
 * This command opens the settings GUI for players and provides admin commands.
 * Supports: /settings [reload|info]
 * </p>
 */
public class SettingsCommand implements CommandExecutor, TabCompleter {

    private final BetterSettings plugin;

    /**
     * Creates a new SettingsCommand instance.
     * 
     * @param plugin The plugin instance
     */
    public SettingsCommand(BetterSettings plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the /settings command.
     *
     * @param sender  The command sender
     * @param command The command being executed
     * @param label   The command label (alias used)
     * @param args    Command arguments
     * @return true to indicate the command was handled
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle subcommands
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "reload":
                    return handleReload(sender);
                case "info":
                    return handleInfo(sender);
                default:
                    sender.sendMessage(getMessage("&cUnknown subcommand. Use /settings [reload|info]"));
                    return true;
            }
        }
        
        // No args - open GUI
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("&cThis command can only be used by players."));
            sender.sendMessage(getMessage("&7Use /settings reload or /settings info from console."));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if categories are enabled and category mode is "menu"
        var uiConfig = plugin.getConfigManager().getUIConfig();
        boolean categoriesEnabled = uiConfig.getBoolean("layout.categories-enabled", true);
        String categoryMode = uiConfig.getString("layout.category-mode", "menu");
        
        if (categoriesEnabled && "menu".equals(categoryMode)) {
            // Open category selection menu
            com.bettersettings.gui.CategoryGUI.open(player);
        } else {
            // Open settings GUI directly
            SettingsGUI.open(player);
        }
        
        return true;
    }

    /**
     * Handles the reload subcommand.
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("bettersettings.reload")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission");
            sender.sendMessage(getMessage(noPermMsg != null ? noPermMsg : "&cYou don't have permission to use this."));
            return true;
        }
        
        try {
            plugin.reload();
            String successMsg = plugin.getConfig().getString("messages.reload-success");
            sender.sendMessage(getMessage(successMsg != null ? successMsg : "&aBetterSettings configuration reloaded!"));
        } catch (Exception e) {
            String failMsg = plugin.getConfig().getString("messages.reload-failed");
            sender.sendMessage(getMessage(failMsg != null ? failMsg : "&cFailed to reload configuration. Check console for errors."));
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }

    /**
     * Handles the info subcommand.
     */
    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("bettersettings.info")) {
            String noPermMsg = plugin.getConfig().getString("messages.no-permission");
            sender.sendMessage(getMessage(noPermMsg != null ? noPermMsg : "&cYou don't have permission to use this."));
            return true;
        }
        
        sender.sendMessage(getMessage("&6&l=== BetterSettings Info ==="));
        sender.sendMessage(getMessage("&eVersion: &7" + plugin.getDescription().getVersion()));
        sender.sendMessage(getMessage("&eRegistered Settings: &7" + 
            com.bettersettings.api.SettingsRegistry.getInstance().getSettings().size()));
        sender.sendMessage(getMessage("&eCached Players: &7" + plugin.getDataManager().getCacheSize()));
        sender.sendMessage(getMessage("&ePending I/O Operations: &7" + 
            plugin.getDataManager().getPendingOperations()));
        
        var perfConfig = plugin.getConfigManager().getPerformanceConfig();
        sender.sendMessage(getMessage("&eAsync I/O: &7" + perfConfig.getBoolean("io.async", true)));
        sender.sendMessage(getMessage("&eBatch Saves: &7" + perfConfig.getBoolean("io.batch-saves", true)));
        
        return true;
    }

    /**
     * Provides tab completion for the command.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("bettersettings.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("bettersettings.info")) {
                completions.add("info");
            }
            
            // Filter based on what user typed
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }
        
        return completions;
    }

    /**
     * Converts a legacy color code string to a Component.
     */
    private Component getMessage(String message) {
        return com.bettersettings.utils.ColorUtils.toComponent(message);
    }
}
