package myau;

import myau.Event;
import net.minecraft.util.MathHelper;

public class RotationUpdateEvent
extends Event {
    private final float previousYaw;
    private final float previousPitch;
    private float yaw;
    private float pitch;
    private float movementYaw;
    private int rotationPriority = -1;
    private int movementPriority = -1;
    private boolean modified;

    public RotationUpdateEvent(float previousYaw, float previousPitch, float yaw, float pitch) {
        this.previousYaw = previousYaw;
        this.previousPitch = previousPitch;
        this.yaw = yaw;
        this.pitch = pitch;
        this.movementYaw = yaw;
    }

    public RotationUpdateEvent(RotationUpdateEvent source) {
        this.previousYaw = source.previousYaw;
        this.previousPitch = source.previousPitch;
        this.yaw = source.yaw;
        this.pitch = source.pitch;
        this.movementYaw = source.movementYaw;
        this.rotationPriority = source.rotationPriority;
        this.movementPriority = source.movementPriority;
        this.modified = source.modified;
    }

    public float getPreviousYaw() {
        return this.previousYaw;
    }

    public float getPreviousPitch() {
        return this.previousPitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public float getMovementYaw() {
        return this.movementYaw;
    }

    public int getMovementPriority() {
        return this.movementPriority;
    }

    public int getRotationPriority() {
        return this.rotationPriority;
    }

    public boolean isModified() {
        return this.modified;
    }

    public void setRotation(float yaw, float pitch, int priority) {
        if (this.rotationPriority > priority) {
            return;
        }
        this.yaw = yaw;
        this.pitch = MathHelper.func_76131_a((float)pitch, (float)-90.0f, (float)90.0f);
        this.rotationPriority = priority;
        this.modified = true;
    }

    public void setMovementYaw(float movementYaw, int priority) {
        if (this.movementPriority >= priority) {
            return;
        }
        this.movementYaw = movementYaw;
        this.movementPriority = priority;
        this.modified = true;
    }
}
