package myau;

import myau.ElapsedTimer;
import myau.EventHandler;
import myau.FloatSetting;
import myau.Module;
import myau.MovementInputEvent;
import myau.PacketSendEvent;
import myau.PercentSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.potion.Potion;

public class WTapModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ElapsedTimer attackCooldownTimer = new ElapsedTimer();
    private int chanceAccumulator = 0;
    private boolean wTapActive = false;
    private boolean forwardSuppressionStarted = false;
    private long delayRemainingMillis = 0L;
    private long suppressionRemainingMillis = 0L;
    public final FloatSetting delay = new FloatSetting("delay", Float.valueOf(5.5f), Float.valueOf(0.0f), Float.valueOf(10.0f));
    public final FloatSetting duration = new FloatSetting("duration", Float.valueOf(1.5f), Float.valueOf(1.0f), Float.valueOf(5.0f));
    public final PercentSetting chance = new PercentSetting("chance", 100);

    public WTapModule() {
        super("WTap", false, 0);
    }

    private boolean canContinueWTap() {
        if (WTapModule.minecraft.field_71439_g.field_71158_b.field_78900_b < 0.8f || WTapModule.minecraft.field_71439_g.field_70123_F) {
            return false;
        }
        if ((float)WTapModule.minecraft.field_71439_g.func_71024_bL().func_75116_a() <= 6.0f && !WTapModule.minecraft.field_71439_g.field_71075_bZ.field_75101_c) {
            return false;
        }
        if (WTapModule.minecraft.field_71439_g.func_70093_af()) {
            return true;
        }
        if (WTapModule.minecraft.field_71439_g.func_71039_bw() || WTapModule.minecraft.field_71439_g.func_70644_a(Potion.field_76440_q)) {
            return false;
        }
        return WTapModule.minecraft.field_71474_y.field_151444_V.func_151470_d();
    }

    @EventHandler(priority=-100)
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
            WTapModule.minecraft.field_71439_g.field_71158_b.field_78900_b = 0.0f;
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
        C02PacketUseEntity packet = (C02PacketUseEntity)event.getPacket();
        if (packet.func_149565_c() != C02PacketUseEntity.Action.ATTACK || this.wTapActive || !this.attackCooldownTimer.hasElapsed(500L) || !WTapModule.minecraft.field_71439_g.func_70051_ag()) {
            return;
        }
        this.attackCooldownTimer.reset();
        this.chanceAccumulator = this.chanceAccumulator % 100 + (Integer)this.chance.getValue();
        if (this.chanceAccumulator < 100) {
            return;
        }
        this.wTapActive = true;
        this.forwardSuppressionStarted = false;
        this.delayRemainingMillis += (long)(50.0f * ((Float)this.delay.getValue()).floatValue());
        this.suppressionRemainingMillis += (long)(50.0f * ((Float)this.duration.getValue()).floatValue());
    }
}
