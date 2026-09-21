# BetterSettings 1.1.0 media manifest

Captured locally with PlugDev from real Minecraft 26.1.2 clients connected to Paper 26.1.2 / Java 25.

## Exact final-candidate screenshots

Release artifact:

- Jar: `target/BetterSettings-1.1.0.jar`
- SHA-256: `c030a2f3de07a4f044cc633618fd333cbf42bb98999a9c0512f560dbeabf213e`
- BetterSettings: `1.1.0`
- Active preset: `donutsmp`

Files:

- `bettersettings-final-candidate-chat.png` - real 54-slot Chat tab from the exact release jar.
- `bettersettings-choice-persisted.png` - `Chat Scope: Friends` after a full server and client restart, proving the typed choice persisted.

Both screenshots were visually inspected. The choice is a local QA definition used to demonstrate the public multi-choice feature; it is not bundled in the release jar or owner defaults.

## Marketplace media captured before final lifecycle repairs

These selected listing assets were captured from predecessor candidate `a7d7daa011cd8ea0f98adb8dd12192f2505b3dba79f0818931b0d1bcc7892887`:

- `bettersettings-donut-chat.png` - 960x540, Chat tab.
- `bettersettings-donut-privacy.png` - 960x540, Privacy tab.
- `bettersettings-donut-general.png` - 960x540, General tab.
- `bettersettings-choice-cycle.gif` - 960x540, 44 frames, 2.93 seconds.
- `bettersettings-tab-tour.gif` - 960x540, 75 frames, 5 seconds.

PlugDev inspection reported no freeze or black-frame diagnostics for either GIF, and all PNGs were visually inspected. The later final-candidate repairs affect persistence ordering, native state restoration, join-time visibility, owner merge behavior, and coordinator cleanup; they do not alter the menu shown in these assets.

The listing media is therefore valid visual evidence, while the two exact-candidate PNGs above provide the byte-for-byte release-artifact proof.
