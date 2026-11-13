package myau.modules.pantswap;

import java.util.Objects;
import myau.modules.PantSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

public final class PantSwapMinecraft implements PantSwap.Host {

    private final Minecraft mc;

    public PantSwapMinecraft(Minecraft mc) {
        this.mc = Objects.requireNonNull(mc, "minecraft");
    }

    public boolean ready() {
        return mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71442_b != null;
    }

    public Object worldIdentity() {
        return mc.field_71441_e;
    }

    public Object playerIdentity() {
        return mc.field_71439_g;
    }

    public boolean playerContainerOpen() {
        return ready()
                && mc.field_71439_g.field_71070_bA == mc.field_71439_g.field_71069_bz
                && mc.field_71439_g.field_71069_bz.field_75152_c == 0;
    }

    public boolean inventoryScreenOpen() {
        return mc.field_71462_r instanceof GuiInventory;
    }

    public boolean otherScreenOpen() {
        return mc.field_71462_r != null && !inventoryScreenOpen();
    }

    public boolean survivalOrAdventure() {
        WorldSettings.GameType type = mc.field_71442_b.func_178889_l();
        return type == WorldSettings.GameType.SURVIVAL || type == WorldSettings.GameType.ADVENTURE;
    }

    public Object screenIdentity() {
        return mc.field_71462_r;
    }

    public void openInventory() {
        mc.func_147108_a(new GuiInventory(mc.field_71439_g));
    }

    public void closeInventory() {
        mc.field_71439_g.func_71053_j();
    }

    public Object stack(int slot) {
        return mc.field_71439_g.field_71069_bz.func_75139_a(slot).func_75211_c();
    }

    public Object cursor() {
        return mc.field_71439_g.field_71071_by.func_70445_o();
    }

    public Object copy(Object stack) {
        return stack == null ? null : ((ItemStack) stack).func_77946_l();
    }

    public boolean equal(Object first, Object second) {
        return ItemStack.func_77989_b((ItemStack) first, (ItemStack) second);
    }

    public boolean leggings(Object value) {
        if (!(value instanceof ItemStack))
            return false;
        ItemStack stack = (ItemStack) value;
        return stack.field_77994_a == 1
                && stack.func_77973_b() instanceof ItemArmor
                && ((ItemArmor) stack.func_77973_b()).field_77881_a == 2;
    }

    public boolean canEquipLeggings(Object value) {
        return leggings(value) && mc.field_71439_g.field_71069_bz.func_75139_a(7).func_75214_a((ItemStack) value);
    }

    public long nowMillis() {
        return System.currentTimeMillis();
    }

    public void click(int slot) {
        mc.field_71442_b.func_78753_a(0, slot, 0, 0, mc.field_71439_g);
    }

    public void message(String text) {
        if (mc.field_71439_g != null)
            mc.field_71439_g.func_145747_a(new ChatComponentText("PantSwap: " + text));
    }
}
