# BetterSettings API Documentation

Complete guide for developers integrating with BetterSettings.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Core Interfaces](#core-interfaces)
3. [Settings Registry](#settings-registry)
4. [Player Data Manager](#player-data-manager)
5. [Categories](#categories)
6. [Best Practices](#best-practices)
7. [Examples](#examples)

## Getting Started

### Adding BetterSettings as a Dependency

To integrate with BetterSettings from your plugin, add it as a dependency:

**Maven:**
```xml
<dependency>
    <groupId>com.bettersettings</groupId>
    <artifactId>BetterSettings</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

**plugin.yml:**
```yaml
depend: [BetterSettings]
```

**Note:** The `provided` scope means BetterSettings.jar must be installed on the server. Your plugin will not bundle BetterSettings - it will use the version installed in the plugins folder.

## Core Interfaces

### Setting Interface

The `Setting` interface is the foundation of the BetterSettings API.

```java
public interface Setting {
    String getId();
    String getDescription();
    ItemStack getIcon(Player player, boolean state);
    boolean getDefaultState();
    String getPermission();
    SettingCategory getCategory();
    int getPriority();
    boolean onToggle(Player player, boolean newState);
}
```

#### Method Details

**getId()**
- Returns unique identifier for the setting
- Must be namespaced: `"myplugin_settingname"`
- Used for data storage and registry lookup
- Must not be null or empty

**getDescription()**
- Human-readable name shown in GUI
- Supports color codes (`&` and `§`)
- Supports hex colors (`&#RRGGBB`)
- Example: `"&eToggle PvP Mode"`

**getIcon(Player player, boolean state)**
- Returns ItemStack displayed in GUI
- Can be customized per player
- Can change based on state
- BetterSettings adds glow and lore automatically

**getDefaultState()**
- Default value for new players
- `true` = enabled by default
- `false` = disabled by default

**getPermission()**
- Permission required to see/toggle setting
- Return `null` for no permission requirement
- Example: `"myplugin.setting.pvp"`

**getCategory()**
- Returns category for organization
- Return `null` for uncategorized
- See [Categories](#categories) section

**getPriority()**
- Sort order within category
- Lower values appear first
- Default: `100`
- Settings with the same priority are sorted alphabetically by ID (deterministic)

**onToggle(Player player, boolean newState)**
- Called when player toggles setting
- Executed on player's region scheduler (Folia-safe)
- Return `true` to allow toggle
- Return `false` to cancel and revert

## Settings Registry

The `SettingsRegistry` is a thread-safe singleton for managing all settings.

### Registering Settings

```java
import com.bettersettings.api.SettingsRegistry;

@Override
public void onEnable() {
    SettingsRegistry registry = SettingsRegistry.getInstance();
    registry.registerSetting(new MyCustomSetting());
}
```

### Retrieving Settings

```java
SettingsRegistry registry = SettingsRegistry.getInstance();

// Get specific setting
Setting setting = registry.getSetting("myplugin_mysetting");

// Get all settings
Collection<Setting> allSettings = registry.getSettings();
```

### Thread Safety

- All methods are thread-safe
- Can be called from any thread
- Uses `ConcurrentHashMap` internally
- Duplicate IDs are rejected with warning

## Player Data Manager

Access player setting states through the `PlayerDataManager`.

### Getting the Manager

```java
import com.bettersettings.BetterSettings;
import com.bettersettings.data.PlayerDataManager;

PlayerDataManager dataManager = BetterSettings.getInstance().getDataManager();
```

### Reading Settings

```java
UUID playerId = player.getUniqueId();

// Get setting state (returns default if not set)
boolean isEnabled = dataManager.getSetting(playerId, "bettersettings_chat");

// Get all settings for a player
Map<String, Boolean> allSettings = dataManager.getAllSettings(playerId);
```

### Writing Settings

```java
// Set a setting (use sparingly - prefer letting players toggle)
dataManager.setSetting(playerId, "bettersettings_chat", false);

// Save player data immediately
dataManager.savePlayerData(playerId);
```

### Data Lifecycle

- Data loaded automatically on player join
- Data saved automatically on player quit
- Periodic auto-save every 5 minutes (configurable)
- Cache cleanup for offline players

## Categories

Categories organize settings in the GUI.

### SettingCategory Interface

```java
public interface SettingCategory {
    String getId();
    String getName();
    ItemStack getIcon();
    int getPriority();
    String getPermission();
}
```

### Creating a Category

```java
import com.bettersettings.api.SettingCategory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MyCategory implements SettingCategory {
    
    @Override
    public String getId() {
        return "myplugin_category";
    }
    
    @Override
    public String getName() {
        return "&6My Settings";
    }
    
    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.DIAMOND);
    }
    
    @Override
    public int getPriority() {
        return 10; // Lower = appears first
    }
    
    @Override
    public String getPermission() {
        return null; // No permission required
    }
}
```

### Registering Categories

```java
SettingsRegistry registry = SettingsRegistry.getInstance();
registry.registerCategory(new MyCategory());
```

### Using Categories in Settings

```java
@Override
public SettingCategory getCategory() {
    return SettingsRegistry.getInstance().getCategory("myplugin_category");
}
```

## Best Practices

### 1. Namespace Your IDs

Always prefix setting IDs with your plugin name:
```java
// Good
"myplugin_pvp"
"myplugin_chat"

// Bad
"pvp"
"chat"
```

### 2. Register During onEnable()

Register settings early in your plugin lifecycle:
```java
@Override
public void onEnable() {
    // Register settings before other plugins might access them
    registerSettings();
}
```

### 3. Use Folia-Safe Schedulers

The `onToggle()` method and all events are called on the player's region scheduler, so you can safely:
- Modify player data
- Send messages
- Update scoreboards
- Show/hide players

### 4. Listen to Events for Integration

Use events instead of polling for better performance:
```java
// Good - Event-driven
@EventHandler
public void onSettingChange(PlayerSettingChangeEvent event) {
    reactToChange(event.getPlayer(), event.getSettingId());
}

// Bad - Polling
Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    // Check all players repeatedly
}, 0L, 20L);
```

### 5. Handle Toggle Failures Gracefully

```java
@Override
public boolean onToggle(Player player, boolean newState) {
    try {
        // Apply setting
        return true;
    } catch (Exception e) {
        player.sendMessage("§cFailed to toggle setting");
        return false; // Revert the toggle
    }
}
```

### 6. Respect Permissions

```java
@Override
public String getPermission() {
    // Return null for public settings
    // Return permission node for restricted settings
    return player.hasPermission("myplugin.vip") ? null : "myplugin.vip";
}
```

### 7. Provide Meaningful Icons

```java
@Override
public ItemStack getIcon(Player player, boolean state) {
    // Use different materials based on state
    Material material = state ? Material.LIME_DYE : Material.GRAY_DYE;
    return new ItemStack(material);
}
```

## Examples

### Example 1: Simple Toggle Setting

```java
public class SimpleSetting implements Setting {
    
    @Override
    public String getId() {
        return "myplugin_simple";
    }
    
    @Override
    public String getDescription() {
        return "&eSimple Setting";
    }
    
    @Override
    public ItemStack getIcon(Player player, boolean state) {
        return new ItemStack(Material.PAPER);
    }
    
    @Override
    public boolean getDefaultState() {
        return true;
    }
    
    @Override
    public String getPermission() {
        return null;
    }
    
    @Override
    public boolean onToggle(Player player, boolean newState) {
        player.sendMessage(newState ? "§aEnabled!" : "§cDisabled!");
        return true;
    }
}
```

### Example 2: Permission-Based Setting

```java
public class VIPSetting implements Setting {
    
    @Override
    public String getId() {
        return "myplugin_vip";
    }
    
    @Override
    public String getDescription() {
        return "&6VIP Feature";
    }
    
    @Override
    public ItemStack getIcon(Player player, boolean state) {
        return new ItemStack(Material.DIAMOND);
    }
    
    @Override
    public boolean getDefaultState() {
        return false;
    }
    
    @Override
    public String getPermission() {
        return "myplugin.vip";
    }
    
    @Override
    public boolean onToggle(Player player, boolean newState) {
        if (newState) {
            // Enable VIP features
            player.setAllowFlight(true);
        } else {
            // Disable VIP features
            player.setAllowFlight(false);
        }
        return true;
    }
}
```

### Example 3: Categorized Setting

```java
public class CategorizedSetting implements Setting {
    
    @Override
    public String getId() {
        return "myplugin_categorized";
    }
    
    @Override
    public String getDescription() {
        return "&eCategorized Setting";
    }
    
    @Override
    public ItemStack getIcon(Player player, boolean state) {
        return new ItemStack(Material.CHEST);
    }
    
    @Override
    public boolean getDefaultState() {
        return true;
    }
    
    @Override
    public String getPermission() {
        return null;
    }
    
    @Override
    public SettingCategory getCategory() {
        return SettingsRegistry.getInstance().getCategory("myplugin_category");
    }
    
    @Override
    public int getPriority() {
        return 50; // Appears in middle of category
    }
    
    @Override
    public boolean onToggle(Player player, boolean newState) {
        return true;
    }
}
```

### Example 4: State-Dependent Icon

```java
public class DynamicIconSetting implements Setting {
    
    @Override
    public String getId() {
        return "myplugin_dynamic";
    }
    
    @Override
    public String getDescription() {
        return "&eDynamic Icon Setting";
    }
    
    @Override
    public ItemStack getIcon(Player player, boolean state) {
        // Different icon based on state
        Material material = state ? Material.LIME_DYE : Material.GRAY_DYE;
        return new ItemStack(material);
    }
    
    @Override
    public boolean getDefaultState() {
        return true;
    }
    
    @Override
    public String getPermission() {
        return null;
    }
    
    @Override
    public boolean onToggle(Player player, boolean newState) {
        return true;
    }
}
```

### Example 5: Checking Other Plugin Settings

```java
public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Wait for BetterSettings to load
        getServer().getScheduler().runTaskLater(this, () -> {
            checkPlayerSettings();
        }, 20L);
    }
    
    private void checkPlayerSettings() {
        PlayerDataManager dataManager = BetterSettings.getInstance().getDataManager();
        
        for (Player player : getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            
            // Check if player has chat enabled
            boolean chatEnabled = dataManager.getSetting(playerId, "bettersettings_chat");
            
            if (!chatEnabled) {
                // Don't send chat messages to this player
            }
        }
    }
}
```

## Events

BetterSettings fires custom events when players toggle settings, allowing third-party plugins to react in real-time.

### PlayerToggleSettingEvent

Called **before** a player toggles a setting. This event is **cancellable**.

```java
import com.bettersettings.api.events.PlayerToggleSettingEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    
    @EventHandler
    public void onSettingToggle(PlayerToggleSettingEvent event) {
        Player player = event.getPlayer();
        String settingId = event.getSettingId();
        boolean newState = event.getNewState();
        
        // Example: Prevent non-VIP players from enabling vanish
        if (settingId.equals("bettersettings_vanish") && event.isEnabling()) {
            if (!player.hasPermission("myplugin.vip")) {
                event.setCancelled(true);
                player.sendMessage("§cVanish is VIP-only!");
            }
        }
    }
}
```

**Event Details:**
- **When**: Before the setting is toggled
- **Cancellable**: Yes
- **Thread**: Player's region scheduler (Folia-safe)

**Methods:**
- `getPlayer()` - The player toggling the setting
- `getSettingId()` - The unique setting identifier
- `getOldState()` - Current state before toggle
- `getNewState()` - New state after toggle
- `isEnabling()` - Returns true if enabling
- `isDisabling()` - Returns true if disabling
- `isCancelled()` / `setCancelled(boolean)` - Cancel the toggle

### PlayerSettingChangeEvent

Called **after** a player successfully toggles a setting. This event is **not cancellable**.

```java
import com.bettersettings.api.events.PlayerSettingChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    
    @EventHandler
    public void onSettingChange(PlayerSettingChangeEvent event) {
        Player player = event.getPlayer();
        String settingId = event.getSettingId();
        
        // Example: React to vanish toggle
        if (settingId.equals("bettersettings_vanish")) {
            if (event.wasEnabled()) {
                // Player enabled vanish
                hidePlayerFromMap(player);
            } else {
                // Player disabled vanish
                showPlayerOnMap(player);
            }
        }
    }
}
```

**Event Details:**
- **When**: After the setting has been toggled
- **Cancellable**: No
- **Thread**: Player's region scheduler (Folia-safe)

**Methods:**
- `getPlayer()` - The player who toggled the setting
- `getSettingId()` - The unique setting identifier
- `getOldState()` - State before the toggle
- `getNewState()` - State after the toggle
- `wasEnabled()` - Returns true if setting was enabled
- `wasDisabled()` - Returns true if setting was disabled

### Event Flow

```
Player clicks setting in GUI
         ↓
PlayerToggleSettingEvent (cancellable)
         ↓
Setting.onToggle() callback
         ↓
Data saved to cache
         ↓
PlayerSettingChangeEvent
         ↓
GUI refreshed
```

### Use Cases

**1. Integration with Vanish Plugins:**
```java
@EventHandler
public void onVanishToggle(PlayerSettingChangeEvent event) {
    if (event.getSettingId().equals("bettersettings_vanish")) {
        if (event.wasEnabled()) {
            myVanishPlugin.hidePlayer(event.getPlayer());
        } else {
            myVanishPlugin.showPlayer(event.getPlayer());
        }
    }
}
```

**2. Logging Setting Changes:**
```java
@EventHandler
public void onSettingChange(PlayerSettingChangeEvent event) {
    String action = event.wasEnabled() ? "enabled" : "disabled";
    logger.info(event.getPlayer().getName() + " " + action + " " + event.getSettingId());
}
```

**3. Enforcing Requirements:**
```java
@EventHandler
public void onFlightToggle(PlayerToggleSettingEvent event) {
    if (event.getSettingId().equals("bettersettings_flight")) {
        if (event.isEnabling() && !canFly(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou must complete the tutorial first!");
        }
    }
}
```

**4. Syncing with External Systems:**
```java
@EventHandler
public void onSettingChange(PlayerSettingChangeEvent event) {
    // Sync to database
    database.updatePlayerSetting(
        event.getPlayer().getUniqueId(),
        event.getSettingId(),
        event.getNewState()
    );
}
```

## Troubleshooting

### Setting Not Appearing in GUI

1. Check setting is registered during `onEnable()`
2. Verify setting ID is unique
3. Check permission requirements
4. Look for warnings in console

### Setting State Not Persisting

1. Ensure auto-save is enabled in `performance.yml`
2. Check file permissions in `plugins/BetterSettings/data/`
3. Verify no errors in console during save

### Folia Compatibility Issues

1. Never use `Bukkit.getScheduler()` in `onToggle()`
2. Use entity/region schedulers for player operations
3. Avoid global state modifications

## Support

For additional help:
- Check the [GitHub Wiki](https://github.com/mattbaconz/BetterSettings/wiki)
- Report issues on [GitHub](https://github.com/mattbaconz/BetterSettings/issues)
- Read the JavaDocs in the source code
