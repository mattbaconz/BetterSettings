# BetterSettings API 1.1

BetterSettings keeps its 1.0 boolean API and adds typed values and capability providers in 1.1.

## Dependency

Declare BetterSettings as a server dependency in `plugin.yml`:

```yaml
depend: [BetterSettings]
```

The local artifact can be installed to a development Maven repository or referenced by the consuming build. Match the API version to the installed BetterSettings jar.

## Existing boolean API

Existing `Setting` implementations and registration calls are unchanged:

```java
SettingsRegistry.getInstance().registerSetting(mySetting);
```

Existing storage calls are unchanged:

```java
boolean enabled = BetterSettings.getInstance()
    .getDataManager()
    .getSetting(player.getUniqueId(), "myplugin_alerts", true);

BetterSettings.getInstance()
    .getDataManager()
    .setSetting(player.getUniqueId(), "myplugin_alerts", false);
```

For toggle changes, the event order is:

1. `PlayerToggleSettingEvent` before commit; it remains cancellable.
2. Value commit.
3. Post-commit callback and optional behavior, both best-effort.
4. `PlayerSettingValueChangeEvent`.
5. Legacy `PlayerSettingChangeEvent`.

Legacy boolean values remain YAML booleans on disk.

## Typed settings

`ValueSetting` supports `SettingType.TOGGLE` and `SettingType.CHOICE`:

```java
public final class ChatScopeSetting implements ValueSetting {
    @Override public String getId() { return "myplugin_chat_scope"; }
    @Override public SettingType getType() { return SettingType.CHOICE; }
    @Override public String getDefaultValue() { return "friends"; }

    @Override
    public List<SettingOption> getOptions() {
        return List.of(
            new SettingOption("everyone", "Everyone"),
            new SettingOption("friends", "Friends"),
            new SettingOption("nobody", "Nobody")
        );
    }

    @Override public String getDescription() { return "Chat Scope"; }
    @Override public String getPermission() { return null; }
    @Override public SettingCategory getCategory() {
        return SettingsRegistry.getInstance().getCategory("communication");
    }
    @Override public int getPriority() { return 10; }

    @Override
    public ItemStack getIcon(Player player, String value) {
        return new ItemStack(Material.WRITABLE_BOOK);
    }
}
```

Register it through the typed method:

```java
SettingsRegistry.getInstance().registerValueSetting(new ChatScopeSetting());
```

Choice options must have unique, non-empty stored values. A choice default must match an option. Choice values are saved as strings.

## Typed values

```java
PlayerDataManager data = BetterSettings.getInstance().getDataManager();
UUID playerId = player.getUniqueId();

String value = data.getSettingValue(playerId, "myplugin_chat_scope", "friends");
data.setSettingValue(playerId, "myplugin_chat_scope", "nobody");

String safeValue = data.getChoice(
    playerId,
    "myplugin_chat_scope",
    "friends",
    Set.of("everyone", "friends", "nobody")
);
```

`getChoice(...)` returns the configured default when a stored choice is not in the allowed set. It does not destroy the owner file merely because a value is from an older definition.

## Typed event

`PlayerSettingValueChangeEvent` fires after a successful toggle or choice commit:

```java
@EventHandler
public void onValueChange(PlayerSettingValueChangeEvent event) {
    String id = event.getSettingId();
    SettingType type = event.getSettingType();
    String oldValue = event.getOldValue();
    String newValue = event.getNewValue();
}
```

Choice changes fire only the typed event. Toggle changes also fire both legacy boolean events as described above.

## Capability-backed behavior

A provider plugin can make a bundled or custom capability-backed setting available:

```java
SettingsRegistry.getInstance().registerBehavior(new SettingBehavior() {
    @Override
    public String getId() {
        return "auction-alerts";
    }

    @Override
    public boolean isAvailable() {
        return auctionService.isReady();
    }

    @Override
    public void apply(Player player, ValueSetting setting,
                      String oldValue, String newValue) throws Exception {
        auctionService.setAlerts(player.getUniqueId(), Boolean.parseBoolean(newValue));
    }
});
```

The behavior ID must match the definition's `behavior` field. Unavailable capability-backed entries are hidden by the Donut preset, and empty tabs disappear. A behavior runs after the BetterSettings value is committed. Failures are logged and do not roll back storage, because arbitrary third-party command or provider effects cannot be transactional.

Behavior registrations and programmatic settings survive `/settings reload`.

## Registry rules

- IDs are globally unique across packaged, preset, owner, custom, and programmatic settings.
- A duplicate custom ID is a validation failure; it never silently replaces another setting.
- YAML reloads build a complete immutable candidate and publish it atomically.
- A rejected reload leaves the previous registry snapshot live.
- `getSettings()` and `getValueSettings()` return snapshot-backed collections.

## Threading

GUI interactions and player-facing changes occur through Paper's player scheduler. Do not block callbacks or events with file, database, or network work. BetterSettings performs player-data writes asynchronously and waits for a bounded final flush during shutdown.

Folia support is not currently advertised because the complete runtime journey has not been verified on Folia.
