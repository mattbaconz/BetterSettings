# Settings catalog status

BetterSettings no longer publishes a hand-maintained total such as "29/60" or "50+". The authoritative catalog is the definition metadata loaded by the running server, because owners can enable, disable, override, and add settings and providers can change capability availability.

Use `/settings info` or `/settings validate` for the current counts:

- `loaded`: valid definitions in the current snapshot;
- `unavailable`: definitions requiring a behavior that is not currently available;
- `skipped`: disabled or otherwise intentionally skipped definitions;
- `duplicate`: conflicting IDs;
- `invalid`: syntax or semantic failures.

## Native behavior

The Classic packaged catalog retains the existing BetterSettings-owned behaviors implemented by `CoreListener`, including chat visibility, player visibility, weather/time preferences, protection and movement options, potion-style modes, auto-respawn, item pickup, collision, and related Paper-native controls.

Some historical Classic entries are integration surfaces rather than claims that BetterSettings alone implements another plugin's feature. Owners should disable an entry that has no local behavior or replace it with a capability-backed definition supplied by the relevant provider.

## Donut-inspired preset

The Donut preset reorganizes native settings into these tabs:

1. Chat
2. Notifications
3. PvP
4. Visuals
5. Privacy
6. Scoreboard
7. General

It also declares generic capability-backed settings for:

- auction alerts;
- bounty alerts;
- payment policy;
- item-worth lore;
- display rank;
- fast crystals;
- scoreboard money, kills, and playtime lines.

These entries remain hidden until another plugin registers the matching `SettingBehavior`. Empty tabs are hidden. BetterSettings does not pretend to provide DonutSMP ranks, economy data, auction data, bounties, or combat mechanics.

## Evidence boundary

Definition metadata proves what the registry can display and how it stores a value. A native listener or registered provider behavior is required before a setting can claim a server-side effect. A successful command action proves only that BetterSettings dispatched the command; arbitrary third-party effects remain owned by the target plugin.
