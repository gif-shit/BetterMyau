package myau;

import myau.mixin.AccessorEntity;
import net.minecraft.client.Minecraft;

public class SpeedModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final FloatSetting multiplier = new FloatSetting("multiplier", 1.0f, 0.0f, 10.0f);
    public final FloatSetting friction = new FloatSetting("friction", 1.0f, 0.0f, 10.0f);
    public final PercentSetting strafe = new PercentSetting("strafe", 0);

    public SpeedModule() {
        super("Speed", false, 0);
    }

    private boolean canApplySpeed() {
        ScaffoldModule conflictingModule = (ScaffoldModule) MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        boolean otherEnabled = conflictingModule.isEnabled();
        if (otherEnabled || !MovementUtils.hasMovementInput()) {
            return false;
        }
        if (minecraft.field_71439_g.func_71024_bL().func_75116_a() <= 6) {
            return false;
        }
        if (minecraft.field_71439_g.func_70093_af()
                || minecraft.field_71439_g.func_70090_H()
                || minecraft.field_71439_g.func_180799_ab()) {
            return false;
        }
        return !((AccessorEntity) minecraft.field_71439_g).getIsInWeb();
    }

    private void applyStrafe() {
        double speed = MovementUtils.horizontalSpeed();
        MovementUtils.setHorizontalMotion(speed * (double) ((float) (100 - this.strafe.getValue()) / 100.0f), MovementUtils.motionYaw());
        MovementUtils.addHorizontalMotion(speed * (double) ((float) this.strafe.getValue() / 100.0f), MovementUtils.movementYaw());
        MovementUtils.setSpeedAlongMotion(speed);
    }

    @EventHandler(priority = -50)
    public void onMoveFlying(MoveFlyingEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!this.canApplySpeed()) {
            return;
        }
        if (minecraft.field_71439_g.field_70122_E) {
            minecraft.field_71439_g.field_70181_x = 0.42f;
            MovementUtils.setHorizontalMotion(MovementUtils.boostedMovementSpeed() * (double) this.multiplier.getValue(), MovementUtils.movementYaw());
            return;
        }
        if (this.friction.getValue() != 1.0f) {
            event.setFriction(event.getFriction() * this.friction.getValue());
        }
        if (this.strafe.getValue() > 0) {
            this.applyStrafe();
        }
    }

    @EventHandler(priority = -50)
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.canApplySpeed()) {
            minecraft.field_71439_g.field_71158_b.field_78901_c = false;
        }
    }
}
