package myau;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class IndicatorsModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final FloatSetting scale = new FloatSetting("scale", 1.0f, 0.5f, 1.5f);
    public final FloatSetting offset = new FloatSetting("offset", 50.0f, 0.0f, 255.0f);
    public final BooleanSetting directionCheck = new BooleanSetting("direction-check", true);
    public final BooleanSetting fireballs = new BooleanSetting("fireballs", true);
    public final BooleanSetting pearls = new BooleanSetting("pearls", true);
    public final BooleanSetting arrows = new BooleanSetting("arrows", true);

    public IndicatorsModule() {
        super("Indicators", false, 0, true);
    }

    private boolean shouldIndicate(Entity entity) {
        double approach = (entity.field_70165_t - entity.field_70142_S) * (minecraft.field_71439_g.field_70165_t - entity.field_70165_t) + (entity.field_70163_u - entity.field_70137_T) * (minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e() - entity.field_70163_u - (double) entity.field_70131_O / 2.0) + (entity.field_70161_v - entity.field_70136_U) * (minecraft.field_71439_g.field_70161_v - entity.field_70161_v);
        if (approach == 0.0 || approach < 0.0 && this.directionCheck.getValue()) {
            return false;
        }
        if (this.fireballs.getValue() && entity instanceof EntityFireball) {
            return true;
        }
        if (this.pearls.getValue() && entity instanceof EntityEnderPearl) {
            return true;
        }
        return this.arrows.getValue() && entity instanceof EntityArrow;
    }

    private Item iconFor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.field_151059_bz;
        }
        if (entity instanceof EntityEnderPearl) {
            return Items.field_151079_bi;
        }
        return entity instanceof EntityArrow ? Items.field_151032_g : new Item();
    }

    private Color colorFor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return new Color(12676363);
        }
        if (entity instanceof EntityEnderPearl) {
            return new Color(2458740);
        }
        return new Color(entity instanceof EntityArrow ? 0x969696 : -1);
    }

    @EventHandler(priority = 50)
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        List<Entity> indicators = EntityUtils.entitiesBackToFront().stream().filter(this::shouldIndicate).collect(Collectors.toList());
        for (Entity entity : indicators) {
            float radius = 10.0f + this.offset.getValue();
            float angle = RotationUtils.horizontalYawError(MathUtils.lerpDouble(minecraft.field_71439_g.field_70169_q, minecraft.field_71439_g.field_70165_t, event.getPartialTicks()), MathUtils.lerpDouble(minecraft.field_71439_g.field_70166_s, minecraft.field_71439_g.field_70161_v, event.getPartialTicks()), MathUtils.lerpDouble(entity.field_70169_q, entity.field_70165_t, event.getPartialTicks()), MathUtils.lerpDouble(entity.field_70166_s, entity.field_70161_v, event.getPartialTicks()));
            if (minecraft.field_71474_y.field_74320_O == 2) {
                angle += 180.0f;
            }
            float directionX = (float) Math.sin(Math.toRadians(angle));
            float directionY = (float) Math.cos(Math.toRadians(angle)) * -1.0f;
            GlStateManager.func_179094_E();
            GlStateManager.func_179097_i();
            GlStateManager.func_179152_a((float) this.scale.getValue(), (float) this.scale.getValue(), (float) 0.0f);
            GlStateManager.func_179109_b((float) ((float) new ScaledResolution(minecraft).func_78326_a() / 2.0f / this.scale.getValue()), (float) ((float) new ScaledResolution(minecraft).func_78328_b() / 2.0f / this.scale.getValue()), (float) 0.0f);
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) ((radius + 0.0f) * directionX - 8.0f), (float) ((radius + 0.0f) * directionY - 8.0f), (float) -300.0f);
            minecraft.func_175599_af().func_180450_b(new ItemStack(this.iconFor(entity)), 0, 0);
            GlStateManager.func_179121_F();
            String distanceLabel = String.format("%dm", (int) minecraft.field_71439_g.func_70032_d(entity));
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) ((radius + 0.0f) * directionX - (float) minecraft.field_71466_p.func_78256_a(distanceLabel) / 2.0f + 1.0f), (float) ((radius + 0.0f) * directionY + 1.0f), (float) -100.0f);
            minecraft.field_71466_p.func_175063_a(distanceLabel, 0.0f, 0.0f, ChatColor.GRAY.getRgb() & 0xFFFFFF | 0xBF000000);
            GlStateManager.func_179121_F();
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) ((radius + 15.0f) * directionX + 1.0f), (float) ((radius + 15.0f) * directionY + 1.0f), (float) -100.0f);
            RenderUtils.beginOverlay();
            RenderUtils.drawChevron(0.0f, 0.0f, (float) (Math.atan2(directionY, directionX) + Math.PI), 7.5f, 1.5f, this.colorFor(entity).getRGB());
            RenderUtils.endOverlay();
            GlStateManager.func_179121_F();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }
}
