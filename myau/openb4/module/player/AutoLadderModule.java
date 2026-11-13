package myau.openb4.module.player;

import myau.BooleanSetting;
import myau.FloatSetting;
import myau.openb4.module.Module;
import myau.openb4.module.ModuleManager;
import myau.openb4.util.BlockUtils;
import myau.openb4.util.ItemUtils;
import myau.openb4.util.PlayerUtils;
import myau.openb4.util.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class AutoLadderModule extends Module {

    public static AutoLadderModule instance;
    public static final FloatSetting minimumFallDistance = new FloatSetting("minimum-fall-distance", 5.0f, 5.0f, 40.0f, 0.5f);
    public static final BooleanSetting onlyWhileHoldingLadder = new BooleanSetting("only-while-holding-ladder", true);
    private static int previousSlot = -1;
    public static BlockPos target;
    private static EnumFacing direction;
    private static EnumFacing face;
    private static boolean ladderNext;
    private static long placedAt;

    public AutoLadderModule(int key) {
        super("AutoLadder", key);
        instance = this;
    }

    public void link() {
    }

    @Override
    public void onPreUpdate() {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.func_147113_T()
                || mc.field_71439_g.field_71075_bZ.field_75100_b
                || mc.field_71439_g.field_71075_bZ.field_75098_d
                || AutoBlockinModule.active
                || ModuleManager.enabled("Scaffold")) {
            reset();
            return;
        }
        if (mc.field_71439_g.field_70122_E || mc.field_71439_g.func_70617_f_() || (double) mc.field_71439_g.field_70143_R < 1.5) {
            reset();
            return;
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - placedAt) < 1000L) {
            reset();
            return;
        }
        int most = 0;
        int blockSlot = -1;
        int ladderSlot = -1;
        for (int i = 0; i < InventoryPlayer.func_70451_h(); ++i) {
            ItemStack s = mc.field_71439_g.field_71071_by.func_70301_a(i);
            if (ItemUtils.isScaffoldBlock(s)) {
                int n = s.field_77994_a;
                if (n <= most)
                    continue;
                blockSlot = i;
                most = n;
                continue;
            }
            if (s == null
                    || !(s.func_77973_b() instanceof ItemBlock)
                    || ((ItemBlock) s.func_77973_b()).func_179223_d() != Blocks.field_150468_ap)
                continue;
            ladderSlot = i;
        }
        if (blockSlot == -1 || ladderSlot == -1) {
            reset();
            return;
        }
        if (onlyWhileHoldingLadder.getValue() && target == null && mc.field_71439_g.field_71071_by.field_70461_c != ladderSlot) {
            reset();
            return;
        }
        MovingObjectPosition mop = PlayerUtils.rayTraceBlocks(7.0, mc.field_71439_g.field_70177_z, mc.field_71439_g.func_70051_ag() ? 80.0f : 90.0f);
        if (mop == null || mop.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK || mop.field_178784_b != EnumFacing.UP) {
            reset();
            return;
        }
        int slot;
        if (ladderNext) {
            ladderNext = false;
            slot = ladderSlot;
            target = target.func_177984_a();
            face = direction.func_176734_d();
            if (!BlockUtils.isReplaceable(target.func_177972_a(face))) {
                reset();
                return;
            }
        } else {
            previousSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            slot = blockSlot;
            BlockPos ground = mop.func_178782_a();
            if (!BlockUtils.isReplaceable(ground.func_177984_a())) {
                reset();
                return;
            }
            EnumFacing facing = mc.field_71439_g.func_174811_aO();
            EnumFacing[] order = new EnumFacing[] { facing.func_176746_e(), facing.func_176735_f(), facing, facing.func_176734_d() };
            for (EnumFacing f : order) {
                BlockPos wall = ground.func_177972_a(f).func_177984_a();
                if (BlockUtils.isReplaceable(wall) || !mc.field_71441_e.func_180495_p(wall).func_177230_c().func_149730_j())
                    continue;
                slot = ladderSlot;
                target = wall;
                direction = f;
                face = direction.func_176734_d();
                break;
            }
            if (target == null) {
                double x = mc.field_71439_g.field_70165_t;
                double z = mc.field_71439_g.field_70161_v;
                double m = 0.415;
                double m1 = m + 1.0;
                for (EnumFacing f : order) {
                    BlockPos beside = ground.func_177972_a(f);
                    BlockPos above = beside.func_177984_a();
                    if (x > (double) above.func_177958_n() - m
                            && x < (double) above.func_177958_n() + m1
                            && z > (double) above.func_177952_p() - m
                            && z < (double) above.func_177952_p() + m1)
                        continue;
                    if (BlockUtils.isReplaceable(beside) || !BlockUtils.isReplaceable(above))
                        continue;
                    target = beside;
                    direction = f;
                    face = EnumFacing.UP;
                    break;
                }
            }
        }
        if (target == null) {
            reset();
            return;
        }
        if ((double) mc.field_71439_g.field_70143_R < minFall()) {
            return;
        }
        mop = PlayerUtils.rayTraceBlocks(mc.field_71442_b.func_78757_d(), mc.field_71439_g.field_70177_z, 90.0f);
        if (mop != null && mop.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
            mc.field_71439_g.field_71071_by.field_70461_c = slot;
            place(new MovingObjectPosition(MovingObjectPosition.MovingObjectType.BLOCK, BlockUtils.hitVec(target, face), face, target));
            if (slot == blockSlot) {
                ladderNext = true;
            } else {
                placedAt = now;
            }
        }
    }

    @Override
    public void onDisable() {
        reset();
    }

    public static void reset() {
        if (previousSlot != -1 && Minecraft.func_71410_x().field_71439_g != null) {
            Minecraft.func_71410_x().field_71439_g.field_71071_by.field_70461_c = previousSlot;
        }
        previousSlot = -1;
        ladderNext = false;
        target = null;
        direction = null;
        face = null;
    }

    private static double minFall() {
        PotionEffect e = Minecraft.func_71410_x().field_71439_g.func_70660_b(Potion.field_76430_j);
        return minimumFallDistance.getValue() - 1.5 + (double) (e == null ? 0 : e.func_76458_c() + 1);
    }

    @Override
    public float[] getRotations(float lastYaw, float lastPitch) {
        if (target != null) {
            return RotationUtils.toBlockFixed(target, face, lastYaw, lastPitch);
        }
        return null;
    }

    private static void place(MovingObjectPosition m) {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc.field_71442_b.func_178890_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm(), m.func_178782_a(), m.field_178784_b, m.field_72307_f)) {
            PlayerUtils.swing(false, true);
        }
    }
}
