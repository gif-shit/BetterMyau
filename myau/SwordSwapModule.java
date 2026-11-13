package myau;

import myau.modules.SwordSwap;
import myau.modules.swordswap.SwordSwapMinecraft;
import net.minecraft.client.Minecraft;

public final class SwordSwapModule extends Module implements SwordSwapMinecraft.Bindings {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final SwordSwap engine = new SwordSwap(new SwordSwapMinecraft(minecraft, this));
    public final BooleanSetting pitOnly = new BooleanSetting("pit-only", true);
    public final BooleanSetting killauraOnly = new BooleanSetting("killaura-only", true);
    public final IntegerSetting slot1 = new IntegerSetting("slot-1", 1, 1, 9);
    public final IntegerSetting slot2 = new IntegerSetting("slot-2", 3, 1, 9);
    public final ModeSetting spoof = new ModeSetting("spoof", 0, new String[] { "NONE", "ANIMATIONS", "FULL" });

    public SwordSwapModule() {
        super("SwordSwap", false, 0);
    }

    @Override
    public void onSettingChanged(String name) {
        engine.settings.pitOnly = pitOnly.getValue();
        engine.settings.killauraOnly = killauraOnly.getValue();
        engine.settings.slot1 = slot1.getValue();
        engine.settings.slot2 = slot2.getValue();
        engine.settings.spoof = SwordSwap.Spoof.values()[spoof.getValue()];
    }

    @Override
    public void onEnable() {
        onSettingChanged("");
        engine.setEnabled(true);
    }

    @Override
    public void onDisable() {
        engine.setEnabled(false);
    }

    @EventHandler(priority = -25)
    public void onPreTick(PreTickEvent event) {
        engine.onTick();
    }

    @EventHandler(priority = -100)
    public void onPostTick(PostTickEvent event) {
        engine.onPostTick();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        engine.onWorldChange();
    }

    public boolean inPit() {
        return PitModule.isPitServer();
    }

    public boolean auraEnabledWithTarget() {
        KillAuraModule aura = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        return aura != null && aura.isEnabled() && aura.getCurrentTargetEntity() != null;
    }

    public boolean attackKeyDown() {
        return minecraft.field_71474_y != null && InputUtils.isKeyDown(minecraft.field_71474_y.field_74312_F.func_151463_i());
    }

    public boolean autoHealing() {
        AutoHealModule heal = (AutoHealModule) MyauClient.moduleManager.modules.get(AutoHealModule.class);
        return heal != null && heal.hasPendingSlotRestore();
    }
}
