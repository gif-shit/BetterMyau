package myau;

import myau.BooleanSetting;
import myau.EventHandler;
import myau.FloatSetting;
import myau.ItemUtils;
import myau.Module;
import myau.MovementUtils;
import myau.MyauClient;
import myau.PlayerUtils;
import myau.RotationUpdateEvent;
import myau.SafeWalkEvent;
import myau.ScaffoldModule;
import net.minecraft.client.Minecraft;

public class SafeWalkModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final FloatSetting motion = new FloatSetting("motion", Float.valueOf(1.0f), Float.valueOf(0.5f), Float.valueOf(1.0f));
    public final FloatSetting speedMotion = new FloatSetting("speed-motion", Float.valueOf(1.0f), Float.valueOf(0.5f), Float.valueOf(1.5f));
    public final BooleanSetting air = new BooleanSetting("air", false);
    public final BooleanSetting directionCheck = new BooleanSetting("direction-check", true);
    public final BooleanSetting pitchCheck = new BooleanSetting("pitch-check", true);
    public final BooleanSetting requirePress = new BooleanSetting("require-press", false);
    public final BooleanSetting blocksOnly = new BooleanSetting("blocks-only", true);

    public SafeWalkModule() {
        super("SafeWalk", false, 0);
    }

    private boolean shouldApplySafeWalk() {
        ScaffoldModule conflictingModule = (ScaffoldModule)MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        boolean otherEnabled = conflictingModule.isEnabled();
        if (otherEnabled) {
            return false;
        }
        if (((Boolean)this.directionCheck.getValue()).booleanValue() && SafeWalkModule.minecraft.field_71474_y.field_74351_w.func_151470_d()) {
            return false;
        }
        if (((Boolean)this.pitchCheck.getValue()).booleanValue() && SafeWalkModule.minecraft.field_71439_g.field_70125_A < 69.0f) {
            return false;
        }
        if (((Boolean)this.blocksOnly.getValue()).booleanValue() && !ItemUtils.isHoldingPlaceableBlock()) {
            return false;
        }
        if (((Boolean)this.requirePress.getValue()).booleanValue() && !SafeWalkModule.minecraft.field_71474_y.field_74313_G.func_151470_d()) {
            return false;
        }
        if (SafeWalkModule.minecraft.field_71439_g.field_70122_E && PlayerUtils.isOffsetCollisionFree(SafeWalkModule.minecraft.field_71439_g.field_70159_w, -1.0, SafeWalkModule.minecraft.field_71439_g.field_70179_y)) {
            return true;
        }
        return (Boolean)this.air.getValue() != false && PlayerUtils.isOffsetCollisionFree(SafeWalkModule.minecraft.field_71439_g.field_70159_w, -2.0, SafeWalkModule.minecraft.field_71439_g.field_70179_y);
    }

    @EventHandler
    public void onSafeWalk(SafeWalkEvent event) {
        if (this.isEnabled() && this.shouldApplySafeWalk()) {
            event.setSafeWalk(true);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!(SafeWalkModule.minecraft.field_71439_g.field_70122_E && MovementUtils.hasMovementInput() && this.shouldApplySafeWalk())) {
            return;
        }
        if (MovementUtils.speedPotionLevel() <= 0) {
            if (((Float)this.motion.getValue()).floatValue() != 1.0f) {
                MovementUtils.setSpeedAlongMotion(MovementUtils.horizontalSpeed() * (double)((Float)this.motion.getValue()).floatValue());
            }
        } else if (((Float)this.speedMotion.getValue()).floatValue() != 1.0f) {
            MovementUtils.setSpeedAlongMotion(MovementUtils.horizontalSpeed() * (double)((Float)this.speedMotion.getValue()).floatValue());
        }
    }
}
