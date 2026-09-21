# Migrating BetterSettings 1.0 to 1.1

BetterSettings 1.1 is designed as an in-place, non-destructive upgrade.

## Before upgrading

1. Stop the server.
2. Back up `plugins/BetterSettings/` as normal operational hygiene.
3. Replace the old plugin jar with `BetterSettings-1.1.0.jar`.
4. Keep every existing YAML and player-data file in place.

## First start

Start with Java 21 or newer. If `preset.active` is missing, BetterSettings uses `classic`, preserving the 1.0 category-menu flow.

The plugin copies a newly bundled resource only when that file does not exist. It does not overwrite `config.yml`, `settings.yml`, custom settings, or player data.

Run:

```text
/settings validate
/settings info
```

Resolve any `duplicate` or `invalid` entry before switching presets. Category and material fallbacks are warnings and remain visible in validation output.

## Existing custom definitions

All of these shapes are supported:

- one setting at the root of `settings/*.yml`;
- several named blocks in one custom file;
- named blocks such as `Test:` in `settings.yml`;
- an explicit `id` within a named block.

IDs must be unique. An invalid reload keeps the previous live menu instead of publishing a partial registry.

## Player values

- Existing boolean values are read unchanged and remain YAML booleans when saved.
- New choice values are YAML strings.
- Stored values are never deleted merely because a preset is changed or a provider is unavailable.
- An obsolete choice value resolves to the configured default until the player selects a valid option.

## Donut-inspired preset

To opt in after validation:

```yaml
preset:
  active: donutsmp
  hide-unavailable: false
```

Reload with `/settings reload`. The preset is merged in memory; it does not rewrite owner files. Provider-backed entries remain hidden until a plugin registers their matching `SettingBehavior`.

To return to the original layout, set `active: classic` and reload.

## Integration compatibility

Existing `Setting` implementations, `registerSetting(...)`, boolean data accessors, and legacy toggle/change events remain supported. Providers can adopt typed values incrementally through `ValueSetting`, string data accessors, `registerValueSetting(...)`, and `PlayerSettingValueChangeEvent`.

Folia support is not advertised in 1.1 because a complete Folia runtime journey has not been verified.
