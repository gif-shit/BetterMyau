package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;

public class WTapModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ElapsedTimer attackCooldownTimer = new ElapsedTimer();
    private int chanceAccumulator = 0;
    private boolean wTapActive = false;
    private boolean forwardSuppressionStarted = false;
    private long delayRemainingMillis = 0L;
    private long suppressionRemainingMillis = 0L;
    public final FloatSetting delay = new FloatSetting("delay", 5.5f, 0.0f, 10.0f);
    public final FloatSetting duration = new FloatSetting("duration", 1.5f, 1.0f, 5.0f);
    public final PercentSetting chance = new PercentSetting("chance", 100);

    public WTapModule() {
        super("WTap", false, 0);
    }

    private boolean canContinueWTap() {
        if (minecraft.field_71439_g.field_71158_b.field_78900_b < 0.8f || minecraft.field_71439_g.field_70123_F) {
            return false;
        }
        if ((float) minecraft.field_71439_g.func_71024_bL().func_75116_a() <= 6.0f
                && !minecraft.field_71439_g.field_71075_bZ.field_75101_c) {
            return false;
        }
        if (minecraft.field_71439_g.func_70093_af()) {
            return true;
        }
        if (minecraft.field_71439_g.func_71039_bw() || minecraft.field_71439_g.func_70644_a(Potion.field_76440_q)) {
            return false;
        }
        return minecraft.field_71474_y.field_151444_V.func_151470_d();
    }

    @EventHandler(priority = -100)
    public void onMovementInput(MovementInputEvent event) {
        if (!this.wTapActive) {
            return;
        }
        if (!this.forwardSuppressionStarted && !this.canContinueWTap()) {
            this.wTapActive = false;
            while (this.delayRemainingMillis > 0L) {
                this.delayRemainingMillis -= 50L;
            }
            while (this.suppressionRemainingMillis > 0L) {
                this.suppressionRemainingMillis -= 50L;
            }
            return;
        }
        if (this.delayRemainingMillis > 0L) {
            this.delayRemainingMillis -= 50L;
            return;
        }
        if (this.suppressionRemainingMillis > 0L) {
            this.suppressionRemainingMillis -= 50L;
            this.forwardSuppressionStarted = true;
            minecraft.field_71439_g.field_71158_b.field_78900_b = 0.0f;
        }
        if (this.suppressionRemainingMillis <= 0L) {
            this.wTapActive = false;
        }
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !(event.getPacket() instanceof C02PacketUseEntity)) {
            return;
        }
        C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
        if (packet.func_149565_c() != C02PacketUseEntity.Action.ATTACK
                || this.wTapActive
                || !this.attackCooldownTimer.hasElapsed(500L)
                || !minecraft.field_71439_g.func_70051_ag()) {
            return;
        }
        this.attackCooldownTimer.reset();
        this.chanceAccumulator = this.chanceAccumulator % 100 + this.chance.getValue();
        if (this.chanceAccumulator < 100) {
            return;
        }
        this.wTapActive = true;
        this.forwardSuppressionStarted = false;
        this.delayRemainingMillis += (long) (50.0f * this.delay.getValue());
        this.suppressionRemainingMillis += (long) (50.0f * this.duration.getValue());
    }
}
