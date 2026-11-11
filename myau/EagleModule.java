package myau;

import java.util.Objects;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.IntegerSetting;
import myau.ItemUtils;
import myau.MathUtils;
import myau.Module;
import myau.MovementInputEvent;
import myau.MovementUtils;
import myau.PercentSetting;
import myau.PlayerUtils;
import myau.PreTickEvent;
import myau.RightClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.RandomUtils;

public class EagleModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int delayTicksRemaining = 0;
    public final IntegerSetting minDelay = new IntegerSetting("min-delay", 2, 0, 10);
    public final IntegerSetting maxDelay = new IntegerSetting("max-delay", 3, 0, 10);
    public final PercentSetting offset = new PercentSetting("offset", 100);
    public final BooleanSetting align = new BooleanSetting("align", true);
    public final BooleanSetting directionCheck = new BooleanSetting("direction-check", true);
    public final BooleanSetting pitchCheck = new BooleanSetting("pitch-check", true);
    public final BooleanSetting blocksOnly = new BooleanSetting("blocks-only", true);
    public final BooleanSetting autoSwap = new BooleanSetting("auto-swap", false);

    public EagleModule() {
        super("Eagle", false, 0);
    }

    private boolean isNearEdge() {
        double[] dArray = MovementUtils.inputAcceleration();
        return PlayerUtils.isNarrowOffsetCollisionFree(EagleModule.minecraft.field_71439_g.field_70159_w + dArray[0], -1.0, EagleModule.minecraft.field_71439_g.field_70179_y + dArray[1], (float)((Integer)this.offset.getValue()).intValue() / 100.0f);
    }

    private boolean canActivate() {
        if (!this.passesMovementChecks()) {
            return false;
        }
        return (Boolean)this.blocksOnly.getValue() == false || ItemUtils.isHoldingPlaceableBlock();
    }

    private boolean passesMovementChecks() {
        if (((Boolean)this.directionCheck.getValue()).booleanValue() && EagleModule.minecraft.field_71474_y.field_74351_w.func_151470_d()) {
            return false;
        }
        if (((Boolean)this.pitchCheck.getValue()).booleanValue() && EagleModule.minecraft.field_71439_g.field_70125_A < 69.0f) {
            return false;
        }
        return EagleModule.minecraft.field_71439_g.field_70122_E;
    }

    private void swapToBlock() {
        if (EagleModule.minecraft.field_71462_r != null || ItemUtils.isHoldingPlaceableBlock()) {
            return;
        }
        if (((Boolean)this.directionCheck.getValue()).booleanValue() && EagleModule.minecraft.field_71474_y.field_74351_w.func_151470_d()) {
            return;
        }
        if (((Boolean)this.pitchCheck.getValue()).booleanValue() && EagleModule.minecraft.field_71439_g.field_70125_A < 69.0f) {
            return;
        }
        for (int i = 0; i < 9; ++i) {
            if (!ItemUtils.isPlaceableBlockStack(EagleModule.minecraft.field_71439_g.field_71071_by.func_70301_a(i))) continue;
            EagleModule.minecraft.field_71439_g.field_71071_by.field_70461_c = i;
            return;
        }
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        if (!this.isEnabled()) {
            return;
        }
        if (((Boolean)this.autoSwap.getValue()).booleanValue()) {
            this.swapToBlock();
        }
        if (this.delayTicksRemaining > 0) {
            --this.delayTicksRemaining;
        }
        if (this.delayTicksRemaining != 0) {
            return;
        }
        if (this.isNearEdge()) {
            this.delayTicksRemaining = RandomUtils.nextInt((int)((Integer)this.minDelay.getValue()), (int)((Integer)this.maxDelay.getValue() + 1));
        }
    }

    @EventHandler
    public void onRightClick(RightClickEvent rightClickEvent) {
        double d;
        if (!this.isEnabled() || EagleModule.minecraft.field_71462_r != null || !((Boolean)this.align.getValue()).booleanValue() || EagleModule.minecraft.field_71474_y.field_74311_E.func_151470_d() || !this.canActivate()) {
            return;
        }
        if (this.delayTicksRemaining <= 0 && !this.isNearEdge()) {
            return;
        }
        float f = MathUtils.toRadians(MovementUtils.movementYawForInput(EagleModule.minecraft.field_71439_g.field_70177_z, MovementUtils.forwardInput(), MovementUtils.strafeInput()) - 180.0f);
        float f2 = -MathHelper.func_76126_a((float)f);
        float f3 = MathHelper.func_76134_b((float)f);
        if (Math.abs(f2) < 0.1f || Math.abs(f3) < 0.1f) {
            return;
        }
        AxisAlignedBB axisAlignedBB = EagleModule.minecraft.field_71439_g.func_174813_aQ();
        double d2 = f2 >= 0.0f ? axisAlignedBB.field_72336_d : axisAlignedBB.field_72340_a;
        double d3 = f3 >= 0.0f ? axisAlignedBB.field_72334_f : axisAlignedBB.field_72339_c;
        double d4 = Math.abs(d2 - Math.rint(d2));
        double d5 = Math.abs(d3 - Math.rint(d3));
        if (Math.abs(d4 - d) > 0.125) {
            rightClickEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=-100)
    public void onMovementInput(MovementInputEvent movementInputEvent) {
        if (!this.isEnabled() || EagleModule.minecraft.field_71462_r != null || EagleModule.minecraft.field_71439_g.field_71158_b.field_78899_d || !this.canActivate()) {
            return;
        }
        if (this.delayTicksRemaining <= 0 && !this.isNearEdge()) {
            return;
        }
        EagleModule.minecraft.field_71439_g.field_71158_b.field_78899_d = true;
        EagleModule.minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.3f;
        EagleModule.minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.3f;
    }

    @Override
    public void onDisable() {
        this.delayTicksRemaining = 0;
    }

    @Override
    public void onSettingChanged(String string) {
        if ("min-delay".equals(string)) {
            if ((Integer)this.minDelay.getValue() > (Integer)this.maxDelay.getValue()) {
                this.maxDelay.setValue(this.minDelay.getValue());
            }
        } else if ("max-delay".equals(string) && (Integer)this.minDelay.getValue() > (Integer)this.maxDelay.getValue()) {
            this.minDelay.setValue(this.maxDelay.getValue());
        }
    }

    @Override
    public String[] getHudInfo() {
        if (Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())) {
            return new String[]{((Integer)this.minDelay.getValue()).toString()};
        }
        return new String[]{String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue())};
    }
}
