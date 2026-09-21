package com.bettersettings.listeners;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingBehavior;
import com.bettersettings.api.SettingType;
import com.bettersettings.api.SettingValues;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.api.ValueSetting;
import com.bettersettings.api.events.PlayerSettingChangeEvent;
import com.bettersettings.api.events.PlayerSettingValueChangeEvent;
import com.bettersettings.api.events.PlayerToggleSettingEvent;
import com.bettersettings.data.PlayerDataManager;
import com.bettersettings.gui.CategoryGUI;
import com.bettersettings.gui.SettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Handles toggle, choice, reset, pagination, category, and Donut-tab interactions. */
public final class GUIListener implements Listener {

    private final BetterSettings plugin;
    private final NamespacedKey settingIdKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;

    public GUIListener(BetterSettings plugin) {
        this.plugin = plugin;
        settingIdKey = new NamespacedKey(plugin, "setting_id");
        actionKey = new NamespacedKey(plugin, "action");
        categoryKey = new NamespacedKey(plugin, "category");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SettingsGUI && event.getPlayer() instanceof Player player) {
            SettingsGUI.cleanup(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SettingsGUI
            || event.getInventory().getHolder() instanceof CategoryGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCategoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CategoryGUI)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemMeta meta = itemMeta(event.getCurrentItem());
        if (meta == null) return;
        String category = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        if (category != null) {
            playSound(player, "navigate");
            SettingsGUI.open(player, 0, category);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SettingsGUI gui)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemMeta meta = itemMeta(event.getCurrentItem());
        if (meta == null) return;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            handleNavigation(player, gui, action);
            return;
        }

        String id = meta.getPersistentDataContainer().get(settingIdKey, PersistentDataType.STRING);
        if (id == null) return;
        SettingsRegistry registry = SettingsRegistry.getInstance();
        Setting rawSetting = registry.getSetting(id);
        ValueSetting setting = registry.getValueSetting(id);
        if (rawSetting == null || setting == null) {
            plugin.getLogger().warning("Player " + player.getName() + " clicked unknown setting " + id);
            return;
        }
        if (setting.getPermission() != null && !player.hasPermission(setting.getPermission())) return;
        if (!registry.isAvailable(setting)) {
            playSound(player, "deny");
            return;
        }

        boolean reset = event.isShiftClick();
        boolean previous = event.isRightClick() && !reset;
        player.getScheduler().run(plugin, task -> changeValue(player, gui, rawSetting, setting, reset, previous), null);
    }

    private void changeValue(Player player, SettingsGUI gui, Setting rawSetting, ValueSetting setting,
                             boolean reset, boolean previous) {
        PlayerDataManager data = plugin.getDataManager();
        String oldValue = data.getSettingValue(player.getUniqueId(), setting.getId(), setting.getDefaultValue());
        if (setting.getType() == SettingType.CHOICE) {
            boolean knownChoice = false;
            for (com.bettersettings.api.SettingOption option : setting.getOptions()) {
                if (option.value().equals(oldValue)) {
                    knownChoice = true;
                    break;
                }
            }
            if (!knownChoice) oldValue = setting.getDefaultValue();
        }

        String newValue;
        if (reset) {
            newValue = setting.getDefaultValue();
        } else if (setting.getType() == SettingType.TOGGLE) {
            newValue = Boolean.toString(!Boolean.parseBoolean(oldValue));
        } else {
            newValue = previous
                ? SettingValues.previous(oldValue, setting.getOptions())
                : SettingValues.next(oldValue, setting.getOptions());
        }
        if (newValue.equals(oldValue)) {
            refresh(player, gui);
            return;
        }

        if (setting.getType() == SettingType.TOGGLE) {
            boolean oldState = Boolean.parseBoolean(oldValue);
            boolean newState = Boolean.parseBoolean(newValue);
            PlayerToggleSettingEvent preEvent = new PlayerToggleSettingEvent(
                player, setting.getId(), oldState, newState);
            Bukkit.getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) {
                playSound(player, "deny");
                return;
            }
            if (!(rawSetting instanceof ValueSetting)) {
                try {
                    if (!rawSetting.onToggle(player, newState)) {
                        playSound(player, "deny");
                        return;
                    }
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING,
                        "Legacy toggle callback failed for " + setting.getId(), exception);
                    playSound(player, "deny");
                    return;
                }
            }
        }

        // The value commit is the transaction boundary. Provider/action effects below are best-effort.
        data.setSettingValue(player.getUniqueId(), setting.getId(), newValue);
        try {
            setting.onValueChange(player, oldValue, newValue);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(java.util.logging.Level.WARNING,
                "Post-commit callback failed for " + setting.getId(), exception);
        }

        SettingBehavior behavior = registryBehavior(setting);
        if (behavior != null) {
            try {
                behavior.apply(player, setting, oldValue, newValue);
            } catch (Exception exception) {
                plugin.getLogger().log(java.util.logging.Level.WARNING,
                    "Provider behavior failed after committing " + setting.getId(), exception);
            }
        }

        Bukkit.getPluginManager().callEvent(new PlayerSettingValueChangeEvent(
            player, setting.getId(), setting.getType(), oldValue, newValue));
        if (setting.getType() == SettingType.TOGGLE) {
            Bukkit.getPluginManager().callEvent(new PlayerSettingChangeEvent(
                player, setting.getId(), Boolean.parseBoolean(oldValue), Boolean.parseBoolean(newValue)));
        }

        if (plugin.getConfigManager().getPerformanceConfig().getBoolean("logging.toggles", false)) {
            plugin.getLogger().info(player.getName() + " changed " + setting.getId()
                + " from " + oldValue + " to " + newValue);
        }
        playSound(player, "toggle");
        refresh(player, gui);
    }

    private SettingBehavior registryBehavior(ValueSetting setting) {
        return setting.getBehaviorId() == null ? null
            : SettingsRegistry.getInstance().getBehavior(setting.getBehaviorId());
    }

    private void refresh(Player player, SettingsGUI gui) {
        if (plugin.getConfigManager().getUIConfig().getBoolean("performance.refresh-on-toggle", true)) {
            SettingsGUI.open(player, gui.getCurrentPage(), gui.getCurrentCategory());
        }
    }

    private void handleNavigation(Player player, SettingsGUI gui, String action) {
        if (action.startsWith("tab:")) {
            playSound(player, "navigate");
            SettingsGUI.open(player, 0, action.substring("tab:".length()));
            return;
        }
        switch (action) {
            case "prev_page" -> {
                if (gui.getCurrentPage() > 0) {
                    playSound(player, "navigate");
                    SettingsGUI.open(player, gui.getCurrentPage() - 1, gui.getCurrentCategory());
                }
            }
            case "next_page" -> {
                if (gui.getCurrentPage() < gui.getTotalPages() - 1) {
                    playSound(player, "navigate");
                    SettingsGUI.open(player, gui.getCurrentPage() + 1, gui.getCurrentCategory());
                }
            }
            case "back" -> {
                playSound(player, "navigate");
                if ("donutsmp".equals(plugin.getConfigManager().getActivePreset())) {
                    SettingsGUI.open(player, 0, SettingsGUI.firstDonutCategory(player));
                } else if (plugin.getConfigManager().getUIConfig().getBoolean("layout.categories-enabled", true)
                    && "menu".equals(plugin.getConfigManager().getUIConfig()
                        .getString("layout.category-mode", "menu"))) {
                    CategoryGUI.open(player);
                } else {
                    SettingsGUI.open(player, 0, null);
                }
            }
            default -> { }
        }
    }

    private static ItemMeta itemMeta(ItemStack item) {
        return item == null || !item.hasItemMeta() ? null : item.getItemMeta();
    }

    private void playSound(Player player, String type) {
        var config = plugin.getConfigManager().getUIConfig();
        if (!config.getBoolean("sounds." + type + ".enabled", false)) return;
        try {
            Sound sound = Sound.valueOf(config.getString("sounds." + type + ".sound", "UI_BUTTON_CLICK"));
            float volume = (float) config.getDouble("sounds." + type + ".volume", 1.0);
            float pitch = (float) config.getDouble("sounds." + type + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
