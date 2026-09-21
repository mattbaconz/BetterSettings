# Changelog

## [1.1.0] - 2026-09-21

### Added

- Typed `TOGGLE` and `CHOICE` settings with string accessors and persistence.
- `ValueSetting`, `SettingType`, `SettingOption`, `SettingBehavior`, typed registration, and `PlayerSettingValueChangeEvent`.
- Opt-in DonutSMP-inspired tab preset with capability-aware visibility.
- Validated message, player-command, and console-command actions.
- `/settings validate` and richer registry counts for info and reload.
- Unit coverage for custom definition shapes, invalid inputs, duplicate IDs, atomic reload behavior, preset precedence, capabilities, and persisted values.

### Changed

- Registry rebuilds are immutable and atomic.
- YAML reloads preserve programmatic settings and behaviors.
- Unknown categories fall back to visible Uncategorized with a warning.
- Invalid materials use a safe `PAPER` fallback and appear in validation output.
- Toggle values continue to use YAML booleans; choice values use strings.
- Java 21 bytecode and build-time Java validation are explicit.
- Version metadata is centralized at `1.1.0`.
- Documentation uses live catalog counts instead of contradictory fixed totals.
- Folia support is no longer advertised without a completed Folia runtime journey.

### Fixed

- Named custom blocks such as `Test:` now load from `settings.yml`.
- Single-setting root and multi-setting custom files are both recognized.
- A malformed or invalid reload no longer replaces the working registry.
- Auto-respawn uses the player's scheduler.
- Shutdown waits for a bounded final player-data flush.
- Per-player saves are ordered so an older asynchronous snapshot cannot overwrite a newer one.
- Choice values that look like booleans remain YAML strings.
- Persisted native state is available to join handlers and reapplied after login.
- Fixed-layout inventories always use the required 54 slots.
- Action conditions such as `ANY` now match case-insensitively.
- GUI drag events are blocked in managed inventories.
- Untouched generated defaults no longer override Donut preset categories on a clean installation.

## [1.0.0] - 2024-11-18

### Added

- Initial categorized player-settings GUI.
- Boolean `Setting` API, registry, data accessors, and toggle/change events.
- YAML configuration and per-player persistence.
- Built-in Paper-native behaviors and integration-oriented setting definitions.
