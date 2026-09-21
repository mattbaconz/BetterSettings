# BetterSettings 1.1 runtime evidence

Date: 2026-09-21

Candidate:

- Artifact: `target/BetterSettings-1.1.0.jar`
- Size: 151,237 bytes
- SHA-256: `a7d7daa011cd8ea0f98adb8dd12192f2505b3dba79f0818931b0d1bcc7892887`
- Class-file major version: 65 (Java 21)
- Packaged metadata: version `1.1.0`, API version `1.20.5`

The final server load/validate/shutdown reruns and rendered PlugDev journey below used that exact candidate hash. The longer 1.20.5 upgrade journey was first completed on the immediately preceding candidate, before the narrow clean-install preset merge repair; that boundary is called out in its section.

## Build and unit verification

Command:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:MAVEN_OPTS = '-Xms16m -Xmx128m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=24m -XX:MaxMetaspaceSize=128m -XX:-TieredCompilation'
mvn -q -DforkCount=0 clean package
```

Result: 17 tests, 0 failures, 0 errors, 0 skipped.

A clean build from the release checkout produced the same 68 packaged file
contents as the runtime-tested jar. Its archive hash differed only because 53
ZIP entry timestamps were regenerated. The release artifact remains the exact
runtime-tested SHA-256 recorded above.

Coverage includes the exact nested `Test:` report, root and multi-definition files, malformed YAML, materials, categories, actions, duplicate IDs, atomic registry replacement, programmatic registration survival, preset and owner-override precedence, an untouched generated owner file not cancelling preset overrides, provider availability, old booleans, choice persistence/restart round-trip, invalid stored choices, and value cycling.

Running `mvn validate` with JDK 17 failed as intended with:

```text
BetterSettings requires JDK 21 or newer to build. Set JAVA_HOME to a JDK 21+ installation.
```

Package inspection confirmed `ValueSetting`, `PlayerSettingValueChangeEvent`, and `presets/donutsmp.yml` are present. The Minecraft plugin inspector found Java/Paper/Bukkit markers and correctly left runtime compatibility and Folia as unknown; the live runs below provide the Paper evidence. The inspector also sees the Maven compile dependency as 1.20.6, while the packaged API declaration is 1.20.5 and the 1.20.5 runtime run below verifies the operational floor.

## Paper 1.20.5 / Java 21

Runtime:

- Paper `git-Paper-22` for Minecraft 1.20.5
- Java `21.0.10`
- Isolated data and worlds in a dedicated local Paper 1.20.5 server directory

Final-candidate rerun:

- The exact final SHA-256 above cold-loaded on Paper 1.20.5 / Java 21.
- `/settings validate` reported `loaded=65, unavailable=9, skipped=1, duplicate=0, invalid=0`; the one warning was the intentional customer fixture's unknown category falling back to Uncategorized.
- Shutdown reported `Saving all player data...` followed by `All player data saved`.

The extended upgrade and player-protocol journey below was completed on predecessor candidate `15eea30a1acb6d1a17cc4c7cc696733c7f93f8d095b913bfb148a848f1724a40`. The final candidate differs in the tested configuration merge repair exercised by the rendered PlugDev journey below; it does not change the value/event/persistence paths used by this earlier journey.

Upgrade fixture:

- Existing `config.yml` intentionally omitted the new `preset` block.
- Existing `settings.yml` was replaced with the exact customer `Test:` shape.
- Existing player-data files were retained across stop/start.

Observed results:

- BetterSettings loaded as `1.1.0`.
- The named `Test:` block became setting ID `test`.
- Its color-formatted unknown category generated a warning and visibly fell back to Uncategorized.
- A protocol-level player client opened Classic, entered Uncategorized, saw `Toggle private messages`, toggled it, and wrote `test: false` as a YAML boolean.
- After another toggle and a full server restart, the same setting reopened as `Enabled`; the file contained `test: true`.
- Appending malformed YAML made `/settings reload` report rejection and `invalid=1`. The already-running GUI still opened the prior Test setting and accepted another toggle. Restoring valid YAML made reload succeed.
- A second live reload changed `preset.active` from `donutsmp` to `classic` while adding a duplicate built-in ID. Reload reported `duplicate=1`; a protocol client then received the existing Donut tabbed menu, proving both the registry and the parsed configuration snapshot rolled back together.
- Shutdown reported `Saving all player data...` followed by `All player data saved`.

Donut journey on the same runtime:

- The 54-slot GUI exposed the seven configured tab categories when they had content.
- Without the test provider, Auction Alerts was absent and the empty Notifications tab was absent.
- With a test `auction-alerts` behavior registered, Notifications and Auction Alerts appeared.
- Toggling Auction Alerts invoked the provider callback and persisted a YAML boolean.
- A custom `CHOICE` setting displayed `Friends`, advanced to `Nobody`, moved back to `Friends`, and treated shift-reset at the default as a no-op.
- The choice persisted as the string `qa_chat_scope: friends`.
- A test listener observed typed events `friends -> nobody` and `nobody -> friends`.
- A typed toggle event was observed after the `auction-alerts` provider callback.

The client was headless and inspected actual server inventory packets and item components; this is functional player-protocol proof, not a rendered visual screenshot.

## Rendered PlugDev journey / Paper 26.1.2 / Java 25

Runtime:

- Paper `26.1.2-74` / API `26.1.2.build.74-stable`
- Java `25.0.2`
- PlugDev `1.4.1` with the project-owned Prism profile `plugdev-26.1.2`
- Exact final candidate SHA-256 `a7d7daa011cd8ea0f98adb8dd12192f2505b3dba79f0818931b0d1bcc7892887`

Observed results:

- An untouched generated `settings.yml` no longer cancelled the active Donut preset's category remaps.
- Registry validation reported `loaded=64, unavailable=9, skipped=1, duplicate=0, invalid=0` with one capture-only custom choice fixture.
- The real client opened populated Chat, Privacy, Scoreboard, Visuals, PvP, and General tabs; the empty unavailable Notifications category stayed hidden.
- A capture-only `CHOICE` definition showed Everyone/Friends/Nobody and cycled using real left/right inventory clicks. It is not included in the release jar or owner defaults.
- Three PNGs and two GIFs were captured at 960x540. PlugDev inspection reported 44 frames / 2.93 seconds for the choice cycle and 75 frames / 5 seconds for the tab tour, with no freeze/black-frame diagnostics on either final GIF.
- The selected release media is under `docs/media/1.1.0/`.
- The server log contained no BetterSettings errors, and shutdown completed the bounded player-data flush.

## Stable Paper 26.2 / Java 25

Runtime:

- Paper `26.2-126` / API `26.2.build.126-stable`
- Java `25.0.2`
- Isolated data and worlds in a dedicated local Paper 26.2 server directory

Observed results with the exact final candidate:

- BetterSettings loaded as `1.1.0`.
- Donut preset registry validation: `loaded=64, unavailable=8, skipped=0, duplicate=0, invalid=0` with one test capability provider installed.
- Provider registration reduced unavailable entries from nine to eight.
- Shutdown completed the final player-data flush.

The installed headless Minecraft client library does not support protocol `26.2`, so no 26.2 player inventory journey is claimed. Server load, registry construction, command validation, provider registration, and shutdown were exercised.

## Experimental Paper 26.3 / Java 25

Runtime:

- Paper `26.3-26-dev` / API `26.3.build.26-alpha`
- Java `25.0.2`
- Paper jar SHA-256: `78e29c6c80fc43c6d46db6ec86f6ce3089a647d881e6bfe262aad9276af9dfcf`
- Isolated data and worlds in a dedicated local Paper 26.3 server directory

Observed results with the exact final candidate:

- BetterSettings loaded as `1.1.0`.
- Donut preset registry validation: `loaded=63, unavailable=9, skipped=1, duplicate=0, invalid=0`.
- Shutdown completed the final player-data flush.

This is experimental smoke evidence only. It is not an advertised stable support claim.

## Boundaries

- Folia runtime behavior was not verified. `folia-supported: true` and public full-support claims were removed.
- The stable 26.2 and experimental 26.3 tests did not render or click the inventory because the available headless client did not support those protocols.
- Rendered capture used Paper 26.1.2; stable 26.2 was separately cold-loaded and validated with the exact same final jar.
- The runtime journey covered the settings infrastructure, custom definitions, reload safety, persistence, typed events, actions, and one capability provider. It did not exhaustively exercise every historical Classic native gameplay behavior.
- A Windows OSHI performance-counter warning appeared during Paper 26.x startup; Paper continued to `Done`, and it was not emitted by BetterSettings.
- The QA host's inherited Windows temp path caused Java selector wakeup-socket failures before plugin loading on the final rerun. Supplying a short runtime-only `-Djdk.net.unixdomain.tmpdir` path resolved the host issue; no plugin code or packaged metadata was changed for it.
