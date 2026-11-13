package myau.openb4.util;

import myau.FloatSetting;
import myau.Module;
import myau.openb4.module.ModuleManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public final class BlockBreaker {

    private static final Minecraft mc = Minecraft.func_71410_x();
    public static boolean sending;
    public static C07PacketPlayerDigging.Action lastAction;

    private BlockBreaker() {
    }

    private static Module bedAura() {
        return ModuleManager.get("BedAura");
    }

    private static double range() {
        return ((FloatSetting) ModuleManager.setting(bedAura(), "Range")).getValue();
    }

    public static void swing() {
        PlayerUtils.swing(!((myau.BedNukerModule) bedAura()).swing.getValue(), false);
    }

    public static void abort(BlockPos pos) {
        mc.field_71441_e.func_175715_c(mc.field_71439_g.func_145782_y(), pos, -1);
        if (mc.field_71439_g.field_71075_bZ.field_75098_d) {
            return;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, EnumFacing.DOWN));
        sending = false;
    }

    public static void stop(BlockPos pos) {
        mc.field_71441_e.func_175715_c(mc.field_71439_g.func_145782_y(), pos, -1);
        EnumFacing facing = EnumFacing.UP;
        float[] r = RotationUtils.toBlock(pos, null);
        MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(range(), r[0], r[1]);
        if (hit != null) {
            facing = hit.field_178784_b;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, facing));
        sending = false;
        mc.field_71441_e.func_175718_b(2001, pos, Block.func_176210_f(BlockUtils.getState(pos)));
        swing();
    }

    public static void start(BlockPos pos) {
        if (!PacketUtils.sentSwing) {
            swing();
        }
        EnumFacing facing = EnumFacing.UP;
        float[] r = RotationUtils.toBlock(pos, null);
        MovingObjectPosition hit = PlayerUtils.rayTraceBlocks(range(), r[0], r[1]);
        if (hit != null) {
            facing = hit.field_178784_b;
        }
        sending = true;
        lastAction = C07PacketPlayerDigging.Action.START_DESTROY_BLOCK;
        PacketUtils.send(new C07PacketPlayerDigging(lastAction, pos, facing));
        sending = false;
        if (!mc.field_71439_g.field_71075_bZ.field_75098_d) {
            swing();
        }
    }
}
