package myau;

import myau.EventHandler;
import myau.IntegerSetting;
import myau.Module;
import myau.PacketSendEvent;
import myau.PercentSetting;
import myau.PostTickEvent;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

public class SpeedMineModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean startDiggingPending;
    private boolean stopDiggingPending;
    private int chanceAccumulator = 100;
    public final PercentSetting speed = new PercentSetting("speed", 15);
    public final IntegerSetting delay = new IntegerSetting("delay", 0, 0, 5);
    public final PercentSetting chance = new PercentSetting("chance", 50);

    public SpeedMineModule() {
        super("SpeedMine", false, 0);
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.startDiggingPending) {
            this.startDiggingPending = false;
            if (((AccessorPlayerControllerMP)SpeedMineModule.minecraft.field_71442_b).getIsHittingBlock() && this.chanceAccumulator >= 100) {
                float damage = ((AccessorPlayerControllerMP)SpeedMineModule.minecraft.field_71442_b).getCurBlockDamageMP();
                float bonus = 0.3f * (((Integer)this.speed.getValue()).floatValue() / 100.0f);
                ((AccessorPlayerControllerMP)SpeedMineModule.minecraft.field_71442_b).setCurBlockDamageMP(damage + bonus);
            }
        }
        if (!this.stopDiggingPending) {
            return;
        }
        this.stopDiggingPending = false;
        if (((AccessorPlayerControllerMP)SpeedMineModule.minecraft.field_71442_b).getBlockHitDelay() != 5) {
            return;
        }
        this.chanceAccumulator = this.chanceAccumulator % 100 + (Integer)this.chance.getValue();
        if (this.chanceAccumulator >= 100) {
            ((AccessorPlayerControllerMP)SpeedMineModule.minecraft.field_71442_b).setBlockHitDelay((Integer)this.delay.getValue());
        }
    }

    @EventHandler(priority=-100)
    public void onPacketSend(PacketSendEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.isCancelled() || !(event.getPacket() instanceof C07PacketPlayerDigging)) {
            return;
        }
        C07PacketPlayerDigging packet = (C07PacketPlayerDigging)event.getPacket();
        switch (packet.func_180762_c()) {
            case START_DESTROY_BLOCK: {
                this.startDiggingPending = true;
                break;
            }
            case STOP_DESTROY_BLOCK: {
                this.stopDiggingPending = true;
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        this.startDiggingPending = false;
        this.stopDiggingPending = false;
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{String.format("%d%%", this.speed.getValue()), String.format("%s", this.delay.getValue())};
    }
}
