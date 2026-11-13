package myau.modules.pit;

import java.util.*;
import myau.modules.Pit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldSettings;

public final class PitMinecraft implements Pit.Host {

    public static final class Details {

        public final String name, enchants;
        public final int categories;

        public Details(String name, String enchants, int categories) {
            this.name = Objects.requireNonNull(name, "name");
            this.enchants = Objects.requireNonNull(enchants, "enchants");
            this.categories = categories;
        }
    }

    public interface Bindings {

        boolean inPit();

        boolean hypixel();

        boolean inSpawn(Entity entity, boolean pitOnly);

        boolean spawnHeight(double y, boolean pitOnly);

        Details playerDetails(EntityPlayer player);

        void updateDenicker(boolean enabled, boolean notifications, boolean pitOnly);

        void updateEventSchedule(boolean enabled, boolean pitOnly);

        List<Pit.Event> events();

        boolean eventsLoading();

        boolean hudShadow();

        int hudColor(long millis, int row);

        int hudBackgroundArgb();

        boolean hudSidebar();
    }

    private final Minecraft mc;
    private final Bindings bindings;

    public PitMinecraft(Minecraft mc, Bindings bindings) {
        this.mc = Objects.requireNonNull(mc, "minecraft");
        this.bindings = Objects.requireNonNull(bindings, "bindings");
    }

    public boolean ready() {
        return mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71442_b != null;
    }

    public Object worldIdentity() {
        return mc.field_71441_e;
    }

    public int playerTick() {
        return mc.field_71439_g.field_70173_aa;
    }

    public String localName() {
        return mc.field_71439_g.func_70005_c_();
    }

    public boolean inPit() {
        return bindings.inPit();
    }

    public boolean hypixel() {
        return bindings.hypixel();
    }

    public boolean localInSpawn(boolean pitOnly) {
        return bindings.inSpawn(mc.field_71439_g, pitOnly);
    }

    public boolean spawnHeight(double y, boolean pitOnly) {
        return bindings.spawnHeight(y, pitOnly);
    }

    public long nowMillis() {
        return System.currentTimeMillis();
    }

    public List<Pit.PlayerInfo> players(boolean pitOnly) {
        List<Pit.PlayerInfo> players = new ArrayList<>();
        for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
            if (p == mc.field_71439_g || p == mc.func_175606_aa())
                continue;
            players.add(playerInfo(p, pitOnly));
        }
        return players;
    }

    public Pit.PlayerInfo playerInfo(EntityPlayer p, boolean pitOnly) {
        if (p == null || p == mc.field_71439_g || p == mc.func_175606_aa())
            return null;
        Details details = Objects.requireNonNull(bindings.playerDetails(p), "actual player details");
        return new Pit.PlayerInfo(p, p.func_110124_au(), details.name, details.enchants, details.categories, mc.field_71439_g.func_70032_d(p), bindings.inSpawn(p, pitOnly));
    }

    public boolean hideEntity(Pit pit, Entity entity) {
        return ready() && entity instanceof EntityPlayer && pit.hideEntity(playerInfo((EntityPlayer) entity, pit.settings.pitOnly));
    }

    public boolean hideVisual(Pit pit, Entity entity) {
        if (!ready() || entity == null || entity == mc.field_71439_g || entity == mc.func_175606_aa())
            return false;
        return entity instanceof EntityPlayer ? pit.hideVisual(playerInfo((EntityPlayer) entity, pit.settings.pitOnly)) : pit.hideSpawn(bindings.inSpawn(entity, pit.settings.pitOnly), false);
    }

    public void updateDenicker(boolean enabled, boolean notifications, boolean pitOnly) {
        bindings.updateDenicker(enabled, notifications, pitOnly);
    }

    public void updateEventSchedule(boolean enabled, boolean pitOnly) {
        bindings.updateEventSchedule(enabled, pitOnly);
    }

    public List<Pit.Event> events() {
        return bindings.events();
    }

    public boolean eventsLoading() {
        return bindings.eventsLoading();
    }

    public void message(String text) {
        if (mc.field_71439_g != null)
            mc.field_71439_g.func_145747_a(new ChatComponentText("Pit: " + text));
    }

    public void sendChat(String text) {
        mc.field_71439_g.func_71165_d(text);
    }

    public boolean inventoryScreenOpen() {
        return mc.field_71462_r instanceof GuiInventory && mc.field_71439_g.field_71070_bA == mc.field_71439_g.field_71069_bz;
    }

    public boolean survivalOrAdventure() {
        WorldSettings.GameType type = mc.field_71442_b.func_178889_l();
        return type == WorldSettings.GameType.SURVIVAL || type == WorldSettings.GameType.ADVENTURE;
    }

    public Pit.InventoryItem inventoryItem(int index) {
        ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(index);
        if (stack == null)
            return null;
        Item item = stack.func_77973_b();
        Pit.ItemKind kind = Pit.ItemKind.OTHER;
        if (item == Items.field_151028_Y
                || item == Items.field_151030_Z
                || item == Items.field_151165_aa
                || item == Items.field_151167_ab)
            kind = Pit.ItemKind.IRON_ARMOR;
        else if (item == Items.field_151040_l)
            kind = Pit.ItemKind.IRON_SWORD;
        else if (item == Items.field_151048_u)
            kind = Pit.ItemKind.DIAMOND_SWORD;
        return new Pit.InventoryItem(kind, stack.func_82837_s(), stack.func_77948_v());
    }

    public void dropStack(int slot) {
        mc.field_71442_b.func_78753_a(mc.field_71439_g.field_71069_bz.field_75152_c, slot, 1, 4, mc.field_71439_g);
    }

    public boolean hudAllowed() {
        return !mc.field_71474_y.field_74330_P && (mc.field_71462_r == null || mc.field_71462_r instanceof GuiChat);
    }

    public int screenWidth() {
        return new ScaledResolution(mc).func_78326_a();
    }

    public int screenHeight() {
        return new ScaledResolution(mc).func_78328_b();
    }

    public int fontHeight() {
        return mc.field_71466_p.field_78288_b;
    }

    public int textWidth(String text) {
        return mc.field_71466_p.func_78256_a(text);
    }

    public boolean textShadow() {
        return bindings.hudShadow();
    }

    public void drawHud(PitHud.Panel panel) {
        GlStateManager.func_179094_E();
        GlStateManager.func_179097_i();
        try {
            GlStateManager.func_179152_a(panel.scale, panel.scale, 1);
            int background = bindings.hudBackgroundArgb();
            if ((background >>> 24) != 0)
                Gui.func_73734_a((int) panel.x, (int) panel.y, (int) Math.ceil(panel.x + panel.width), (int) Math.ceil(panel.y + panel.height), background);
            for (int i = 0; i < panel.rows.size(); i++) {
                float y = panel.y + i * panel.lineHeight;
                if (bindings.hudSidebar()) {
                    float x = panel.rightAligned ? panel.x + panel.width : panel.x - 1;
                    Gui.func_73734_a((int) x, (int) y, (int) x + 1, (int) Math.ceil(y + panel.lineHeight + 1), bindings.hudColor(nowMillis(), i));
                }
                float x = panel.rightAligned ? panel.x + panel.width - 3 - textWidth(panel.rows.get(i)) : panel.x + 2;
                mc.field_71466_p.func_175065_a(panel.rows.get(i), x, y + 1, bindings.hudColor(nowMillis(), i), textShadow());
            }
        } finally {
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }
}
