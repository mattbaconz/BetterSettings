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
        
        // Calculate pagination
        int itemsPerPage = uiConfig.getInt("layout.items-per-page", 45);
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
        
        // Add border if enabled
        if (uiConfig.getBoolean("visual.border.enabled", false)) {
            addBorder();
        }
        
        // Calculate range for current page
        int itemsPerPage = uiConfig.getInt("layout.items-per-page", 45);
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, accessibleSettings.size());
        
        // Add setting items
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Setting setting = accessibleSettings.get(i);
            
            // Get current setting state
            boolean currentState = dataManager.getSetting(
                player.getUniqueId(), 
                setting.getId(), 
                setting.getDefaultState()
            );
            
            // Create and place setting item
            ItemStack item = createSettingItem(setting, currentState);
            inventory.setItem(slot, item);
            slot++;
        }
        
        // Add filler items if enabled
        if (uiConfig.getBoolean("visual.filler.enabled", false)) {
            addFillers(endIndex - startIndex);
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
     * Adds border decoration to the GUI.
     */
    private void addBorder() {
        try {
            Material borderMaterial = Material.valueOf(uiConfig.getString("visual.border.material", "BLACK_STAINED_GLASS_PANE"));
            String borderName = uiConfig.getString("visual.border.name", " ");
            
            ItemStack border = new ItemStack(borderMaterial);
            ItemMeta meta = border.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(borderName));
                border.setItemMeta(meta);
            }
            
            // Add border to top and bottom rows (only if inventory is large enough)
            // Skip border if it would overlap with settings area
            int size = inventory.getSize();
            int itemsPerPage = uiConfig.getInt("layout.items-per-page", 45);
            
            if (size >= 27 && itemsPerPage <= size - 18) {
                // Safe to add border - won't overlap with settings
                for (int i = 0; i < 9; i++) {
                    inventory.setItem(i, border.clone());
                    inventory.setItem(size - 9 + i, border.clone());
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid material, skip border
            plugin.getLogger().warning("Invalid border material in ui.yml: " + e.getMessage());
        }
    }
    
    /**
     * Adds filler items to empty slots.
     * 
     * @param usedSlots Number of slots already used by settings
     */
    private void addFillers(int usedSlots) {
        try {
            Material fillerMaterial = Material.valueOf(uiConfig.getString("visual.filler.material", "GRAY_STAINED_GLASS_PANE"));
            String fillerName = uiConfig.getString("visual.filler.name", " ");
            
            ItemStack filler = new ItemStack(fillerMaterial);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(fillerName));
                filler.setItemMeta(meta);
            }
            
            // Fill empty slots in the settings area (not navigation row)
            int itemsPerPage = uiConfig.getInt("layout.items-per-page", 45);
            for (int i = usedSlots; i < itemsPerPage; i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler.clone());
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid material, skip fillers
        }
    }
    
    /**
     * Adds the back to categories button.
     */
    private void addBackButton() {
        if (!uiConfig.getBoolean("navigation.back.enabled", true)) {
            return;
        }
        
        try {
            int size = inventory.getSize();
            int defaultBackSlot = size - 9; // First slot of bottom row
            int slot = uiConfig.getInt("navigation.back.slot", defaultBackSlot);
            Material material = Material.valueOf(uiConfig.getString("navigation.back.material", "BARRIER"));
            String name = uiConfig.getString("navigation.back.name", "&cBack to Categories");
            List<String> loreStrings = uiConfig.getStringList("navigation.back.lore");
            
            ItemStack backButton = new ItemStack(material);
            ItemMeta meta = backButton.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(name));
                
                List<Component> lore = loreStrings.stream()
                    .map(com.bettersettings.utils.ColorUtils::toComponent)
                    .collect(Collectors.toList());
                meta.lore(lore);
                
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
                backButton.setItemMeta(meta);
            }
            
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, backButton);
            }
        } catch (IllegalArgumentException e) {
            // Invalid material or slot, skip button
        }
    }
    
    /**
     * Adds the info/help button.
     */
    private void addInfoButton() {
        if (!uiConfig.getBoolean("navigation.info.enabled", true)) {
            return;
        }
        
        try {
            int size = inventory.getSize();
            int defaultInfoSlot = size - 5; // Middle of bottom row
            int slot = uiConfig.getInt("navigation.info.slot", defaultInfoSlot);
            Material material = Material.valueOf(uiConfig.getString("navigation.info.material", "BOOK"));
            String name = uiConfig.getString("navigation.info.name", "&6Settings Menu");
            List<String> loreStrings = uiConfig.getStringList("navigation.info.lore");
            
            ItemStack infoButton = new ItemStack(material);
            ItemMeta meta = infoButton.getItemMeta();
            if (meta != null) {
                meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(name));
                
                List<Component> lore = new ArrayList<>();
                for (String loreString : loreStrings) {
                    String formatted = loreString
                        .replace("{page}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages))
                        .replace("{count}", String.valueOf(accessibleSettings.size()));
                    lore.add(com.bettersettings.utils.ColorUtils.toComponent(formatted));
                }
                meta.lore(lore);
                
                infoButton.setItemMeta(meta);
            }
            
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, infoButton);
            }
        } catch (IllegalArgumentException e) {
            // Invalid material or slot, skip button
        }
    }

    /**
     * Adds navigation buttons to the bottom row.
     */
    private void addNavigationButtons() {
        int size = inventory.getSize();
        int defaultPrevSlot = size - 9; // First slot of bottom row
        int defaultNextSlot = size - 1;  // Last slot of bottom row
        
        // Previous page button
        if (currentPage > 0 && uiConfig.getBoolean("navigation.previous.enabled", true)) {
            try {
                int slot = uiConfig.getInt("navigation.previous.slot", defaultPrevSlot);
                Material material = Material.valueOf(uiConfig.getString("navigation.previous.material", "ARROW"));
                String name = uiConfig.getString("navigation.previous.name", "&e← Previous Page");
                List<String> loreStrings = uiConfig.getStringList("navigation.previous.lore");
                
                ItemStack prevButton = new ItemStack(material);
                ItemMeta meta = prevButton.getItemMeta();
                if (meta != null) {
                    meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(name));
                    
                    List<Component> lore = new ArrayList<>();
                    for (String loreString : loreStrings) {
                        String formatted = loreString
                            .replace("{page}", String.valueOf(currentPage))
                            .replace("{total}", String.valueOf(totalPages));
                        lore.add(com.bettersettings.utils.ColorUtils.toComponent(formatted));
                    }
                    meta.lore(lore);
                    
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "prev_page");
                    prevButton.setItemMeta(meta);
                }
                
                if (slot < inventory.getSize()) {
                    inventory.setItem(slot, prevButton);
                }
            } catch (IllegalArgumentException e) {
                // Invalid material or slot, skip button
            }
        }
        
        // Next page button
        if (currentPage < totalPages - 1 && uiConfig.getBoolean("navigation.next.enabled", true)) {
            try {
                int slot = uiConfig.getInt("navigation.next.slot", defaultNextSlot);
                Material material = Material.valueOf(uiConfig.getString("navigation.next.material", "ARROW"));
                String name = uiConfig.getString("navigation.next.name", "&eNext Page →");
                List<String> loreStrings = uiConfig.getStringList("navigation.next.lore");
                
                ItemStack nextButton = new ItemStack(material);
                ItemMeta meta = nextButton.getItemMeta();
                if (meta != null) {
                    meta.displayName(com.bettersettings.utils.ColorUtils.toComponent(name));
                    
                    List<Component> lore = new ArrayList<>();
                    for (String loreString : loreStrings) {
                        String formatted = loreString
                            .replace("{page}", String.valueOf(currentPage + 2))
                            .replace("{total}", String.valueOf(totalPages));
                        lore.add(com.bettersettings.utils.ColorUtils.toComponent(formatted));
                    }
                    meta.lore(lore);
                    
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "next_page");
                    nextButton.setItemMeta(meta);
                }
                
                if (slot < inventory.getSize()) {
                    inventory.setItem(slot, nextButton);
                }
            } catch (IllegalArgumentException e) {
                // Invalid material or slot, skip button
            }
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
