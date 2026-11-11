package myau;

import myau.BooleanSetting;
import myau.EventHandler;
import myau.InputUtils;
import myau.Module;
import myau.PostTickEvent;
import myau.PreTickEvent;
import myau.mixin.AccessorEntityLivingBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class SprintModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean wasSprinting;
    public final BooleanSetting fovFix = new BooleanSetting("fov-fix", true);

    public SprintModule() {
        super("Sprint", true, 0, true);
    }

    public boolean shouldCompensateFov(IAttributeInstance movementAttribute) {
        boolean fixEnabled = (Boolean)this.fovFix.getValue();
        if (!fixEnabled) {
            return false;
        }
        AttributeModifier boost = ((AccessorEntityLivingBase)SprintModule.minecraft.field_71439_g).getSprintingSpeedBoostModifier();
        return movementAttribute.func_111127_a(boost.func_111167_a()) == null && this.wasSprinting;
    }

    public boolean shouldCompensateFov(boolean sprintModifierPresent) {
        boolean fixEnabled = (Boolean)this.fovFix.getValue();
        return fixEnabled && !sprintModifierPresent && this.wasSprinting;
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        boolean enabled = this.isEnabled();
        if (enabled) {
            InputUtils.setKeyState(SprintModule.minecraft.field_71474_y.field_151444_V.func_151463_i(), true);
        }
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.wasSprinting = SprintModule.minecraft.field_71439_g.func_70051_ag();
    }

    @Override
    public void onDisable() {
        this.wasSprinting = false;
        InputUtils.updateKeyState(SprintModule.minecraft.field_71474_y.field_151444_V.func_151463_i());
    }
}
