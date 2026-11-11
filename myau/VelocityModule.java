package myau;

import com.google.common.base.CaseFormat;
import myau.BooleanSetting;
import myau.ChatColor;
import myau.ChatUtils;
import myau.EventHandler;
import myau.IncomingBufferOwner;
import myau.IntegerSetting;
import myau.LivingUpdateEvent;
import myau.LongJumpModule;
import myau.ModeSetting;
import myau.Module;
import myau.MovementUtils;
import myau.MyauClient;
import myau.PacketReceiveEvent;
import myau.PercentSetting;
import myau.PostPlayerUpdateEvent;
import myau.RawPacketReceiveEvent;
import myau.VelocityEvent;
import myau.WorldLoadEvent;
import myau.mixin.AccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;

public class VelocityModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int chanceAccumulator;
    private int delayChanceAccumulator;
    private boolean explosionPending;
    private boolean awaitingDamageStatus = true;
    private boolean jumpPending;
    private boolean bufferingVelocity;
    private boolean reversePending;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"VANILLA", "JUMP", "DELAY", "REVERSE"});
    public final IntegerSetting delayTicks = new IntegerSetting("delay-ticks", 3, 1, 20, this::showStaticDelaySettings);
    public final BooleanSetting dynamicDelay = new BooleanSetting("dynamic-delay", false, this::showDelaySettings);
    public final PercentSetting delayChance = new PercentSetting("delay-chance", 100, this::showDelaySettings);
    public final PercentSetting chance = new PercentSetting("chance", 100);
    public final PercentSetting horizontal = new PercentSetting("horizontal", 0);
    public final PercentSetting vertical = new PercentSetting("vertical", 100);
    public final PercentSetting explosionsHorizontal = new PercentSetting("explosions-horizontal", 100);
    public final PercentSetting explosionsVertical = new PercentSetting("explosions-vertical", 100);
    public final BooleanSetting fakeCheck = new BooleanSetting("fake-check", true);
    public final BooleanSetting debugLog = new BooleanSetting("debug-log", false);

    public VelocityModule() {
        super("Velocity", false, 0);
    }

    private boolean inFluidOrWeb() {
        boolean bl = VelocityModule.minecraft.field_71439_g.func_70090_H();
        return bl || VelocityModule.minecraft.field_71439_g.func_180799_ab() || ((AccessorEntity)VelocityModule.minecraft.field_71439_g).getIsInWeb();
    }

    private boolean showDelaySettings() {
        int n = (Integer)this.mode.getValue();
        return n == 2;
    }

    private boolean showStaticDelaySettings() {
        return this.showDelaySettings() && (Boolean)this.dynamicDelay.getValue() == false;
    }

    private int effectiveDelayTicks() {
        NetworkPlayerInfo networkPlayerInfo;
        if (!((Boolean)this.dynamicDelay.getValue()).booleanValue()) {
            return (Integer)this.delayTicks.getValue();
        }
        NetworkPlayerInfo networkPlayerInfo2 = networkPlayerInfo = minecraft.func_147114_u() == null ? null : minecraft.func_147114_u().func_175102_a(VelocityModule.minecraft.field_71439_g.func_110124_au());
        if (networkPlayerInfo == null) {
            return (Integer)this.delayTicks.getValue();
        }
        int n = (int)Math.ceil((double)networkPlayerInfo.func_178853_c() / 50.0);
        return Math.max(1, Math.min(20, n));
    }

    @EventHandler
    public void onVelocity(VelocityEvent velocityEvent) {
        if (!this.isEnabled() || velocityEvent.isCancelled()) {
            this.explosionPending = false;
            this.awaitingDamageStatus = true;
            return;
        }
        if (this.awaitingDamageStatus && ((Boolean)this.fakeCheck.getValue()).booleanValue()) {
            return;
        }
        this.awaitingDamageStatus = true;
        if (this.explosionPending) {
            this.explosionPending = false;
            if ((Integer)this.explosionsHorizontal.getValue() > 0) {
                velocityEvent.setVelocityX(velocityEvent.getVelocityX() * (double)((Integer)this.explosionsHorizontal.getValue()).intValue() / 100.0);
                velocityEvent.setVelocityZ(velocityEvent.getVelocityZ() * (double)((Integer)this.explosionsHorizontal.getValue()).intValue() / 100.0);
            } else {
                velocityEvent.setVelocityX(VelocityModule.minecraft.field_71439_g.field_70159_w);
                velocityEvent.setVelocityZ(VelocityModule.minecraft.field_71439_g.field_70179_y);
            }
            if ((Integer)this.explosionsVertical.getValue() > 0) {
                velocityEvent.setVelocityY(velocityEvent.getVelocityY() * (double)((Integer)this.explosionsVertical.getValue()).intValue() / 100.0);
            } else {
                velocityEvent.setVelocityY(VelocityModule.minecraft.field_71439_g.field_70181_x);
            }
            return;
        }
        this.chanceAccumulator = this.chanceAccumulator % 100 + (Integer)this.chance.getValue();
        if (this.chanceAccumulator < 100) {
            return;
        }
        this.jumpPending = ((Integer)this.mode.getValue() == 1 || (Integer)this.mode.getValue() == 2) && velocityEvent.getVelocityY() > 0.0;
        this.reversePending = (Integer)this.mode.getValue() == 3;
        boolean bl = this.reversePending;
        if ((Integer)this.horizontal.getValue() > 0) {
            velocityEvent.setVelocityX(velocityEvent.getVelocityX() * (double)((Integer)this.horizontal.getValue()).intValue() / 100.0);
            velocityEvent.setVelocityZ(velocityEvent.getVelocityZ() * (double)((Integer)this.horizontal.getValue()).intValue() / 100.0);
        } else {
            velocityEvent.setVelocityX(VelocityModule.minecraft.field_71439_g.field_70159_w);
            velocityEvent.setVelocityZ(VelocityModule.minecraft.field_71439_g.field_70179_y);
        }
        if ((Integer)this.vertical.getValue() > 0) {
            velocityEvent.setVelocityY(velocityEvent.getVelocityY() * (double)((Integer)this.vertical.getValue()).intValue() / 100.0);
        } else {
            velocityEvent.setVelocityY(VelocityModule.minecraft.field_71439_g.field_70181_x);
        }
    }

    @EventHandler
    public void onPostPlayerUpdate(PostPlayerUpdateEvent postPlayerUpdateEvent) {
        boolean bl = this.bufferingVelocity;
        if (this.bufferingVelocity && (VelocityModule.minecraft.field_71439_g.field_70122_E || this.inFluidOrWeb() || MyauClient.incomingPacketBuffer.bufferedTickCount() >= (long)this.effectiveDelayTicks())) {
            MyauClient.incomingPacketBuffer.setEnabled(false, IncomingBufferOwner.VELOCITY);
            this.bufferingVelocity = false;
        }
        if (bl = this.reversePending) {
            MovementUtils.setHorizontalMotion(MovementUtils.horizontalSpeed(), MovementUtils.movementYaw());
            this.reversePending = false;
        }
    }

    @EventHandler
    public void onLivingUpdate(LivingUpdateEvent livingUpdateEvent) {
        if (!this.jumpPending) {
            return;
        }
        this.jumpPending = false;
        if (!VelocityModule.minecraft.field_71439_g.field_70122_E || !VelocityModule.minecraft.field_71439_g.func_70051_ag() || VelocityModule.minecraft.field_71439_g.func_70644_a(Potion.field_76430_j)) {
            return;
        }
        if (!this.inFluidOrWeb()) {
            VelocityModule.minecraft.field_71439_g.field_71158_b.field_78901_c = true;
        }
    }

    @EventHandler
    public void onRawPacketReceive(RawPacketReceiveEvent rawPacketReceiveEvent) {
        Packet<?> packet = rawPacketReceiveEvent.getPacket();
        if (!(packet instanceof S12PacketEntityVelocity)) {
            return;
        }
        packet = rawPacketReceiveEvent.getPacket();
        S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity)packet;
        int n = s12PacketEntityVelocity.func_149412_c();
        if (n != VelocityModule.minecraft.field_71439_g.func_145782_y() || !this.bufferingVelocity) {
            return;
        }
        MyauClient.incomingPacketBuffer.setEnabled(false, IncomingBufferOwner.VELOCITY);
        this.bufferingVelocity = false;
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
        boolean bl = this.isEnabled();
        if (!bl || packetReceiveEvent.isCancelled()) {
            return;
        }
        if (packetReceiveEvent.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity)packetReceiveEvent.getPacket();
            if (s12PacketEntityVelocity.func_149412_c() == VelocityModule.minecraft.field_71439_g.func_145782_y()) {
                LongJumpModule longJumpModule = (LongJumpModule)MyauClient.moduleManager.modules.get(LongJumpModule.class);
                if (!((Integer)this.mode.getValue() != 2 || this.bufferingVelocity || MyauClient.incomingPacketBuffer.owner() != IncomingBufferOwner.NONE || VelocityModule.minecraft.field_71439_g.field_70122_E || this.inFluidOrWeb() || this.explosionPending || this.awaitingDamageStatus && ((Boolean)this.fakeCheck.getValue()).booleanValue() || longJumpModule.isEnabled() && longJumpModule.isAwaitingLaunchVelocity())) {
                    this.delayChanceAccumulator = this.delayChanceAccumulator % 100 + (Integer)this.delayChance.getValue();
                    if (this.delayChanceAccumulator >= 100) {
                        MyauClient.incomingPacketBuffer.setEnabled(true, IncomingBufferOwner.VELOCITY);
                        MyauClient.incomingPacketBuffer.queuedPackets.offer((Packet<INetHandlerPlayClient>)s12PacketEntityVelocity);
                        packetReceiveEvent.setCancelled(true);
                        this.bufferingVelocity = true;
                        return;
                    }
                }
                if (((Boolean)this.debugLog.getValue()).booleanValue()) {
                    ChatUtils.addFormattedMessage(String.format("%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r", "&7[&bMyau&7] &r", VelocityModule.minecraft.field_71439_g.field_70173_aa, (double)s12PacketEntityVelocity.func_149411_d() / 8000.0, (double)s12PacketEntityVelocity.func_149410_e() / 8000.0, (double)s12PacketEntityVelocity.func_149409_f() / 8000.0));
                }
            }
            return;
        }
        if (packetReceiveEvent.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion s27PacketExplosion = (S27PacketExplosion)packetReceiveEvent.getPacket();
            if (s27PacketExplosion.func_149149_c() != 0.0f || s27PacketExplosion.func_149144_d() != 0.0f || s27PacketExplosion.func_149147_e() != 0.0f) {
                this.explosionPending = true;
                if ((Integer)this.explosionsHorizontal.getValue() == 0 || (Integer)this.explosionsVertical.getValue() == 0) {
                    packetReceiveEvent.setCancelled(true);
                }
                if (((Boolean)this.debugLog.getValue()).booleanValue()) {
                    ChatUtils.addFormattedMessage(String.format("%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r", "&7[&bMyau&7] &r", VelocityModule.minecraft.field_71439_g.field_70173_aa, VelocityModule.minecraft.field_71439_g.field_70159_w + (double)s27PacketExplosion.func_149149_c(), VelocityModule.minecraft.field_71439_g.field_70181_x + (double)s27PacketExplosion.func_149144_d(), VelocityModule.minecraft.field_71439_g.field_70179_y + (double)s27PacketExplosion.func_149147_e()));
                }
            }
            return;
        }
        if (packetReceiveEvent.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus s19PacketEntityStatus = (S19PacketEntityStatus)packetReceiveEvent.getPacket();
            Entity entity = s19PacketEntityStatus.func_149161_a((World)VelocityModule.minecraft.field_71441_e);
            if (entity == null || !entity.equals((Object)VelocityModule.minecraft.field_71439_g)) {
                return;
            }
            if (s19PacketEntityStatus.func_149160_c() == 2) {
                this.awaitingDamageStatus = false;
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoadEvent) {
        this.onDisable();
    }

    @Override
    public void onDisable() {
        this.explosionPending = false;
        this.awaitingDamageStatus = true;
    }

    @Override
    public String[] getHudInfo() {
        int n;
        int n2 = n = ((Integer)this.mode.getValue()).intValue();
        boolean bl = n == 1 || (Integer)this.mode.getValue() == 2;
        boolean bl2 = bl;
        if (bl && (Integer)this.horizontal.getValue() == 100 && (Integer)this.vertical.getValue() == 100) {
            return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
        }
        n2 = 2;
        String[] stringArray = new String[n2];
        stringArray[0] = ChatColor.formatText(String.format((Integer)this.mode.getValue() == 3 ? "&m%d%%&r" : "%d%%", this.horizontal.getValue()));
        stringArray[1] = String.format("%d%%", this.vertical.getValue());
        return stringArray;
    }
}
