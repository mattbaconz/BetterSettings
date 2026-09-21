package com.bettersettings.commands;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.gui.CategoryGUI;
import com.bettersettings.gui.SettingsGUI;
import com.bettersettings.settings.ValidationIssue;
import com.bettersettings.settings.ValidationReport;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Opens settings and reports honest registry validation/reload outcomes. */
public final class SettingsCommand implements CommandExecutor, TabCompleter {
    private final BetterSettings plugin;

    public SettingsCommand(BetterSettings plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> reload(sender);
                case "info" -> info(sender);
                case "validate" -> validate(sender);
                default -> {
                    sender.sendMessage(message("&cUnknown subcommand. Use /settings [reload|info|validate]"));
                    yield true;
                }
            };
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(message("&cThis command can only be used by players."));
            return true;
        }
        if ("donutsmp".equals(plugin.getConfigManager().getActivePreset())) {
            SettingsGUI.open(player, 0, SettingsGUI.firstDonutCategory(player));
        } else {
            var ui = plugin.getConfigManager().getUIConfig();
            if (ui.getBoolean("layout.categories-enabled", true)
                && "menu".equals(ui.getString("layout.category-mode", "menu"))) {
                CategoryGUI.open(player);
            } else {
                SettingsGUI.open(player);
            }
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("bettersettings.reload")) return denied(sender);
        ValidationReport report = plugin.reloadSettings();
        if (report.applied()) {
            sender.sendMessage(message("&aBetterSettings reloaded. " + counts(report)));
        } else {
            sender.sendMessage(message("&cReload rejected; the previous menu remains live. " + counts(report)));
            sender.sendMessage(message("&7Run /settings validate for details."));
        }
        return true;
    }

    private boolean info(CommandSender sender) {
        if (!sender.hasPermission("bettersettings.info")) return denied(sender);
        SettingsRegistry registry = SettingsRegistry.getInstance();
        ValidationReport report = registry.getValidationReport();
        sender.sendMessage(message("&6&l=== BetterSettings Info ==="));
        sender.sendMessage(message("&eVersion: &7" + plugin.getDescription().getVersion()));
        sender.sendMessage(message("&ePreset: &7" + report.activePreset()));
        sender.sendMessage(message("&eRegistry: &7" + counts(report)));
        sender.sendMessage(message("&eTotal API settings: &7" + registry.getSettings().size()));
        sender.sendMessage(message("&eCached players: &7" + plugin.getDataManager().getCacheSize()));
        sender.sendMessage(message("&ePending I/O: &7" + plugin.getDataManager().getPendingOperations()));
        return true;
    }

    private boolean validate(CommandSender sender) {
        if (!sender.hasPermission("bettersettings.validate")) return denied(sender);
        ValidationReport report = SettingsRegistry.getInstance().getValidationReport();
        sender.sendMessage(message((report.valid() ? "&a" : "&c") + "BetterSettings validation: " + counts(report)));
        if (report.issues().isEmpty()) {
            sender.sendMessage(message("&7No validation issues."));
            return true;
        }
        int shown = 0;
        for (ValidationIssue issue : report.issues()) {
            if (shown++ >= 12) {
                sender.sendMessage(message("&7...and " + (report.issues().size() - 12) + " more; see the server log."));
                break;
            }
            String color = issue.severity() == ValidationIssue.Severity.ERROR ? "&c" : "&e";
            sender.sendMessage(message(color + issue.code() + " &7" + issue.source()
                + (issue.path().isBlank() ? "" : " [" + issue.path() + "]") + ": " + issue.message()));
        }
        return true;
    }

    private boolean denied(CommandSender sender) {
        sender.sendMessage(message(plugin.getConfigManager().getMessagesConfig()
            .getString("no-permission", "&cYou don't have permission to use this.")));
        return true;
    }

    private static String counts(ValidationReport report) {
        return "loaded=" + report.loadedCount()
            + ", unavailable=" + report.unavailableCount()
            + ", skipped=" + report.skippedCount()
            + ", duplicate=" + report.duplicateCount()
            + ", invalid=" + report.invalidCount();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) return List.of();
        List<String> values = new ArrayList<>();
        if (sender.hasPermission("bettersettings.reload")) values.add("reload");
        if (sender.hasPermission("bettersettings.info")) values.add("info");
        if (sender.hasPermission("bettersettings.validate")) values.add("validate");
        String input = args[0].toLowerCase(Locale.ROOT);
        values.removeIf(value -> !value.startsWith(input));
        return values;
    }

    private static Component message(String value) {
        return com.bettersettings.utils.ColorUtils.toComponent(value);
    }
}
