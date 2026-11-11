package myau;

import com.google.common.base.CaseFormat;
import static myau.ModuleModeIds.KillAura.AUTO_BLOCK_NONE;
import static myau.ModuleModeIds.KillAura.AUTO_BLOCK_LEGIT;
import static myau.ModuleModeIds.KillAura.AUTO_BLOCK_BLINK;
import static myau.ModuleModeIds.KillAura.AUTO_BLOCK_FULL;
import static myau.ModuleModeIds.KillAura.AUTO_BLOCK_PREDICTIVE;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;
import myau.AuraTarget;
import myau.AutoHealModule;
import myau.BedNukerModule;
import myau.BlockClickEvent;
import myau.BooleanSetting;
import myau.ChatUtils;
import myau.ElapsedTimer;
import myau.EntityUtils;
import myau.EventHandler;
import myau.FloatSetting;
import myau.HUDModule;
import myau.HitSelectModule;
import myau.IntegerSetting;
import myau.ItemUtils;
import myau.KeepSprintModule;
import myau.LeftClickEvent;
import myau.ModeSetting;
import myau.Module;
import myau.MovementInputEvent;
import myau.MovementUtils;
import myau.MyauClient;
import myau.NoSlowModule;
import myau.NumericConversions;
import myau.OutgoingBufferOwner;
import myau.PacketReceiveEvent;
import myau.PacketSendEvent;
import myau.PacketUtils;
import myau.PercentSetting;
import myau.PlayerUtils;
import myau.PostPlayerUpdateEvent;
import myau.PostTickEvent;
import myau.PreTickEvent;
import myau.RandomUtils;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.RightClickEvent;
import myau.RotationState;
import myau.RotationUpdateEvent;
import myau.RotationUtils;
import myau.ScaffoldModule;
import myau.StopUsingItemEvent;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.DataWatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;

public class KillAuraModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat damageFormat = new DecimalFormat("+0.0;-0.0");
    private final ElapsedTimer targetSwitchTimer = new ElapsedTimer();
    private AuraTarget currentTarget = null;
    private boolean hasTargetInAutoBlockRange = false;
    private int targetIndex = 0;
    private boolean attackedThisUpdate = false;
    private boolean serverBlocking = false;
    private boolean autoBlockActive = false;
    private boolean renderBlocking = false;
    private boolean bufferRestartPending = false;
    private long attackCooldownMillis = 0L;
    private long blockHoldMillis = 0L;
    private long blockReleaseDelayMillis = 0L;
    private int autoBlockPhase = 0;
    private boolean lagBlockFlushPending = false;
    private int fullGrace;
    private int predictiveTick;
    private int predictiveNextTick;
    private long predictiveTimer;
    private final java.util.Random blockRandom = new java.util.Random();
    private int lastDamageLogTick = 0;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"SINGLE", "SWITCH"});
    public final ModeSetting sort = new ModeSetting("sort", 0, new String[]{"DISTANCE", "HEALTH", "HURT_TIME", "FOV"});
    public final ModeSetting autoBlock = new ModeSetting("auto-block", AUTO_BLOCK_LEGIT,
            new String[]{"NONE", "LEGIT", "BLINK", "Full", "Predictive"}) {
        @Override
        public boolean readJson(com.google.gson.JsonObject json) {
            if (super.readJson(json)) return true;
            return this.setValue(AUTO_BLOCK_NONE);
        }
    };
    public final BooleanSetting autoBlockRequirePress = new BooleanSetting("auto-block-require-press", true, this::isAutoBlockRequirePressVisible);
    public final BooleanSetting autoBlockNoSlow = new BooleanSetting("auto-block-no-slow", false, this::isAutoBlockNoSlowVisible);
    public final FloatSetting autoBlockHold = new FloatSetting("auto-block-hold", Float.valueOf(1.5f), Float.valueOf(1.0f), Float.valueOf(20.0f), this::isAutoBlockHoldVisible);
    public final FloatSetting autoBlockDelay = new FloatSetting("auto-block-delay", Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(20.0f), this::isAutoBlockDelayVisible);
    public final IntegerSetting autoBlockHurtTime = new IntegerSetting("auto-block-hurt-time", 6, 0, 10, this::isAutoBlockHurtTimeVisible);
    public final FloatSetting autoBlockRange = new FloatSetting("auto-block-range", Float.valueOf(4.0f), Float.valueOf(3.0f), Float.valueOf(8.0f), this::isAutoBlockRangeVisible);
    public final BooleanSetting autoBlockSmart = new BooleanSetting("auto-block-smart", false, this::isAutoBlockSmartVisible);
    public final FloatSetting autoBlockSmartRange = new FloatSetting("auto-block-smart-range", Float.valueOf(4.0f), Float.valueOf(3.0f), Float.valueOf(6.0f), () -> this.isAutoBlockSmartVisible() && (Boolean)this.autoBlockSmart.getValue() != false);
    public final FloatSetting swingRange = new FloatSetting("swing-range", Float.valueOf(4.0f), Float.valueOf(3.0f), Float.valueOf(6.0f));
    public final FloatSetting attackRange = new FloatSetting("attack-range", Float.valueOf(3.0f), Float.valueOf(3.0f), Float.valueOf(6.0f));
    public final IntegerSetting fov = new IntegerSetting("fov", 360, 30, 360);
    public final IntegerSetting minAps = new IntegerSetting("min-aps", 14, 1, 20);
    public final IntegerSetting maxAps = new IntegerSetting("max-aps", 14, 1, 20);
    public final IntegerSetting switchDelay = new IntegerSetting("switch-delay", 150, 0, 1000);
    public final ModeSetting rotations = new ModeSetting("rotations", 2, new String[]{"NONE", "LEGIT", "SILENT", "LOCK_VIEW"});
    public final ModeSetting moveFix = new ModeSetting("move-fix", 1, new String[]{"NONE", "SILENT", "STRICT"});
    public final PercentSetting smoothing = new PercentSetting("smoothing", 0);
    public final IntegerSetting angleStep = new IntegerSetting("angle-step", 90, 30, 180);
    public final BooleanSetting throughWalls = new BooleanSetting("through-walls", true);
    public final BooleanSetting requirePress = new BooleanSetting("require-press", false);
    public final BooleanSetting allowMining = new BooleanSetting("allow-mining", true);
    public final BooleanSetting weaponsOnly = new BooleanSetting("weapons-only", true);
    public final BooleanSetting allowTools = new BooleanSetting("allow-tools", false, this.weaponsOnly::getValue);
    public final BooleanSetting inventoryCheck = new BooleanSetting("inventory-check", true);
    public final BooleanSetting botCheck = new BooleanSetting("bot-check", true);
    public final BooleanSetting players = new BooleanSetting("players", true);
    public final BooleanSetting bosses = new BooleanSetting("bosses", false);
    public final BooleanSetting mobs = new BooleanSetting("mobs", false);
    public final BooleanSetting animals = new BooleanSetting("animals", false);
    public final BooleanSetting golems = new BooleanSetting("golems", false);
    public final BooleanSetting silverfish = new BooleanSetting("silverfish", false);
    public final BooleanSetting teams = new BooleanSetting("teams", true);
    public final ModeSetting showTarget = new ModeSetting("show-target", 0, new String[]{"NONE", "DEFAULT", "HUD"});
    public final ModeSetting debugLog = new ModeSetting("debug-log", 0, new String[]{"NONE", "HEALTH"});

    private long nextAttackDelayMillis() {
        return 1000L / RandomUtils.randomLong(((Integer)this.minAps.getValue()).longValue(), ((Integer)this.maxAps.getValue()).longValue());
    }

    private long blockHoldDurationMillis() {
        return NumericConversions.truncateFloatToLong(((Float)this.autoBlockHold.getValue()).floatValue() * 50.0f);
    }

    private long blockReleaseDurationMillis() {
        return NumericConversions.truncateFloatToLong(((Float)this.autoBlockDelay.getValue()).floatValue() * 50.0f);
    }

    private boolean tryAttackTarget(RotationUpdateEvent event) {
        float f = event.getYaw(), f2 = event.getPitch();
        boolean bl;
        if (MyauClient.packetActionTracker.sentDigging || MyauClient.packetActionTracker.sentBlockPlacement) {
            return false;
        }
        if (this.isBlockingSword() && this.autoBlock.getValue() != AUTO_BLOCK_FULL) {
            return false;
        }
        if (this.attackCooldownMillis > 0L) {
            return false;
        }
        KeepSprintModule keepSprintModule = (KeepSprintModule)MyauClient.moduleManager.modules.get(KeepSprintModule.class);
        if (keepSprintModule.shouldHoldAttack(this.currentTarget.entity)) {
            return false;
        }
        if (((NoSlowModule)MyauClient.moduleManager.modules.get(NoSlowModule.class)).shouldHoldAttack()) {
            return false;
        }
        HitSelectModule hitSelectModule = (HitSelectModule)MyauClient.moduleManager.modules.get(HitSelectModule.class);
        if (hitSelectModule.isEnabled() && hitSelectModule.shouldDelayAttack(this.currentTarget.entity)) {
            return false;
        }
        if ((Integer)this.rotations.getValue() == 0) {
            bl = this.isWithinAttackRange(this.currentTarget.distance);
        } else {
            Vec3 vec3 = KillAuraModule.minecraft.field_71439_g.func_174824_e(1.0f);
            if (((Boolean)this.throughWalls.getValue()).booleanValue()) {
                bl = RotationUtils.intersectBoundsFrom(this.currentTarget.bounds, vec3, f, f2, ((Float)this.attackRange.getValue()).doubleValue()) != null;
            } else {
                bl = !RotationUtils.isBoundsRayObstructed(this.currentTarget.bounds, vec3, f, f2, ((Float)this.attackRange.getValue()).doubleValue());
                boolean bl2 = bl;
            }
        }
        DisplaceModule displace = DisplaceModule.current();
        if (bl && displace != null && displace.interceptAura(this.currentTarget.entity, event)) return false;
        this.attackCooldownMillis += this.nextAttackDelayMillis();
        minecraft.field_71439_g.func_71038_i();
        if (!bl) return false;
        this.sendAttack(this.currentTarget.entity);
        return true;
    }

    boolean replayDisplacedAttack(EntityLivingBase target) {
        if (!this.isEnabled() || this.currentTarget == null || this.currentTarget.entity != target
                || !this.canRunCombat() || !this.isWithinAttackRange(RotationUtils.distanceToEntity(target))
                || (this.isBlockingSword() && this.autoBlock.getValue() != AUTO_BLOCK_FULL)) return false;
        this.attackCooldownMillis += this.nextAttackDelayMillis();
        minecraft.field_71439_g.func_71038_i();
        this.sendAttack(target);
        return true;
    }

    private void sendAttack(EntityLivingBase target) {
        ((AccessorPlayerControllerMP)minecraft.field_71442_b).callSyncCurrentPlayItem();
        PacketUtils.send(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
        if (minecraft.field_71442_b.func_178889_l() != WorldSettings.GameType.SPECTATOR) {
            PlayerUtils.attackLocally(target);
        }
        this.attackedThisUpdate = true;
    }

    private void startBlockingHeldItem() {
        ((AccessorPlayerControllerMP)KillAuraModule.minecraft.field_71442_b).callSyncCurrentPlayItem();
        this.startBlockingWithStack(KillAuraModule.minecraft.field_71439_g.func_70694_bm());
    }

    private void startBlockingWithStack(ItemStack itemStack) {
        PacketUtils.send(new C08PacketPlayerBlockPlacement(itemStack));
        KillAuraModule.minecraft.field_71439_g.func_71008_a(itemStack, itemStack.func_77988_m());
        this.serverBlocking = true;
    }

    private void releaseBlocking() {
        PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN));
        KillAuraModule.minecraft.field_71439_g.func_71034_by();
        this.serverBlocking = false;
    }

    private void interactWithTargetAndBlock(float f, float f2) {
        if (this.currentTarget == null) {
            return;
        }
        MovingObjectPosition movingObjectPosition = RotationUtils.intersectBounds(this.currentTarget.bounds, f, f2, 8.0);
        if (movingObjectPosition == null) {
            return;
        }
        ((AccessorPlayerControllerMP)KillAuraModule.minecraft.field_71442_b).callSyncCurrentPlayItem();
        PacketUtils.send(new C02PacketUseEntity((Entity)this.currentTarget.entity, new Vec3(movingObjectPosition.field_72307_f.field_72450_a - this.currentTarget.x, movingObjectPosition.field_72307_f.field_72448_b - this.currentTarget.y, movingObjectPosition.field_72307_f.field_72449_c - this.currentTarget.z)));
        PacketUtils.send(new C02PacketUseEntity((Entity)this.currentTarget.entity, C02PacketUseEntity.Action.INTERACT));
        this.startBlockingWithStack(KillAuraModule.minecraft.field_71439_g.func_70694_bm());
    }

    private boolean canRunCombat() {
        if (this.currentTarget == null) {
            return false;
        }
        if (((Boolean)this.inventoryCheck.getValue()).booleanValue() && KillAuraModule.minecraft.field_71462_r instanceof GuiContainer) {
            return false;
        }
        if (!(!((Boolean)this.weaponsOnly.getValue()).booleanValue() || ItemUtils.isHoldingWeapon() || ((Boolean)this.allowTools.getValue()).booleanValue() && ItemUtils.isHoldingTool())) {
            return false;
        }
        if ((ItemUtils.isHoldingConsumable() || ItemUtils.isHoldingBow()) && PlayerUtils.isUseItemKeyDown()) {
            return false;
        }
        AutoHealModule autoHealModule = (AutoHealModule)MyauClient.moduleManager.modules.get(AutoHealModule.class);
        if (autoHealModule.isEnabled() && autoHealModule.hasPendingSlotRestore()) {
            return false;
        }
        BedNukerModule bedNukerModule = (BedNukerModule)MyauClient.moduleManager.modules.get(BedNukerModule.class);
        if (bedNukerModule.isEnabled() && bedNukerModule.isControllingInteractions()) {
            return false;
        }
        ScaffoldModule scaffoldModule = (ScaffoldModule)MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        if (scaffoldModule.isEnabled()) {
            return false;
        }
        boolean bl = PlayerUtils.isAttackKeyDown();
        if (((Boolean)this.allowMining.getValue()).booleanValue() && bl && KillAuraModule.minecraft.field_71476_x != null && KillAuraModule.minecraft.field_71476_x.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        return (Boolean)this.requirePress.getValue() == false || bl;
    }

    boolean wantsNoSlowBlock() {
        return this.isEnabled() && this.currentTarget != null && this.autoBlock.getValue() != AUTO_BLOCK_NONE
                && this.canRunCombat() && this.canAutoBlock();
    }

    private boolean canAutoBlock() {
        if (!ItemUtils.isHoldingSword()) {
            return false;
        }
        if (this.autoBlock.getValue() == AUTO_BLOCK_NONE) {
            return true;
        }
        if (((Boolean)this.autoBlockSmart.getValue()).booleanValue() && !this.isBlockUseful()) {
            return false;
        }
        if (!((Boolean)this.autoBlockRequirePress.getValue()).booleanValue() && this.hasTargetInAutoBlockRange) {
            return true;
        }
        return PlayerUtils.isUseItemKeyDown();
    }

    private boolean isValidTargetEntity(EntityLivingBase entityLivingBase) {
        if (entityLivingBase == KillAuraModule.minecraft.field_71439_g || entityLivingBase == KillAuraModule.minecraft.field_71439_g.field_70154_o || entityLivingBase == minecraft.func_175606_aa() || entityLivingBase == KillAuraModule.minecraft.func_175606_aa().field_70154_o) {
            return false;
        }
        return this.passesEntityCategoryFilters(entityLivingBase);
    }

    private boolean passesEntityCategoryFilters(EntityLivingBase entityLivingBase) {
        if (entityLivingBase.field_70725_aQ > 0) {
            return false;
        }
        if (entityLivingBase instanceof EntityOtherPlayerMP) {
            if (!((Boolean)this.players.getValue()).booleanValue()) {
                return false;
            }
            EntityPlayer entityPlayer = (EntityPlayer)entityLivingBase;
            if (EntityUtils.isFriend(entityPlayer)) {
                return false;
            }
            if (EntityUtils.isEnemy(entityPlayer)) {
                return true;
            }
            if (((Boolean)this.teams.getValue()).booleanValue() && EntityUtils.isTeammate(entityPlayer)) {
                return false;
            }
            return (Boolean)this.botCheck.getValue() == false || !EntityUtils.isBot(entityPlayer);
        }
        if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
            return (Boolean)this.bosses.getValue();
        }
        if (entityLivingBase instanceof EntityMob || entityLivingBase instanceof EntitySlime) {
            if (entityLivingBase instanceof EntitySilverfish) {
                if (!((Boolean)this.silverfish.getValue()).booleanValue()) {
                    return false;
                }
                return (Boolean)this.teams.getValue() == false || !EntityUtils.hasTeamArmorStand(entityLivingBase);
            }
            return (Boolean)this.mobs.getValue();
        }
        if (entityLivingBase instanceof EntityAnimal || entityLivingBase instanceof EntityBat || entityLivingBase instanceof EntitySquid || entityLivingBase instanceof EntityVillager) {
            return (Boolean)this.animals.getValue();
        }
        if (entityLivingBase instanceof EntityIronGolem) {
            if (!((Boolean)this.golems.getValue()).booleanValue()) {
                return false;
            }
            return (Boolean)this.teams.getValue() == false || !EntityUtils.hasTeamArmorStand(entityLivingBase);
        }
        return false;
    }

    private boolean isWithinAnyCombatRange(double d) {
        return this.isWithinAutoBlockRange(d) || this.isWithinSwingRange(d) || this.isWithinAttackRange(d);
    }

    private boolean isWithinAutoBlockRange(double d) {
        return (Integer)this.autoBlock.getValue() != 0 && d <= ((Float)this.autoBlockRange.getValue()).doubleValue();
    }

    private boolean isWithinSwingRange(double d) {
        return d <= ((Float)this.swingRange.getValue()).doubleValue();
    }

    private boolean isWithinAttackRange(double d) {
        return d <= ((Float)this.attackRange.getValue()).doubleValue();
    }

    private boolean isBlockUseful() {
        if (KillAuraModule.minecraft.field_71439_g.field_70737_aN > 2) {
            return false;
        }
        double d = ((Float)this.autoBlockSmartRange.getValue()).doubleValue();
        double d2 = Math.cos(Math.toRadians(70.0));
        Vec3 vec3 = KillAuraModule.minecraft.field_71439_g.func_174824_e(1.0f);
        for (Object e : KillAuraModule.minecraft.field_71441_e.field_73010_i) {
            EntityOtherPlayerMP entityOtherPlayerMP;
            if (!(e instanceof EntityOtherPlayerMP) || !(entityOtherPlayerMP = (EntityOtherPlayerMP)e).func_70089_S() || !this.passesEntityCategoryFilters((EntityLivingBase)entityOtherPlayerMP) || (double)entityOtherPlayerMP.func_70032_d((Entity)KillAuraModule.minecraft.field_71439_g) > d) continue;
            Vec3 vec32 = entityOtherPlayerMP.func_174824_e(1.0f);
            Vec3 vec33 = vec3.func_178788_d(vec32).func_72432_b();
            if (!(entityOtherPlayerMP.func_70676_i(1.0f).func_72432_b().func_72430_b(vec33) >= d2)) continue;
            return true;
        }
        return false;
    }

    private boolean isAutoBlockSmartVisible() {
        int n = (Integer)this.autoBlock.getValue();
        return n != AUTO_BLOCK_NONE;
    }

    private boolean isEnemyPlayer(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && EntityUtils.isEnemy((EntityPlayer)entityLivingBase);
    }

    public KillAuraModule() {
        super("KillAura", false, 0);
    }

    public EntityLivingBase getTargetEntity() {
        return this.currentTarget == null ? null : this.currentTarget.entity;
    }

    public boolean isCombatInputAllowed() {
        if (((Boolean)this.inventoryCheck.getValue()).booleanValue() && KillAuraModule.minecraft.field_71462_r instanceof GuiContainer) {
            return false;
        }
        if (((AccessorPlayerControllerMP)KillAuraModule.minecraft.field_71442_b).getIsHittingBlock()) {
            return false;
        }
        ScaffoldModule scaffoldModule = (ScaffoldModule)MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        if (scaffoldModule.isEnabled()) {
            return false;
        }
        if (!(!((Boolean)this.weaponsOnly.getValue()).booleanValue() || ItemUtils.isHoldingWeapon() || ((Boolean)this.allowTools.getValue()).booleanValue() && ItemUtils.isHoldingTool())) {
            return false;
        }
        return (Boolean)this.requirePress.getValue() == false || PlayerUtils.isAttackKeyDown();
    }

    public boolean shouldRenderBlocking() {
        return this.renderBlocking && ItemUtils.isHoldingSword();
    }

    public boolean isBlockingSword() {
        return (KillAuraModule.minecraft.field_71439_g.func_71039_bw() || this.serverBlocking) && ItemUtils.isHoldingSword();
    }

    public boolean isAttackDueNextTick() {
        return this.isEnabled() && this.currentTarget != null && this.attackCooldownMillis <= 50L && this.currentTarget.hasUsableAim && this.isWithinSwingRange(this.currentTarget.distance) && this.canRunCombat();
    }

    public EntityLivingBase getCurrentTargetEntity() {
        return this.currentTarget == null ? null : this.currentTarget.entity;
    }

    public boolean isServerBlocking() {
        return this.serverBlocking && ItemUtils.isHoldingSword();
    }

    public boolean isAutoBlockActive() {
        return this.autoBlockActive;
    }

    private float[] calculateCombatRotation(RotationUpdateEvent rotationUpdateEvent) {
        float f = rotationUpdateEvent.getPreviousYaw();
        float f2 = rotationUpdateEvent.getPreviousPitch();
        float f3 = MathHelper.func_76142_g((float)(this.currentTarget.yaw - f));
        float f4 = this.currentTarget.pitch - f2;
        if (Math.abs(f3) < 0.5f) {
            f3 = 0.0f;
        }
        if (Math.abs(f4) < 0.5f) {
            f4 = 0.0f;
        }
        f3 = RotationUtils.clampAngleChange(f3, (float)((Integer)this.angleStep.getValue()).intValue() * RandomUtils.randomFloat(0.75f, 1.0f));
        f4 = RotationUtils.clampAngleChange(f4, (float)((Integer)this.angleStep.getValue()).intValue() * RandomUtils.randomFloat(0.75f, 1.0f) * 0.5f);
        f3 = RotationUtils.scaleAngleChange(f3, (float)((Integer)this.smoothing.getValue()).intValue() / 100.0f + RandomUtils.randomFloat(-0.05f, 0.05f));
        f4 = RotationUtils.scaleAngleChange(f4, (float)((Integer)this.smoothing.getValue()).intValue() / 100.0f + RandomUtils.randomFloat(-0.05f, 0.05f));
        float f5 = KillAuraModule.minecraft.field_71474_y.field_74341_c * 0.6f + 0.2f;
        float f6 = f5 * f5 * f5 * 8.0f;
        if (f3 == 0.0f && f4 != 0.0f) {
            f3 += f6 * 0.15f * (RandomUtils.randomBoolean() ? 1.0f : -1.0f);
        }
        if (f3 != 0.0f && f4 == 0.0f) {
            f4 += f6 * 0.15f * (RandomUtils.randomBoolean() ? 1.0f : -1.0f);
        }
        float f7 = RotationUtils.snapToMouseSensitivity(f + f3, f);
        float f8 = RotationUtils.snapToMouseSensitivity(f2 + f4, f2);
        return new float[]{f7, MathHelper.func_76131_a((float)f8, (float)-90.0f, (float)90.0f)};
    }

    @EventHandler(priority=-50)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled()) return;
        int blockMode = this.autoBlock.getValue();
        boolean combatEligible = this.canRunCombat();
        boolean canAttack = combatEligible;
        NoSlowModule noSlow = (NoSlowModule) MyauClient.moduleManager.modules.get(NoSlowModule.class);
        boolean noSlowOwnsBlock = blockMode == AUTO_BLOCK_PREDICTIVE && noSlow.isEnabled()
                && noSlow.swordMode.getValue() > 1 && ItemUtils.isHoldingSword();
        boolean canBlock = combatEligible && this.canAutoBlock() && !noSlowOwnsBlock;
        boolean blockSuppressed = blockMode != AUTO_BLOCK_NONE && canBlock
                && (MyauClient.hurtTimeTracker.remainingHurtTicks() > this.autoBlockHurtTime.getValue()
                        || this.blockReleaseDelayMillis > 0L);
        this.attackCooldownMillis = Math.max(0L, this.attackCooldownMillis - 50L);
        this.blockHoldMillis = Math.max(0L, this.blockHoldMillis - 50L);
        this.blockReleaseDelayMillis = Math.max(0L, this.blockReleaseDelayMillis - 50L);

        boolean fullGraceHold = false;
        if (blockMode == AUTO_BLOCK_FULL) {
            if (canBlock && !blockSuppressed) {
                this.fullGrace = 2 + this.blockRandom.nextInt(2);
            } else if (!canBlock && this.fullGrace > 0 && this.autoBlockActive && this.isBlockingSword()) {
                --this.fullGrace;
                fullGraceHold = true;
                canAttack = false;
            }
        }
        if (noSlowOwnsBlock) {
            this.resetBlockCycle();
            this.autoBlockActive = combatEligible && this.canAutoBlock();
            this.renderBlocking = this.autoBlockActive;
        } else if ((!canBlock || blockSuppressed) && !fullGraceHold) {
            boolean manualUse = !combatEligible && !blockSuppressed
                    && ItemUtils.isHoldingSword() && PlayerUtils.isUseItemKeyDown();
            if (!manualUse && this.autoBlockActive && this.isBlockingSword() && !blockActionSent()) {
                this.releaseBlocking();
                canAttack = false;
            }
            this.resetBlockCycle();
            this.autoBlockActive = blockSuppressed;
            this.renderBlocking = blockSuppressed;
        }
        if (!combatEligible) return;

        boolean startBlock = false;
        if (canBlock && !blockSuppressed) {
            switch (blockMode) {
                case AUTO_BLOCK_NONE:
                    startBlock = PlayerUtils.isUseItemKeyDown() && !this.isBlockingSword() && !blockActionSent();
                    break;
                case AUTO_BLOCK_BLINK: {
                    this.autoBlockActive = true;
                    this.renderBlocking = true;
                    if (MyauClient.packetActionTracker.sentDigging || MyauClient.packetActionTracker.sentBlockPlacement) break;
                    switch (this.autoBlockPhase) {
                        case 0: {
                            if (!this.isBlockingSword()) {
                                startBlock = true;
                                this.blockHoldMillis += this.blockHoldDurationMillis();
                            }
                            this.bufferRestartPending = true;
                            this.autoBlockPhase = 1;
                            break;
                        }
                        case 1: {
                            if (this.isBlockingSword()) {
                                this.releaseBlocking();
                                canAttack = false;
                            }
                            if (this.blockHoldMillis > 0L) break;
                            this.blockReleaseDelayMillis += this.blockReleaseDurationMillis();
                            this.autoBlockPhase = 0;
                            break;
                        }
                        default: {
                            this.autoBlockPhase = 0;
                            break;
                        }
                    }
                    break;
                }
                case AUTO_BLOCK_LEGIT: {
                    MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.AUTO_BLOCK);
                    this.autoBlockActive = true;
                    this.renderBlocking = true;
                    if (MyauClient.packetActionTracker.sentDigging || MyauClient.packetActionTracker.sentBlockPlacement) break;
                    switch (this.autoBlockPhase) {
                        case 0: {
                            if (!this.isBlockingSword()) {
                                startBlock = true;
                                this.blockHoldMillis += this.blockHoldDurationMillis();
                            }
                            this.autoBlockPhase = 1;
                            break;
                        }
                        case 1: {
                            if (this.blockHoldMillis > 0L) break;
                            if (this.isBlockingSword()) {
                                this.releaseBlocking();
                                canAttack = false;
                            }
                            this.blockReleaseDelayMillis += this.blockReleaseDurationMillis();
                            this.autoBlockPhase = 0;
                            break;
                        }
                        default: {
                            this.autoBlockPhase = 0;
                            break;
                        }
                    }
                    break;
                }
                case AUTO_BLOCK_FULL:
                    this.autoBlockActive = true;
                    this.renderBlocking = true;
                    startBlock = !this.isBlockingSword() && !blockActionSent();
                    break;
                case AUTO_BLOCK_PREDICTIVE:
                    this.autoBlockActive = true;
                    this.renderBlocking = true;
                    ++this.predictiveTick;
                    long now = System.currentTimeMillis();
                    if (this.autoBlockPhase == 2 && now - this.predictiveTimer >= this.blockReleaseDurationMillis()) {
                        this.autoBlockPhase = 0;
                        this.lagBlockFlushPending = true;
                    }
                    if (this.autoBlockPhase == 0 && this.predictiveTick >= this.predictiveNextTick) {
                        this.autoBlockPhase = 1;
                        this.predictiveNextTick = this.predictiveTick + 10;
                        this.predictiveTimer = now;
                    }
                    if (this.autoBlockPhase != 1 || blockActionSent()) break;
                    if (now - this.predictiveTimer >= this.blockHoldDurationMillis()) {
                        this.autoBlockPhase = 2;
                        this.predictiveTimer = now;
                        if (this.isBlockingSword()) {
                            this.releaseBlocking();
                            canAttack = false;
                        }
                        this.bufferRestartPending = true;
                    } else {
                        startBlock = !this.isBlockingSword();
                    }
                    break;
            }
        }

        boolean attacked = false;
        if (this.currentTarget.hasUsableAim && this.isWithinSwingRange(this.currentTarget.distance)) {
            int rotationMode = this.rotations.getValue();
            if (rotationMode == 2 || rotationMode == 3) {
                float[] rotation = this.calculateCombatRotation(event);
                event.setRotation(rotation[0], rotation[1], 1);
                if (rotationMode == 3) {
                    MyauClient.rotationSmoother.requestRotation(rotation[0], rotation[1], 1, true);
                }
                if (this.moveFix.getValue() != 0 || rotationMode == 3) {
                    event.setMovementYaw(rotation[0], 1);
                }
            }
            if (canAttack) attacked = this.tryAttackTarget(event);
        }
        DisplaceModule displace = DisplaceModule.current();
        if (startBlock && (displace == null || !displace.isPendingAuraAttack())) {
            if (attacked) this.interactWithTargetAndBlock(event.getYaw(), event.getPitch());
            else this.startBlockingHeldItem();
        }
    }

    private boolean blockActionSent() {
        return MyauClient.packetActionTracker.sentDigging || MyauClient.packetActionTracker.sentBlockPlacement;
    }

    @EventHandler(priority=-50)
    public void onPostPlayerUpdate(PostPlayerUpdateEvent event) {
        if (this.lagBlockFlushPending || this.bufferRestartPending) {
            MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.AUTO_BLOCK);
        }
        if (this.bufferRestartPending) {
            MyauClient.outgoingPacketBuffer.setEnabled(true, OutgoingBufferOwner.AUTO_BLOCK);
        }
        this.bufferRestartPending = this.lagBlockFlushPending = false;
    }

    private void resetBlockCycle() {
        MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.AUTO_BLOCK);
        this.bufferRestartPending = this.lagBlockFlushPending = false;
        this.autoBlockPhase = 0;
        this.blockHoldMillis = 0L;
    }

    private void resetAutoBlock() {
        this.resetBlockCycle();
        this.blockReleaseDelayMillis = this.predictiveTimer = 0L;
        this.fullGrace = this.predictiveTick = this.predictiveNextTick = 0;
        this.serverBlocking = this.autoBlockActive = this.renderBlocking = false;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (MyauClient.outgoingPacketBuffer.owner() == OutgoingBufferOwner.AUTO_BLOCK) {
            MyauClient.outgoingPacketBuffer.queuedPackets.clear();
        }
        this.resetAutoBlock();
        this.currentTarget = null;
        this.hasTargetInAutoBlockRange = false;
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        Optional<AuraTarget> optional;
        if (!this.isEnabled()) {
            return;
        }
        ArrayList<AuraTarget> arrayList = this.collectTargetCandidates();
        if (arrayList.isEmpty()) {
            this.currentTarget = null;
            this.hasTargetInAutoBlockRange = false;
            return;
        }
        this.hasTargetInAutoBlockRange = arrayList.stream().anyMatch(this::isCandidateWithinAutoBlockRange);
        if (arrayList.stream().anyMatch(KillAuraModule::hasCandidateUsableAim)) {
            arrayList.removeIf(KillAuraModule::shouldDiscardUnusableAim);
        }
        if (arrayList.stream().anyMatch(this::isCandidateWithinSwingRange)) {
            arrayList.removeIf(this::shouldDiscardOutsideSwingRange);
        }
        if (arrayList.stream().anyMatch(this::isCandidateWithinAttackRange)) {
            arrayList.removeIf(this::shouldDiscardOutsideAttackRange);
        }
        if (arrayList.stream().anyMatch(this::isEnemyCandidate)) {
            arrayList.removeIf(this::shouldDiscardNonEnemyCandidate);
        }
        if (arrayList.isEmpty()) {
            this.currentTarget = null;
            return;
        }
        if (this.currentTarget != null && !this.targetSwitchTimer.hasElapsed(((Integer)this.switchDelay.getValue()).intValue()) && (optional = arrayList.stream().filter(this::matchesCurrentTarget).findFirst()).isPresent()) {
            this.currentTarget = optional.get();
            return;
        }
        arrayList.sort(this::compareTargetCandidates);
        if ((Integer)this.mode.getValue() == 1 && this.attackedThisUpdate) {
            this.attackedThisUpdate = false;
            ++this.targetIndex;
        }
        if ((Integer)this.mode.getValue() == 0 || this.targetIndex >= arrayList.size()) {
            this.targetIndex = 0;
        }
        this.currentTarget = arrayList.get(this.targetIndex);
        this.targetSwitchTimer.reset();
    }

    private ArrayList<AuraTarget> collectTargetCandidates() {
        Vec3 vec3 = KillAuraModule.minecraft.field_71439_g.func_174824_e(1.0f);
        ArrayList<AuraTarget> arrayList = new ArrayList<AuraTarget>();
        for (Entity entity : KillAuraModule.minecraft.field_71441_e.field_72996_f) {
            EntityLivingBase entityLivingBase;
            if (!(entity instanceof EntityLivingBase) || !this.isValidTargetEntity(entityLivingBase = (EntityLivingBase)entity)) continue;
            double d = entityLivingBase.func_70111_Y();
            AxisAlignedBB axisAlignedBB = entityLivingBase.func_174813_aQ().func_72314_b(d, d, d);
            double d2 = RotationUtils.distanceToBoundsFrom(axisAlignedBB, vec3);
            if (!this.isWithinAnyCombatRange(d2)) continue;
            float f2 = RotationUtils.boundsAimError(axisAlignedBB, vec3);
            if (f2 > this.fov.getValue()) continue;
            float[] fArray = RotationUtils.aimAtBounds(axisAlignedBB);
            float f3 = fArray[0];
            float f4 = fArray[1];
            boolean bl = true;
            if (!((Boolean)this.throughWalls.getValue()).booleanValue() && RotationUtils.isBoundsRayObstructed(axisAlignedBB, vec3, f3, f4, 8.0)) {
                if (RotationUtils.isBoundsRayObstructed(axisAlignedBB, vec3, KillAuraModule.minecraft.field_71439_g.field_70177_z, KillAuraModule.minecraft.field_71439_g.field_70125_A, 8.0)) {
                    bl = false;
                } else {
                    f3 = KillAuraModule.minecraft.field_71439_g.field_70177_z;
                    f4 = KillAuraModule.minecraft.field_71439_g.field_70125_A;
                }
            }
            arrayList.add(new AuraTarget(entityLivingBase, axisAlignedBB, entityLivingBase.field_70165_t, entityLivingBase.field_70163_u, entityLivingBase.field_70161_v, f3, f4, d2, f2, bl));
        }
        return arrayList;
    }

    @EventHandler
    public void onPostTick(PostTickEvent postTickEvent) {
        ItemStack itemStack;
        if (this.isEnabled() && this.isBlockingSword() && !KillAuraModule.minecraft.field_71439_g.func_70632_aY() && (itemStack = KillAuraModule.minecraft.field_71439_g.func_70694_bm()) != null) {
            KillAuraModule.minecraft.field_71439_g.func_71008_a(itemStack, itemStack.func_77988_m());
        }
    }

    @EventHandler(priority=-100)
    public void onPacketSend(PacketSendEvent packetSendEvent) {
        if (!this.isEnabled() || packetSendEvent.isCancelled()) {
            return;
        }
        if (packetSendEvent.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07PacketPlayerDigging = (C07PacketPlayerDigging)packetSendEvent.getPacket();
            if (c07PacketPlayerDigging.func_180762_c() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                this.serverBlocking = false;
            }
            return;
        }
        if (packetSendEvent.getPacket() instanceof C09PacketHeldItemChange) {
            this.serverBlocking = false;
            if (this.autoBlockActive) {
                KillAuraModule.minecraft.field_71439_g.func_71034_by();
            }
        }
    }

    private void logHealthDelta(float f) {
        if (f == 0.0f || this.lastDamageLogTick == KillAuraModule.minecraft.field_71439_g.field_70173_aa) {
            return;
        }
        this.lastDamageLogTick = KillAuraModule.minecraft.field_71439_g.field_70173_aa;
        String string = f > 0.0f ? "&a" : "&c";
        ChatUtils.addFormattedMessage(String.format("%sHealth: %s&l%s&r (&otick: %d&r)&r", "&7[&bMyau&7] &r", string, damageFormat.format(f), KillAuraModule.minecraft.field_71439_g.field_70173_aa));
    }

    @EventHandler(priority=-100)
    public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
        if (!this.isEnabled() || packetReceiveEvent.isCancelled() || (Integer)this.debugLog.getValue() != 1) {
            return;
        }
        if (packetReceiveEvent.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth s06PacketUpdateHealth = (S06PacketUpdateHealth)packetReceiveEvent.getPacket();
            this.logHealthDelta(s06PacketUpdateHealth.func_149332_c() - KillAuraModule.minecraft.field_71439_g.func_110143_aJ());
            return;
        }
        if (!(packetReceiveEvent.getPacket() instanceof S1CPacketEntityMetadata)) {
            return;
        }
        S1CPacketEntityMetadata s1CPacketEntityMetadata = (S1CPacketEntityMetadata)packetReceiveEvent.getPacket();
        if (s1CPacketEntityMetadata.func_149375_d() != KillAuraModule.minecraft.field_71439_g.func_145782_y()) {
            return;
        }
        for (DataWatcher.WatchableObject watchableObject : s1CPacketEntityMetadata.func_149376_c()) {
            if (watchableObject.func_75672_a() != 6) continue;
            this.logHealthDelta(((Float)watchableObject.func_75669_b()).floatValue() - KillAuraModule.minecraft.field_71439_g.func_110143_aJ());
        }
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent movementInputEvent) {
        if (!this.isEnabled() || (Integer)this.moveFix.getValue() != 1) {
            return;
        }
        if ((Integer)this.rotations.getValue() == 3) {
            return;
        }
        if (RotationState.isActive() && RotationState.movementPriority() == 1.0f && MovementUtils.hasMovementInput()) {
            MovementUtils.correctMovementForYaw(RotationState.movementYaw());
        }
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent renderWorldEvent) {
        if (!(this.isEnabled() && (Integer)this.showTarget.getValue() != 0 && this.currentTarget != null && EntityUtils.isLoaded((Entity)this.currentTarget.entity) && this.isCombatInputAllowed())) {
            return;
        }
        Color color = Color.WHITE;
        if ((Integer)this.showTarget.getValue() == 1) {
            color = this.currentTarget.entity.field_70737_aN > 0 ? new Color(0xFF5555) : new Color(0x55FF55);
        } else if ((Integer)this.showTarget.getValue() == 2) {
            color = ((HUDModule)MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis());
        }
        RenderUtils.beginOverlay();
        RenderUtils.fillEntityBounds((Entity)this.currentTarget.entity, color.getRed(), color.getGreen(), color.getBlue(), 63, 0.1f);
        RenderUtils.endOverlay();
    }

    @EventHandler
    public void onLeftClick(LeftClickEvent leftClickEvent) {
        if (this.autoBlockActive) {
            leftClickEvent.setCancelled(true);
            return;
        }
        if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            leftClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClick(RightClickEvent rightClickEvent) {
        if (this.autoBlockActive) {
            rightClickEvent.setCancelled(true);
            return;
        }
        if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            rightClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockClick(BlockClickEvent blockClickEvent) {
        if (this.autoBlockActive) {
            blockClickEvent.setCancelled(true);
            return;
        }
        if (this.isEnabled() && this.canRunCombat() && this.currentTarget != null && this.isWithinSwingRange(this.currentTarget.distance)) {
            blockClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onStopUsingItem(StopUsingItemEvent stopUsingItemEvent) {
        if (this.autoBlockActive) {
            stopUsingItemEvent.setCancelled(true);
        }
    }

    @Override
    public void onEnable() {
        this.currentTarget = null;
        this.hasTargetInAutoBlockRange = false;
        this.targetIndex = 0;
        this.attackedThisUpdate = false;
        this.attackCooldownMillis = 0L;
        this.resetAutoBlock();
    }

    @Override
    public void onDisable() {
        if (this.serverBlocking && minecraft.field_71439_g != null && minecraft.func_147114_u() != null
                && !blockActionSent()) {
            this.releaseBlocking();
        }
        this.resetAutoBlock();
        this.currentTarget = null;
        this.hasTargetInAutoBlockRange = false;
    }

    @Override
    public void onSettingChanged(String string) {
        if (this.autoBlock.getName().equals(string) && this.isEnabled()) {
            if (this.serverBlocking && minecraft.field_71439_g != null && minecraft.func_147114_u() != null
                    && !blockActionSent()) this.releaseBlocking();
            this.resetAutoBlock();
        }
        if (this.swingRange.getName().equals(string) && ((Float)this.swingRange.getValue()).floatValue() < ((Float)this.attackRange.getValue()).floatValue()) {
            this.attackRange.setValue(this.swingRange.getValue());
        } else if (this.attackRange.getName().equals(string) && ((Float)this.attackRange.getValue()).floatValue() > ((Float)this.swingRange.getValue()).floatValue()) {
            this.swingRange.setValue(this.attackRange.getValue());
        }
        if ((Integer)this.minAps.getValue() > (Integer)this.maxAps.getValue()) {
            if (this.minAps.getName().equals(string)) {
                this.maxAps.setValue(this.minAps.getValue());
            } else {
                this.minAps.setValue(this.maxAps.getValue());
            }
        }
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
    }

    private int compareTargetCandidates(AuraTarget auraTarget, AuraTarget auraTarget2) {
        int n;
        switch ((Integer)this.sort.getValue()) {
            case 1: {
                n = Float.compare(EntityUtils.armorAdjustedHealth(auraTarget.entity), EntityUtils.armorAdjustedHealth(auraTarget2.entity));
                break;
            }
            case 2: {
                n = Integer.compare(auraTarget.entity.field_70172_ad, auraTarget2.entity.field_70172_ad);
                break;
            }
            case 3: {
                n = Float.compare(auraTarget.angularDistance, auraTarget2.angularDistance);
                break;
            }
            default: {
                n = Double.compare(auraTarget.distance, auraTarget2.distance);
            }
        }
        return n != 0 ? n : Double.compare(auraTarget.distance, auraTarget2.distance);
    }

    private boolean matchesCurrentTarget(AuraTarget auraTarget) {
        return this.currentTarget != null && auraTarget.entity == this.currentTarget.entity;
    }

    private boolean shouldDiscardNonEnemyCandidate(AuraTarget auraTarget) {
        return !this.isEnemyPlayer(auraTarget.entity);
    }

    private boolean isEnemyCandidate(AuraTarget auraTarget) {
        return this.isEnemyPlayer(auraTarget.entity);
    }

    private boolean shouldDiscardOutsideAttackRange(AuraTarget auraTarget) {
        return !this.isWithinAttackRange(auraTarget.distance);
    }

    private boolean isCandidateWithinAttackRange(AuraTarget auraTarget) {
        return this.isWithinAttackRange(auraTarget.distance);
    }

    private boolean shouldDiscardOutsideSwingRange(AuraTarget auraTarget) {
        return !this.isWithinSwingRange(auraTarget.distance);
    }

    private boolean isCandidateWithinSwingRange(AuraTarget auraTarget) {
        return this.isWithinSwingRange(auraTarget.distance);
    }

    private static boolean shouldDiscardUnusableAim(AuraTarget auraTarget) {
        return !auraTarget.hasUsableAim;
    }

    private static boolean hasCandidateUsableAim(AuraTarget auraTarget) {
        return auraTarget.hasUsableAim;
    }

    private boolean isCandidateWithinAutoBlockRange(AuraTarget auraTarget) {
        return this.isWithinAutoBlockRange(auraTarget.distance);
    }

    private boolean isAutoBlockRangeVisible() {
        return (Integer)this.autoBlock.getValue() != 0;
    }

    private boolean isAutoBlockHurtTimeVisible() {
        int n = (Integer)this.autoBlock.getValue();
        return n != AUTO_BLOCK_NONE;
    }

    private boolean isAutoBlockDelayVisible() {
        int n = (Integer)this.autoBlock.getValue();
        return n == AUTO_BLOCK_BLINK || n == AUTO_BLOCK_LEGIT || n == AUTO_BLOCK_PREDICTIVE;
    }

    private boolean isAutoBlockHoldVisible() {
        int n = (Integer)this.autoBlock.getValue();
        return n == AUTO_BLOCK_BLINK || n == AUTO_BLOCK_LEGIT || n == AUTO_BLOCK_PREDICTIVE;
    }

    private boolean isAutoBlockNoSlowVisible() {
        return (Integer)this.autoBlock.getValue() != 0;
    }

    private boolean isAutoBlockRequirePressVisible() {
        return (Integer)this.autoBlock.getValue() != 0;
    }
}
