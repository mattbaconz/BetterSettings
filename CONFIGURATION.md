# Configuration Guide

Complete guide to configuring BetterSettings.

## Configuration Files

BetterSettings uses multiple YAML files for configuration:

| File | Purpose |
|------|---------|
| `config.yml` | Main plugin settings |
| `settings.yml` | Built-in settings configuration |
| `messages.yml` | User-facing messages |
| `performance.yml` | Performance tuning |
| `ui.yml` | GUI appearance and layout |
| `settings/*.yml` | Custom setting definitions |

All files are located in `plugins/BetterSettings/`.

## config.yml

Main plugin configuration.

```yaml
# GUI Settings
gui:
  # Title displayed at the top of the settings GUI
  # Supports legacy color codes (&) and hex colors (&#RRGGBB)
  title: "&6⚙ &ePlayer Settings"
```

### Options

**gui.title**
- Type: String
- Default: `"&6⚙ &ePlayer Settings"`
- Description: Main GUI title
- Supports: Color codes (`&`), hex colors (`&#RRGGBB`)

## settings.yml

Configure all 50+ built-in settings.

### Setting Structure

Each setting has the following options:

```yaml
setting-name:
  enabled: true              # Enable/disable this setting
  default-state: true        # Default value for new players
  permission: null           # Permission required (null = none)
  icon: MATERIAL_NAME        # Material for GUI icon
  description: "&eText"      # Display name (supports colors)
  category: "category_id"    # Category for organization
  priority: 100              # Sort order (lower = first)
```

### Example Settings

**Communication Settings:**
```yaml
chat:
  enabled: true
  default-state: true
  permission: null
  icon: PAPER
  description: "&eToggle global chat messages"
  category: "communication"
  priority: 1

private-messages:
  enabled: true
  default-state: true
  permission: null
  icon: ENDER_PEARL
  description: "&eToggle private messages"
  category: "communication"
  priority: 2
```

**Gameplay Settings:**
```yaml
flight:
  enabled: true
  default-state: false
  permission: "bettersettings.flight"
  icon: ELYTRA
  description: "&eToggle flight mode"
  category: "gameplay"
  priority: 2

god-mode:
  enabled: true
  default-state: false
  permission: "bettersettings.godmode"
  icon: ENCHANTED_GOLDEN_APPLE
  description: "&eToggle invincibility"
  category: "gameplay"
  priority: 6
```

### Disabling Settings

To disable a setting, set `enabled: false`:

```yaml
flight:
  enabled: false  # This setting won't appear in GUI
```

### Custom Icons

Use any valid Bukkit Material name:

```yaml
chat:
  icon: PAPER           # Default
  icon: WRITABLE_BOOK   # Alternative
  icon: BOOK            # Another option
```

[Full Material List](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)

## messages.yml

Customize all user-facing messages.

```yaml
# Command Messages
no-permission: "&cYou don't have permission to use this."
player-only: "&cThis command can only be used by players."
unknown-subcommand: "&cUnknown subcommand. Use /settings [reload|info]"

# Setting Messages
pm-disabled: "&cThat player has private messages disabled."
setting-enabled: "&aEnabled: &7{setting}"
setting-disabled: "&cDisabled: &7{setting}"

# Admin Messages
reload-success: "&aBetterSettings configuration reloaded!"
reload-failed: "&cFailed to reload configuration. Check console for errors."

# Info Command
info-header: "&6&l=== BetterSettings Info ==="
info-version: "&eVersion: &7{version}"
info-settings: "&eRegistered Settings: &7{count}"
info-cached: "&eCached Players: &7{count}"
info-pending: "&ePending I/O Operations: &7{count}"
info-async: "&eAsync I/O: &7{enabled}"
info-batch: "&eBatch Saves: &7{enabled}"
```

### Placeholders

Messages support the following placeholders:

| Placeholder | Description | Used In |
|-------------|-------------|---------|
| `{setting}` | Setting name | setting-enabled, setting-disabled |
| `{version}` | Plugin version | info-version |
| `{count}` | Numeric value | info-settings, info-cached, info-pending |
| `{enabled}` | Boolean value | info-async, info-batch |
| `{current}` | Current page | GUI title |
| `{total}` | Total pages | GUI title |
| `{category}` | Category name | GUI title |

### Color Codes

Supports both legacy and hex colors:

```yaml
# Legacy colors
message: "&aGreen &cRed &eYellow"

# Hex colors
message: "&#FF5555Red &#55FF55Green &#5555FFBlue"

# Mixed
message: "&6Gold &#FFD700Custom Gold"
```

## performance.yml

Fine-tune performance for your server.

### I/O Settings

```yaml
io:
  # Use async I/O for all file operations
  async: true
  
  # Batch save operations
  batch-saves: true
  
  # Maximum concurrent file operations
  # SSD: 4-8, HDD: 2-4, NVMe: 8-16
  max-concurrent: 4
  
  # Use buffered I/O
  buffered: true
```

**Recommendations:**

| Server Type | max-concurrent |
|-------------|----------------|
| HDD | 2-4 |
| SSD | 4-8 |
| NVMe | 8-16 |

### Cache Settings

```yaml
cache:
  # Cleanup interval in ticks (20 ticks = 1 second)
  cleanup-interval: 12000  # 10 minutes
  
  # Maximum age for cached offline player data (milliseconds)
  max-age: 300000  # 5 minutes
  
  # Initial cache capacity (number of players)
  initial-capacity: 100
```

**Recommendations:**

| Server Size | initial-capacity |
|-------------|------------------|
| Small (< 50) | 50 |
| Medium (50-200) | 100 |
| Large (200-500) | 250 |
| Huge (500+) | 500 |

### Auto-Save Settings

```yaml
auto-save:
  # Auto-save interval in ticks
  interval: 6000  # 5 minutes
  
  # Save data when player quits
  save-on-quit: true
  
  # Load data when player joins
  load-on-join: true
```

**Interval Guidelines:**

| Interval | Ticks | Recommended For |
|----------|-------|-----------------|
| 1 minute | 1200 | High-risk servers |
| 5 minutes | 6000 | Normal servers |
| 10 minutes | 12000 | Low-risk servers |
| Disabled | 0 | Manual saves only |

### GUI Settings

```yaml
gui:
  # Cache GUI items
  cache-items: true
  
  # Refresh GUI after toggling
  refresh-on-toggle: true
  
  # Items per page
  items-per-page: 45
  
  # Show page numbers in GUI title
  show-page-numbers: true
```

### Logging Settings

```yaml
logging:
  # Log debug information
  debug: false
  
  # Log setting registrations
  registrations: true
  
  # Log data operations (load/save)
  data-operations: false
  
  # Log setting toggles
  toggles: false
```

**Warning:** Enabling all logging options can impact performance on busy servers.

## ui.yml

Customize the GUI appearance and layout.

### Title Configuration

```yaml
title:
  # Main title
  text: "&6⚙ &ePlayer Settings"
  
  # Show page numbers when paginated
  show-page-numbers: true
  
  # Page number format
  page-format: " &7({current}/{total})"
  
  # Category view title format
  category-format: "&6⚙ &e{category}"
```

### Layout Configuration

```yaml
layout:
  # GUI size (9, 18, 27, 36, 45, 54)
  size: 54
  
  # Items per page
  items-per-page: 45
  
  # Enable category system
  categories-enabled: true
  
  # Category selection mode: "menu" or "tabs"
  category-mode: "menu"
```

**Size Recommendations:**

| Size | Rows | items-per-page | Use Case |
|------|------|----------------|----------|
| 27 | 3 | 18 | Few settings (< 20) |
| 54 | 6 | 45 | Many settings (20+) |

**Category Modes:**

- **menu**: Show category selection first, then settings
- **tabs**: Show all settings with category separators

### Visual Elements

```yaml
visual:
  # Border decoration
  border:
    enabled: true
    material: "BLACK_STAINED_GLASS_PANE"
    name: " "
    
  # Filler items (empty slots)
  filler:
    enabled: false
    material: "GRAY_STAINED_GLASS_PANE"
    name: " "
  
  # Setting item appearance
  setting-item:
    # Show enchantment glow when enabled
    glow-when-enabled: true
    
    # Lore format
    lore:
      enabled-text: "&a✓ Enabled"
      disabled-text: "&c✗ Disabled"
      click-hint: "&7Click to toggle"
      category-hint: "&8Category: &7{category}"
      permission-hint: "&8Permission: &7{permission}"
      
    # Show additional info in lore
    show-category: true
    show-permission: false
    show-click-hint: true
```

### Navigation Buttons

```yaml
navigation:
  # Previous page button
  previous:
    enabled: true
    slot: 45
    material: "ARROW"
    name: "&e← Previous Page"
    lore:
      - "&7Page {page}/{total}"
      - ""
      - "&7Click to go back"
  
  # Next page button
  next:
    enabled: true
    slot: 53
    material: "ARROW"
    name: "&eNext Page →"
    lore:
      - "&7Page {page}/{total}"
      - ""
      - "&7Click to continue"
  
  # Info/Help button
  info:
    enabled: true
    slot: 49
    material: "BOOK"
    name: "&6Settings Menu"
    lore:
      - "&7Page &e{page}&7/&e{total}"
      - "&7Total Settings: &e{count}"
      - ""
      - "&7Click settings to toggle"
```

**Slot Numbers:**

```
0  1  2  3  4  5  6  7  8
9  10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53  <- Navigation row
```

### Categories

```yaml
categories:
  communication:
    id: "communication"
    name: "&bCommunication"
    icon: "PAPER"
    priority: 1
    permission: null
    description:
      - "&7Chat and messaging settings"
      - ""
      - "&eClick to view settings"
  
  display:
    id: "display"
    name: "&dDisplay"
    icon: "ENDER_EYE"
    priority: 2
    permission: null
    description:
      - "&7Visual and display settings"
      - ""
      - "&eClick to view settings"
```

### Sounds

```yaml
sounds:
  # Play sound when opening GUI
  open:
    enabled: true
    sound: "UI_BUTTON_CLICK"
    volume: 1.0
    pitch: 1.0
  
  # Play sound when toggling setting
  toggle:
    enabled: true
    sound: "BLOCK_NOTE_BLOCK_PLING"
    volume: 0.5
    pitch: 1.5
```

[Full Sound List](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html)

## Custom Settings (YAML)

Create custom settings without coding by placing YAML files in `plugins/BetterSettings/settings/`.

### Example: custom_setting.yml

```yaml
# Unique identifier (must be unique across all plugins)
id: "myplugin_mysetting"

# Display name (supports color codes)
description: "&eToggle My Feature"

# Icon material
icon: "DIAMOND"

# Default state for new players
default-state: true

# Permission required (null = none)
permission: "myplugin.setting.mysetting"

# Category (optional)
category: "gameplay"

# Priority (optional, default: 100)
priority: 50

# Actions to execute when toggled
actions:
  enable:
    - "command: give {player} diamond 1"
    - "message: &aFeature enabled!"
  disable:
    - "message: &cFeature disabled!"
```

### Action Types

**command**: Execute console command
```yaml
- "command: give {player} diamond 1"
```

**message**: Send message to player
```yaml
- "message: &aYou enabled the setting!"
```

**broadcast**: Broadcast to all players
```yaml
- "broadcast: &e{player} toggled a setting!"
```

**permission**: Grant/revoke permission
```yaml
- "permission: myplugin.feature"
```

**effect**: Apply potion effect
```yaml
- "effect: SPEED 999999 1"
```

### Placeholders in Actions

| Placeholder | Description |
|-------------|-------------|
| `{player}` | Player name |
| `{uuid}` | Player UUID |
| `{world}` | Player's world |
| `{x}` | Player's X coordinate |
| `{y}` | Player's Y coordinate |
| `{z}` | Player's Z coordinate |

## Color Codes Reference

### Legacy Colors

| Code | Color | Code | Color |
|------|-------|------|-------|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Light Purple |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |

### Formatting Codes

| Code | Format |
|------|--------|
| `&l` | Bold |
| `&m` | Strikethrough |
| `&n` | Underline |
| `&o` | Italic |
| `&r` | Reset |

### Hex Colors

Format: `&#RRGGBB`

Examples:
- `&#FF0000` - Red
- `&#00FF00` - Green
- `&#0000FF` - Blue
- `&#FFD700` - Gold
- `&#FF69B4` - Hot Pink

## Reloading Configuration

Reload all configuration files without restarting:

```
/settings reload
```

This reloads:
- config.yml
- settings.yml
- messages.yml
- performance.yml
- ui.yml
- Custom settings

**Note:** Player data is not reloaded. Players must rejoin for setting changes to apply.

## Troubleshooting

### Configuration Not Loading

1. Check YAML syntax (use [YAML Lint](https://www.yamllint.com/))
2. Look for errors in console
3. Verify file encoding is UTF-8
4. Check file permissions

### Colors Not Working

1. Ensure using `&` not `§`
2. For hex colors, use `&#RRGGBB` format
3. Check client supports colors (1.16+)

### Settings Not Appearing

1. Verify `enabled: true` in settings.yml
2. Check permission requirements
3. Ensure icon material is valid
4. Look for registration errors in console

### Performance Issues

1. Reduce `max-concurrent` in performance.yml
2. Increase `auto-save.interval`
3. Disable `logging.debug`
4. Enable `io.batch-saves`
