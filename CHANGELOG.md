# Changelog

All notable changes to BetterSettings will be documented in this file.

## [1.0.0] - 2024-11-18

### Added
- **Event System** - Two new events for deep plugin integration:
  - `PlayerToggleSettingEvent` - Cancellable event fired before a setting is toggled
  - `PlayerSettingChangeEvent` - Event fired after a setting is successfully changed
- **Deterministic Sorting** - Settings with the same priority are now sorted alphabetically by ID, ensuring consistent GUI layout across reloads
- 50+ built-in settings across 5 categories
- Categorized GUI with pagination support
- Full Folia compatibility with regionized threading
- Async I/O for optimal performance
- Thread-safe data management
- Comprehensive configuration system (5 YAML files)
- Developer API for custom settings
- Custom setting loader (YAML-based)

### Features

#### Communication Settings (9)
- Global Chat, Private Messages, Death Messages, Join/Leave Messages
- Teleport Requests, Trade Requests, Friend Requests
- Chat Mentions, DM Sound

#### Display Settings (15)
- Scoreboard, Player Visibility, Particle Effects, Sound Effects
- Weather, Time, Damage Indicators, Night Vision
- AFK Status, Vanish, Block Break Particles, Scoreboard Numbers
- Tab List, Coordinates Display, Biome Display

#### Gameplay Settings (15)
- Auto-Pickup, Flight, PvP, Auto-Sprint, Auto-Respawn
- God Mode, Speed Boost, Jump Boost, Water Breathing, Fire Resistance
- Hunger Loss, Item Pickup, Entity Collision, Teleport Cooldown Bypass, Build Mode

#### Protection Settings (4)
- Drop Protection, Inventory Protection, Fall Damage, Mob Targeting

#### Notifications Settings (11)
- Mob Spawn, Achievements, Action Bar, Boss Bar, Titles
- Keep Inventory Reminder, Mob Griefing, Fire Spread, Explosions
- Command Spy, Social Spy

### Technical Details

#### Event System
The event system allows third-party plugins to:
- Cancel setting toggles before they occur (`PlayerToggleSettingEvent`)
- React to setting changes after they occur (`PlayerSettingChangeEvent`)
- Integrate seamlessly with BetterSettings without polling

Example use cases:
- Vanish plugins can react to vanish setting toggles
- Permission plugins can enforce requirements before enabling settings
- Logging plugins can track all setting changes
- Database plugins can sync settings to external storage

#### Deterministic Sorting
Settings are now sorted using a two-tier system:
1. Primary: Priority (ascending)
2. Secondary: ID (alphabetically)

This ensures:
- Consistent GUI layout across server restarts
- Predictable setting order for players
- No random shuffling when settings have the same priority

#### Performance Optimizations
- Async I/O with configurable concurrency
- Batch save operations
- Smart caching with automatic cleanup
- Buffered file operations
- Minimal object allocation

#### Folia Compatibility
- All player operations use region schedulers
- Global operations use global region scheduler
- No blocking operations on main thread
- Thread-safe data structures throughout

### Configuration

#### config.yml
- Main plugin settings
- GUI title customization

#### settings.yml
- Enable/disable individual settings
- Configure default states
- Set permissions
- Customize icons and descriptions
- Assign categories and priorities

#### messages.yml
- All user-facing messages
- Supports color codes and hex colors
- Placeholder support

#### performance.yml
- I/O settings (async, batch, concurrency)
- Cache configuration
- Auto-save intervals
- GUI performance options
- Logging levels

#### ui.yml
- GUI size and layout
- Category system configuration
- Visual elements (borders, fillers)
- Navigation buttons
- Sound effects
- Animations

### API

#### Core Interfaces
- `Setting` - Main interface for creating settings
- `SettingCategory` - Interface for organizing settings
- `SettingsRegistry` - Singleton for managing all settings
- `PlayerDataManager` - Access player setting states

#### Events
- `PlayerToggleSettingEvent` - Pre-toggle (cancellable)
- `PlayerSettingChangeEvent` - Post-change (not cancellable)

### Documentation

- **README.md** - Quick start guide and overview
- **API.md** - Complete developer documentation with examples
- **CONFIGURATION.md** - Detailed configuration guide
- **CONTRIBUTING.md** - Contribution guidelines
- **LICENSE** - MIT License

### Dependencies

- Paper API 1.20.6+ (provided by server)
- Java 21+
- No external plugin dependencies

### Known Limitations

- Custom YAML settings support basic actions only
- GUI animations are optional and disabled by default
- Maximum GUI size is 54 slots (6 rows)

### Future Considerations

- Additional action types for YAML settings
- More animation options
- Setting presets/profiles
- Import/export functionality
- Web-based configuration editor

---

## Version Format

This project follows [Semantic Versioning](https://semver.org/):
- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality in a backwards compatible manner
- **PATCH** version for backwards compatible bug fixes

## Links

- [GitHub Repository](https://github.com/mattbaconz/BetterSettings)
- [Issue Tracker](https://github.com/mattbaconz/BetterSettings/issues)
- [Wiki](https://github.com/mattbaconz/BetterSettings/wiki)
