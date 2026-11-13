package myau;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;

public final class EnchantmentText {

    private static final Map<Integer, EnchantmentLabel> ENCHANTMENT_LABELS = new EnchantmentLabels();

    private EnchantmentText() {
    }

    private static ChatColor colorForLevel(int level, int maximumLevel) {
        if (level > maximumLevel) {
            return ChatColor.LIGHT_PURPLE;
        }
        if (level == maximumLevel) {
            return ChatColor.RED;
        }
        switch(level) {
            case 1:
                {
                    return ChatColor.AQUA;
                }
            case 2:
                {
                    return ChatColor.GREEN;
                }
            case 3:
                {
                    return ChatColor.YELLOW;
                }
            case 4:
                {
                    return ChatColor.GOLD;
                }
        }
        return ChatColor.GRAY;
    }

    public static ArrayList<String> labels(ItemStack stack) {
        NBTTagList enchantments;
        ArrayList<String> labels = new ArrayList<String>();
        NBTTagList nBTTagList = enchantments = stack.func_77973_b() == Items.field_151134_bR ? Items.field_151134_bR.func_92110_g(stack) : stack.func_77986_q();
        if (enchantments == null) {
            return labels;
        }
        for (int index = 0; index < enchantments.func_74745_c(); ++index) {
            int enchantmentId = enchantments.func_150305_b(index).func_74762_e("id");
            EnchantmentLabel label = ENCHANTMENT_LABELS.get(enchantmentId);
            if (label == null)
                continue;
            short level = enchantments.func_150305_b(index).func_74765_d("lvl");
            labels.add(ChatColor.formatText(String.format("&r%s%s%d&r", new Object[] { label.abbreviation, colorForLevel(level, label.maxLevel), level })));
        }
        return labels;
    }
}
