package myau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

enum ModuleCategory {
    COMBAT("Combat", "AimAssist", "AutoClicker", "Backtrack", "HitSelect", "KeepSprint", "KillAura",
            "LagRange", "NoHitDelay", "Reach", "TargetStrafe", "Velocity", "WTap", "AntiFireball"),
    PLAYER("Player", "AntiDebuff", "AntiObbyTrap", "AutoHeal", "AutoTool", "BedNuker", "Blink",
            "ChestStealer", "FastPlace", "GhostHand", "InvManager", "NoFall", "NoRotate", "Scaffold", "SpeedMine"),
    MOVEMENT("Movement", "AntiVoid", "Eagle", "Fly", "InvWalk", "Jesus", "LongJump", "NoJumpDelay",
            "NoSlow", "SafeWalk", "Speed", "Sprint", "Timer"),
    RENDER("Render", "BedESP", "BedTracker", "BPS", "Chams", "ChestESP", "ClickGui", "ESP", "FreeLook",
            "Fullbright", "HUD", "Indicators", "ItemESP", "LightningTracker", "NameTags", "NickHider",
            "NoHurtCam", "TargetHUD", "Tracers", "Trajectories", "ViewClip", "Xray"),
    EXPLOITS("Exploits");

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

    List<Module> modules() {
        List<Module> result = new ArrayList<>();
        for (Module module : MyauClient.moduleManager.modules.values()) {
            if (of(module) == this) result.add(module);
        }
        return result;
    }
}
