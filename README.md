# ⚙️ BetterSettings

> A lightweight, API-first player settings management system for Paper/Purpur/Folia servers (1.20.5+)

[![GitHub release](https://img.shields.io/github/v/release/mattbaconz/BetterSettings)](https://github.com/mattbaconz/BetterSettings/releases)
[![bStats Servers](https://img.shields.io/bstats/servers/28034)](https://bstats.org/plugin/bukkit/BetterSettings/28034)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/Paper-1.20.5+-blue)](https://papermc.io/)

---

## ✨ Features

- 🎯 **50+ Built-in Settings** - Communication, display, gameplay, protection, and notification preferences
- 🔌 **Developer API** - Easy-to-use API for third-party plugins to register custom settings
- 🎪 **Event System** - Cancellable pre-toggle and post-change events for deep integration
- 📂 **Categorized GUI** - Organized settings menu with category navigation and pagination
- 🌐 **Folia Compatible** - Full support for Folia's regionized threading model
- 🔒 **Thread-Safe** - Concurrent data access with proper synchronization
- ⚡ **Async I/O** - Non-blocking file operations for optimal performance
- 🎨 **Highly Configurable** - Customize every aspect via YAML configuration files
- 🪶 **Lightweight** - Only 92KB! No external plugin dependencies required

## 🚀 Quick Start

1. 📥 Download `BetterSettings-1.0.0.jar` from [Releases](https://github.com/mattbaconz/BetterSettings/releases)
2. 📁 Place in your server's `plugins` folder
3. 🔄 Start/restart your server
4. 🎮 Use `/settings` to open the settings GUI

That's it! Your players can now customize their experience.

## 📜 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/settings` | 🎮 Open settings GUI | `bettersettings.use` |
| `/settings reload` | 🔄 Reload configuration | `bettersettings.reload` |
| `/settings info` | ℹ️ View plugin information | `bettersettings.info` |

**Aliases:** `/setting`, `/prefs`, `/preferences`

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `bettersettings.use` | Access settings GUI | ✅ `true` |
| `bettersettings.reload` | Reload plugin config | 👑 `op` |
| `bettersettings.info` | View plugin info | 👑 `op` |
| `bettersettings.*` | All permissions | 👑 `op` |

## 📋 Configuration Files

- ⚙️ **config.yml** - Main plugin settings
- 🎯 **settings.yml** - Configure built-in settings (enable/disable, icons, descriptions)
- 💬 **messages.yml** - Customize all user-facing messages
- ⚡ **performance.yml** - Performance tuning (I/O, caching, auto-save)
- 🎨 **ui.yml** - GUI appearance and layout customization
- 📁 **settings/*.yml** - Custom setting definitions (optional)

📖 **[Full Configuration Guide →](CONFIGURATION.md)**

## 🎯 Built-in Settings

**29 Fully Functional Settings** + 31 API Framework Settings

### 💬 Communication (4 working, 5 API-only)
✅ Global Chat • ✅ Private Messages • ✅ Death Messages • ✅ Join/Leave Messages • 🔌 Teleport Requests • 🔌 Trade Requests • 🔌 Friend Requests • 🔌 Chat Mentions • 🔌 DM Sound

### 👁️ Display (7 working, 8 API-only)
✅ Scoreboard • ✅ Player Visibility • 🔌 Particle Effects • 🔌 Sound Effects • ✅ Weather • ✅ Time • 🔌 Damage Indicators • ✅ Night Vision • 🔌 AFK Status • ✅ Vanish • 🔌 Block Break Particles • 🔌 Scoreboard Numbers • ✅ Tab List • 🔌 Coordinates Display • 🔌 Biome Display

### 🎮 Gameplay (13 working, 2 API-only)
✅ Auto-Pickup • ✅ Flight • ✅ PvP • ✅ Auto-Sprint • ✅ Auto-Respawn • ✅ God Mode • ✅ Speed Boost • ✅ Jump Boost • ✅ Water Breathing • ✅ Fire Resistance • ✅ Hunger Loss • ✅ Item Pickup • ✅ Entity Collision • 🔌 Teleport Cooldown Bypass • 🔌 Build Mode

### 🛡️ Protection (4 working)
✅ Drop Protection • ✅ Inventory Protection • ✅ Fall Damage • ✅ Mob Targeting

### 🔔 Notifications (11 API-only)
🔌 Mob Spawn • 🔌 Achievements • 🔌 Action Bar • 🔌 Boss Bar • 🔌 Titles • 🔌 Keep Inventory Reminder • 🔌 Mob Griefing • 🔌 Fire Spread • 🔌 Explosions • 🔌 Command Spy • 🔌 Social Spy

**Legend:** ✅ = Fully Functional | 🔌 = API Framework (requires plugin integration)

> **Note:** Settings marked with 🔌 are placeholders that save player preferences but require integration with other plugins to function. See [SETTINGS_STATUS.md](SETTINGS_STATUS.md) for details.

## 🔌 Developer API

📖 **[Full API Documentation →](API.md)**

### 🎯 Registering a Custom Setting

```java
import com.bettersettings.api.Setting;
import com.bettersettings.api.SettingsRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MyCustomSetting implements Setting {
    
    @Override
    public String getId() {
        return "myplugin_mysetting";
    }
    
    @Override
    public String getDescription() {
        return "&eToggle My Feature";
    }
    
    @Override
    public ItemStack getIcon(Player player, boolean state) {
        return new ItemStack(Material.DIAMOND);
    }
    
    @Override
    public boolean getDefaultState() {
        return true;
    }
    
    @Override
    public String getPermission() {
        return "myplugin.setting.mysetting";
    }
    
    @Override
    public boolean onToggle(Player player, boolean newState) {
        // Apply the setting effect
        if (newState) {
            // Enable feature
        } else {
            // Disable feature
        }
        return true; // Return false to cancel toggle
    }
}

// Register during plugin initialization
@Override
public void onEnable() {
    SettingsRegistry.getInstance().registerSetting(new MyCustomSetting());
}
```

### 📊 Accessing Player Settings

```java
import com.bettersettings.BetterSettings;
import com.bettersettings.data.PlayerDataManager;

// Get the data manager
PlayerDataManager dataManager = BetterSettings.getInstance().getDataManager();

// Check if a player has a setting enabled
UUID playerId = player.getUniqueId();
boolean isEnabled = dataManager.getSetting(playerId, "bettersettings_chat");

// Set a player's setting (use with caution)
dataManager.setSetting(playerId, "bettersettings_chat", false);
```

### 🎪 Listening to Setting Changes

```java
import com.bettersettings.api.events.PlayerSettingChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    
    @EventHandler
    public void onSettingChange(PlayerSettingChangeEvent event) {
        if (event.getSettingId().equals("bettersettings_vanish")) {
            if (event.wasEnabled()) {
                // Player enabled vanish
                hideFromMap(event.getPlayer());
            }
        }
    }
}
```

## ⚡ Performance

BetterSettings is designed for high-performance servers:

- 🚀 **Async I/O** - All file operations are non-blocking
- 📦 **Batch Saves** - Multiple players saved in single operation
- 🧹 **Smart Caching** - Automatic cleanup of offline player data
- 🎯 **Minimal Overhead** - Optimized data structures and algorithms
- 🌐 **Folia Ready** - Proper scheduler usage for regionized threading

### ⚙️ Recommended Settings

**For SSD/NVMe servers:**
```yaml
io:
  async: true
  batch-saves: true
  max-concurrent: 8
```

**For HDD servers:**
```yaml
io:
  async: true
  batch-saves: true
  max-concurrent: 2
```

## 🎨 Customization

### 🖼️ GUI Layout

Edit `ui.yml` to customize:
- GUI size (27 or 54 slots)
- Items per page
- Category system (menu or tabs mode)
- Navigation button positions
- Colors and icons

### 🎯 Setting Icons

Edit `settings.yml` to change icons:
```yaml
chat:
  icon: PAPER  # Change to any Material name
  description: "&eToggle global chat messages"
```

### 💬 Messages

Edit `messages.yml` to customize all text:
```yaml
setting-enabled: "&aEnabled: &7{setting}"
setting-disabled: "&cDisabled: &7{setting}"
```

## 📊 Statistics

Track your server's usage on [bStats](https://bstats.org/plugin/bukkit/BetterSettings/28034)!

## 💬 Support

- 🐛 **Issues**: [Report bugs on GitHub](https://github.com/mattbaconz/BetterSettings/issues)
- 📖 **Wiki**: [Full documentation](https://github.com/mattbaconz/BetterSettings/wiki)
- 💡 **Discussions**: [Ask questions](https://github.com/mattbaconz/BetterSettings/discussions)
- 📚 **API**: JavaDocs included in source

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details

## 🔨 Building from Source

```bash
git clone https://github.com/mattbaconz/BetterSettings.git
cd BetterSettings
mvn clean package
```

The compiled JAR will be in `target/BetterSettings-1.0.0.jar`

## 📦 Requirements

- ☕ **Java 21+**
- 📄 **Paper/Purpur/Folia 1.20.5+**
- ✅ **No external plugin dependencies required**

> **Note:** BetterSettings is a standalone plugin that only requires the Paper API (provided by your server). Third-party plugins can optionally depend on BetterSettings to integrate with its API.

---

<div align="center">

**Made with ❤️ by [mattbaconz](https://github.com/mattbaconz)**

⭐ Star this repo if you find it useful!

</div>
