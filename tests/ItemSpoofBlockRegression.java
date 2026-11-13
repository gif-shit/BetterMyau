package myau;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import myau.mixin.AccessorEntityPlayer;
import myau.mixin.MixinEntityRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;

public final class ItemSpoofBlockRegression {
    private static int checks;
    private static class Renderer extends MixinEntityRenderer { }
    private static class Screen extends GuiScreen { }
    private static class Player extends EntityPlayerSP implements AccessorEntityPlayer {
        private ItemStack use;
        private int count;
        Player() { super(null, null, null, null); }
        public ItemStack getItemInUse() { return use; }
        public void setItemInUse(ItemStack stack) { use = stack; }
        public int getItemInUseCount() { return count; }
        public void setItemInUseCount(int ticks) { count = ticks; }
        public ItemStack func_71011_bu() { return use; }
        public int func_71052_bv() { return count; }
    }

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    private static void set(Object owner, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static Method hook(String name) throws Exception {
        Method method = MixinEntityRenderer.class.getDeclaredMethod(name, float.class, long.class, CallbackInfo.class);
        method.setAccessible(true);
        return method;
    }

    private static void frame(Renderer renderer, Player player, boolean blocking, String message) throws Exception {
        ItemStack realUse = player.getItemInUse();
        int realCount = player.getItemInUseCount();
        int realSlot = player.field_71071_by.field_70461_c;
        hook("beforeUpdateCameraAndRender").invoke(renderer, 0f, 0L, null);
        if (blocking) {
            check(player.func_71052_bv() > 0, message + " supplies the renderer use count");
            check(player.func_71011_bu() == player.func_70694_bm(), message + " uses the displayed stack");
            check(player.func_71011_bu().func_77975_n() == EnumAction.BLOCK, message + " selects the sword block pose");
        } else {
            check(player.func_71011_bu() == realUse && player.func_71052_bv() == realCount, message + " leaves item use unchanged");
        }
        hook("afterUpdateCameraAndRender").invoke(renderer, 0f, 0L, null);
        check(player.field_71071_by.field_70461_c == realSlot, message + " restores the real held slot");
        check(player.getItemInUse() == realUse && player.getItemInUseCount() == realCount, message + " restores the real use state");
    }

    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Unsafe unsafe = (Unsafe) access.get(null);
        Field registered = Bootstrap.class.getDeclaredField("field_151355_a");
        registered.setAccessible(true);
        registered.setBoolean(null, true);
        Block.func_149671_p();
        Minecraft mc = Minecraft.func_71410_x();
        Player player = (Player) unsafe.allocateInstance(Player.class);
        player.field_71071_by = new InventoryPlayer(player);
        ItemStack sword = new ItemStack(new ItemSword(Item.ToolMaterial.IRON));
        ItemStack blocks = new ItemStack(new ItemBlock(Blocks.field_150348_b), 64);
        player.field_71071_by.field_70462_a[0] = sword;
        player.field_71071_by.field_70462_a[1] = blocks;
        mc.field_71439_g = player;
        mc.field_71474_y = (GameSettings) unsafe.allocateInstance(GameSettings.class);
        mc.field_71474_y.field_74313_G = new KeyBinding("use", -99, "test");
        mc.field_71415_G = true;
        MyauClient.moduleManager.modules.clear();
        ScaffoldModule scaffold = new ScaffoldModule();
        KillAuraModule aura = new KillAuraModule();
        MyauClient.moduleManager.modules.put(ScaffoldModule.class, scaffold);
        MyauClient.moduleManager.modules.put(KillAuraModule.class, aura);
        scaffold.itemSpoof.setValue(true);
        scaffold.setEnabled(true);
        player.field_71071_by.field_70461_c = 1;
        Renderer renderer = new Renderer();
        set(renderer, MixinEntityRenderer.class, "field_78531_r", mc);
        final int[] packets = {0};
        MyauClient.eventBus.register(new Object() {
            @EventHandler public void packet(PacketSendEvent event) { ++packets[0]; }
        });
        InputUtils.keys.add(-99);
        frame(renderer, player, true, "right-click with a spoofed sword");
        player.setItemInUse(blocks);
        player.setItemInUseCount(7);
        frame(renderer, player, true, "an existing item-use state");
        player.setItemInUse(null);
        player.setItemInUseCount(0);
        InputUtils.keys.clear();
        frame(renderer, player, false, "releasing right-click");
        InputUtils.keys.add(-99);
        mc.field_71462_r = new Screen();
        frame(renderer, player, false, "a GUI click");
        mc.field_71462_r = null;
        mc.field_71415_G = false;
        frame(renderer, player, false, "an unfocused game");
        mc.field_71415_G = true;
        scaffold.itemSpoof.setValue(false);
        frame(renderer, player, false, "disabled item spoof");
        scaffold.itemSpoof.setValue(true);
        player.field_71071_by.field_70462_a[0] = blocks;
        frame(renderer, player, false, "a displayed block");
        player.field_71071_by.field_70462_a[0] = null;
        frame(renderer, player, false, "an empty display slot");
        player.field_71071_by.field_70462_a[0] = sword;
        scaffold.setEnabled(false);
        player.field_71071_by.field_70461_c = 1;
        frame(renderer, player, false, "disabled Scaffold");
        player.field_71071_by.field_70461_c = 0;
        InputUtils.keys.clear();
        set(aura, Module.class, "enabled", true);
        set(aura, KillAuraModule.class, "renderBlocking", true);
        frame(renderer, player, true, "existing KillAura block rendering");
        check(packets[0] == 0 && mc.handler == null, "render-only blocking never sends a packet or needs a connection");
        System.out.println("Item-spoof visual block checks passed: " + checks);
    }
}
