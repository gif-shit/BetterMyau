package myau;

public abstract class Module {

    protected final String name;
    protected final boolean defaultEnabled;
    protected final int defaultKeyCode;
    protected final boolean defaultHidden;
    protected boolean enabled;
    protected int keyCode;
    protected boolean hidden;
    protected MutableValue<Boolean> previousKeyState;

    public Module(String name, boolean defaultEnabled, int defaultKeyCode) {
        this(name, defaultEnabled, defaultKeyCode, false);
    }

    public Module(String name, boolean defaultEnabled, int defaultKeyCode, boolean defaultHidden) {
        this.name = name;
        this.defaultEnabled = defaultEnabled;
        this.enabled = false;
        this.keyCode = this.defaultKeyCode = defaultKeyCode;
        this.hidden = this.defaultHidden = defaultHidden;
        this.previousKeyState = new MutableValue<Boolean>(false);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        switch (this.name) {
            case "AimAssist": return "helps aim at nearby players";
            case "AntiDebuff": return "hides blindness and nausea effects";
            case "AntiFireball": return "hits nearby fireballs away";
            case "AntiObbyTrap": return "mines blocks trapping your head";
            case "AntiObfuscate": return "removes obfuscated text formatting";
            case "AntiVoid": return "tries to stop you falling into the void";
            case "AutoClicker": return "clicks automatically while holding attack";
            case "AutoHeal": return "uses healing items automatically";
            case "AutoTool": return "selects a tool for the block you mine";
            case "Backtrack": return "delays enemy position updates";
            case "BedESP": return "highlights nearby beds";
            case "BedNuker": return "breaks nearby enemy beds automatically";
            case "DamageTags": return "shows floating damage and healing numbers";
            case "DebugAC": return "shows movement and packet diagnostics";
            case "ViewPackets": return "displays selected incoming and outgoing packets";
            case "Test": return "provides experimental movement and game tools";
            case "BedWars": return "adds bed tracking and BedWars game information";
            case "SkyWars": return "shows nearby enemies, strength and pearl timers";
            case "AutoBlockin": return "places blocks around you to make an enclosure";
            case "AutoLadder": return "places ladders automatically";
            case "Weather": return "changes the weather and time you see";
            case "Blink": return "holds movement packets until you disable it";
            case "Chams": return "makes players visible through blocks";
            case "ClickGui": return "opens the module and settings menu";
            case "ChestESP": return "highlights nearby chests";
            case "ChestStealer": return "takes items from open chests automatically";
            case "Displace": return "changes your facing direction when hitting";
            case "Eagle": return "sneaks automatically at block edges";
            case "ESP": return "draws player outlines, boxes and health bars";
            case "FastPlace": return "reduces the delay between placing blocks";
            case "Fly": return "provides configurable flight modes";
            case "FreeLook": return "moves your camera without turning your player";
            case "Fullbright": return "brightens dark areas";
            case "GhostHand": return "lets you interact through selected blocks";
            case "HitSelect": return "delays hits while targets resist damage";
            case "HUD": return "customizes the client overlay and module list";
            case "Indicators": return "warns about nearby incoming projectiles";
            case "InvManager": return "sorts equipment and removes unwanted items";
            case "InvWalk": return "allows movement with inventory screens open";
            case "ItemESP": return "highlights dropped items";
            case "ItemEffects": return "changes held item size and swing visuals only";
            case "Jesus": return "allows walking on water";
            case "KeepSprint": return "keeps sprinting when attacking";
            case "KillAura": return "attacks nearby targets automatically";
            case "LagRange": return "delays packets around nearby targets";
            case "LightningTracker": return "reports where lightning strikes";
            case "LongJump": return "extends your jump distance";
            case "MCF": return "adds or removes friends with middle click";
            case "NameTags": return "shows player names and equipment";
            case "NickHider": return "hides your name in displayed text";
            case "NoFall": return "tries to prevent fall damage";
            case "NoHitDelay": return "removes the delay after missed attacks";
            case "NoHurtCam": return "removes the camera shake when hurt";
            case "NoJumpDelay": return "removes the delay between jumps";
            case "NoRotate": return "ignores server requests to turn your camera";
            case "NoSlow": return "reduces slowdown while using items";
            case "PantSwap": return "switches pants using your configured rules";
            case "Pit": return "adds Pit player information and inventory tools";
            case "Reach": return "adjusts the range of melee attacks";
            case "SafeWalk": return "stops movement over block edges";
            case "Scaffold": return "places blocks beneath you while moving";
            case "Spammer": return "sends repeated chat messages";
            case "Speed": return "increases movement speed";
            case "SpeedMine": return "reduces the time needed to mine blocks";
            case "Sprint": return "sprints automatically while moving";
            case "SwordSwap": return "switches swords using your configured rules";
            case "TargetHUD": return "shows information about your current target";
            case "TargetStrafe": return "moves around your combat target";
            case "Timer": return "changes the speed of client game ticks";
            case "Tracers": return "draws lines to nearby players";
            case "Trajectories": return "shows projectile paths and landing points";
            case "Velocity": return "adjusts knockback from incoming hits";
            case "ViewClip": return "lets the third-person camera pass through blocks";
            case "WTap": return "briefly resets sprinting when attacking";
            case "Xray": return "shows selected blocks through the world";
            case "BPS": return "shows your movement speed in blocks per second";
            default: return "";
        }
    }

    public String getDisplayText() {
        String keyPrefix = this.keyCode == 0 ? "" : String.format("&l[%s] &r", InputUtils.getKeyName(this.keyCode));
        String stateText = this.enabled ? "&a&lON" : "&c&lOFF";
        return String.format("%s%s &r(%s&r)", keyPrefix, this.name, stateText);
    }

    public String[] getHudInfo() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public void resetEnabled() {
        this.setEnabled(this.defaultEnabled);
    }

    public int getKeyCode() {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public void resetKeyCode() {
        this.setKeyCode(this.defaultKeyCode);
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void resetHidden() {
        this.setHidden(this.defaultHidden);
    }

    public MutableValue<Boolean> getPreviousKeyState() {
        return this.previousKeyState;
    }

    public void setPreviousKeyState(MutableValue<Boolean> previousKeyState) {
        this.previousKeyState = previousKeyState;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onSettingChanged(String settingName) {
    }
}
