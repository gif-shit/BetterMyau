package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MovingObjectPosition;

public class KeepSprintModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean sprintDropped = false;
    private Entity deferredTarget = null;
    private int deferredTicks = 0;
    private boolean serverSprinting = false;
    private boolean lastSentSprinting = false;
    private int heldAttackTicks = 0;
    public final PercentSetting slowdown = new PercentSetting("slowdown", 0, this::isSlowdownVisible);
    public final BooleanSetting groundOnly = new BooleanSetting("ground-only", false, this::isSlowdownVisible);
    public final BooleanSetting reachOnly = new BooleanSetting("reach-only", false, this::isSlowdownVisible);
    public final BooleanSetting watchdog = new BooleanSetting("watchdog", false);
    public final BooleanSetting skipImmuneHits = new BooleanSetting("skip-immune-hits", true, this.watchdog::getValue);

    public KeepSprintModule() {
        super("KeepSprint", false, 0);
    }

    public boolean shouldPreserveSprint() {
        if (this.watchdog.getValue()) {
            return false;
        }
        boolean bl = this.groundOnly.getValue();
        if (bl && !minecraft.field_71439_g.field_70122_E) {
            return false;
        }
        if (!this.reachOnly.getValue()) {
            return true;
        }
        return minecraft.field_71476_x.field_72307_f.func_72438_d(minecraft.func_175606_aa().func_174824_e(1.0f)) > 3.0;
    }

    private boolean isWatchdogActive() {
        return this.isEnabled() && this.watchdog.getValue() && minecraft.field_71439_g != null;
    }

    private boolean isAttackDueNextTick() {
        if (this.deferredTarget != null) {
            return true;
        }
        KillAuraModule killAuraModule = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        return killAuraModule.isAttackDueNextTick() && !this.isImmune(killAuraModule.getCurrentTargetEntity(), 1);
    }

    private boolean isImmune(EntityLivingBase entityLivingBase, int n) {
        return this.skipImmuneHits.getValue() && entityLivingBase != null && entityLivingBase.field_70737_aN - n > 3;
    }

    private boolean isDropWorthIt() {
        double d = minecraft.field_71439_g.field_70122_E ? 0.03 : 0.006;
        return MovementUtils.horizontalSpeed() * 0.4 > d;
    }

    @EventHandler
    public void onLeftClick(LeftClickEvent leftClickEvent) {
        if (!this.isWatchdogActive() || leftClickEvent.isCancelled()) {
            return;
        }
        MovingObjectPosition movingObjectPosition = minecraft.field_71476_x;
        if (movingObjectPosition == null || movingObjectPosition.field_72313_a != MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }
        if (this.deferredTarget != null || !minecraft.field_71439_g.func_70051_ag()) {
            this.deferredTarget = null;
            return;
        }
        leftClickEvent.setCancelled(true);
        this.deferredTarget = movingObjectPosition.field_72308_g;
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent packetSendEvent) {
        if (packetSendEvent.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction) packetSendEvent.getPacket()).func_180764_b();
            if (action == C0BPacketEntityAction.Action.START_SPRINTING) {
                this.serverSprinting = true;
            } else if (action == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                this.serverSprinting = false;
            }
        } else if (packetSendEvent.getPacket() instanceof C03PacketPlayer) {
            this.lastSentSprinting = this.serverSprinting;
        }
    }

    private boolean wouldHitSlow() {
        return this.lastSentSprinting && minecraft.field_71439_g.func_70051_ag();
    }

    public boolean shouldHoldAttack(EntityLivingBase entityLivingBase) {
        if (!this.isWatchdogActive()) {
            return false;
        }
        if (this.isImmune(entityLivingBase, 0)) {
            return true;
        }
        if (!this.wouldHitSlow() || this.heldAttackTicks >= 2 || !this.isDropWorthIt()) {
            this.heldAttackTicks = 0;
            return false;
        }
        ++this.heldAttackTicks;
        return true;
    }

    private boolean isJumpTick() {
        return minecraft.field_71439_g.field_70122_E && minecraft.field_71439_g.field_71158_b.field_78901_c;
    }

    @EventHandler(priority = 100)
    public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
        if (!this.isWatchdogActive() || this.deferredTarget == null) {
            return;
        }
        if (this.wouldHitSlow() && this.deferredTicks < 2) {
            ++this.deferredTicks;
            return;
        }
        Entity entity = this.deferredTarget;
        this.deferredTarget = null;
        this.deferredTicks = 0;
        MovingObjectPosition movingObjectPosition = minecraft.field_71476_x;
        if (movingObjectPosition == null || movingObjectPosition.field_72308_g != entity || !entity.func_70089_S()) {
            return;
        }
        minecraft.field_71439_g.func_71038_i();
        minecraft.field_71442_b.func_78764_a((EntityPlayer) minecraft.field_71439_g, entity);
    }

    @EventHandler(priority = -100)
    public void onLivingUpdate(LivingUpdateEvent livingUpdateEvent) {
        if (!this.isWatchdogActive()) {
            this.sprintDropped = false;
            return;
        }
        boolean bl = minecraft.field_71439_g.func_70051_ag();
        if ((bl || this.sprintDropped) && this.isAttackDueNextTick() && !this.isJumpTick() && this.isDropWorthIt()) {
            minecraft.field_71439_g.func_70031_b(false);
            this.sprintDropped = true;
            return;
        }
        if (this.sprintDropped) {
            this.sprintDropped = false;
            if (minecraft.field_71439_g.field_71158_b.field_78900_b >= 0.8f && !minecraft.field_71439_g.func_71039_bw()) {
                minecraft.field_71439_g.func_70031_b(true);
            }
        }
    }

    @Override
    public void onDisable() {
        this.sprintDropped = false;
        this.deferredTarget = null;
        this.deferredTicks = 0;
        this.heldAttackTicks = 0;
    }

    private boolean isSlowdownVisible() {
        return !this.watchdog.getValue();
    }
}
