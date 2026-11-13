package myau.openb4.util;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public final class ItemUtils {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private ItemUtils() {
    }

    public static boolean is(ItemStack itemStack, Item item) {
        return itemStack != null && itemStack.func_77973_b() == item;
    }

    public static boolean isBlock(ItemStack itemStack, Block block) {
        if (itemStack == null) {
            return false;
        }
        Item item = itemStack.func_77973_b();
        return item instanceof ItemBlock && ((ItemBlock) item).func_179223_d() == block;
    }

    public static boolean isSword(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() instanceof ItemSword;
    }

    public static String name(ItemStack itemStack) {
        return name(itemStack == null ? null : itemStack.func_77973_b());
    }

    public static String name(Item item) {
        return item == null ? "null" : item.getRegistryName().replaceFirst("minecraft:", "");
    }

    public static boolean isPlaceable(Item item) {
        return item instanceof ItemBlock
                || item == Items.field_151059_bz
                || item == Items.field_151063_bx
                || item == Items.field_151104_aV
                || item == Items.field_151107_aW
                || item == Items.field_151132_bS
                || item == Items.field_151144_bL
                || item == Items.field_151160_bD;
    }

    public static boolean isBlock(ItemStack itemStack) {
        return itemStack != null && itemStack.func_77973_b() instanceof ItemBlock;
    }

    public static boolean isScaffoldBlock(ItemStack itemStack) {
        if (BlockUtils.isBlockItem(itemStack)) {
            Block block = ((ItemBlock) itemStack.func_77973_b()).func_179223_d();
            return BlockUtils.isPlaceable(block) && block != Blocks.field_150423_aK;
        }
        return false;
    }
}
