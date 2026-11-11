package myau;

import com.google.common.base.CaseFormat;
import myau.EventHandler;
import myau.FloatSetting;
import myau.KeyPressEvent;
import myau.LongJumpModule;
import myau.ModeSetting;
import myau.Module;
import myau.MyauClient;
import myau.OutgoingBufferOwner;
import myau.PlayerUtils;
import myau.PreWalkingUpdateEvent;
import myau.RandomUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;

public class AntiVoidModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean overVoid = false;
    private boolean previouslyOverVoid = false;
    private double[] lastSafePosition = null;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"BLINK"});
    public final FloatSetting distance = new FloatSetting("distance", Float.valueOf(5.0f), Float.valueOf(0.0f), Float.valueOf(16.0f));

    public AntiVoidModule() {
        super("AntiVoid", false, 0);
    }

    private void releaseBufferAndClearSafePosition() {
        MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.ANTI_VOID);
        this.lastSafePosition = null;
    }

    private boolean canStartBuffering() {
        LongJumpModule longJump = (LongJumpModule)MyauClient.moduleManager.modules.get(LongJumpModule.class);
        return !longJump.hasJumpStarted();
    }

    @EventHandler
    public void onPreWalkingUpdate(PreWalkingUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        boolean bl = this.overVoid = !AntiVoidModule.minecraft.field_71439_g.field_71075_bZ.field_75101_c && PlayerUtils.isOverVoid();
        if ((Integer)this.mode.getValue() == 0) {
            float playerHeight;
            float halfWidth;
            AxisAlignedBB savedBounds;
            if (!this.overVoid) {
                this.releaseBufferAndClearSafePosition();
            }
            if (this.lastSafePosition != null && PlayerUtils.isBoundsOverVoid(savedBounds = new AxisAlignedBB(this.lastSafePosition[0] - (double)(halfWidth = AntiVoidModule.minecraft.field_71439_g.field_70130_N / 2.0f), this.lastSafePosition[1], this.lastSafePosition[2] - (double)halfWidth, this.lastSafePosition[0] + (double)halfWidth, this.lastSafePosition[1] + (double)(playerHeight = AntiVoidModule.minecraft.field_71439_g.field_70131_O), this.lastSafePosition[2] + (double)halfWidth))) {
                this.releaseBufferAndClearSafePosition();
            }
            if (!this.previouslyOverVoid && this.overVoid && this.canStartBuffering()) {
                MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.AUTO_BLOCK);
                if (MyauClient.outgoingPacketBuffer.setEnabled(true, OutgoingBufferOwner.ANTI_VOID)) {
                    this.lastSafePosition = new double[]{AntiVoidModule.minecraft.field_71439_g.field_70169_q, AntiVoidModule.minecraft.field_71439_g.field_70167_r, AntiVoidModule.minecraft.field_71439_g.field_70166_s};
                }
            }
            if (MyauClient.outgoingPacketBuffer.owner() == OutgoingBufferOwner.ANTI_VOID && this.lastSafePosition != null && this.lastSafePosition[1] - (double)((Float)this.distance.getValue()).floatValue() > AntiVoidModule.minecraft.field_71439_g.field_70163_u) {
                MyauClient.outgoingPacketBuffer.queuedPackets.offerFirst((Packet<?>)new C03PacketPlayer.C04PacketPlayerPosition(this.lastSafePosition[0], this.lastSafePosition[1] - RandomUtils.randomDouble(10.0, 20.0), this.lastSafePosition[2], false));
                this.releaseBufferAndClearSafePosition();
            }
        }
        this.previouslyOverVoid = this.overVoid;
    }

    @EventHandler
    public void onKeyPress(KeyPressEvent event) {
        if (event.getKeyCode() != AntiVoidModule.minecraft.field_71474_y.field_74313_G.func_151463_i()) {
            return;
        }
        ItemStack selectedStack = AntiVoidModule.minecraft.field_71439_g.field_71071_by.func_70448_g();
        if (selectedStack != null && selectedStack.func_77973_b() instanceof ItemEnderPearl) {
            this.releaseBufferAndClearSafePosition();
        }
    }

    @Override
    public void onEnable() {
        this.overVoid = false;
        this.previouslyOverVoid = false;
        this.releaseBufferAndClearSafePosition();
    }

    @Override
    public void onDisable() {
        MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.ANTI_VOID);
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
