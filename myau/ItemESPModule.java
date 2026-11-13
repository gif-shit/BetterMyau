package myau;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import myau.mixin.AccessorRenderManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

public class ItemESPModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final PercentSetting opacity = new PercentSetting("opacity", 25);
    public final BooleanSetting outline = new BooleanSetting("outline", false);
    public final BooleanSetting itemCount = new BooleanSetting("item-count", true);
    public final BooleanSetting autoScale = new BooleanSetting("auto-scale", true);
    public final BooleanSetting emeralds = new BooleanSetting("emeralds", true);
    public final BooleanSetting diamonds = new BooleanSetting("diamonds", true);
    public final BooleanSetting gold = new BooleanSetting("gold", true);
    public final BooleanSetting iron = new BooleanSetting("iron", true);

    public ItemESPModule() {
        super("ItemESP", false, 0);
    }

    private boolean shouldShowItem(int id) {
        if (this.emeralds.getValue() && this.isEmerald(id)) {
            return true;
        }
        if (this.diamonds.getValue() && this.isDiamond(id)) {
            return true;
        }
        if (this.gold.getValue() && this.isGold(id)) {
            return true;
        }
        return this.iron.getValue() && this.isIron(id);
    }

    private boolean isEmerald(int id) {
        Item item = Item.func_150899_d((int) id);
        Block block = Block.func_149634_a((Item) item);
        return item == Items.field_151166_bC || block == Blocks.field_150475_bE || block == Blocks.field_150412_bA;
    }

    private boolean isDiamond(int id) {
        Item item = Item.func_150899_d((int) id);
        Block block = Block.func_149634_a((Item) item);
        return item == Items.field_151045_i
                || item == Items.field_151048_u
                || item == Items.field_151046_w
                || item == Items.field_151047_v
                || item == Items.field_151056_x
                || item == Items.field_151012_L
                || item == Items.field_151161_ac
                || item == Items.field_151163_ad
                || item == Items.field_151173_ae
                || item == Items.field_151175_af
                || block == Blocks.field_150484_ah
                || block == Blocks.field_150482_ag;
    }

    private boolean isGold(int id) {
        Item item = Item.func_150899_d((int) id);
        Block block = Block.func_149634_a((Item) item);
        return item == Items.field_151043_k
                || item == Items.field_151074_bl
                || item == Items.field_151153_ao
                || block == Blocks.field_150340_R
                || block == Blocks.field_150352_o;
    }

    private boolean isIron(int id) {
        Item item = Item.func_150899_d((int) id);
        Block block = Block.func_149634_a((Item) item);
        return item == Items.field_151042_j || block == Blocks.field_150339_S || block == Blocks.field_150366_p;
    }

    private Color colorFor(int id) {
        if (this.isEmerald(id)) {
            return new Color(ChatColor.GREEN.getRgb());
        }
        if (this.isDiamond(id)) {
            return new Color(ChatColor.AQUA.getRgb());
        }
        if (this.isGold(id)) {
            return new Color(ChatColor.YELLOW.getRgb());
        }
        return new Color(this.isIron(id) ? ChatColor.WHITE.getRgb() : ChatColor.GRAY.getRgb());
    }

    private int sortPriority(int id) {
        if (this.isEmerald(id)) {
            return 4;
        }
        if (this.isDiamond(id)) {
            return 3;
        }
        if (this.isGold(id)) {
            return 2;
        }
        return this.isIron(id) ? 1 : 0;
    }

    private int compareItemEntries(Map.Entry<DroppedItemGroup, Integer> first, Map.Entry<DroppedItemGroup, Integer> second) {
        int firstPriority = this.sortPriority(first.getKey().itemId);
        int secondPriority = this.sortPriority(second.getKey().itemId);
        return Integer.compare(firstPriority, secondPriority);
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        LinkedHashMap<DroppedItemGroup, Integer> counts = new LinkedHashMap<DroppedItemGroup, Integer>();
        for (Entity entity : EntityUtils.entitiesBackToFront()) {
            int itemId;
            if (entity.field_70173_aa < 3
                    || !entity.field_70158_ak
                    && !RenderUtils.isEntityInFrustum(entity, 0.125)
                    || !(entity instanceof EntityItem))
                continue;
            EntityItem droppedItem = (EntityItem) entity;
            ItemStack stack = droppedItem.func_92059_d();
            if (stack.field_77994_a <= 0 || !this.shouldShowItem(itemId = Item.func_150891_b((Item) stack.func_77973_b())))
                continue;
            double worldX = MathUtils.lerpDouble(droppedItem.field_70142_S, droppedItem.field_70165_t, event.getPartialTicks());
            double worldY = MathUtils.lerpDouble(droppedItem.field_70137_T, droppedItem.field_70163_u, event.getPartialTicks());
            double worldZ = MathUtils.lerpDouble(droppedItem.field_70136_U, droppedItem.field_70161_v, event.getPartialTicks());
            DroppedItemGroup group = new DroppedItemGroup(itemId, worldX, worldY, worldZ);
            Integer previousCount = (Integer) counts.get(group);
            counts.put(group, stack.field_77994_a + (previousCount == null ? 0 : previousCount));
        }
        List<Map.Entry<DroppedItemGroup, Integer>> entries = counts.entrySet().stream().sorted(this::compareItemEntries).collect(Collectors.toList());
        for (Map.Entry<DroppedItemGroup, Integer> entry : entries) {
            DroppedItemGroup group = (DroppedItemGroup) entry.getKey();
            Color tint = this.colorFor(group.itemId);
            double x = group.x - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosX();
            double y = group.y - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosY();
            double z = group.z - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosZ();
            double distance = minecraft.func_175606_aa().func_70011_f(group.x, group.y, group.z);
            double size = 0.5 + 0.375 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
            AxisAlignedBB bounds = new AxisAlignedBB(x - size * 0.5, y, z - size * 0.5, x + size * 0.5, y + size, z + size * 0.5);
            RenderUtils.beginOverlay();
            if (this.opacity.getValue() > 0) {
                RenderUtils.fillBounds(bounds, tint.getRed(), tint.getGreen(), tint.getBlue(), (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
                GlStateManager.func_179117_G();
            }
            if (this.outline.getValue()) {
                RenderUtils.outlineBounds(bounds, tint.getRed(), tint.getGreen(), tint.getBlue(), 255, 1.5f);
                GlStateManager.func_179117_G();
            }
            RenderUtils.endOverlay();
            if (!this.itemCount.getValue())
                continue;
            GlStateManager.func_179094_E();
            GlStateManager.func_179137_b((double) x, (double) (y + size * 0.5), (double) z);
            GlStateManager.func_179114_b((float) (minecraft.func_175598_ae().field_78735_i * -1.0f), (float) 0.0f, (float) 1.0f, (float) 0.0f);
            GlStateManager.func_179114_b((float) minecraft.func_175598_ae().field_78732_j, (float) (minecraft.field_71474_y.field_74320_O == 2 ? -1.0f : 1.0f), (float) 0.0f, (float) 0.0f);
            double textScale = -0.04375 - 0.0328125 * ((Math.max(6.0, this.autoScale.getValue() ? distance : 6.0) - 6.0) / 28.0);
            GlStateManager.func_179139_a((double) textScale, (double) textScale, (double) 1.0);
            GlStateManager.func_179097_i();
            String label = String.format("%d", entry.getValue());
            RenderUtils.drawOutlinedText(label, ((float) minecraft.field_71466_p.func_78256_a(label) / 2.0f - 0.5f) * -1.0f, ((float) (minecraft.field_71466_p.field_78288_b / 2) - 0.5f) * -1.0f, tint.getRGB());
            GlStateManager.func_179126_j();
            GlStateManager.func_179117_G();
            GlStateManager.func_179121_F();
        }
    }
}
