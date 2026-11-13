# BetterMyau

BetterMyau 5.5 is a client-side mod for Minecraft Forge 1.8.9. The client sources are in `myau/`, and the bundled profile and Forge metadata are in `resources/`.

## Install

Download [`bettermyau u5.5.jar`](https://github.com/gif-shit/BetterMyau/releases/tag/v5.5) and put it in the `mods` folder of a Forge 1.8.9 instance. Profiles, friends, enemies, and config binds use `config/Myau/`.

## What's new in 5.5

- Added Raven and Myau TargetHUD modes. Myau remains the default.
- Added DamageTags, DebugAC, ViewPackets, and Test in Render, and AutoBlockin in Player.
- Added BedWars, SkyWars, AutoLadder, and Weather in Misc. The modules are off by default; enable `show-misc` in ClickGui settings to show the category.
- Moved BedTracker into BedWars, with a new bed status card, threat indicator, distance display, and bed marker.
- Replaced RGB sliders with a colour wheel and brightness control.
- Replaces stable rotations with smooth rotations.
- Added portable build and test tools for Windows, Linux, and macOS.
- Renamed the release JAR to `bettermyau u5.5.jar`.

-bleach

## What's new in 5.4

- Added pit module, sword swap, and pantswap.
- Fixed Trajectories to stop at the first block or entity impact and mark the landing point.
- Cleaned up client code, simplified config loading, and enabled compilation of all client sources.

-bleach

## What's new in 5.3

- Added STABLE rotations to Scaffold: follows your camera and movement inputs without turning toward individual blocks. Pitch adjusts for placement. The existing Hypixel rotations are retained.
- Added Stable Smoothing: 0% gives the fastest turn; higher values make yaw and pitch changes more gradual.
- Faster Stable yaw and pitch with a hardcoded 1.8x turn-step speed multiplier. The smoothing slider is unchanged.
- Stable checks placement again after movement to place newly reachable blocks in the same tick.
- Added tower-on-press: hold right-click to activate the selected tower mode.
- Fixed Stable + Extra diagonal tower missing reachable block faces at corners.

-bleach

## What's new in 5.2

- Rewound.cc flight module, with Rewound A (Vanilla and OldNCP Slow/Damage), Rewound B, bow damage, drop flight, optional vertical ascent, and timer controls. The existing `Fly` config section and horizontal/vertical speed keys are retained.
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

## Pit and swaps

- Added Pit, PantSwap, and SwordSwap. Pit includes player lists, chat filters, auto-math, inventory cleaning, and player hiding. PantSwap waits for server confirmations; SwordSwap includes animation and full item spoofing.
- Pit uses loaded equipment, friends/enemies, and matching cached profile data. Spawn hiding learns the spawn level from the scoreboard's idle/spawn status; it waits until that level is known. Event scheduling reports unavailable because a schedule provider is not included.
- Pit's inventory cleaner is disabled by default. When enabled, it drops whole iron armor stacks, including named or enchanted armor, and plain iron swords when a diamond sword is in the hotbar.

## Build

The Python build compiles every Java source with JDK 8 and packages it with the bundled runtime from an existing BetterMyau JAR. A rebuild also requires an SRG-mapped Minecraft 1.8.9 JAR and the Forge 1.8.9 client libraries.

```powershell
.\build.ps1 -BaseJar 'C:/path/to/5.1.jar' -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

On Windows, Linux, and macOS, install Python 3 and JDK 8, then run:

```sh
python build.py --base-jar '/path/to/5.1.jar' --minecraft-srg '/path/to/mc-srg.jar' --libraries '/path/to/libraries' --jdk '/path/to/jdk8'
```

The build writes `build/bettermyau u5.5.jar`. The PowerShell command above calls the same builder. If `--libraries` is omitted, it uses the platform's standard Minecraft libraries folder; `--jdk` can be omitted when `JAVA_HOME` or `PATH` supplies the JDK.

The same Forge JAR is used on all three systems. The launcher must supply Java 8 and the matching LWJGL natives. Linux/macOS game rendering has not been tested here.

Run the headless Fly regression checks after building:

```powershell
.\tests\test-fly.ps1 -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

These checks cover movement, packet flags, damage setup, bow timeout, and timer restoration. They do not establish live server compatibility.

Run the Scaffold rotation and tower checks, including the Fly checks:

```powershell
.\tests\test-scaffold.ps1 -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

Run all regression checks, including settings, config files, and event dispatch:

```powershell
.\tests\test-codebase.ps1 -MinecraftSrg 'C:/path/to/mc-srg.jar' -Libraries 'C:/path/to/libraries' -Jdk 'C:/path/to/jdk8/bin'
```

The complete suite also runs through Python on all three systems:

```sh
python tests/test.py --base-jar '/path/to/5.1.jar' --minecraft-srg '/path/to/mc-srg.jar' --libraries '/path/to/libraries' --jdk '/path/to/jdk8'
```

These checks cover Smooth bridging and diagonal towers, profile migration, colour selection, Raven health animation, module defaults and categories, weather restoration, and damage/heal tracking. They run without launching Minecraft or connecting to a server.
