package myau;

import com.google.common.base.CaseFormat;
import myau.ChatUtils;
import myau.ElapsedTimer;
import myau.EventHandler;
import myau.FloatSetting;
import myau.IntegerSetting;
import myau.ModeSetting;
import myau.Module;
import myau.MyauClient;
import myau.OutgoingBufferOwner;
import myau.PacketReceiveEvent;
import myau.PacketSendEvent;
import myau.PacketUtils;
import myau.PlayerUtils;
import myau.PreTickEvent;
import myau.ScoreboardUtils;
import myau.mixin.AccessorC03PacketPlayer;
import myau.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;

public class NoFallModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final int MAX_DELAY_MILLIS = 10000;
    private static final long ARM_DELAY_MILLIS = 3000L;
    private final ElapsedTimer actionDelayTimer = new ElapsedTimer();
    private final ElapsedTimer armDelayTimer = new ElapsedTimer();
    private boolean packetModeActive = false;
    private boolean blinkReady = false;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"PACKET", "BLINK", "NO_GROUND", "SPOOF"});
    public final FloatSetting distance = new FloatSetting("distance", Float.valueOf(3.0f), Float.valueOf(0.0f), Float.valueOf(20.0f));
    public final IntegerSetting delay = new IntegerSetting("delay", 0, 0, 10000);

    public NoFallModule() {
        super("NoFall", false, 0);
    }

    private boolean isActionReady() {
        return this.armDelayTimer.hasElapsed(3000L) && this.actionDelayTimer.hasElapsed(((Integer)this.delay.getValue()).longValue());
    }

    public boolean isPacketModeActive() {
        return this.packetModeActive;
    }

    @EventHandler(priority=50)
    public void onPacketSend(PacketSendEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
        switch ((Integer)this.mode.getValue()) {
            case 0: {
                this.handlePacketMode(packet);
                return;
            }
            case 1: {
                this.handleBlinkMode(packet);
                return;
            }
            case 2: {
                ((AccessorC03PacketPlayer)packet).setOnGround(false);
                return;
            }
            case 3: {
                this.handleSpoofMode(packet);
                return;
            }
        }
    }

    private void handlePacketMode(C03PacketPlayer packet) {
        if (this.packetModeActive) {
            this.packetModeActive = false;
            ((AccessorMinecraft)NoFallModule.minecraft).getTimer().field_74278_d = 1.0f;
            return;
        }
        if (packet.func_149465_i()) {
            return;
        }
        AxisAlignedBB bounds = NoFallModule.minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
        if (!PlayerUtils.wouldTakeFallDamage(((Float)this.distance.getValue()).floatValue()) || PlayerUtils.isBoundsOverVoid(bounds) || !this.isActionReady()) {
            return;
        }
        this.actionDelayTimer.reset();
        this.packetModeActive = true;
        ((AccessorMinecraft)NoFallModule.minecraft).getTimer().field_74278_d = 0.5f;
    }

    private void handleBlinkMode(C03PacketPlayer packet) {
        boolean playerSafe;
        boolean bl = playerSafe = !NoFallModule.minecraft.field_71439_g.func_70617_f_() && !NoFallModule.minecraft.field_71439_g.field_71075_bZ.field_75101_c && NoFallModule.minecraft.field_71439_g.field_70737_aN == 0;
        if (MyauClient.outgoingPacketBuffer.owner() != OutgoingBufferOwner.NO_FALL) {
            if (this.blinkReady && !packet.func_149465_i() && playerSafe && PlayerUtils.isAirBelow(((Float)this.distance.getValue()).intValue()) && NoFallModule.minecraft.field_71439_g.field_70181_x < 0.0) {
                MyauClient.outgoingPacketBuffer.setEnabled(false, MyauClient.outgoingPacketBuffer.owner());
                MyauClient.outgoingPacketBuffer.setEnabled(true, OutgoingBufferOwner.NO_FALL);
            }
        } else if (!playerSafe) {
            MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.NO_FALL);
            ChatUtils.addFormattedMessage(String.format("%s%s: &cFailed player check!&r", "&7[&bMyau&7] &r", this.getName()));
        } else if (PlayerUtils.isBoundsOverVoid(NoFallModule.minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0))) {
            MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.NO_FALL);
            ChatUtils.addFormattedMessage(String.format("%s%s: &cFailed void check!&r", "&7[&bMyau&7] &r", this.getName()));
        } else if (packet.func_149465_i()) {
            for (Packet<?> queuedPacket : MyauClient.outgoingPacketBuffer.queuedPackets) {
                if (!(queuedPacket instanceof C03PacketPlayer)) continue;
                ((AccessorC03PacketPlayer)queuedPacket).setOnGround(true);
            }
            MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.NO_FALL);
            this.actionDelayTimer.reset();
        }
        this.blinkReady = packet.func_149465_i() && playerSafe && this.isActionReady();
    }

    private void handleSpoofMode(C03PacketPlayer packet) {
        if (packet.func_149465_i()) {
            return;
        }
        AxisAlignedBB bounds = NoFallModule.minecraft.field_71439_g.func_174813_aQ().func_72314_b(2.0, 0.0, 2.0);
        if (!PlayerUtils.wouldTakeFallDamage(((Float)this.distance.getValue()).floatValue()) || PlayerUtils.isBoundsOverVoid(bounds) || !this.isActionReady()) {
            return;
        }
        this.actionDelayTimer.reset();
        ((AccessorC03PacketPlayer)packet).setOnGround(true);
        NoFallModule.minecraft.field_71439_g.field_70143_R = 0.0f;
    }

    @EventHandler(priority=50)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            this.onDisable();
        }
    }

    @EventHandler(priority=100)
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (ScoreboardUtils.hasLobbyPlayerCount()) {
            this.armDelayTimer.reset();
        }
        if ((Integer)this.mode.getValue() == 0 && this.packetModeActive) {
            PacketUtils.sendWithoutSendEvent(new C03PacketPlayer(true));
            NoFallModule.minecraft.field_71439_g.field_70143_R = 0.0f;
        }
    }

    @Override
    public void onDisable() {
        this.blinkReady = false;
        MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.NO_FALL);
        if (this.packetModeActive) {
            this.packetModeActive = false;
            ((AccessorMinecraft)NoFallModule.minecraft).getTimer().field_74278_d = 1.0f;
        }
    }

    @Override
    public void onSettingChanged(String settingName) {
        if (this.isEnabled()) {
            this.onDisable();
        }
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
    }
}
