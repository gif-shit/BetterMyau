package myau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

enum ModuleCategory {

    COMBAT("Combat", "Displace", "AimAssist", "AutoClicker", "Backtrack", "HitSelect", "KeepSprint", "KillAura", "LagRange", "NoHitDelay", "Reach", "TargetStrafe", "Velocity", "WTap", "AntiFireball"), PLAYER("Player", "AntiDebuff", "AntiObbyTrap", "AutoHeal", "AutoBlockin", "AutoTool", "BedNuker", "Blink", "ChestStealer", "FastPlace", "GhostHand", "InvManager", "NoFall", "NoRotate", "PantSwap", "Scaffold", "SpeedMine", "SwordSwap"), MOVEMENT("Movement", "AntiVoid", "Eagle", "Fly", "InvWalk", "Jesus", "LongJump", "NoJumpDelay", "NoSlow", "SafeWalk", "Speed", "Sprint", "Timer"), RENDER("Render", "DamageTags", "BedESP", "BPS", "Chams", "ChestESP", "ClickGui", "ESP", "FreeLook", "Fullbright", "HUD", "Indicators", "ItemEffects", "ItemESP", "LightningTracker", "NameTags", "NickHider", "NoHurtCam", "TargetHUD", "Tracers", "Trajectories", "ViewClip", "Xray"), EXPLOITS("Exploits", "Pit"), MISC("Misc", "BedWars", "SkyWars", "AutoLadder", "Weather", "DebugAC", "ViewPackets", "Test");

    private static final Map<String, ModuleCategory> BY_MODULE = new HashMap<>();

    final String label;

    private final String[] moduleNames;

    static {
        for (ModuleCategory category : values()) {
            for (String name : category.moduleNames) {
                BY_MODULE.put(name.toLowerCase(Locale.ROOT), category);
            }
        }
    }

    ModuleCategory(String label, String... moduleNames) {
        this.label = label;
        this.moduleNames = moduleNames;
    }

    static ModuleCategory of(Module module) {
        return BY_MODULE.getOrDefault(module.getName().toLowerCase(Locale.ROOT), EXPLOITS);
    }

    static boolean miscVisible() {
        ClickGuiModule gui = (ClickGuiModule) MyauClient.moduleManager.modules.get(ClickGuiModule.class);
        return gui != null && gui.showMisc.getValue();
    }

    static ModuleCategory[] visibleCategories() {
        return java.util.Arrays.stream(values()).filter(category -> category != MISC || miscVisible()).toArray(ModuleCategory[]::new);
    }

    List<Module> modules() {
        List<Module> result = new ArrayList<>();
        for (Module module : MyauClient.moduleManager.modules.values()) {
            if (of(module) == this)
                result.add(module);
        }
        return result;
    }
}
