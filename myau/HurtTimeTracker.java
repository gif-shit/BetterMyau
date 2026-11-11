package myau;

import myau.EventHandler;
import myau.PacketReceiveEvent;
import myau.PreTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;

public class HurtTimeTracker {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final int HEALTH_METADATA_ID = 6;
    private boolean healthDecreasePending;
    private int remainingHurtTicks;

    public int remainingHurtTicks() {
        return this.remainingHurtTicks;
    }

    @EventHandler(priority=100)
    public void onPreTick(PreTickEvent event) {
        if (this.remainingHurtTicks > 0) {
            --this.remainingHurtTicks;
        }
        if (this.healthDecreasePending) {
            this.healthDecreasePending = false;
            this.remainingHurtTicks = HurtTimeTracker.minecraft.field_71439_g.field_70737_aN;
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S06PacketUpdateHealth) {
            S06PacketUpdateHealth packet = (S06PacketUpdateHealth)event.getPacket();
            if (packet.func_149332_c() < HurtTimeTracker.minecraft.field_71439_g.func_110143_aJ()) {
                this.healthDecreasePending = true;
            }
            return;
        }
        if (!(event.getPacket() instanceof S1CPacketEntityMetadata)) {
            return;
        }
        S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata)event.getPacket();
        if (packet.func_149375_d() != HurtTimeTracker.minecraft.field_71439_g.func_145782_y()) {
            return;
        }
        for (DataWatcher.WatchableObject metadata : packet.func_149376_c()) {
            if (metadata.func_75672_a() != 6 || !(((Float)metadata.func_75669_b()).floatValue() < HurtTimeTracker.minecraft.field_71439_g.func_110143_aJ())) continue;
            this.healthDecreasePending = true;
            break;
        }
    }
}
