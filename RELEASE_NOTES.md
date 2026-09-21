# BetterSettings 1.1.0

## Highlights

- Fixed custom definitions nested under names such as `Test:`.
- Added legacy root, multi-setting, and explicit-ID custom file forms.
- Added typed toggle and multi-choice settings with persisted string values for choices.
- Added atomic registry reloads so malformed or invalid configuration cannot blank a live menu.
- Preserved third-party programmatic registrations across YAML reloads.
- Added validated message, player-command, and console-command post-commit actions.
- Added an opt-in DonutSMP-inspired seven-tab inventory preset.
- Added capability-backed settings that stay hidden until a provider is registered.
- Added `/settings validate` and expanded reload/info counts.
- Fixed auto-respawn scheduling and added a bounded final data flush at shutdown.
- Centralized version `1.1.0`, Java 21 bytecode, and a Java-version build check.

## Compatibility

- Java 21 bytecode.
- Declared Paper API floor: 1.20.5.
- Isolated load and player journey completed on Paper 1.20.5 with Java 21.
- Isolated load and capability-registration smoke completed on stable Paper 26.2 with Java 25.
- Paper 26.3 is an experimental smoke target only and is not advertised as stable support.
- Folia support is not advertised because the full Folia runtime journey has not been verified.

See [RUNTIME_EVIDENCE_1.1.md](RUNTIME_EVIDENCE_1.1.md) for exact proof and boundaries.

## Migration from 1.0

No file conversion is required.

- Keep the existing `plugins/BetterSettings` directory.
- Replace only the plugin jar.
- Existing boolean player values remain booleans.
- Existing owner configuration is not overwritten.
- `preset.active` defaults to `classic` when the key is absent, preserving the existing menu.
- New bundled files are created only when missing.

Before production rollout, back up the plugin directory as normal operational hygiene and run `/settings validate`.

## Upgrade checklist

1. Back up `plugins/BetterSettings` as normal operational hygiene.
2. Replace the 1.0.0 jar with `BetterSettings-1.1.0.jar`.
3. Restart the server.
4. Run `/settings validate` and review any owner-configuration warnings.

Existing configuration and player data remain in place. The active preset remains
`classic` unless the owner explicitly selects `donutsmp`.
