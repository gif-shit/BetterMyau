package myau;

import java.awt.Color;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class BacktrackModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final long POSITION_INTERP_MS = 80L;
    private static final double POS_EPS = 1.0E-6;
    private static final double OPEN_ADVANTAGE_EPS = 0.025;
    private static final double HOLD_ADVANTAGE_EPS = 0.01;
    private static final double FULL_ADAPTIVE_ADVANTAGE = 0.35;
    private static final int MAX_QUEUED_PACKETS = 512;
    private static final int DELAY_JITTER_MS = 4;
    public final IntegerSetting delay = new IntegerSetting("delay", 200, 50, 500);
    public final FloatSetting minDistance = new FloatSetting("min-distance", 0.0f, 0.0f, 3.0f);
    public final FloatSetting maxDistance = new FloatSetting("max-distance", 4.0f, 1.0f, 6.0f);
    public final IntegerSetting cooldown = new IntegerSetting("cooldown", 0, 0, 2000);
    public final IntegerSetting maxHurtTime = new IntegerSetting("max-hurt-time", 500, 0, 500);
    public final BooleanSetting onlyAttacking = new BooleanSetting("only-attacking", true);
    public final IntegerSetting attackWindow = new IntegerSetting("attack-window", 500, 100, 1500, this.onlyAttacking::getValue);
    public final BooleanSetting adaptiveDelay = new BooleanSetting("adaptive-delay", true);
    public final BooleanSetting disableAdventure = new BooleanSetting("disable-adventure", true);
    public final BooleanSetting flushOnDamage = new BooleanSetting("flush-on-damage", true);
    public final BooleanSetting ignoreTeammates = new BooleanSetting("ignore-teammates", true);
    public final BooleanSetting weaponOnly = new BooleanSetting("weapon-only", false);
    public final ModeSetting showPosition = new ModeSetting("show-position", 1, new String[] { "NONE", "DEFAULT", "HUD" });
    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<TimedPacket>();
    private volatile Vec3 targetPos = null;
    private volatile EntityPlayer target = null;
    private volatile int currentDelay = 0;
    private volatile boolean delayingPackets = false;
    private long lastDeactivationNanos = 0L;
    private long lastAttackNanos = 0L;
    private long lastQueuedReleaseAtNanos = 0L;
    private Vec3 positionInterpFrom;
    private Vec3 positionInterpTo;
    private long positionInterpStartMs;

    public BacktrackModule() {
        super("Backtrack", false, 0);
    }

    @Override
    public void onSettingChanged(String string) {
        if (this.minDistance.getValue() > this.maxDistance.getValue()) {
            if (string.equals("min-distance")) {
                this.maxDistance.setValue(this.minDistance.getValue());
            } else if (string.equals("max-distance")) {
                this.minDistance.setValue(this.maxDistance.getValue());
            }
        }
    }

    @Override
    public void onEnable() {
        this.resetWithoutProcessingPackets();
    }

    @Override
    public void onDisable() {
        if (minecraft.func_71356_B() || minecraft.func_147114_u() == null) {
            this.resetWithoutProcessingPackets();
        } else {
            this.flushPendingPackets();
            this.clearRuntimeState();
        }
    }

    @Override
    public String[] getHudInfo() {
        if (this.currentDelay > 0 && !this.isPacketQueueEmpty()) {
            return new String[] { this.currentDelay + "ms" };
        }
        return new String[] { this.delay.getValue() + "ms" };
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoadEvent) {
        if (this.isEnabled()) {
            this.resetWithoutProcessingPackets();
        }
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        if (!this.isEnabled()) {
            return;
        }
        if (minecraft.func_71356_B() || minecraft.field_71439_g == null || minecraft.field_71441_e == null) {
            this.resetWithoutProcessingPackets();
            return;
        }
        this.processDelayedPackets();
        EntityPlayer entityPlayer = this.target;
        if (entityPlayer == null) {
            if (!this.isPacketQueueEmpty()) {
                this.flushPendingPackets();
            }
            this.currentDelay = 0;
            this.delayingPackets = false;
            return;
        }
        if (!this.isTargetStillValid(entityPlayer)) {
            this.deactivateBacktrack(true);
            this.clearTarget();
            return;
        }
        if (this.shouldDeactivateForState()) {
            this.deactivateBacktrack(true);
            return;
        }
        if (this.flushOnDamage.getValue() && minecraft.field_71439_g.field_70737_aN > 0) {
            this.deactivateBacktrack(true);
            return;
        }
        if (this.isAttackWindowExpired()) {
            this.deactivateBacktrack(true);
            return;
        }
        double d = this.distanceFromEyeTo(entityPlayer, entityPlayer.func_174791_d());
        if (d > (double) this.maxDistance.getValue() || d < (double) this.minDistance.getValue()) {
            this.deactivateBacktrack(true);
            return;
        }
        if (this.isPacketQueueEmpty()) {
            this.targetPos = entityPlayer.func_174791_d();
            this.delayingPackets = false;
        }
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent renderWorldEvent) {
        double d;
        EntityPlayer entityPlayer = this.target;
        Vec3 vec3 = this.targetPos;
        if (!this.isEnabled() || minecraft.func_71356_B() || this.showPosition.getValue() == 0) {
            return;
        }
        if (entityPlayer == null || vec3 == null || entityPlayer.field_70128_L || this.currentDelay <= 0) {
            return;
        }
        long l = System.currentTimeMillis();
        if (this.positionInterpTo == null) {
            this.positionInterpFrom = vec3;
            this.positionInterpTo = vec3;
            this.positionInterpStartMs = l;
        } else if (positionChanged(vec3, this.positionInterpTo)) {
            d = Math.min(1.0, (double) (l - this.positionInterpStartMs) / 80.0);
            this.positionInterpFrom = lerpVec3(this.positionInterpFrom, this.positionInterpTo, d);
            this.positionInterpTo = vec3;
            this.positionInterpStartMs = l;
        }
        d = Math.min(1.0, (double) (l - this.positionInterpStartMs) / 80.0);
        Vec3 vec32 = lerpVec3(this.positionInterpFrom, this.positionInterpTo, d);
        Color color = this.showPosition.getValue() == 2 ? ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis()) : EntityUtils.teamColor(entityPlayer, 1.0f);
        double d2 = (double) entityPlayer.field_70130_N / 2.0;
        AccessorRenderManager accessorRenderManager = (AccessorRenderManager) minecraft.func_175598_ae();
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(vec32.field_72450_a - d2, vec32.field_72448_b, vec32.field_72449_c - d2, vec32.field_72450_a + d2, vec32.field_72448_b + (double) entityPlayer.field_70131_O, vec32.field_72449_c + d2).func_72317_d(-accessorRenderManager.getRenderPosX(), -accessorRenderManager.getRenderPosY(), -accessorRenderManager.getRenderPosZ());
        RenderUtils.beginOverlay();
        RenderUtils.fillBounds(axisAlignedBB, color.getRed(), color.getGreen(), color.getBlue(), 63);
        RenderUtils.outlineBounds(axisAlignedBB, color.getRed(), color.getGreen(), color.getBlue(), 200, 1.5f);
        RenderUtils.endOverlay();
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent packetSendEvent) {
        if (!this.isEnabled() || packetSendEvent.isCancelled() || !(packetSendEvent.getPacket() instanceof C02PacketUseEntity)) {
            return;
        }
        C02PacketUseEntity c02PacketUseEntity = (C02PacketUseEntity) packetSendEvent.getPacket();
        if (c02PacketUseEntity.func_149565_c() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }
        if (minecraft.func_71356_B() || minecraft.field_71439_g == null || minecraft.field_71441_e == null) {
            return;
        }
        if (this.shouldDeactivateForState()) {
            return;
        }
        Entity entity = c02PacketUseEntity.func_149564_a((World) minecraft.field_71441_e);
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer entityPlayer = (EntityPlayer) entity;
        if (!this.isTargetAllowed(entityPlayer) || this.isInCooldown()) {
            return;
        }
        double d = minecraft.field_71439_g.func_70032_d((Entity) entityPlayer);
        if (d > (double) this.maxDistance.getValue() || d < (double) this.minDistance.getValue()) {
            return;
        }
        if (this.maxHurtTime.getValue() < 500 && entityPlayer.field_70737_aN * 50 > this.maxHurtTime.getValue()) {
            return;
        }
        if (this.target == null || entityPlayer != this.target) {
            this.flushPendingPackets();
            this.delayingPackets = false;
            this.clearPositionInterp();
            this.target = entityPlayer;
            this.targetPos = entityPlayer.func_174791_d();
        } else if (this.currentDelay <= 0 || this.targetPos == null) {
            this.flushPendingPackets();
            this.delayingPackets = false;
            this.clearPositionInterp();
            this.targetPos = entityPlayer.func_174791_d();
        }
        this.lastAttackNanos = System.nanoTime();
        this.currentDelay = Math.max(1, this.delay.getValue());
    }

    @EventHandler(priority = -10)
    public void onReceivePacket(PacketReceiveEvent packetReceiveEvent) {
        if (!this.isEnabled() || packetReceiveEvent.isCancelled()) {
            return;
        }
        if (minecraft.func_71356_B() || minecraft.field_71439_g == null || minecraft.field_71441_e == null) {
            return;
        }
        Packet<?> packet = packetReceiveEvent.getPacket();
        if (packet instanceof S01PacketJoinGame || packet instanceof S07PacketRespawn || packet instanceof S40PacketDisconnect) {
            this.resetWithoutProcessingPackets();
            return;
        }
        if (minecraft.field_71439_g.field_70173_aa < 20) {
            this.resetWithoutProcessingPackets();
            return;
        }
        this.processDelayedPackets();
        EntityPlayer entityPlayer = this.target;
        if (entityPlayer != null && this.delayingPackets && this.isPacketQueueEmpty()) {
            this.targetPos = entityPlayer.func_174791_d();
            this.delayingPackets = false;
        }
        if (this.shouldDeactivateForState()) {
            this.deactivateBacktrack(true);
            return;
        }
        if (entityPlayer == null) {
            if (!this.isPacketQueueEmpty()) {
                this.flushPendingPackets();
            }
            return;
        }
        if (!this.isTargetStillValid(entityPlayer)) {
            this.deactivateBacktrack(true);
            this.clearTarget();
            return;
        }
        if (packet instanceof S13PacketDestroyEntities) {
            for (int n : ((S13PacketDestroyEntities) packet).func_149098_c()) {
                if (n != entityPlayer.func_145782_y())
                    continue;
                this.deactivateBacktrack(true);
                this.clearTarget();
                return;
            }
        }
        if (this.currentDelay <= 0) {
            return;
        }
        if (this.isAttackWindowExpired()) {
            this.deactivateBacktrack(true);
            return;
        }
        if (this.shouldFlushForPacket(packet)) {
            this.deactivateBacktrack(true);
            return;
        }
        Vec3 vec3 = this.getNextServerShadow(packet, entityPlayer);
        if (vec3 != null) {
            int n = this.delayingPackets ? 1 : 0;
            this.targetPos = vec3;
            double d = this.distanceFromEyeTo(entityPlayer, vec3) - this.distanceFromEyeTo(entityPlayer, entityPlayer.func_174791_d());
            double d2 = n != 0 ? 0.01 : 0.025;
            boolean bl = this.delayingPackets = d > d2;
            if (this.delayingPackets && n == 0) {
                this.currentDelay = this.chooseDelayForAdvantage(d);
            }
            if (!this.delayingPackets && n != 0 && !this.isPacketQueueEmpty()) {
                this.flushPendingPackets();
            }
        }
        if (this.delayingPackets) {
            if (!this.queuePacket(packet)) {
                this.deactivateBacktrack(true);
                return;
            }
            packetReceiveEvent.setCancelled(true);
        } else if (!this.isPacketQueueEmpty()) {
            this.flushPendingPackets();
        }
    }

    private boolean shouldDeactivateForState() {
        if (this.weaponOnly.getValue() && !ItemUtils.isHoldingWeapon()) {
            return true;
        }
        return this.disableAdventure.getValue()
                && minecraft.field_71442_b != null
                && minecraft.field_71442_b.func_178889_l().func_82752_c();
    }

    private double distanceFromEyeTo(EntityPlayer entityPlayer, Vec3 vec3) {
        double d = (double) entityPlayer.field_70130_N / 2.0;
        float f = entityPlayer.func_70111_Y();
        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(vec3.field_72450_a - d, vec3.field_72448_b, vec3.field_72449_c - d, vec3.field_72450_a + d, vec3.field_72448_b + (double) entityPlayer.field_70131_O, vec3.field_72449_c + d).func_72314_b((double) f, (double) f, (double) f);
        return RotationUtils.distanceToBoundsFrom(axisAlignedBB, minecraft.field_71439_g.func_174824_e(1.0f));
    }

    private Vec3 getNextServerShadow(Packet<?> packet, EntityPlayer entityPlayer) {
        S14PacketEntity s14PacketEntity;
        S18PacketEntityTeleport teleport;
        if (packet instanceof S14PacketEntity
                && (s14PacketEntity = (S14PacketEntity) packet).func_149065_a((World) minecraft.field_71441_e) == entityPlayer) {
            Vec3 vec3 = this.targetPos != null ? this.targetPos : entityPlayer.func_174791_d();
            return vec3.func_72441_c((double) s14PacketEntity.func_149062_c() / 32.0, (double) s14PacketEntity.func_149061_d() / 32.0, (double) s14PacketEntity.func_149064_e() / 32.0);
        }
        if (packet instanceof S18PacketEntityTeleport
                && (teleport = (S18PacketEntityTeleport) packet).func_149451_c() == entityPlayer.func_145782_y()) {
            return new Vec3((double) teleport.func_149449_d() / 32.0, (double) teleport.func_149448_e() / 32.0, (double) teleport.func_149446_f() / 32.0);
        }
        return null;
    }

    private int chooseDelayForAdvantage(double d) {
        int n = Math.max(1, this.delay.getValue());
        if (!this.adaptiveDelay.getValue()) {
            return n;
        }
        double d2 = Math.max(1.0E-6, 0.325);
        double d3 = Math.max(0.0, Math.min(1.0, (d - 0.025) / d2));
        double d4 = 0.6 + 0.4 * d3;
        int n2 = (int) Math.round((double) n * d4);
        return Math.max(50, Math.min(n, n2 += ThreadLocalRandom.current().nextInt(-4, 5)));
    }

    private boolean shouldFlushForPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook || packet instanceof S27PacketExplosion) {
            return true;
        }
        if (packet instanceof S12PacketEntityVelocity) {
            return ((S12PacketEntityVelocity) packet).func_149412_c() == minecraft.field_71439_g.func_145782_y();
        }
        if (!this.flushOnDamage.getValue()) {
            return false;
        }
        if (packet instanceof S06PacketUpdateHealth) {
            return ((S06PacketUpdateHealth) packet).func_149332_c() < minecraft.field_71439_g.func_110143_aJ();
        }
        if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus s19PacketEntityStatus = (S19PacketEntityStatus) packet;
            Entity entity = s19PacketEntityStatus.func_149161_a((World) minecraft.field_71441_e);
            byte by = s19PacketEntityStatus.func_149160_c();
            return entity == minecraft.field_71439_g && (by == 2 || by == 3);
        }
        return false;
    }

    private boolean isTargetAllowed(EntityPlayer entityPlayer) {
        if (entityPlayer == null || entityPlayer == minecraft.field_71439_g || entityPlayer.field_70128_L) {
            return false;
        }
        if (minecraft.func_147114_u() == null || EntityUtils.isBot(entityPlayer)) {
            return false;
        }
        if (this.ignoreTeammates.getValue() && EntityUtils.isTeammate(entityPlayer)) {
            return false;
        }
        return !EntityUtils.isFriend(entityPlayer);
    }

    private boolean isTargetStillValid(EntityPlayer entityPlayer) {
        if (!this.isTargetAllowed(entityPlayer) || minecraft.field_71441_e == null) {
            return false;
        }
        return minecraft.field_71441_e.func_73045_a(entityPlayer.func_145782_y()) == entityPlayer;
    }

    private boolean isAttackWindowExpired() {
        if (!this.onlyAttacking.getValue()) {
            return false;
        }
        if (this.lastAttackNanos <= 0L) {
            return true;
        }
        return System.nanoTime() - this.lastAttackNanos > millisToNanos(this.attackWindow.getValue());
    }

    private boolean isInCooldown() {
        if (this.cooldown.getValue() <= 0 || this.lastDeactivationNanos <= 0L) {
            return false;
        }
        return System.nanoTime() - this.lastDeactivationNanos < millisToNanos(this.cooldown.getValue());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void processDelayedPackets() {
        Queue<TimedPacket> queue = this.packetQueue;
        synchronized (queue) {
            TimedPacket timedPacket;
            long l = System.nanoTime();
            while (!this.packetQueue.isEmpty() && (timedPacket = this.packetQueue.peek()) != null && l >= timedPacket.releaseAtNanos) {
                this.packetQueue.poll();
                this.processPacket(timedPacket.packet);
            }
            if (this.packetQueue.isEmpty()) {
                this.lastQueuedReleaseAtNanos = 0L;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean queuePacket(Packet<?> packet) {
        Queue<TimedPacket> queue = this.packetQueue;
        synchronized (queue) {
            if (this.packetQueue.size() >= 512) {
                return false;
            }
            long l = System.nanoTime() + millisToNanos(this.currentDelay);
            if (this.lastQueuedReleaseAtNanos > 0L && l <= this.lastQueuedReleaseAtNanos) {
                l = this.lastQueuedReleaseAtNanos + 1L;
            }
            this.packetQueue.add(new TimedPacket(packet, l));
            this.lastQueuedReleaseAtNanos = l;
            return true;
        }
    }

    private void deactivateBacktrack(boolean bl) {
        boolean bl2 = this.currentDelay > 0 || this.delayingPackets || !this.isPacketQueueEmpty();
        this.flushPendingPackets();
        this.currentDelay = 0;
        this.delayingPackets = false;
        EntityPlayer entityPlayer = this.target;
        if (entityPlayer != null && !entityPlayer.field_70128_L) {
            this.targetPos = entityPlayer.func_174791_d();
        }
        this.clearPositionInterp();
        if (bl && bl2) {
            this.lastDeactivationNanos = System.nanoTime();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void flushPendingPackets() {
        Queue<TimedPacket> queue = this.packetQueue;
        synchronized (queue) {
            while (!this.packetQueue.isEmpty()) {
                TimedPacket timedPacket = this.packetQueue.poll();
                if (timedPacket == null)
                    continue;
                this.processPacket(timedPacket.packet);
            }
            this.lastQueuedReleaseAtNanos = 0L;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean isPacketQueueEmpty() {
        Queue<TimedPacket> queue = this.packetQueue;
        synchronized (queue) {
            return this.packetQueue.isEmpty();
        }
    }

    private void clearTarget() {
        this.target = null;
        this.targetPos = null;
        this.lastAttackNanos = 0L;
        this.clearPositionInterp();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void resetWithoutProcessingPackets() {
        Queue<TimedPacket> queue = this.packetQueue;
        synchronized (queue) {
            this.packetQueue.clear();
            this.lastQueuedReleaseAtNanos = 0L;
        }
        this.clearRuntimeState();
    }

    private void clearRuntimeState() {
        this.target = null;
        this.targetPos = null;
        this.currentDelay = 0;
        this.lastDeactivationNanos = 0L;
        this.lastAttackNanos = 0L;
        this.delayingPackets = false;
        this.clearPositionInterp();
    }

    private void clearPositionInterp() {
        this.positionInterpFrom = null;
        this.positionInterpTo = null;
        this.positionInterpStartMs = 0L;
    }

    private static long millisToNanos(int n) {
        return (long) n * 1000000L;
    }

    private static boolean positionChanged(Vec3 vec3, Vec3 vec32) {
        return Math.abs(vec3.field_72450_a - vec32.field_72450_a) > 1.0E-6
                || Math.abs(vec3.field_72448_b - vec32.field_72448_b) > 1.0E-6
                || Math.abs(vec3.field_72449_c - vec32.field_72449_c) > 1.0E-6;
    }

    private static Vec3 lerpVec3(Vec3 vec3, Vec3 vec32, double d) {
        if (d <= 0.0) {
            return vec3;
        }
        if (d >= 1.0) {
            return vec32;
        }
        return new Vec3(vec3.field_72450_a + (vec32.field_72450_a - vec3.field_72450_a) * d, vec3.field_72448_b + (vec32.field_72448_b - vec3.field_72448_b) * d, vec3.field_72449_c + (vec32.field_72449_c - vec3.field_72449_c) * d);
    }

    private void processPacket(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        try {
            if (minecraft.func_147114_u() != null) {
                ((Packet) packet).func_148833_a(minecraft.func_147114_u());
            }
        } catch (Exception exception) {
        }
    }

    private static class TimedPacket {

        final Packet<?> packet;

        final long releaseAtNanos;

        TimedPacket(Packet<?> packet, long l) {
            this.packet = packet;
            this.releaseAtNanos = l;
        }
    }
}
