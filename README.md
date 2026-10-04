# BetterMyau

BetterMyau is an updated build of **Myau** (Forge 1.8.9, client-side). It keeps everything the original has — all modules,
commands, settings and the `config/Myau` profile folder — and adds a ClickGui, a Backtrack module, a FreeLook module,
config keybinds and in-GUI module binding.

The mod id (`myau`) and the config folder (`config/Myau`) are unchanged, so existing profiles, friends and enemies keep working.
Only the display name and jar name changed (`BetterMyau-1.0.0.jar`).

## What's new compared to the original Myau

### ClickGui
- Opens with **Insert** (default). The same key closes it; Esc also closes it. Rebind it with the **ClickGui** module in the Render
  panel, or with `.bind ClickGui <key>`.
- Insert was chosen because Meowtils' own GUI uses Right Shift, and both reacting to one key made the GUI flash closed.
- Modules are grouped into draggable panels: Combat, Movement, Player, Render, Misc and Configs. The mouse wheel scrolls.
- **Left-click** toggles a module. **Right-click** expands its settings. Right-click a panel header to collapse it.
- Settings are real controls: checkboxes, click-to-cycle modes, sliders for numbers, R/G/B sliders for colors, and inline text
  editing for string settings (click, type, Enter to confirm, Esc to revert).
- **Hide button (H)** on every module row toggles "hide in HUD", the same as the `.hide` command.
- **Bind button (B)** on every module row: click it, then press a key or mouse button to bind. Right-click **B** to unbind.
  Delete/Backspace while listening also unbinds, Esc cancels. Middle-clicking a module row also starts a bind.
- A large **BetterMyau** watermark is drawn in the middle of the screen behind the panels, coloured from the HUD colour setting.
- A hint line at the bottom of the screen lists the controls.

### Configs panel and config keybinds
- The **Configs** panel lists every `.json` profile in `config/Myau`. Left-click one to load it (same as `.config load <name>`).
  The active config is highlighted.
- Each config has a **B** button to bind it to a key or mouse button; pressing that key in-game loads the config.
  Right-click **B** to unbind. Binds are stored in `config/Myau/config_binds.txt`.

### Backtrack (new module, Combat)
Ported from the Raven (keystrokesmod) `Backtrack`. It briefly holds back the target's movement packets while you attack, so you hit
them at their older server-side position. Delay is adaptive, and packets are flushed on damage, velocity, explosions, teleports,
respawn and world change. The delayed position is drawn as a box.

Settings: `delay`, `min-distance`, `max-distance`, `cooldown`, `max-hurt-time`, `only-attacking`, `attack-window`,
`adaptive-delay`, `disable-adventure`, `flush-on-damage`, `ignore-teammates`, `weapon-only`, `show-position` (NONE / DEFAULT / HUD).

Differences from the Raven version: attacks are detected from the outgoing attack packet, and the position display is a mode setting
instead of a toggle plus colour picker.

### FreeLook (new module, Render)
Ported from the Raven `Freelook`. Look around in third person without changing the direction your player faces.
It uses the module's own keybind (default **Left Alt**, hold) instead of a separate key setting.

Settings: `hold`, `invert-pitch`, `lock-pitch`, `custom-fov`, `fov`. Opening a menu or changing world turns it off.
This adds two mixins (`MixinFreelookEntity`, `MixinFreelookRenderer`), registered in `mixins.myau.json`.

### NickHider
The `name` setting can now be typed directly in the ClickGui.

## Changed / added files

New classes in `myau/`: `BacktrackModule`, `ClickGuiModule`, `ClickGuiScreen`, `FreelookModule`, `ConfigBinds`,
`mixin/MixinFreelookEntity`, `mixin/MixinFreelookRenderer`.

Edited: `MyauClient` (registers the new modules), `MyauForgeMod` (mod name), `mixins.myau.json` (two new mixins), `mcmod.info`.

## Not changed
Chat messages still show the `[Myau]` prefix, and the mod id, package name and `config/Myau` folder keep the original names
so nothing existing breaks.

## Notes on building
The original jar is SRG-named (`func_*` / `field_*`). The new classes were compiled against a Minecraft 1.8.9 jar remapped to SRG
and patched into the existing jar; no other classes were rebuilt. A copy of the original jar is kept as
`myau-forge-1.8.9-1.0.0-original-backup.jar` next to this folder.
