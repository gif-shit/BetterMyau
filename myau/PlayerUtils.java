package myau;

import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeHooks;

public final class PlayerUtils {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    private PlayerUtils() {
    }

    public static boolean isJumpKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74314_A.func_151463_i());
    }

    public static boolean isSneakKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i());
    }

    public static boolean isLeftKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74370_x.func_151463_i());
    }

    public static boolean isRightKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74366_z.func_151463_i());
    }

    public static boolean isAttackKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74312_F.func_151463_i());
    }

    public static boolean isUseItemKeyDown() {
        return isGameplayKeyDown(minecraft.field_71474_y.field_74313_G.func_151463_i());
    }

    private static boolean isGameplayKeyDown(int n) {
        return minecraft.field_71462_r == null && InputUtils.isKeyDown(n);
    }

    public static boolean wouldTakeFallDamage(float f) {
        if (minecraft.field_71439_g.field_71075_bZ.field_75101_c || minecraft.field_71439_g.field_71075_bZ.field_75102_a) {
            return false;
        }
        PotionEffect potionEffect = minecraft.field_71439_g.func_70660_b(Potion.field_76430_j);
        float f2 = potionEffect == null ? 0.0f : (float) (potionEffect.func_76458_c() + 1);
        float f3 = minecraft.field_71439_g.field_70143_R;
        if (minecraft.field_71439_g.field_70181_x < -0.67 || !hasCollisionBelow()) {
            f3 -= (float) minecraft.field_71439_g.field_70181_x;
        }
        return MathHelper.func_76123_f((float) (f3 - f - f2)) > 0;
    }

    public static boolean isAirBelow(int n) {
        int n2;
        if (minecraft.field_71439_g.field_71075_bZ.field_75101_c || minecraft.field_71439_g.field_71075_bZ.field_75102_a) {
            return false;
        }
        int n3 = MathHelper.func_76128_c((double) minecraft.field_71439_g.field_70163_u);
        for (int i = 0; i <= n && (n2 = n3 - i) >= 0; ++i) {
            Block block = minecraft.field_71441_e.func_180495_p(new BlockPos(minecraft.field_71439_g.field_70165_t, (double) n2, minecraft.field_71439_g.field_70161_v)).func_177230_c();
            if (block instanceof BlockAir)
                continue;
            return false;
        }
        return true;
    }

    public static boolean isOverVoid() {
        AxisAlignedBB axisAlignedBB = minecraft.field_71439_g.func_174813_aQ().func_72314_b(-1.0E-6, 0.0, -1.0E-6);
        return isBoundsOverVoid(axisAlignedBB);
    }

    public static boolean isBoundsOverVoid(AxisAlignedBB axisAlignedBB) {
        if (minecraft.field_71439_g.func_70090_H() || minecraft.field_71439_g.func_180799_ab()) {
            return false;
        }
        int n = MathHelper.func_76128_c((double) axisAlignedBB.field_72338_b);
        if (n < 0) {
            return true;
        }
        int n2 = MathHelper.func_76128_c((double) axisAlignedBB.field_72340_a);
        int n3 = MathHelper.func_76128_c((double) (axisAlignedBB.field_72336_d + 1.0));
        int n4 = MathHelper.func_76128_c((double) axisAlignedBB.field_72339_c);
        int n5 = MathHelper.func_76128_c((double) (axisAlignedBB.field_72334_f + 1.0));
        for (int i = n2; i < n3; ++i) {
            for (int j = n4; j < n5; ++j) {
                for (int k = n; k >= 0; --k) {
                    if (BlockUtils.isReplaceableAt(new BlockPos(i, k, j)))
                        continue;
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isOffsetCollisionFree(double d, double d2, double d3) {
        AxisAlignedBB axisAlignedBB = minecraft.field_71439_g.func_174813_aQ().func_72317_d(d, d2, d3);
        return minecraft.field_71441_e.func_72945_a((Entity) minecraft.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean isNarrowOffsetCollisionFree(double d, double d2, double d3, float f) {
        double d4 = minecraft.field_71439_g.field_70130_N / -2.0f * (1.0f - f);
        AxisAlignedBB axisAlignedBB = minecraft.field_71439_g.func_174813_aQ().func_72317_d(d, d2, d3).func_72314_b(d4, 0.0, d4);
        return minecraft.field_71441_e.func_72945_a((Entity) minecraft.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean hasCollisionBelow() {
        AxisAlignedBB axisAlignedBB = minecraft.field_71439_g.func_174813_aQ().func_72317_d(0.0, -1.0, 0.0);
        return !minecraft.field_71441_e.func_72945_a((Entity) minecraft.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean hasCollisionAbove() {
        AxisAlignedBB axisAlignedBB = minecraft.field_71439_g.func_174813_aQ().func_72317_d(0.0, 1.0, 0.0);
        return !minecraft.field_71441_e.func_72945_a((Entity) minecraft.field_71439_g, axisAlignedBB).isEmpty();
    }

    public static boolean isBlockWithinReach(BlockPos blockPos, double d) {
        return isBlockWithinReachFrom(blockPos, minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e(), minecraft.field_71439_g.field_70161_v, d);
    }

    public static boolean isBlockWithinReachFrom(BlockPos blockPos, double d, double d2, double d3, double d4) {
        return blockPos.func_177957_d(d, d2, d3) < d4 * d4;
    }

    public static void attackLocally(Entity entity) {
        if (!ForgeHooks.onPlayerAttackTarget((EntityPlayer) minecraft.field_71439_g, (Entity) entity)
                || !entity.func_70075_an()
                || entity.func_85031_j((Entity) minecraft.field_71439_g)) {
            return;
        }
        float f = (float) minecraft.field_71439_g.func_110148_a(SharedMonsterAttributes.field_111264_e).func_111126_e();
        ItemStack itemStack = minecraft.field_71439_g.func_70694_bm();
        EnumCreatureAttribute enumCreatureAttribute = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).func_70668_bt() : EnumCreatureAttribute.UNDEFINED;
        float f2 = EnchantmentHelper.func_152377_a((ItemStack) itemStack, (EnumCreatureAttribute) enumCreatureAttribute);
        int n = EnchantmentHelper.func_77501_a((EntityLivingBase) minecraft.field_71439_g);
        if (minecraft.field_71439_g.func_70051_ag()) {
            ++n;
        }
        if (f <= 0.0f && f2 <= 0.0f) {
            return;
        }
        boolean bl = isCriticalAttack();
        if (bl && f > 0.0f) {
            f *= 1.5f;
        }
        float f3 = f + f2;
        int n2 = EnchantmentHelper.func_90036_a((EntityLivingBase) minecraft.field_71439_g);
        boolean bl2 = false;
        if (entity instanceof EntityLivingBase && n2 > 0 && !entity.func_70027_ad()) {
            bl2 = true;
            entity.func_70015_d(1);
        }
        double d = entity.field_70159_w;
        double d2 = entity.field_70181_x;
        double d3 = entity.field_70179_y;
        boolean bl3 = entity.func_70097_a(DamageSource.func_76365_a((EntityPlayer) minecraft.field_71439_g), f3);
        if (!bl3) {
            if (bl2) {
                entity.func_70066_B();
            }
            return;
        }
        if (n > 0) {
            entity.func_70024_g((double) (-MathHelper.func_76126_a((float) (minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0f)) * (float) n * 0.5f), 0.1, (double) (MathHelper.func_76134_b((float) (minecraft.field_71439_g.field_70177_z * (float) Math.PI / 180.0f)) * (float) n * 0.5f));
            applyPostHitSprintSlowdown(entity);
        }
        if (entity instanceof EntityPlayerMP && entity.field_70133_I) {
            ((EntityPlayerMP) entity).field_71135_a.func_147359_a((Packet) new S12PacketEntityVelocity(entity));
            entity.field_70133_I = false;
            entity.field_70159_w = d;
            entity.field_70181_x = d2;
            entity.field_70179_y = d3;
        }
        if (bl) {
            minecraft.field_71439_g.func_71009_b(entity);
        }
        if (f2 > 0.0f) {
            minecraft.field_71439_g.func_71047_c(entity);
        }
        if (f3 >= 18.0f) {
            minecraft.field_71439_g.func_71029_a((StatBase) AchievementList.field_75999_E);
        }
        minecraft.field_71439_g.func_130011_c(entity);
        if (entity instanceof EntityLivingBase) {
            EnchantmentHelper.func_151384_a((EntityLivingBase) ((EntityLivingBase) entity), (Entity) minecraft.field_71439_g);
        }
        EnchantmentHelper.func_151385_b((EntityLivingBase) minecraft.field_71439_g, (Entity) entity);
        if (entity instanceof EntityLivingBase) {
            minecraft.field_71439_g.func_71064_a(StatList.field_75951_w, Math.round(f3 * 10.0f));
            if (n2 > 0) {
                entity.func_70015_d(n2 * 4);
            }
        }
        minecraft.field_71439_g.func_71020_j(0.3f);
    }

    private static boolean isCriticalAttack() {
        return minecraft.field_71439_g.field_70143_R > 0.0f
                && !minecraft.field_71439_g.field_70122_E
                && !minecraft.field_71439_g.func_70617_f_()
                && !minecraft.field_71439_g.func_70090_H()
                && !minecraft.field_71439_g.func_70644_a(Potion.field_76440_q)
                && minecraft.field_71439_g.field_70154_o == null;
    }

    private static void applyPostHitSprintSlowdown(Entity entity) {
        KeepSprintModule keepSprintModule = (KeepSprintModule) MyauClient.moduleManager.modules.get(KeepSprintModule.class);
        boolean bl = keepSprintModule != null && keepSprintModule.isEnabled() && !keepSprintModule.watchdog.getValue();
        boolean bl2 = bl;
        if (bl && keepSprintModule.groundOnly.getValue() && !minecraft.field_71439_g.field_70122_E) {
            bl = false;
        }
        if (bl && keepSprintModule.reachOnly.getValue() && RotationUtils.distanceToEntity(entity) <= 3.0) {
            bl = false;
        }
        if (bl) {
            double d = 0.6 + 0.4 * (1.0 - keepSprintModule.slowdown.getValue().doubleValue() / 100.0);
            minecraft.field_71439_g.field_70159_w *= d;
            minecraft.field_71439_g.field_70179_y *= d;
        } else {
            minecraft.field_71439_g.field_70159_w *= 0.6;
            minecraft.field_71439_g.field_70179_y *= 0.6;
            minecraft.field_71439_g.func_70031_b(false);
        }
    }

    public static float estimateAttackDamage(Entity entity) {
        Multimap modifiers;
        ItemStack heldItem = minecraft.field_71439_g.func_70694_bm();
        float damage = 1.0f;
        if (heldItem != null) {
            modifiers = heldItem.func_111283_C();
            String attributeName = SharedMonsterAttributes.field_111264_e.func_111108_a();
            for (Object modifier : modifiers.get(attributeName)) {
                damage += (float) ((AttributeModifier) modifier).func_111164_d();
            }
        }
        EnumCreatureAttribute creature = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).func_70668_bt() : EnumCreatureAttribute.UNDEFINED;
        float enchantmentDamage = EnchantmentHelper.func_152377_a((ItemStack) heldItem, creature);
        if (damage <= 0.0f && enchantmentDamage <= 0.0f) {
            return 0.0f;
        }
        if (isCriticalAttack() && damage > 0.0f) {
            damage *= 1.5f;
        }
        return damage + enchantmentDamage;
    }

    public static int swingDuration() {
        if (minecraft.field_71439_g.func_70644_a(Potion.field_76422_e)) {
            return 6 - (1 + minecraft.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c());
        }
        if (minecraft.field_71439_g.func_70644_a(Potion.field_76419_f)) {
            return 6 + (1 + minecraft.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) * 2;
        }
        return 6;
    }

    public static void swingLocally() {
        if (minecraft.field_71439_g.field_82175_bq
                && minecraft.field_71439_g.field_110158_av < swingDuration() / 2
                && minecraft.field_71439_g.field_110158_av >= 0) {
            return;
        }
        minecraft.field_71439_g.field_110158_av = -1;
        minecraft.field_71439_g.field_82175_bq = true;
    }
}
