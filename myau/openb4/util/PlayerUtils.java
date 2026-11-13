package myau.openb4.util;

import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

public final class PlayerUtils {

    private static final Minecraft mc = Minecraft.func_71410_x();
    public static boolean attacking;
    public static boolean sendingSlot;
    public static long lastPearl;
    public static long lastFireball;
    public static long lastFireballOnBlock;
    public static long groundTicks;
    public static long airTicks;
    public static int bhopJumpTick;
    public static final double FALL_MOTION = -0.0784000015258789;
    public static final double EPSILON = 1.0E-13;
    public static final double SMALL = 0.001;

    private PlayerUtils() {
    }

    public static Vec3 eyes(double x, double y, double z) {
        return new Vec3(x, y + (double) mc.field_71439_g.func_70047_e(), z);
    }

    public static int potionLevel(Potion potion) {
        PotionEffect effect = mc.field_71439_g.func_70660_b(potion);
        return effect == null ? 0 : effect.func_76458_c() + 1;
    }

    public static int speedLevel() {
        return potionLevel(Potion.field_76424_c);
    }

    public static void setSlot(int slot, boolean sync) {
        mc.field_71439_g.field_71071_by.field_70461_c = slot;
        if (sync) {
            syncCurrentPlayItem();
        }
    }

    public static void syncCurrentPlayItem() {
        ((AccessorPlayerControllerMP) mc.field_71442_b).callSyncCurrentPlayItem();
    }

    public static boolean holdingBlock(Block block) {
        return ItemUtils.isBlock(mc.field_71439_g.func_70694_bm(), block);
    }

    public static boolean holdingBlock() {
        return ItemUtils.isBlock(mc.field_71439_g.func_70694_bm());
    }

    public static void swing(boolean packetOnly, boolean resetEquip) {
        if (packetOnly) {
            PacketUtils.send(new C0APacketAnimation());
        } else {
            mc.field_71439_g.func_71038_i();
            if (resetEquip) {
                mc.func_175597_ag().func_78444_b();
            }
        }
    }

    public static void sendChatMessage(String s) {
        mc.field_71439_g.func_71165_d(s);
    }

    public static double blockReach() {
        return mc.field_71442_b.func_78757_d();
    }

    public static double vanillaReach() {
        return mc.field_71442_b.func_78749_i() ? 6.0 : 3.0;
    }

    public static boolean isCreative() {
        return mc.field_71439_g.field_71075_bZ.field_75098_d;
    }

    public static boolean isAdventure() {
        return !mc.field_71439_g.field_71075_bZ.field_75099_e;
    }

    public static MovingObjectPosition rayTraceFrom(double range, double x, double y, double z, float yaw, float pitch) {
        Vec3 from = eyes(x, y, z);
        Vec3 dir = RotationUtils.getVectorForRotation(yaw, pitch);
        Vec3 to = from.func_72441_c(dir.field_72450_a * range, dir.field_72448_b * range, dir.field_72449_c * range);
        return mc.field_71441_e.func_147447_a(from, to, false, false, false);
    }

    public static MovingObjectPosition rayTraceBlocks(double range, float yaw, float pitch) {
        return rayTraceFrom(range, mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, yaw, pitch);
    }

    public static MovingObjectPosition rayTraceBlock(BlockPos pos, EnumFacing facing, double range, float yaw, float pitch) {
        MovingObjectPosition hit = rayTraceBlocks(range, yaw, pitch);
        if (hit == null || hit.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        if (!BlockUtils.samePos(hit.func_178782_a(), pos) || facing != null && hit.field_178784_b != facing) {
            return null;
        }
        return hit;
    }

    public static void ping(float volume, float pitch) {
        mc.field_71439_g.func_85030_a("note.pling", volume, pitch);
    }

    public static void ping() {
        ping(1.0f, 1.0f);
    }

    public static Slot chestSlotUnderMouse(GuiChest chest, int rows) {
        int n2 = rows / 9;
        int ySize = 114 + n2 * 18;
        int left = (chest.field_146294_l - 176) / 2;
        int top = (chest.field_146295_m - ySize) / 2;
        int mx = Mouse.getEventX() * chest.field_146294_l / mc.field_71443_c;
        int my = chest.field_146295_m - Mouse.getEventY() * chest.field_146295_m / mc.field_71440_d - 1;
        for (Slot slot : mc.field_71439_g.field_71070_bA.field_75151_b) {
            int x = mx - left;
            int y = my - top;
            if (x < slot.field_75223_e - 1
                    || x >= slot.field_75223_e + 16 + 1
                    || y < slot.field_75221_f - 1
                    || y >= slot.field_75221_f + 16 + 1)
                continue;
            return slot;
        }
        return null;
    }
}
