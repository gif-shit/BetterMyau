# BetterMyau

BetterMyau 5.2 is a client-side mod for Minecraft Forge 1.8.9. The client sources are in `myau/`, and the bundled profile and Forge metadata are in `resources/`.

## Install

Download [`5.2.jar`](https://github.com/gif-shit/BetterMyau/releases/tag/v5.2) and put it in the `mods` folder of a Forge 1.8.9 instance. Profiles, friends, enemies, and config binds use `config/Myau/`.

## Development changes

- Scaffold now uses the supplied 260811 reconstruction for placement, hotbar selection, movement, towers, keep-Y, sneaking, and multi-place. The existing Hypixel rotation smoothing and turn pacing are retained. STABLE rotations, OLDNCP keep-Y, vertical tower prediction, and tower-on-press are available.
- The supplied Scaffold is a functional reconstruction with inferred search and advanced movement policies; original-client and live-server equivalence remain unverified.

## What's new in 5.2

- Rewound.cc flight module: Fly ported from Ivory's active Fly3 module, with Rewound A (Vanilla and OldNCP Slow/Damage), Rewound B, bow damage, drop flight, optional vertical ascent, and timer controls. The existing `Fly` config section and horizontal/vertical speed keys are retained; new settings use Ivory's defaults.
- 1st commit from bleach

## What's new in 5.1

- NoSlow Blink takes priority over KillAura AutoBlock while holding a sword. The selected AutoBlock mode resumes when Blink stops taking priority.
- NoSlow shows its current mode in the ArrayList when HUD suffixes are enabled.

## What's new in 5.0.0

- Displace adapted from [OpenB4](https://codeberg.org/abusez/OpenB4/commit/3d63944cc79be890c013cc79e5ed92d3533b48a5), with Left, Right, Random, and Void modes.
- Scaffold's Hypixel rotation mode and NoSlow's Blink mode adapted from BetterIvory.
- KillAura AutoBlock includes Full, Predictive, Legit, and Blink.
- Cleaner ClickGUI with draggable category panels, plus a vanilla-font `myau` watermark and TabGUI.
- Optional `wrap-bar` setting under HUD for an outline around the entire ArrayList. Enable `bar` to show the option.

## Controls

- **Insert** opens the ClickGUI by default. Left-click a module to toggle it, right-click to expand its settings, and use **B** to bind or **H** to hide it from the ArrayList.
- **Up/Down** move through the TabGUI, **Right** opens a category, **Left** closes it, and **Enter** toggles the selected module. The watermark, TabGUI, and accent color are configurable under HUD.
- In **Configs**, click a profile to load it or use **B** to assign a shortcut.

## Build

The PowerShell build compiles the updated classes with JDK 8 and overlays them onto an existing BetterMyau JAR. The tree includes decompiled client sources. A rebuild also requires an SRG-mapped Minecraft 1.8.9 JAR and the Forge 1.8.9 client libraries.

```powershell
.\build.ps1 -BaseJar 'C:/path/to/5.1.jar' -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

The build writes `build/5.2.jar`.

Run the headless Fly regression checks after building:

```powershell
.\tests\test-fly.ps1 -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

These checks cover the port's movement, packet flags, damage setup, bow timeout, and timer restoration. They do not establish live server compatibility.

Run the Scaffold core and integration checks, including the Fly checks for the shared event hooks:

```powershell
.\tests\test-scaffold.ps1 -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```
