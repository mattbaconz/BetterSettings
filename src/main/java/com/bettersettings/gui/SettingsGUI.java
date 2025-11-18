package com.bettersettings.gui;

import com.bettersettings.BetterSettings;
import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingCategory;
import com.bettersettings.api.SettingsRegistry;
import com.bettersettings.data.PlayerDataManager;
import com.bettersettings.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUI for displaying and managing player settings with pagination support.
 * <p>
 * This class implements InventoryHolder to allow validation of click events.
 * All GUI operations are executed on the player's scheduler for Folia compatibility.
 * Supports unlimited settings through multi-page navigation.
 * </p>
 */
public class SettingsGUI implements InventoryHolder {

    private static final ConcurrentHashMap<Player, Integer> playerPages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Player, String> playerCategories = new ConcurrentHashMap<>();
    
    private final Player player;
    private final Inventory inventory;
    private final BetterSettings plugin;
    private final NamespacedKey settingIdKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey categoryKey;
    private final int currentPage;
    private final int totalPages;
    private final List<Setting> accessibleSettings;
    private final FileConfiguration uiConfig;
    private final String currentCategory;

    /**
     * Creates a new SettingsGUI instance.
     * 
     * @param player The player viewing the GUI
     * @param plugin The plugin instance
     * @param page The page number to display (0-indexed)
     * @param category The category to display (null for all or category menu)
     */
    private SettingsGUI(Player player, BetterSettings plugin, int page, String category) {
        this.player = player;
        this.plugin = plugin;
        this.settingIdKey = new NamespacedKey(plugin, "setting_id");
        this.actionKey = new NamespacedKey(plugin, "action");
        this.categoryKey = new NamespacedKey(plugin, "category");
        this.currentCategory = category;
        this.uiConfig = plugin.getConfigManager().getUIConfig();
        
        // Get all accessible settings for this player
        this.accessibleSettings = getAccessibleSettings(category);
        
        // Calculate pagination (28 items per page in new centered layout)
        int itemsPerPage = 28; // 4 rows of 7 items
        this.totalPages = accessibleSettings.isEmpty() ? 1 : (int) Math.ceil((double) accessibleSettings.size() / itemsPerPage);
        this.currentPage = Math.max(0, Math.min(page, totalPages - 1));
        
        // Get GUI title from config
        String titleString = buildTitle();
        Component title = com.bettersettings.utils.ColorUtils.toComponent(titleString);
        
        // Create inventory with size validation
        int size = uiConfig.getInt("layout.size", 54);
        if (size == 0) {
            size = 54; // Auto-size to 54 for now
        }
        // Validate size is a multiple of 9 and within valid range
        if (size % 9 != 0 || size < 9 || size > 54) {
            plugin.getLogger().warning("Invalid GUI size " + size + " in ui.yml, using 54");
            size = 54;
        }
        this.inventory = Bukkit.createInventory(this, size, title);
        
        // Populate inventory with settings
        populateInventory();
        
        // Play open sound
        playSound("open");
    }

    /**
     * Opens the settings GUI for a player.
     * <p>
     * This method executes on the player's scheduler for Folia compatibility.
     * </p>
     * 
     * @param player The player to open the GUI for
     */
    public static void open(Player player) {
        open(player, playerPages.getOrDefault(player, 0), playerCategories.get(player));
    }
    
    /**
     * Opens a specific page of the settings GUI.
     * 
     * @param player The player to open the GUI for
     * @param page The page number (0-indexed)
     */
    public static void open(Player player, int page) {
        open(player, page, playerCategories.get(player));
    }
    
    /**
     * Opens a specific page and category of the settings GUI.
     * 
     * @param player The player to open the GUI for
     * @param page The page number (0-indexed)
     * @param category The category to display (null for all or category menu)
     */
    public static void open(Player player, int page, String category) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        BetterSettings plugin = BetterSettings.getInstance();
        if (plugin == null) {
            return;
        }
        
        // Store current page and category
        playerPages.put(player, Math.max(0, page));
        if (category != null) {
            playerCategories.put(player, category);
        } else {
            playerCategories.remove(player);
        }
        
        // Execute GUI creation and opening on player's scheduler
        player.getScheduler().run(plugin, scheduledTask -> {
            try {
                SettingsGUI gui = new SettingsGUI(player, plugin, page, category);
                player.openInventory(gui.getInventory());
            } catch (Exception e) {
                plugin.getLogger().severe("Error opening settings GUI for " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }, null);
    }
    
    /**
     * Cleans up stored page data when player closes GUI.
     * 
     * @param player The player
     */
    public static void cleanup(Player player) {
        if (player != null) {
            playerPages.remove(player);
            playerCategories.remove(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gets the current page number.
     * 
     * @return current page (0-indexed)
     */
    public int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * Gets the total number of pages.
     * 
     * @return total pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Gets all settings accessible to the player, optionally filtered by category.
     * <p>
     * Settings are sorted deterministically by:
     * 1. Priority (ascending)
     * 2. ID (alphabetically) for settings with the same priority
     * </p>
     * 
     * @param category The category to filter by (null for all)
     * @return list of accessible settings, sorted deterministically
     */
    private List<Setting> getAccessibleSettings(String category) {
        List<Setting> accessible = new ArrayList<>();
        
        for (Setting setting : SettingsRegistry.getInstance().getSettings()) {
            String permission = setting.getPermission();
            if (permission == null || player.hasPermission(permission)) {
                // Filter by category if specified
                if (category != null) {
                    SettingCategory settingCategory = setting.getCategory();
                    if (settingCategory != null && settingCategory.getId().equals(category)) {
                        accessible.add(setting);
                    }
                } else {
                    accessible.add(setting);
                }
            }
        }
        
        // Sort deterministically: first by priority, then by ID
        accessible.sort((s1, s2) -> {
            int priorityCompare = Integer.compare(s1.getPriority(), s2.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // If priorities are equal, sort alphabetically by ID
            return s1.getId().compareTo(s2.getId());
        });
        
        return accessible;
    }
    
    /**
     * Builds the GUI title based on configuration and current state.
     * 
     * @return The formatted title string
     */
    private String buildTitle() {
        String titleText = uiConfig.getString("title.text", "&6⚙ &ePlayer Settings");
        
        // Add category name if viewing a specific category
        if (currentCategory != null) {
            String categoryFormat = uiConfig.getString("title.category-format", "&6⚙ &e{category}");
            SettingCategory cat = SettingsRegistry.getInstance().getCategory(currentCategory);
            String categoryName = cat != null ? cat.getName() : currentCategory;
            titleText = categoryFormat.replace("{category}", categoryName);
        }
        
        // Add page numbers if enabled and multiple pages exist
        if (totalPages > 1 && uiConfig.getBoolean("title.show-page-numbers", true)) {
            String pageFormat = uiConfig.getString("title.page-format", " &7({current}/{total})");
            titleText += pageFormat
                .replace("{current}", String.valueOf(currentPage + 1))
                .replace("{total}", String.valueOf(totalPages));
        }
        
        return titleText;
    }
    
    /**
     * Plays a sound effect for the player if enabled in config.
     * 
     * @param soundType The type of sound (open, toggle, navigate, deny)
     */
    private void playSound(String soundType) {
        if (!uiConfig.getBoolean("sounds." + soundType + ".enabled", false)) {
            return;
        }
        
        try {
            String soundName = uiConfig.getString("sounds." + soundType + ".sound");
            float volume = (float) uiConfig.getDouble("sounds." + soundType + ".volume", 1.0);
            float pitch = (float) uiConfig.getDouble("sounds." + soundType + ".pitch", 1.0);
            
            if (soundName != null) {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            // Invalid sound name, silently ignore
        }
    }
    
    /**
     * Gets the current category being displayed.
     * 
     * @return The category ID or null
     */
    public String getCurrentCategory() {
        return currentCategory;
    }

    /**
     * Populates the inventory with setting items and navigation buttons.
     */
    private void populateInventory() {
        PlayerDataManager dataManager = plugin.getDataManager();
        
        // Add decorative border
        addDecorativeBorder();
        
        // Calculate range for current page
        int itemsPerPage = 28; // 4 rows of 7 items (centered)
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, accessibleSettings.size());
        
        // Define centered slots for settings (4 rows, 7 columns, centered)
        int[] settingSlots = {
            10, 11, 12, 13, 14, 15, 16,  // Row 2
            19, 20, 21, 22, 23, 24, 25,  // Row 3
            28, 29, 30, 31, 32, 33, 34,  // Row 4
            37, 38, 39, 40, 41, 42, 43   // Row 5
        };
        
        // Add setting items in centered positions
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < settingSlots.length; i++) {
            Setting setting = accessibleSettings.get(i);
            
            // Get current setting state
            boolean currentState = dataManager.getSetting(
                player.getUniqueId(), 
                setting.getId(), 
                setting.getDefaultState()
            );
            
            // Create and place setting item
            ItemStack item = createSettingItem(setting, currentState);
            inventory.setItem(settingSlots[slotIndex], item);
            slotIndex++;
        }
        
        // Add navigation buttons if needed
        if (totalPages > 1) {
            addNavigationButtons();
        } else if (currentCategory != null) {
            // Add back button if in category view
            addBackButton();
        }
        
        // Add info button
        addInfoButton();
    }
    
    /**
     * Adds decorative glass pane border to the GUI.
     */
    private void addDecorativeBorder() {
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
        
        // Left and right columns (rows 1-4)
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border.clone());
            inventory.setItem(row * 9 + 8, border.clone());
        }
    }
    

    
    /**
     * Adds the back to categories button.
     */
    private void addBackButton() {
        ItemStack backButton = new ItemStack(Material.BARRIER);
        ItemMeta meta = backButton.getItemMeta();
        if (meta != null) {
            meta.displayName(com.bettersettings.utils.ColorUtils.toComponent("&c&lBack to Categories"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Return to category selection"));
            lore.add(Component.empty());
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&eClick to go back"));
            meta.lore(lore);
            
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
            backButton.setItemMeta(meta);
        }
        
        inventory.setItem(45, backButton);
    }
    
    /**
     * Adds the info/help button.
     */
    private void addInfoButton() {
        ItemStack infoButton = new ItemStack(Material.BOOK);
        ItemMeta meta = infoButton.getItemMeta();
        if (meta != null) {
            String categoryName = currentCategory != null ? 
                SettingsRegistry.getInstance().getCategory(currentCategory).getName() : "All Settings";
            
            meta.displayName(com.bettersettings.utils.ColorUtils.toComponent("&6&l" + categoryName));
            
            List<Component> lore = new ArrayList<>();
            if (totalPages > 1) {
                lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Page &e" + (currentPage + 1) + "&7/&e" + totalPages));
            }
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Total Settings: &e" + accessibleSettings.size()));
            lore.add(Component.empty());
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Click settings to toggle"));
            lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7them on or off"));
            
            meta.lore(lore);
            infoButton.setItemMeta(meta);
        }
        
        inventory.setItem(49, infoButton);
    }

    /**
     * Adds navigation buttons to the bottom row.
     */
    private void addNavigationButtons() {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent("&e&l← Previous Page"));
                
                List<Component> lore = new ArrayList<>();
                lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Page &e" + currentPage + "&7/&e" + totalPages));
                lore.add(Component.empty());
                lore.add(com.bettersettings.utils.ColorUtils.toComponent("&eClick to go back"));
                meta.lore(lore);
                
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "prev_page");
                prevButton.setItemMeta(meta);
            }
            
            inventory.setItem(48, prevButton);
        }
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent("&e&lNext Page →"));
                
                List<Component> lore = new ArrayList<>();
                lore.add(com.bettersettings.utils.ColorUtils.toComponent("&7Page &e" + (currentPage + 2) + "&7/&e" + totalPages));
                lore.add(Component.empty());
                lore.add(com.bettersettings.utils.ColorUtils.toComponent("&eClick to continue"));
                meta.lore(lore);
                
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "next_page");
                nextButton.setItemMeta(meta);
            }
            
            inventory.setItem(50, nextButton);
        }
        
        // Back button if in category view
        if (currentCategory != null) {
            addBackButton();
        }
    }

    /**
     * Creates an ItemStack for a setting with appropriate visual indicators.
     * 
     * @param setting The setting to create an item for
     * @param enabled Whether the setting is currently enabled
     * @return The created ItemStack
     */
    private ItemStack createSettingItem(Setting setting, boolean enabled) {
        if (setting == null) {
            return new ItemStack(Material.BARRIER);
        }
        
        // Get base icon from setting
        ItemStack item = setting.getIcon(player, enabled);
        if (item == null) {
            // Fallback to a default item if setting returns null
            item = new ItemStack(Material.PAPER);
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                // If still null, return the item as-is
                return item;
            }
        }
        
        // Set display name to setting description
        String description = setting.getDescription();
        if (description != null) {
            Component displayName = com.bettersettings.utils.ColorUtils.toComponent(description);
            meta.displayName(displayName);
        }
        
        // Create lore
        List<Component> lore = new ArrayList<>();
        
        // Add enabled/disabled status
        String statusText;
        if (enabled) {
            statusText = uiConfig.getString("visual.setting-item.lore.enabled-text", "&a✓ Enabled");
            lore.add(com.bettersettings.utils.ColorUtils.toComponent(statusText));
            
            // Add enchantment glow if enabled
            if (uiConfig.getBoolean("visual.setting-item.glow-when-enabled", true)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } else {
            statusText = uiConfig.getString("visual.setting-item.lore.disabled-text", "&c✗ Disabled");
            lore.add(com.bettersettings.utils.ColorUtils.toComponent(statusText));
        }
        
        lore.add(Component.empty());
        
        // Add category info if enabled
        if (uiConfig.getBoolean("visual.setting-item.show-category", true)) {
            SettingCategory category = setting.getCategory();
            if (category != null) {
                String categoryHint = uiConfig.getString("visual.setting-item.lore.category-hint", "&8Category: &7{category}");
                categoryHint = categoryHint.replace("{category}", category.getName());
                lore.add(com.bettersettings.utils.ColorUtils.toComponent(categoryHint));
            }
        }
        
        // Add permission info if enabled
        if (uiConfig.getBoolean("visual.setting-item.show-permission", false)) {
            String permission = setting.getPermission();
            if (permission != null) {
                String permissionHint = uiConfig.getString("visual.setting-item.lore.permission-hint", "&8Permission: &7{permission}");
                permissionHint = permissionHint.replace("{permission}", permission);
                lore.add(com.bettersettings.utils.ColorUtils.toComponent(permissionHint));
            }
        }
        
        // Add click hint if enabled
        if (uiConfig.getBoolean("visual.setting-item.show-click-hint", true)) {
            lore.add(Component.empty());
            String clickHint = uiConfig.getString("visual.setting-item.lore.click-hint", "&7Click to toggle");
            lore.add(com.bettersettings.utils.ColorUtils.toComponent(clickHint));
        }
        
        meta.lore(lore);
        
        // Store setting ID in PersistentDataContainer for click handling
        meta.getPersistentDataContainer().set(
            settingIdKey, 
            PersistentDataType.STRING, 
            setting.getId()
        );
        
        item.setItemMeta(meta);
        return item;
    }
}
