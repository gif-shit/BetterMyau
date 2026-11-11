package myau;

import myau.EventHandler;
import myau.PreTickEvent;
import myau.RenderWorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class RotationSmoother {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private float lastPartialTicks;
    private float yawDelta;
    private float pitchDelta;
    private int requestPriority;
    private boolean requestActive;

    public RotationSmoother() {
        this.resetRotationRequest();
    }

    private void applyRotationDelta(float partialTicks) {
        float pitchStep;
        if (RotationSmoother.minecraft.field_71439_g == null || Float.isNaN(this.yawDelta) || Float.isNaN(this.pitchDelta) || Float.isNaN(this.lastPartialTicks)) {
            return;
        }
        float partialDelta = partialTicks - this.lastPartialTicks;
        float yawStep = this.yawDelta * partialDelta;
        if (yawStep != 0.0f) {
            RotationSmoother.minecraft.field_71439_g.field_70126_B = RotationSmoother.minecraft.field_71439_g.field_70177_z;
            RotationSmoother.minecraft.field_71439_g.field_70177_z += yawStep;
        }
        if ((pitchStep = this.pitchDelta * partialDelta) != 0.0f) {
            RotationSmoother.minecraft.field_71439_g.field_70127_C = RotationSmoother.minecraft.field_71439_g.field_70125_A;
            RotationSmoother.minecraft.field_71439_g.field_70125_A += pitchStep;
            RotationSmoother.minecraft.field_71439_g.field_70125_A = MathHelper.func_76131_a((float)RotationSmoother.minecraft.field_71439_g.field_70125_A, (float)-90.0f, (float)90.0f);
        }
        this.lastPartialTicks = partialTicks;
    }

    private void resetRotationRequest() {
        this.lastPartialTicks = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.requestPriority = Integer.MIN_VALUE;
        this.requestActive = false;
    }

    public void requestRotation(float yaw, float pitch, int priority, boolean active) {
        if (this.requestPriority > priority) {
            return;
        }
        this.requestPriority = priority;
        this.yawDelta = MathHelper.func_76142_g((float)(yaw - RotationSmoother.minecraft.field_71439_g.field_70177_z));
        this.pitchDelta = pitch - RotationSmoother.minecraft.field_71439_g.field_70125_A;
        this.lastPartialTicks = 0.0f;
        this.requestActive = active;
    }

    public boolean isRequestActive() {
        return this.requestActive;
    }

    @EventHandler(priority=100)
    public void onPreTick(PreTickEvent event) {
        this.applyRotationDelta(1.0f);
        this.resetRotationRequest();
    }

    @EventHandler(priority=100)
    public void onRenderWorld(RenderWorldEvent event) {
        this.applyRotationDelta(event.getPartialTicks());
    }
}
