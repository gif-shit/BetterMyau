package myau;

import myau.EventHandler;
import myau.FloatSetting;
import myau.InputUtils;
import myau.Module;
import myau.MoveFlyingEvent;
import myau.MovementUtils;
import myau.RotationUpdateEvent;
import net.minecraft.client.Minecraft;

public class FlyModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private double verticalMotion;
    public final FloatSetting horizontalSpeed = new FloatSetting("horizontal-speed", Float.valueOf(1.0f), Float.valueOf(0.0f), Float.valueOf(100.0f));
    public final FloatSetting verticalSpeed = new FloatSetting("vertical-speed", Float.valueOf(1.0f), Float.valueOf(0.0f), Float.valueOf(100.0f));

    public FlyModule() {
        super("Fly", false, 0);
    }

    @EventHandler
    public void onMoveFlying(MoveFlyingEvent event) {
        boolean keepVerticalMotion = this.isEnabled();
        if (!keepVerticalMotion) {
            return;
        }
        keepVerticalMotion = FlyModule.minecraft.field_71439_g.field_70122_E;
        if (!keepVerticalMotion) {
            FlyModule.minecraft.field_71439_g.field_70181_x = this.verticalMotion;
        }
        MovementUtils.setSpeedAlongMotion(0.0);
        event.setFriction((float)MovementUtils.baseMovementSpeed() * ((Float)this.horizontalSpeed.getValue()).floatValue());
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.verticalMotion = 0.0;
        if (FlyModule.minecraft.field_71462_r != null) {
            return;
        }
        boolean jumpPressed = InputUtils.isKeyDown(FlyModule.minecraft.field_71474_y.field_74314_A.func_151463_i());
        if (jumpPressed) {
            this.verticalMotion += ((Float)this.verticalSpeed.getValue()).doubleValue() * (double)0.42f;
        }
        if (InputUtils.isKeyDown(FlyModule.minecraft.field_71474_y.field_74311_E.func_151463_i())) {
            this.verticalMotion -= ((Float)this.verticalSpeed.getValue()).doubleValue() * (double)0.42f;
        }
        InputUtils.setKeyState(FlyModule.minecraft.field_71474_y.field_74311_E.func_151463_i(), false);
    }

    @Override
    public void onDisable() {
        FlyModule.minecraft.field_71439_g.field_70181_x = 0.0;
        MovementUtils.setSpeedAlongMotion(0.0);
        InputUtils.updateKeyState(FlyModule.minecraft.field_71474_y.field_74311_E.func_151463_i());
    }
}
