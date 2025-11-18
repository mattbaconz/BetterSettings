package com.bettersettings.gui;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.SettingCategory;
import com.bettersettings.api.SettingsRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for displaying category selection menu.
 */
public class CategoryGUI implements InventoryHolder {
    
    private final Player player;
    private final Inventory inventory;
    private final BetterSettings plugin;
    private final NamespacedKey categoryKey;
    private final FileConfiguration uiConfig;
    
    /**
     * Creates a new CategoryGUI instance.
     * 
     * @param player The player viewing the GUI
     * @param plugin The plugin instance
     */
    private CategoryGUI(Player player, BetterSettings plugin) {
        this.player = player;
        this.plugin = plugin;
        this.categoryKey = new NamespacedKey(plugin, "category");
        this.uiConfig = plugin.getConfigManager().getUIConfig();
        
        // Get GUI title
        String titleString = uiConfig.getString("title.text", "&6⚙ &ePlayer Settings");
        Component title = com.bettersettings.utils.ColorUtils.toComponent(titleString);
        
        // Create inventory
        int size = uiConfig.getInt("layout.size", 54);
        if (size == 0) {
            size = 54;
        }
        this.inventory = Bukkit.createInventory(this, size, title);
        
        // Populate with categories
        populateInventory();
    }
    
    /**
     * Opens the category selection GUI for a player.
     * 
     * @param player The player to open the GUI for
     */
    public static void open(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        BetterSettings plugin = BetterSettings.getInstance();
        if (plugin == null) {
            return;
        }
        
        player.getScheduler().run(plugin, scheduledTask -> {
            try {
                CategoryGUI gui = new CategoryGUI(player, plugin);
                player.openInventory(gui.getInventory());
            } catch (Exception e) {
                plugin.getLogger().severe("Error opening category GUI for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, null);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Populates the inventory with category items.
     */
    private void populateInventory() {
        // Add decorative border
        addBorder();
        
        // Get all categories sorted by priority
        List<SettingCategory> categories = SettingsRegistry.getInstance().getCategories().stream()
            .filter(cat -> cat.getPermission() == null || player.hasPermission(cat.getPermission()))
            .sorted(Comparator.comparingInt(SettingCategory::getPriority))
            .collect(Collectors.toList());
        
        // Define centered slots for categories (3x3 grid in center)
        int[] categorySlots = {
            20, 21, 22, 23, 24,  // Row 3
            29, 30, 31, 32, 33   // Row 4
        };
        
        // Add category items in centered positions
        for (int i = 0; i < Math.min(categories.size(), categorySlots.length); i++) {
            ItemStack item = createCategoryItem(categories.get(i));
            inventory.setItem(categorySlots[i], item);
        }
        
        // Add info button in center bottom
        addInfoButton();
    }
    
    /**
     * Adds decorative glass pane border.
     */
    private void addBorder() {
        Material borderMaterial = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack border = new ItemStack(borderMaterial);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(" "));
            border.setItemMeta(meta);
        }
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border.clone());
            inventory.setItem(45 + i, border.clone());
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border.clone());
            inventory.setItem(row * 9 + 8, border.clone());
        }
    }
    
    /**
     * Adds info button.
     */
    private void addInfoButton() {
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta meta = infoButton.getItemMeta();
        if (meta != null) {
            meta.displayName(com.bettersettings.utils.ColorUtils.toComponent("&6&lSettings Categories"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Select a category to view"));
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7and manage your settings"));
            lore.add(Component.empty());
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&e" + SettingsRegistry.getInstance().getCategories().size() + " categories available"));
            
            meta.lore(lore);
            infoButton.setItemMeta(meta);
        }
        
        inventory.setItem(49, infoButton);
    }
    
    /**
     * Creates an ItemStack for a category.
     * 
     * @param category The category
     * @return The created ItemStack
     */
    private ItemStack createCategoryItem(SettingCategory category) {
        if (category == null) {
            return new ItemStack(Material.BARRIER);
        }
        
        Material icon = category.getIcon();
        if (icon == null) {
            icon = Material.CHEST;
        }
        
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String name = category.getName();
            if (name != null) {
                Component displayName = com.bettersettings.utils.ColorUtils.toComponent(name);
                meta.displayName(displayName);
            }
            
            // Set lore from description
            List<String> description = category.getDescription();
            if (description != null && !description.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : description) {
                    if (line != null) {
                        lore.add(com.bettersettings.utils.ColorUtils.toComponent(line));
                    }
                }
                meta.lore(lore);
            }
            
            // Store category ID
            String categoryId = category.getId();
            if (categoryId != null) {
                meta.getPersistentDataContainer().set(
                    categoryKey,
                    PersistentDataType.STRING,
                    categoryId
                );
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
