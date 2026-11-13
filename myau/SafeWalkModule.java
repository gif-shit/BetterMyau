package myau;

import net.minecraft.client.Minecraft;

public class SafeWalkModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final FloatSetting motion = new FloatSetting("motion", 1.0f, 0.5f, 1.0f);
    public final FloatSetting speedMotion = new FloatSetting("speed-motion", 1.0f, 0.5f, 1.5f);
    public final BooleanSetting air = new BooleanSetting("air", false);
    public final BooleanSetting directionCheck = new BooleanSetting("direction-check", true);
    public final BooleanSetting pitchCheck = new BooleanSetting("pitch-check", true);
    public final BooleanSetting requirePress = new BooleanSetting("require-press", false);
    public final BooleanSetting blocksOnly = new BooleanSetting("blocks-only", true);

    public SafeWalkModule() {
        super("SafeWalk", false, 0);
    }

    private boolean shouldApplySafeWalk() {
        ScaffoldModule conflictingModule = (ScaffoldModule) MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        boolean otherEnabled = conflictingModule.isEnabled();
        if (otherEnabled) {
            return false;
        }
        if (this.directionCheck.getValue() && minecraft.field_71474_y.field_74351_w.func_151470_d()) {
            return false;
        }
        if (this.pitchCheck.getValue() && minecraft.field_71439_g.field_70125_A < 69.0f) {
            return false;
        }
        if (this.blocksOnly.getValue() && !ItemUtils.isHoldingPlaceableBlock()) {
            return false;
        }
        if (this.requirePress.getValue() && !minecraft.field_71474_y.field_74313_G.func_151470_d()) {
            return false;
        }
        if (minecraft.field_71439_g.field_70122_E
                && PlayerUtils.isOffsetCollisionFree(minecraft.field_71439_g.field_70159_w, -1.0, minecraft.field_71439_g.field_70179_y)) {
            return true;
        }
        return this.air.getValue()
                && PlayerUtils.isOffsetCollisionFree(minecraft.field_71439_g.field_70159_w, -2.0, minecraft.field_71439_g.field_70179_y);
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
        if (!(minecraft.field_71439_g.field_70122_E && MovementUtils.hasMovementInput() && this.shouldApplySafeWalk())) {
            return;
        }
        if (MovementUtils.speedPotionLevel() <= 0) {
            if (this.motion.getValue() != 1.0f) {
                MovementUtils.setSpeedAlongMotion(MovementUtils.horizontalSpeed() * (double) this.motion.getValue());
            }
        } else if (this.speedMotion.getValue() != 1.0f) {
            MovementUtils.setSpeedAlongMotion(MovementUtils.horizontalSpeed() * (double) this.speedMotion.getValue());
        }
    }
}
