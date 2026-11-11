package myau;

import myau.EventHandler;
import myau.Module;
import myau.PacketReceiveEvent;
import myau.PacketUtils;
import myau.RandomUtils;
import myau.RotationUtils;
import myau.WorldLoadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.MathHelper;

public class NoRotateModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean skipNextCorrection;

    public NoRotateModule() {
        super("NoRotate", false, 0);
    }

    public boolean handleServerPositionAndRotation(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        if (this.skipNextCorrection) {
            this.skipNextCorrection = false;
            return false;
        }
        if (NoRotateModule.minecraft.field_71439_g.field_70177_z == -180.0f && NoRotateModule.minecraft.field_71439_g.field_70125_A == 0.0f) {
            return false;
        }
        float sensitivity = 0.2f + NoRotateModule.minecraft.field_71474_y.field_74341_c * 0.6f;
        float mouseStep = sensitivity * sensitivity * sensitivity * 8.0f * 0.15f;
        float yawJitter = RandomUtils.randomBoolean() ? mouseStep : -mouseStep;
        float localYaw = NoRotateModule.minecraft.field_71439_g.field_70177_z + yawJitter;
        float serverYaw = yaw % 360.0f;
        float restoredYaw = RotationUtils.snapToMouseSensitivity(localYaw, serverYaw);
        float pitchJitter = RandomUtils.randomBoolean() ? mouseStep : -mouseStep;
        float localPitch = NoRotateModule.minecraft.field_71439_g.field_70125_A + pitchJitter;
        float serverPitch = pitch % 360.0f;
        float restoredPitch = MathHelper.func_76131_a((float)RotationUtils.snapToMouseSensitivity(localPitch, serverPitch), (float)-90.0f, (float)90.0f);
        player.func_70080_a(x, y, z, yaw, pitch);
        PacketUtils.send(new C03PacketPlayer.C06PacketPlayerPosLook(NoRotateModule.minecraft.field_71439_g.field_70165_t, NoRotateModule.minecraft.field_71439_g.func_174813_aQ().field_72338_b, NoRotateModule.minecraft.field_71439_g.field_70161_v, serverYaw, serverPitch, false));
        NoRotateModule.minecraft.field_71439_g.field_70177_z = restoredYaw;
        NoRotateModule.minecraft.field_71439_g.field_70125_A = restoredPitch;
        return true;
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !(event.getPacket() instanceof S02PacketChat)) {
            return;
        }
        String message = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d();
        if (message.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy beds.") || message.contains("\u00a7eYou will respawn in \u00a7r\u00a7c1 \u00a7r\u00a7esecond!")) {
            this.skipNextCorrection = true;
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.skipNextCorrection = false;
    }

    @Override
    public void onDisable() {
        this.skipNextCorrection = false;
    }
}
