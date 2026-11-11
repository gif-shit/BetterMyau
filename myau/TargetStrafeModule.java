package myau;

import java.awt.Color;
import java.util.ArrayList;
import myau.BooleanSetting;
import myau.ColorUtils;
import myau.EntityUtils;
import myau.EventHandler;
import myau.FloatSetting;
import myau.FlyModule;
import myau.HUDModule;
import myau.IntegerSetting;
import myau.KillAuraModule;
import myau.LongJumpModule;
import myau.ModeSetting;
import myau.Module;
import myau.MoveFlyingEvent;
import myau.MovementUtils;
import myau.MyauClient;
import myau.PlayerUtils;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.RotationUpdateEvent;
import myau.RotationUtils;
import myau.SpeedModule;
import myau.StrafeOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

public class TargetStrafeModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private EntityLivingBase strafeTarget;
    private float strafeYaw = Float.NaN;
    private int strafeDirection = 1;
    public final FloatSetting radius = new FloatSetting("radius", Float.valueOf(1.0f), Float.valueOf(0.0f), Float.valueOf(6.0f));
    public final IntegerSetting points = new IntegerSetting("points", 6, 3, 24);
    public final BooleanSetting requirePress = new BooleanSetting("require-press", true);
    public final BooleanSetting speedOnly = new BooleanSetting("speed-only", true);
    public final ModeSetting showTarget = new ModeSetting("show-target", 1, new String[]{"NONE", "DEFAULT", "HUD"});

    public TargetStrafeModule() {
        super("TargetStrafe", false, 0);
    }

    private boolean canStrafe() {
        if (((Boolean)this.speedOnly.getValue()).booleanValue()) {
            boolean movementModuleActive;
            SpeedModule speed = (SpeedModule)MyauClient.moduleManager.modules.get(SpeedModule.class);
            FlyModule fly = (FlyModule)MyauClient.moduleManager.modules.get(FlyModule.class);
            LongJumpModule longJump = (LongJumpModule)MyauClient.moduleManager.modules.get(LongJumpModule.class);
            boolean bl = movementModuleActive = speed.isEnabled() || fly.isEnabled() || longJump.isEnabled() && longJump.hasJumpStarted();
            if (!movementModuleActive) {
                return false;
            }
        }
        return (Boolean)this.requirePress.getValue() == false || PlayerUtils.isJumpKeyDown();
    }

    private EntityLivingBase findTarget() {
        KillAuraModule killAura = (KillAuraModule)MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (!killAura.isEnabled() || !killAura.isCombatInputAllowed()) {
            return null;
        }
        EntityLivingBase target = killAura.getTargetEntity();
        return EntityUtils.isLoaded((Entity)target) ? target : null;
    }

    private Color targetColor(EntityLivingBase target) {
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)target;
            if (EntityUtils.isFriend(player)) {
                return MyauClient.friendList.color();
            }
            if (EntityUtils.isEnemy(player)) {
                return MyauClient.enemyList.color();
            }
        }
        switch ((Integer)this.showTarget.getValue()) {
            case 1: {
                return target instanceof EntityPlayer ? EntityUtils.teamColor((EntityPlayer)target, 1.0f) : Color.WHITE;
            }
            case 2: {
                HUDModule hud = (HUDModule)MyauClient.moduleManager.modules.get(HUDModule.class);
                return new Color(hud.getColor(System.currentTimeMillis()).getRGB());
            }
        }
        return Color.WHITE;
    }

    private boolean isPointOverVoid(double x, double z) {
        return PlayerUtils.isBoundsOverVoid(new AxisAlignedBB(x - 0.015, TargetStrafeModule.minecraft.field_71439_g.field_70163_u, z - 0.015, x + 0.015, TargetStrafeModule.minecraft.field_71439_g.field_70163_u + (double)TargetStrafeModule.minecraft.field_71439_g.field_70131_O, z + 0.015));
    }

    private static int wrapPointIndex(int index, int size) {
        if (index < 0) {
            return size - 1;
        }
        return index >= size ? 0 : index;
    }

    public float getStrafeYaw() {
        return this.strafeYaw;
    }

    @EventHandler(priority=100)
    public void onRotationUpdate(RotationUpdateEvent event) {
        double targetZ;
        int nextIndex;
        StrafeOffset nextPoint;
        double targetX;
        boolean rightPressed;
        if (!this.isEnabled()) {
            return;
        }
        boolean leftPressed = PlayerUtils.isLeftKeyDown();
        if (leftPressed ^ (rightPressed = PlayerUtils.isRightKeyDown())) {
            int n = this.strafeDirection = leftPressed ? 1 : -1;
        }
        if (!this.canStrafe()) {
            this.strafeTarget = null;
            this.strafeYaw = Float.NaN;
            return;
        }
        this.strafeTarget = this.findTarget();
        if (this.strafeTarget == null) {
            this.strafeYaw = Float.NaN;
            return;
        }
        ArrayList<StrafeOffset> orbitPoints = new ArrayList<StrafeOffset>();
        for (int index = 0; index < (Integer)this.points.getValue(); ++index) {
            double angle = (double)index * (Math.PI * 2 / (double)((Integer)this.points.getValue()).intValue());
            orbitPoints.add(new StrafeOffset((double)((Float)this.radius.getValue()).floatValue() * Math.cos(angle), (double)((Float)this.radius.getValue()).floatValue() * Math.sin(angle)));
        }
        if (orbitPoints.isEmpty()) {
            this.strafeTarget = null;
            this.strafeYaw = Float.NaN;
            return;
        }
        double nearestDistance = 0.0;
        int nearestIndex = -1;
        for (int index = 0; index < orbitPoints.size(); ++index) {
            StrafeOffset point = (StrafeOffset)orbitPoints.get(index);
            double distance = TargetStrafeModule.minecraft.field_71439_g.func_70011_f(this.strafeTarget.field_70165_t + point.x(), TargetStrafeModule.minecraft.field_71439_g.field_70163_u, this.strafeTarget.field_70161_v + point.z());
            if (nearestIndex != -1 && !(distance < nearestDistance)) continue;
            nearestDistance = distance;
            nearestIndex = index;
        }
        if (TargetStrafeModule.minecraft.field_71439_g.field_70123_F) {
            this.strafeDirection *= -1;
        }
        if (this.isPointOverVoid(targetX = this.strafeTarget.field_70165_t + (nextPoint = (StrafeOffset)orbitPoints.get(nextIndex = TargetStrafeModule.wrapPointIndex(nearestIndex + this.strafeDirection, orbitPoints.size()))).x(), targetZ = this.strafeTarget.field_70161_v + nextPoint.z())) {
            this.strafeDirection *= -1;
            nextIndex = TargetStrafeModule.wrapPointIndex(nearestIndex + this.strafeDirection, orbitPoints.size());
            nextPoint = (StrafeOffset)orbitPoints.get(nextIndex);
            targetX = this.strafeTarget.field_70165_t + nextPoint.x();
            targetZ = this.strafeTarget.field_70161_v + nextPoint.z();
        }
        this.strafeYaw = RotationUtils.rotationsFromDelta(targetX - TargetStrafeModule.minecraft.field_71439_g.field_70165_t, 0.0, targetZ - TargetStrafeModule.minecraft.field_71439_g.field_70161_v)[0];
        event.setMovementYaw(this.strafeYaw, 10);
    }

    @EventHandler
    public void onMoveFlying(MoveFlyingEvent event) {
        if (this.isEnabled() && !Float.isNaN(this.strafeYaw) && MovementUtils.hasMovementInput()) {
            event.setStrafe(0.0f);
            event.setForward(1.0f);
        }
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled() || !EntityUtils.isLoaded((Entity)this.strafeTarget) || (Integer)this.showTarget.getValue() == 0) {
            return;
        }
        Color color = this.targetColor(this.strafeTarget);
        RenderUtils.beginOverlay();
        RenderUtils.drawEntityCircle((Entity)this.strafeTarget, ((Float)this.radius.getValue()).floatValue(), (Integer)this.points.getValue(), 3.0f, ColorUtils.scaleBrightness(color, 0.2f).getRGB());
        RenderUtils.drawEntityCircle((Entity)this.strafeTarget, ((Float)this.radius.getValue()).floatValue(), (Integer)this.points.getValue(), 1.5f, color.getRGB());
        RenderUtils.endOverlay();
    }

    @Override
    public void onDisable() {
        this.strafeTarget = null;
        this.strafeYaw = Float.NaN;
    }
}
