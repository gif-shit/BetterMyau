package myau;

import java.util.ArrayDeque;
import java.util.Deque;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.Packet;
import net.minecraft.util.MovingObjectPosition;

/** BetterIvory's Semi use-key cycle, exposed as Blink. */
public final class NoSlowBlink {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final int MAX_QUEUED_PACKETS = 128;

    private final NoSlowModule owner;
    private final Deque<Packet<?>> packets = new ArrayDeque<>();
    private Object world;
    private Object connection;
    private boolean inputScope;
    private boolean useRead;
    private boolean physicalUse;
    private boolean forceHeld;
    private boolean forcePress;
    private boolean suppressUse;
    private boolean qualified;
    private boolean rearmAfterMotion;
    private boolean buffering;
    private int useTick;
    private long bufferedAt;

    NoSlowBlink(NoSlowModule owner) {
        this.owner = owner;
    }

    public static NoSlowBlink current() {
        if (MyauClient.moduleManager == null) return null;
        NoSlowModule module = (NoSlowModule) MyauClient.moduleManager.modules.get(NoSlowModule.class);
        return module == null ? null : module.blink;
    }

    private Object connection() {
        return mc.func_147114_u() == null ? null : mc.func_147114_u().func_147298_b();
    }

    private boolean sameSession() {
        return world != null && world == mc.field_71441_e && connection != null && connection == connection();
    }

    private boolean auraUse() {
        KillAuraModule aura = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        return aura != null && aura.isEnabled()
                && (aura.isServerBlocking() || aura.isAutoBlockActive() || aura.wantsNoSlowBlock());
    }

    private boolean canUse() {
        if (((AccessorPlayerControllerMP) mc.field_71442_b).getIsHittingBlock()) return false;
        if (mc.field_71439_g.func_71039_bw() || auraUse()) return true;
        MovingObjectPosition hit = mc.field_71476_x;
        if (hit == null) return true;
        if (hit.field_72313_a == MovingObjectPosition.MovingObjectType.ENTITY) {
            return hit.field_72308_g == null || hit.field_72308_g.getClass() != EntityVillager.class;
        }
        return hit.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK
                || PlayerUtils.isSneakKeyDown() || !BlockUtils.isInteractableAt(hit.func_178782_a());
    }

    public void beforeSync() {
        if (world == null) return;
        if (!sameSession() || !owner.isBlinkSelected()) {
            reset(true);
        } else if (qualified) {
            qualified = false;
        } else {
            resetCycle();
        }
    }

    public void beginInput() {
        if (inputScope) endInput();
        if (!owner.isBlinkSelected() || mc.field_71439_g == null || mc.field_71441_e == null
                || mc.field_71442_b == null) {
            reset(true);
            return;
        }
        if (!sameSession()) reset(false);
        world = mc.field_71441_e;
        connection = connection();
        inputScope = true;
        physicalUse = mc.field_71474_y.field_74313_G.func_151470_d() || PlayerUtils.isUseItemKeyDown();
        NoFallModule noFall = (NoFallModule) MyauClient.moduleManager.modules.get(NoFallModule.class);
        if (mc.field_71462_r != null || !mc.field_71415_G || mc.field_71439_g.field_70128_L
                || mc.field_71439_g.func_70115_ae() || otherBufferOwner() || !physicalUse
                || (noFall != null && noFall.isEnabled() && noFall.isPacketModeActive()) || !canUse()) {
            resetCycle();
            return;
        }

        qualified = true;
        if (++useTick == 1) {
            forceHeld = true;
            forcePress = !useRead;
            rearmAfterMotion = true;
        } else {
            buffering = true;
            suppressUse = true;
            if (useTick > owner.blinkTicks.getValue()) useTick = 0;
        }
    }

    public boolean held(KeyBinding key) {
        if (!inputScope || key != mc.field_71474_y.field_74313_G || auraUse()) return key.func_151470_d();
        return !suppressUse && (forceHeld || physicalUse);
    }

    public boolean pressed(KeyBinding key) {
        boolean actual = key.func_151468_f();
        if (!inputScope || key != mc.field_71474_y.field_74313_G || auraUse()) return actual;
        if (suppressUse) return false;
        if (!forcePress) return actual;
        forcePress = false;
        return true;
    }

    public void endInput() {
        if (!inputScope) return;
        useRead = !suppressUse && (forceHeld || physicalUse);
        forceHeld = forcePress = suppressUse = false;
        inputScope = false;
    }

    void postMotion() {
        if (!rearmAfterMotion) return;
        flush();
        buffering = true;
        rearmAfterMotion = false;
    }

    void packet(PacketSendEvent event) {
        if (!buffering || event.isCancelled() || !event.getPacket().getClass().getName()
                .startsWith("net.minecraft.network.play.client.")) return;
        if (!sameSession()) {
            reset(false);
            return;
        }
        if (MyauClient.outgoingPacketBuffer.owner() == OutgoingBufferOwner.AUTO_BLOCK) {
            flush();
            return;
        }
        if (otherBufferOwner()) {
            reset(true);
            return;
        }
        event.setCancelled(true);
        if (packets.isEmpty()) bufferedAt = System.nanoTime();
        packets.addLast(event.getPacket());
        if (packets.size() >= MAX_QUEUED_PACKETS) flush();
    }

    void watchdog() {
        if (world == null) return;
        if (!sameSession() || !owner.isBlinkSelected()) {
            reset(true);
        } else if (!packets.isEmpty()
                && System.nanoTime() - bufferedAt >= 150_000_000L + owner.blinkTicks.getValue() * 50_000_000L) {
            flush();
        }
    }

    private boolean otherBufferOwner() {
        OutgoingBufferOwner buffer = MyauClient.outgoingPacketBuffer.owner();
        return (buffer != OutgoingBufferOwner.NONE && buffer != OutgoingBufferOwner.AUTO_BLOCK)
                || !MyauClient.packetDelayManager.queuedPackets.isEmpty();
    }

    private void flush() {
        buffering = false;
        if (!sameSession()) {
            packets.clear();
            return;
        }
        Packet<?> packet;
        while ((packet = packets.pollFirst()) != null) {
            MyauClient.packetActionTracker.record(packet);
            PacketUtils.sendWithoutSendEvent(packet);
        }
    }

    private void resetCycle() {
        useTick = 0;
        qualified = rearmAfterMotion = false;
        flush();
    }

    void reset(boolean sendPending) {
        if (!sendPending) packets.clear();
        resetCycle();
        world = connection = null;
        inputScope = useRead = physicalUse = forceHeld = forcePress = suppressUse = false;
    }
}
