package myau;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class AimAssistModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final long ATTACK_COOLDOWN_MILLIS = 350L;
    private final ElapsedTimer lastAttackTimer = new ElapsedTimer();
    private final float[] snappedRotations = new float[] { -180.0f, 0.0f };
    private final float[] smoothedRotations = new float[] { -180.0f, 0.0f };
    public final FloatSetting horizontalSpeed = new FloatSetting("horizontal-speed", 3.0f, 0.0f, 30.0f);
    public final FloatSetting verticalSpeed = new FloatSetting("vertical-speed", 0.0f, 0.0f, 30.0f);
    public final PercentSetting smoothing = new PercentSetting("smoothing", 50);
    public final FloatSetting range = new FloatSetting("range", 4.5f, 3.0f, 8.0f);
    public final IntegerSetting fov = new IntegerSetting("fov", 90, 30, 360);
    public final BooleanSetting requirePress = new BooleanSetting("require-press", true);
    public final BooleanSetting allowMining = new BooleanSetting("allow-mining", true);
    public final BooleanSetting weaponsOnly = new BooleanSetting("weapons-only", true);
    public final BooleanSetting allowTools = new BooleanSetting("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanSetting botCheck = new BooleanSetting("bot-check", true);
    public final BooleanSetting teams = new BooleanSetting("teams", true);

    public AimAssistModule() {
        super("AimAssist", false, 0);
    }

    private boolean isValidAimTarget(EntityLivingBase candidate) {
        if (candidate == minecraft.field_71439_g || candidate == minecraft.field_71439_g.field_70154_o) {
            return false;
        }
        if (candidate == minecraft.func_175606_aa() || candidate == minecraft.func_175606_aa().field_70154_o) {
            return false;
        }
        if (candidate.field_70725_aQ > 0) {
            return false;
        }
        if (RotationUtils.distanceToEntity((Entity) candidate) > (double) this.range.getValue()) {
            return false;
        }
        if (RotationUtils.entityAimError((Entity) candidate) > (float) this.fov.getValue()) {
            return false;
        }
        if (RotationUtils.traceToEntity((Entity) candidate) != null) {
            return false;
        }
        if (!(candidate instanceof EntityPlayer)) {
            return true;
        }
        EntityPlayer player = (EntityPlayer) candidate;
        if (EntityUtils.isFriend(player)) {
            return false;
        }
        if (EntityUtils.isEnemy(player)) {
            return true;
        }
        if (this.teams.getValue() && EntityUtils.isTeammate(player)) {
            return false;
        }
        return !this.botCheck.getValue() || !EntityUtils.isBot(player);
    }

    private boolean isWithinEffectiveReach(EntityPlayer player) {
        ReachModule reach = (ReachModule) MyauClient.moduleManager.modules.get(ReachModule.class);
        double effectiveReach = reach.isEnabled() ? (double) ((Float) reach.range.getValue()).floatValue() : 3.0;
        return RotationUtils.distanceToEntity((Entity) player) <= effectiveReach;
    }

    private EntityLivingBase findTarget() {
        EntityLivingBase auraTarget;
        KillAuraModule killAura = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (killAura.isEnabled() && (auraTarget = killAura.getTargetEntity()) != null && this.isValidAimTarget(auraTarget)) {
            return auraTarget;
        }
        List<EntityPlayer> candidates = minecraft.field_71441_e.field_72996_f.stream().filter(EntityPlayer.class::isInstance).map(EntityPlayer.class::cast).filter(this::isValidAimTarget).sorted(Comparator.comparingDouble(RotationUtils::distanceToEntity)).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.stream().anyMatch(this::isWithinEffectiveReach)) {
            candidates.removeIf(player -> !this.isWithinEffectiveReach((EntityPlayer) player));
        }
        return (EntityLivingBase) candidates.get(0);
    }

    private boolean isLookingAtBlock() {
        MovingObjectPosition hitResult = minecraft.field_71476_x;
        return hitResult != null && hitResult.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || minecraft.field_71462_r != null) {
            return;
        }
        if (!(!this.weaponsOnly.getValue() || ItemUtils.isHoldingWeapon() || this.allowTools.getValue() && ItemUtils.isHoldingTool())) {
            return;
        }
        boolean attackPressed = PlayerUtils.isAttackKeyDown();
        if (this.allowMining.getValue() && attackPressed && this.isLookingAtBlock()) {
            return;
        }
        if (this.requirePress.getValue() && !attackPressed && this.lastAttackTimer.hasElapsed(350L)) {
            return;
        }
        EntityLivingBase target = this.findTarget();
        if (target == null || RotationUtils.distanceToEntity((Entity) target) <= 0.0) {
            return;
        }
        if (this.snappedRotations[0] != minecraft.field_71439_g.field_70177_z) {
            this.snappedRotations[0] = minecraft.field_71439_g.field_70177_z;
            this.smoothedRotations[0] = minecraft.field_71439_g.field_70177_z;
        }
        if (this.snappedRotations[1] != minecraft.field_71439_g.field_70125_A) {
            this.snappedRotations[1] = minecraft.field_71439_g.field_70125_A;
            this.smoothedRotations[1] = minecraft.field_71439_g.field_70125_A;
        }
        AxisAlignedBB bounds = target.func_174813_aQ();
        double border = target.func_70111_Y();
        float[] targetRotation = RotationUtils.aimAtBounds(bounds.func_72314_b(border, border, border));
        float yawDelta = MathHelper.func_76142_g((float) (targetRotation[0] - this.smoothedRotations[0]));
        float pitchDelta = targetRotation[1] - this.smoothedRotations[1];
        if (Math.abs(yawDelta) < 0.5f) {
            yawDelta = 0.0f;
        }
        if (Math.abs(pitchDelta) < 0.5f) {
            pitchDelta = 0.0f;
        }
        yawDelta = RotationUtils.clampAngleChange(yawDelta, this.horizontalSpeed.getValue() * 6.0f * RandomUtils.randomFloat(0.75f, 1.0f));
        pitchDelta = RotationUtils.clampAngleChange(pitchDelta, this.verticalSpeed.getValue() * 6.0f * RandomUtils.randomFloat(0.75f, 1.0f) * 0.5f);
        float smoothingAmount = (float) this.smoothing.getValue() / 100.0f;
        yawDelta = RotationUtils.scaleAngleChange(yawDelta, smoothingAmount + RandomUtils.randomFloat(-0.05f, 0.05f));
        pitchDelta = RotationUtils.scaleAngleChange(pitchDelta, smoothingAmount + RandomUtils.randomFloat(-0.05f, 0.05f));
        if (yawDelta == 0.0f && pitchDelta != 0.0f) {
            yawDelta += RandomUtils.randomFloat(0.1f, 0.2f) * (RandomUtils.randomBoolean() ? 1.0f : -1.0f);
        } else if (yawDelta != 0.0f && pitchDelta == 0.0f) {
            pitchDelta += RandomUtils.randomFloat(0.1f, 0.2f) * (RandomUtils.randomBoolean() ? 1.0f : -1.0f);
        }
        this.smoothedRotations[0] = this.smoothedRotations[0] + yawDelta;
        this.smoothedRotations[1] = this.smoothedRotations[1] + pitchDelta;
        this.smoothedRotations[1] = MathHelper.func_76131_a((float) this.smoothedRotations[1], (float) -90.0f, (float) 90.0f);
        this.snappedRotations[0] = RotationUtils.snapToMouseSensitivity(this.smoothedRotations[0], this.snappedRotations[0]);
        this.snappedRotations[1] = RotationUtils.snapToMouseSensitivity(this.smoothedRotations[1], this.snappedRotations[1]);
        this.snappedRotations[1] = MathHelper.func_76131_a((float) this.snappedRotations[1], (float) -90.0f, (float) 90.0f);
        MyauClient.rotationSmoother.requestRotation(this.snappedRotations[0], this.snappedRotations[1], 0, false);
    }

    @EventHandler
    public void onKeyPress(KeyPressEvent event) {
        if (event.getKeyCode() != minecraft.field_71474_y.field_74312_F.func_151463_i()) {
            return;
        }
        AutoClickerModule autoClicker = (AutoClickerModule) MyauClient.moduleManager.modules.get(AutoClickerModule.class);
        if (!autoClicker.isEnabled()) {
            this.lastAttackTimer.reset();
        }
    }
}
