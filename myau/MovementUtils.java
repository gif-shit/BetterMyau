package myau;

import myau.MyauClient;
import myau.RotationState;
import myau.TargetStrafeModule;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;

public final class MovementUtils {
    private static final Minecraft minecraft = Minecraft.func_71410_x();

    private MovementUtils() {
    }

    public static boolean hasMovementInput() {
        return MovementUtils.minecraft.field_71474_y.field_74351_w.func_151470_d() != MovementUtils.minecraft.field_71474_y.field_74368_y.func_151470_d() || MovementUtils.minecraft.field_71474_y.field_74370_x.func_151470_d() != MovementUtils.minecraft.field_71474_y.field_74366_z.func_151470_d();
    }

    public static int forwardInput() {
        int input = 0;
        if (MovementUtils.minecraft.field_71474_y.field_74351_w.func_151470_d()) {
            ++input;
        }
        if (MovementUtils.minecraft.field_71474_y.field_74368_y.func_151470_d()) {
            --input;
        }
        return input;
    }

    public static int strafeInput() {
        int input = 0;
        if (MovementUtils.minecraft.field_71474_y.field_74370_x.func_151470_d()) {
            ++input;
        }
        if (MovementUtils.minecraft.field_71474_y.field_74366_z.func_151470_d()) {
            --input;
        }
        return input;
    }

    public static float movementYaw() {
        float baseYaw = RotationState.isActive() ? RotationState.movementYaw() : MovementUtils.minecraft.field_71439_g.field_70177_z;
        return MovementUtils.movementYawForInput(baseYaw, MovementUtils.minecraft.field_71439_g.field_71158_b.field_78900_b, MovementUtils.minecraft.field_71439_g.field_71158_b.field_78902_a);
    }

    public static float movementYawForInput(float yaw, float forward, float strafe) {
        float strafeYaw;
        TargetStrafeModule targetStrafe = (TargetStrafeModule)MyauClient.moduleManager.modules.get(TargetStrafeModule.class);
        if (targetStrafe != null && targetStrafe.isEnabled() && !Float.isNaN(strafeYaw = targetStrafe.getStrafeYaw())) {
            return strafeYaw;
        }
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float forwardFactor = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * forwardFactor * Math.signum(strafe);
        }
        return MathHelper.func_76142_g((float)yaw);
    }

    public static float motionYaw() {
        if (MovementUtils.horizontalSpeed() == 0.0) {
            return MathHelper.func_76142_g((float)MovementUtils.minecraft.field_71439_g.field_70177_z);
        }
        return MathHelper.func_76142_g((float)((float)Math.toDegrees(Math.atan2(MovementUtils.minecraft.field_71439_g.field_70179_y, MovementUtils.minecraft.field_71439_g.field_70159_w)) - 90.0f));
    }

    public static double baseMovementSpeed() {
        double speed = 0.28015;
        if (MovementUtils.speedPotionTicks() > 0) {
            speed *= 1.0 + 0.15 * (double)MovementUtils.speedPotionLevel();
        }
        return speed;
    }

    public static double boostedSpeedForPotionLevel(int potionLevel) {
        double speed = 0.452;
        if (potionLevel == 1) {
            return speed * 1.1;
        }
        if (potionLevel >= 2) {
            speed *= 1.2;
        }
        return speed;
    }

    public static double boostedMovementSpeed() {
        int potionLevel = MovementUtils.speedPotionTicks() > 0 ? MovementUtils.speedPotionLevel() : 0;
        return MovementUtils.boostedSpeedForPotionLevel(potionLevel);
    }

    public static double horizontalSpeed() {
        return MovementUtils.horizontalMagnitude(MovementUtils.minecraft.field_71439_g.field_70159_w, MovementUtils.minecraft.field_71439_g.field_70179_y);
    }

    public static double horizontalMagnitude(double motionX, double motionZ) {
        return Math.hypot(motionX, motionZ);
    }

    public static void setSpeedAlongMotion(double speed) {
        MovementUtils.setHorizontalMotion(speed, MovementUtils.motionYaw());
    }

    public static void setHorizontalMotion(double speed, float yaw) {
        MovementUtils.minecraft.field_71439_g.field_70159_w = -Math.sin(Math.toRadians(yaw)) * speed;
        MovementUtils.minecraft.field_71439_g.field_70179_y = Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static void addHorizontalMotion(double speed, float yaw) {
        MovementUtils.minecraft.field_71439_g.field_70159_w += -Math.sin(Math.toRadians(yaw)) * speed;
        MovementUtils.minecraft.field_71439_g.field_70179_y += Math.cos(Math.toRadians(yaw)) * speed;
    }

    public static int speedPotionLevel() {
        if (!MovementUtils.minecraft.field_71439_g.func_70644_a(Potion.field_76424_c)) {
            return 0;
        }
        return MovementUtils.minecraft.field_71439_g.func_70660_b(Potion.field_76424_c).func_76458_c() + 1;
    }

    public static int speedPotionTicks() {
        if (!MovementUtils.minecraft.field_71439_g.func_70644_a(Potion.field_76424_c)) {
            return 0;
        }
        return MovementUtils.minecraft.field_71439_g.func_70660_b(Potion.field_76424_c).func_76459_b();
    }

    public static float groundAcceleration() {
        float friction = MovementUtils.minecraft.field_71439_g.field_70170_p.func_180495_p((BlockPos)new BlockPos((int)MathHelper.func_76128_c((double)MovementUtils.minecraft.field_71439_g.field_70165_t), (int)(MathHelper.func_76128_c((double)MovementUtils.minecraft.field_71439_g.func_174813_aQ().field_72338_b) - 1), (int)MathHelper.func_76128_c((double)MovementUtils.minecraft.field_71439_g.field_70161_v))).func_177230_c().field_149765_K * 0.91f;
        return MovementUtils.minecraft.field_71439_g.func_70689_ay() * (0.16277136f / (friction * friction * friction));
    }

    public static double[] inputAcceleration() {
        float forward;
        float strafe = (float)MovementUtils.strafeInput() * 0.98f;
        float magnitudeSquared = strafe * strafe + (forward = (float)MovementUtils.forwardInput() * 0.98f) * forward;
        if (magnitudeSquared < 1.0E-4f) {
            return new double[]{0.0, 0.0};
        }
        float magnitude = MathHelper.func_76129_c((float)magnitudeSquared);
        if (magnitude < 1.0f) {
            magnitude = 1.0f;
        }
        float acceleration = MovementUtils.groundAcceleration() / magnitude;
        float sinYaw = MathHelper.func_76126_a((float)(MovementUtils.minecraft.field_71439_g.field_70177_z * (float)Math.PI / 180.0f));
        float cosYaw = MathHelper.func_76134_b((float)(MovementUtils.minecraft.field_71439_g.field_70177_z * (float)Math.PI / 180.0f));
        return new double[]{(strafe *= acceleration) * cosYaw - (forward *= acceleration) * sinYaw, forward * cosYaw + strafe * sinYaw};
    }

    public static void correctMovementForYaw(float yaw) {
        float relativeYaw = MathHelper.func_76142_g((float)(MovementUtils.movementYawForInput(MovementUtils.minecraft.field_71439_g.field_70177_z, MovementUtils.forwardInput(), MovementUtils.strafeInput()) - yaw + 22.5f));
        int direction = (int)(relativeYaw + 180.0f) / 45 % 8;
        switch (direction) {
            case 0: {
                MovementUtils.setMovementInput(-1.0f, 0.0f);
                break;
            }
            case 1: {
                MovementUtils.setMovementInput(-1.0f, 1.0f);
                break;
            }
            case 2: {
                MovementUtils.setMovementInput(0.0f, 1.0f);
                break;
            }
            case 3: {
                MovementUtils.setMovementInput(1.0f, 1.0f);
                break;
            }
            case 4: {
                MovementUtils.setMovementInput(1.0f, 0.0f);
                break;
            }
            case 5: {
                MovementUtils.setMovementInput(1.0f, -1.0f);
                break;
            }
            case 6: {
                MovementUtils.setMovementInput(0.0f, -1.0f);
                break;
            }
            case 7: {
                MovementUtils.setMovementInput(-1.0f, -1.0f);
                break;
            }
        }
        if (MovementUtils.minecraft.field_71439_g.field_71158_b.field_78899_d) {
            MovementUtils.minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.3f;
            MovementUtils.minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.3f;
        }
    }

    private static void setMovementInput(float forward, float strafe) {
        MovementInput input = MovementUtils.minecraft.field_71439_g.field_71158_b;
        input.field_78900_b = forward;
        input.field_78902_a = strafe;
    }
}
