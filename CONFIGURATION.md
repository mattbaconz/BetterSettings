# BetterSettings 1.1 configuration

BetterSettings loads packaged defaults, applies the active preset in memory, then applies owner configuration. It never rewrites an existing owner file or deletes stored player values.

## Precedence

From lowest to highest precedence:

1. Packaged defaults.
2. The active bundled preset.
3. Owner overrides in `settings.yml`.
4. Unique additions from `settings/*.yml`.
5. Programmatic registrations from other plugins, which must also use unique IDs.

A duplicate addition fails validation. It is not an override mechanism.

## Preset selection

```yaml
preset:
  active: classic
  hide-unavailable: false
```

Available bundled presets:

- `classic`: existing category-menu behavior.
- `donutsmp`: 54-slot tabs for Chat, Notifications, PvP, Visuals, Privacy, Scoreboard, and General.

The Donut-inspired preset always hides capability-backed entries until their provider behavior is registered, then hides any empty tab. It does not install auction, bounty, economy, rank, item-lore, scoreboard-provider, or fast-crystal mechanics.

## Supported custom file shapes

### Legacy single setting at the file root

```yaml
enabled: true
id: myplugin_alerts
default-state: true
permission: null
icon: BELL
description: "&eAlerts"
category: notifications
priority: 1
```

### Multiple named settings

```yaml
staff-alerts:
  enabled: true
  default-state: true
  icon: BELL
  description: "&eStaff Alerts"
  category: notifications

chat-scope:
  enabled: true
  type: choice
  default: friends
  icon: WRITABLE_BOOK
  description: "&eChat Scope"
  category: communication
  options:
    everyone: Everyone
    friends: Friends
    nobody: Nobody
```

### Named block with an explicit ID

```yaml
Test:
  id: test_private_messages
  enabled: true
  default-state: true
  icon: ENDER_PEARL
  description: "&eToggle private messages"
  category: "&7Uncategorized"
```

When a named block omits `id`, its normalized block name becomes the ID. The exact customer form above without an `id` therefore becomes `test` and appears under Uncategorized.

## Toggle fields

```yaml
enabled: true
id: unique_id
type: toggle # optional; toggle is the default
default-state: true
permission: null
icon: PAPER
description: "&eDisplay label"
category: communication
priority: 1
behavior: optional-provider-id
```

## Choice fields

```yaml
enabled: true
id: chat_scope
type: choice
default: friends
permission: null
icon: WRITABLE_BOOK
description: "&eChat Scope"
category: communication
priority: 1
options:
  everyone: Everyone
  friends: Friends
  nobody: Nobody
```

A choice needs at least two unique options, and `default` must be one of them.

## Validated actions

Actions execute after a successful value commit:

```yaml
actions:
  - type: message
    value: "&aPreference changed to {value}."
  - type: player-command
    value: "example preference {value}"
  - type: console-command
    value: "example sync {uuid} {value}"
```

Supported action types are `message`, `player-command`, and `console-command`. Available substitutions are `{player}`, `{uuid}`, `{old-value}`, and `{value}`.

Actions can be conditional:

```yaml
actions:
  - type: message
    when: "true"
    value: "&aEnabled."
  - type: message
    when: "false"
    value: "&cDisabled."
```

Action syntax is validated during registry construction. Execution is best-effort after storage commits; command side effects are not transactional and are not rolled back if a later action fails.

## Validation and reload behavior

Use:

```text
/settings validate
/settings reload
/settings info
```

Reports include loaded, unavailable, skipped, duplicate, and invalid counts.

- Unknown or missing categories fall back to visible Uncategorized and produce a warning.
- Invalid material names fall back to `PAPER` and produce a warning.
- Invalid YAML, invalid choice definitions, invalid actions, and duplicate IDs reject the candidate reload.
- A rejected reload leaves the current live menu and registry operational.
- Programmatic settings and behaviors survive YAML reloads.

## Player data

Player files live in `plugins/BetterSettings/playerdata/`.

- Toggle values are saved as YAML booleans, preserving the 1.0 format.
- Choice values are saved as strings.
- An unknown stored choice displays and behaves as the current configured default.
- Existing files are loaded in place and are not rewritten as a migration step.

On normal updates, writes are asynchronous. Plugin shutdown performs a bounded final flush and reports whether it completed.
