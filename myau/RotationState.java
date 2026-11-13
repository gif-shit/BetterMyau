package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public final class RotationState {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static int ticksSinceActive = -1;
    private static int updateTicks = -1;
    private static float previousBodyYaw;
    private static float bodyYaw;
    private static float previousHeadYaw;
    private static float headYaw;
    private static float previousPitch;
    private static float pitch;
    private static float movementYaw;
    private static int movementPriority;

    private RotationState() {
    }

    private static float computeBodyYaw(float requestedHeadYaw, float currentBodyYaw) {
        float headBodyDifference;
        float targetBodyYaw = currentBodyYaw;
        double deltaX = RotationState.minecraft.field_71439_g.field_70165_t - RotationState.minecraft.field_71439_g.field_70169_q;
        double deltaZ = RotationState.minecraft.field_71439_g.field_70161_v - RotationState.minecraft.field_71439_g.field_70166_s;
        if ((float)(deltaX * deltaX + deltaZ * deltaZ) > 0.0025000002f) {
            targetBodyYaw = (float)MathHelper.func_181159_b((double)deltaZ, (double)deltaX) * 180.0f / (float)Math.PI - 90.0f;
        }
        if (RotationState.minecraft.field_71439_g.field_70733_aJ > 0.0f) {
            targetBodyYaw = requestedHeadYaw;
        }
        if ((headBodyDifference = MathHelper.func_76142_g((float)(requestedHeadYaw - (currentBodyYaw += MathHelper.func_76142_g((float)(targetBodyYaw - currentBodyYaw)) * 0.3f)))) < -75.0f) {
            headBodyDifference = -75.0f;
        }
        if (headBodyDifference >= 75.0f) {
            headBodyDifference = 75.0f;
        }
        float result = requestedHeadYaw - headBodyDifference;
        if (headBodyDifference * headBodyDifference > 2500.0f) {
            result += headBodyDifference * 0.2f;
        }
        return result;
    }

    public static void update(boolean active, float requestedYaw, float requestedPitch, float requestedMovementYaw, int requestedMovementPriority) {
        ticksSinceActive = active ? 0 : ticksSinceActive + 1;
        updateTicks = ticksSinceActive;
        previousBodyYaw = bodyYaw;
        bodyYaw = active ? RotationState.computeBodyYaw(requestedYaw, bodyYaw) : RotationState.minecraft.field_71439_g.field_70761_aq;
        previousHeadYaw = headYaw;
        headYaw = active ? requestedYaw : RotationState.minecraft.field_71439_g.field_70759_as;
        previousPitch = pitch;
        pitch = active ? requestedPitch : RotationState.minecraft.field_71439_g.field_70125_A;
        movementYaw = requestedMovementYaw;
        movementPriority = requestedMovementPriority;
    }

    public static boolean isActive() {
        return RotationState.isActiveWithinTicks(0);
    }

    public static void replace(boolean active, float requestedYaw, float requestedPitch, float requestedMovementYaw, int requestedMovementPriority) {
        ticksSinceActive = active ? 0 : updateTicks;
        bodyYaw = active ? RotationState.computeBodyYaw(requestedYaw, previousBodyYaw) : minecraft.field_71439_g.field_70761_aq;
        headYaw = active ? requestedYaw : minecraft.field_71439_g.field_70759_as;
        pitch = active ? requestedPitch : minecraft.field_71439_g.field_70125_A;
        movementYaw = requestedMovementYaw;
        movementPriority = requestedMovementPriority;
    }

    public static boolean isActiveWithinTicks(int maximumInactiveTicks) {
        return ticksSinceActive >= 0 && ticksSinceActive <= maximumInactiveTicks;
    }

    public static float previousBodyYaw() {
        return previousBodyYaw;
    }

    public static float bodyYaw() {
        return bodyYaw;
    }

    public static float previousHeadYaw() {
        return previousHeadYaw;
    }

    public static float headYaw() {
        return headYaw;
    }

    public static float previousPitch() {
        return previousPitch;
    }

    public static float pitch() {
        return pitch;
    }

    public static float movementYaw() {
        return movementYaw;
    }

    public static float movementPriority() {
        return movementPriority;
    }
}
