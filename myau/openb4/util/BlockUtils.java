package myau.openb4.util;

import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

public final class BlockUtils {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private BlockUtils() {
    }

    public static float breakSpeed(Block block, ItemStack stack, boolean ignoreSlowdowns) {
        if (block == null) {
            return 0.0f;
        }
        float hardness = block.func_176195_g(mc.field_71441_e, mc.field_71439_g.func_180425_c());
        if (hardness < 0.0f) {
            return 0.0f;
        }
        boolean harvest = block.func_149688_o().func_76229_l() || stack != null && stack.func_150998_b(block);
        if (harvest) {
            return digSpeed(stack, block, ignoreSlowdowns) / hardness / 30.0f;
        }
        return digSpeed(stack, block, ignoreSlowdowns) / hardness / 100.0f;
    }

    public static float digSpeed(ItemStack stack, Block block, boolean ignoreSlowdowns) {
        int n;
        float f = stack == null ? 1.0f : stack.func_77973_b().func_150893_a(stack, block);
        if (f > 1.0f && (n = EnchantmentHelper.func_77506_a(Enchantment.field_77349_p.field_77352_x, stack)) > 0) {
            f += (float) (n * n + 1);
        }
        if (mc.field_71439_g.func_70644_a(Potion.field_76422_e)) {
            f *= 1.0f + (float) (mc.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c() + 1) * 0.2f;
        }
        if (!ignoreSlowdowns) {
            if (mc.field_71439_g.func_70644_a(Potion.field_76419_f)) {
                float f3;
                switch(mc.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) {
                    case 0:
                        f3 = 0.3f;
                        break;
                    case 1:
                        f3 = 0.09f;
                        break;
                    case 2:
                        f3 = 0.0027f;
                        break;
                    default:
                        f3 = 8.1E-4f;
                }
                f *= f3;
            }
            if (mc.field_71439_g.func_70055_a(Material.field_151586_h) && !EnchantmentHelper.func_77510_g(mc.field_71439_g)) {
                f /= 5.0f;
            }
            if (!mc.field_71439_g.field_70122_E) {
                f /= 5.0f;
            }
        }
        return f;
    }

    public static net.minecraft.util.BlockPos offset(int n, net.minecraft.util.BlockPos pos) {
        if (n == 0) {
            return pos.func_177984_a();
        }
        if (n == 1) {
            return pos.func_177977_b();
        }
        if (n == 2) {
            return pos.func_177978_c();
        }
        if (n == 3) {
            return pos.func_177968_d();
        }
        if (n == 4) {
            return pos.func_177974_f();
        }
        return pos.func_177976_e();
    }

    private static final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    public static IBlockState getState(BlockPos pos) {
        return mc.field_71441_e.func_180495_p(pos);
    }

    public static Block getBlock(BlockPos pos) {
        return getState(pos).func_177230_c();
    }

    public static BlockPos pos(double x, double y, double z) {
        return new BlockPos(x, y, z);
    }

    public static Block getBlock(double x, double y, double z) {
        return getBlock(pos(x, y, z));
    }

    public static boolean is(BlockPos pos, Block block) {
        return pos != null && getBlock(pos) == block;
    }

    public static BlockPos below(double d) {
        return new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - d, mc.field_71439_g.field_70161_v);
    }

    public static boolean isReplaceable(BlockPos pos) {
        Block block = getBlock(pos);
        return block == Blocks.field_150350_a
                || block == Blocks.field_150480_ab
                || block instanceof BlockLiquid
                || block.func_149688_o().func_76222_j();
    }

    public static boolean isPlaceable(Block block) {
        return block == Blocks.field_150359_w
                || block.func_149730_j()
                && block != Blocks.field_150351_n
                && block != Blocks.field_150354_m
                && block != Blocks.field_150425_aM
                && block != Blocks.field_150335_W
                && block != Blocks.field_150462_ai
                && block != Blocks.field_150460_al
                && block != Blocks.field_150367_z
                && block != Blocks.field_150409_cd
                && block != Blocks.field_150323_B
                && block != Blocks.field_150483_bI;
    }

    public static boolean isBlockItem(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() instanceof ItemBlock;
    }

    public static boolean isContainer(Block block) {
        return block instanceof BlockContainer || block == Blocks.field_150462_ai || block == Blocks.field_150467_bQ;
    }

    public static boolean isInteractable(Block block) {
        if (isContainer(block) || block == Blocks.field_150324_C || block == Blocks.field_150442_at) {
            return true;
        }
        if (block instanceof BlockTrapDoor || block instanceof BlockFence || block instanceof BlockFenceGate) {
            return true;
        }
        if (!(block instanceof BlockDoor)) {
            return false;
        }
        return block != Blocks.field_150454_av;
    }

    public static Block blockOf(ItemStack itemStack) {
        return isBlockItem(itemStack) ? ((ItemBlock) itemStack.func_77973_b()).func_179223_d() : null;
    }

    public static boolean nextToBed(BlockPos pos) {
        for (int i = 0; i < 6; ++i) {
            if (!is(offset(i, pos), Blocks.field_150324_C))
                continue;
            return true;
        }
        return false;
    }

    public static boolean canPlace(MovingObjectPosition hit, ItemStack itemStack) {
        return ((ItemBlock) itemStack.func_77973_b()).func_179222_a(mc.field_71441_e, hit.func_178782_a(), hit.field_178784_b, (EntityPlayer) mc.field_71439_g, itemStack);
    }

    public static float breakTicks(Block block, ItemStack itemStack, boolean ignoreSlowdowns) {
        float f = breakSpeed(block, itemStack, ignoreSlowdowns);
        if (f <= 0.0f) {
            return 0.0f;
        }
        return 1.0f / f;
    }

    public static boolean isPassable(BlockPos pos) {
        if (isReplaceable(pos)) {
            return true;
        }
        Block block = getBlock(pos);
        if (!block.func_149730_j()) {
            return block instanceof BlockSlab || block == Blocks.field_150431_aC;
        }
        return false;
    }

    public static boolean samePos(BlockPos a, BlockPos b) {
        return a == b
                || a.func_177958_n() == b.func_177958_n()
                && a.func_177956_o() == b.func_177956_o()
                && a.func_177952_p() == b.func_177952_p();
    }

    public static int horizontalDistance(BlockPos a, BlockPos b) {
        return Math.max(Math.abs(a.func_177958_n() - b.func_177958_n()), Math.abs(a.func_177952_p() - b.func_177952_p()));
    }

    public static boolean bedAdjacent(BlockPos pos) {
        int x = pos.func_177958_n();
        int y = pos.func_177956_o();
        int z = pos.func_177952_p();
        return getBlock(scratch.func_181079_c(x + 1, y, z)) == Blocks.field_150324_C
                || getBlock(scratch.func_181079_c(x - 1, y, z)) == Blocks.field_150324_C
                || getBlock(scratch.func_181079_c(x, y, z + 1)) == Blocks.field_150324_C
                || getBlock(scratch.func_181079_c(x, y, z - 1)) == Blocks.field_150324_C
                || getBlock(scratch.func_181079_c(x, y - 1, z)) == Blocks.field_150324_C;
    }

    public static boolean hasSupport(BlockPos pos) {
        for (EnumFacing facing : EnumFacing.field_82609_l) {
            if (!isSupport(getBlock(pos.func_177972_a(facing.func_176734_d()))))
                continue;
            return true;
        }
        return false;
    }

    public static void touching(List<BlockPos> out, Set<BlockPos> solids, Set<BlockPos> candidates, BlockPos center) {
        block0: for (BlockPos p : candidates) {
            if (horizontalDistance(p, center) > 3)
                continue;
            for (EnumFacing facing : EnumFacing.field_82609_l) {
                if (!solids.contains(p.func_177972_a(facing)))
                    continue;
                out.add(p);
                continue block0;
            }
        }
    }

    public static void scan(Set<BlockPos> solids, Set<BlockPos> open, Set<BlockPos> exclude, BlockPos center, int r, int minY, int maxY) {
        int x = center.func_177958_n();
        int y = center.func_177956_o();
        int z = center.func_177952_p();
        for (int i = -r; i <= r; ++i) {
            int ai = Math.abs(i);
            for (int j = -r; j <= r; ++j) {
                if (Math.max(ai, Math.abs(j)) > r)
                    continue;
                for (int k = minY - 1; k <= maxY + 1; ++k) {
                    BlockPos p;
                    scratch.func_181079_c(x + i, y + k, z + j);
                    if (isPlayerBlock(scratch))
                        continue;
                    if (isSupport(getBlock(scratch))) {
                        solids.add(new BlockPos((Vec3i) scratch));
                        continue;
                    }
                    if (k < minY || k > maxY || !isPassable(scratch) || exclude.contains(p = new BlockPos((Vec3i) scratch)))
                        continue;
                    open.add(p);
                }
            }
        }
    }

    public static boolean isSupport(Block block) {
        if (block == null || block == Blocks.field_150350_a) {
            return false;
        }
        if (block instanceof BlockLiquid) {
            return false;
        }
        if (block.func_149688_o().func_76222_j()) {
            return false;
        }
        return !isInteractable(block);
    }

    public static BlockPos playerPos() {
        return new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.field_70163_u), MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
    }

    public static boolean isPlayerBlock(BlockPos pos) {
        BlockPos p = playerPos();
        return pos.equals(p) || pos.equals(p.func_177984_a());
    }

    public static int nearestDistance(BlockPos pos, Set<BlockPos> set) {
        int n = Integer.MAX_VALUE;
        for (BlockPos p : set) {
            int dx = Math.abs(pos.func_177958_n() - p.func_177958_n());
            int dz = Math.abs(pos.func_177952_p() - p.func_177952_p());
            int dy = Math.abs(pos.func_177956_o() - p.func_177956_o());
            int d = Math.max(dx, dz) + dy * 2;
            if (d >= n)
                continue;
            n = d;
        }
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    public static Vec3 hitVec(BlockPos pos, net.minecraft.util.EnumFacing facing) {
        double lo = 0.1, hi = 0.9, ylo = 0.05, yhi = 0.95, top = 1.0;
        double x = (double) pos.func_177958_n() + MathUtils.randomBetween(lo, hi);
        double y = (double) pos.func_177956_o() + MathUtils.randomBetween(ylo, yhi);
        double z = (double) pos.func_177952_p() + MathUtils.randomBetween(lo, hi);
        IBlockState state = getState(pos);
        Block block = state.func_177230_c();
        if (!block.func_149730_j() && block instanceof net.minecraft.block.BlockSlab) {
            if (state.func_177229_b(net.minecraft.block.BlockSlab.field_176554_a) == net.minecraft.block.BlockSlab.EnumBlockHalf.TOP) {
                ylo = 0.505;
            } else if (facing == net.minecraft.util.EnumFacing.UP) {
                top = 0.5;
            } else {
                yhi = 0.495;
            }
            y = (double) pos.func_177956_o() + MathUtils.randomBetween(ylo, yhi);
        }
        switch(facing) {
            case EAST:
                x = pos.func_177958_n() + 1;
                break;
            case WEST:
                x = pos.func_177958_n();
                break;
            case NORTH:
                z = pos.func_177952_p();
                break;
            case SOUTH:
                z = pos.func_177952_p() + 1;
                break;
            case UP:
                y = (double) pos.func_177956_o() + top;
                break;
            case DOWN:
                y = pos.func_177956_o();
                break;
        }
        return new Vec3(x, y, z);
    }
}
