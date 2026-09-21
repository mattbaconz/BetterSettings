# BetterSettings 1.1 runtime evidence

Date: 2026-09-21

## Release candidate

- Artifact: `target/BetterSettings-1.1.0.jar`
- Size: 162,729 bytes
- SHA-256: `c030a2f3de07a4f044cc633618fd333cbf42bb98999a9c0512f560dbeabf213e`
- Class-file major version: 65 (Java 21)
- Packaged metadata: BetterSettings `1.1.0`, Paper API `1.20.5`

Every run identified as an exact-candidate run below used that byte-for-byte jar.

## Clean build and automated checks

Command, using JDK 21.0.11:

```powershell
mvn -B -DforkCount=0 clean verify
```

Result: 27 tests, 0 failures, 0 errors, 0 skipped.

The clean build regenerated ZIP timestamps, producing a different archive hash. All 74 decompressed entries were compared against the runtime-tested jar and had zero content differences. The release file in `target` was then restored from the exact runtime-tested `c030...` artifact named above.

Coverage includes the customer's nested `Test:` definition, root and multi-definition custom files, malformed YAML, actions, materials, unknown categories, duplicate IDs, atomic reload, programmatic registration survival, preset/owner precedence, unavailable capabilities, legacy boolean data, inactive boolean retention, choice persistence, invalid stored choices, ordered asynchronous saving, fixed inventory sizing, native state restoration, and join-time visibility behavior.

Running Maven validation with JDK 17 fails with the intended Java 21 requirement. Package inspection also confirmed the typed public API and bundled Donut preset are present.

## Paper 1.20.5 / Java 21 compatibility floor

Exact-candidate runtime:

- Paper `git-Paper-22` for Minecraft 1.20.5
- Java `21.0.11`
- Isolated server, data, and worlds under the local runtime evidence directory

The fixture contained an existing `config.yml` without the new preset block, the customer's exact nested `Test:` definition, and legacy boolean player data.

Observed:

- BetterSettings `1.1.0` enabled successfully.
- Registry result: `loaded=55, unavailable=0, skipped=1, duplicate=0, invalid=0, preset=classic`.
- The unknown formatted category on `Test:` produced the expected warning and resolved to Uncategorized.
- Existing owner configuration, custom definitions, and player data remained byte-for-byte unchanged during startup and shutdown.
- Shutdown logged `Saving all player data...` followed by `All player data saved`.

An earlier 1.1 candidate completed the longer protocol journey: open the Uncategorized category, toggle the customer setting, reload malformed YAML without blanking the live registry, restore valid YAML, and restart with the boolean preserved. The exact release candidate repeats the affected parsing, rollback, persistence, and save-order paths in automated coverage, but that earlier rendered/protocol journey is not represented as byte-for-byte final-jar evidence.

## Rendered PlugDev journey / Paper 26.1.2 / Java 25

Exact-candidate runtime:

- Paper `26.1.2-74` / API `26.1.2.build.74-stable`
- Java `25.0.2`
- PlugDev with the project-owned Prism instance `FO 26.1.2`
- Candidate hash matched in both `target` and the live server plugin directory

Observed:

- Donut preset validation reported `loaded=63, unavailable=9, skipped=1, duplicate=0, invalid=0` before the local choice fixture and `loaded=64` afterward.
- The real client opened the 54-slot tabbed menu. Populated Chat, PvP, Visuals, Privacy, Scoreboard, and General tabs appeared; the unavailable empty Notifications tab stayed hidden.
- The local `Chat Scope` choice displayed `Everyone`, advanced to `Friends` with left-click, and returned to `Everyone` with right-click.
- A second left-click saved `qa_chat_scope: friends` as a YAML string.
- After a graceful client disconnect, bounded plugin flush, full server restart, and client reconnect, the Privacy menu still displayed `Current: Friends`.
- Shutdown logged `Saving all player data...` followed by `All player data saved`.

Exact-candidate screenshots are stored as `docs/media/1.1.0/bettersettings-final-candidate-chat.png` and `docs/media/1.1.0/bettersettings-choice-persisted.png`.

The previously selected marketplace GIFs were captured from predecessor candidate `a7d7daa011cd8ea0f98adb8dd12192f2505b3dba79f0818931b0d1bcc7892887`. The final candidate only changes persistence/lifecycle and owner-merge behavior, not the rendered menu. New GIF capture attempts on the final jar stopped with PlugDev `CAPTURE_NO_FRAMES`; the exact-candidate PNGs and live client state were retained instead of mislabelling the older GIFs.

## Stable Paper 26.2 / Java 25

Exact-candidate runtime:

- Paper `26.2-126` / API `26.2.build.126-stable`
- Java `25.0.1`
- Isolated server, data, and worlds under the local runtime evidence directory

Observed:

- BetterSettings `1.1.0` enabled successfully.
- Donut preset result: `loaded=63, unavailable=9, skipped=1, duplicate=0, invalid=0`.
- Shutdown completed the bounded final data flush.

No Paper 26.2 inventory render is claimed; the available project-owned client is Minecraft 26.1.2. Server load, registry construction, validation, and shutdown were exercised with the exact jar.

## Experimental Paper 26.3

Paper 26.3 was smoke-tested on the preceding candidate and loaded BetterSettings successfully. It was not rerun after the final persistence/lifecycle repairs and is not advertised as stable support.

## Boundaries

- Folia runtime behavior was not verified, so the plugin does not advertise a Folia-compatible claim.
- The stable 26.2 check is server-runtime proof, not a rendered player journey.
- The exact rendered journey used Paper 26.1.2; stable 26.2 used the same exact jar in a separate isolated runtime.
- Capability-backed Donut settings stay unavailable until a provider registers their behavior; no unsupported feature is claimed as native.
- A Windows OSHI performance-counter warning appeared during Paper 26.x startup. Paper continued to `Done`, and BetterSettings did not emit it.
