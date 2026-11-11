package myau;

import java.util.ArrayList;
import java.util.Arrays;
import myau.ChatColor;
import myau.ChatUtils;
import myau.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ItemNameCommand
extends Command {
    private static final Minecraft minecraft = Minecraft.func_71410_x();

    public ItemNameCommand() {
        super(new ArrayList<String>(Arrays.asList("itemname", "item")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        ItemStack stack = ItemNameCommand.minecraft.field_71439_g.field_71071_by.func_70448_g();
        if (stack == null) {
            return;
        }
        String displayName = stack.func_82833_r().replace('\u00a7', '&');
        String registryName = stack.func_77973_b().getRegistryName();
        String nbt = stack.func_77942_o() ? stack.func_77978_p().toString().replace('\u00a7', '&') : "";
        ChatUtils.addRawMessage(String.format("%s%s (%s) %s", ChatColor.formatText("&7[&bMyau&7] &r"), displayName, registryName, nbt));
    }
}
