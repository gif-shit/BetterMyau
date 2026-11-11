package myau;

import java.awt.Color;
import java.util.ArrayList;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.Module;
import myau.PercentSetting;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.mixin.AccessorRenderManager;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

public class TrajectoriesModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final PercentSetting opacity = new PercentSetting("opacity", 100);
    public final BooleanSetting bow = new BooleanSetting("bow", true);
    public final BooleanSetting projectiles = new BooleanSetting("projectiles", false);
    public final BooleanSetting pearls = new BooleanSetting("pearls", true);

    public TrajectoriesModule() {
        super("Trajectories", false, 0, true);
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        float radius;
        float gravity;
        if (!this.isEnabled()) {
            return;
        }
        if (TrajectoriesModule.minecraft.field_71439_g.func_70694_bm() == null) {
            return;
        }
        if (TrajectoriesModule.minecraft.field_71474_y.field_74320_O != 0) {
            return;
        }
        Item item = TrajectoriesModule.minecraft.field_71439_g.func_70694_bm().func_77973_b();
        RenderManager renderManager = minecraft.func_175598_ae();
        boolean arrow = false;
        float speed = 1.5f;
        float drag = 0.99f;
        if (item instanceof ItemBow && ((Boolean)this.bow.getValue()).booleanValue()) {
            if (!TrajectoriesModule.minecraft.field_71439_g.func_71039_bw()) {
                return;
            }
            arrow = true;
            gravity = 0.05f;
            radius = 0.3f;
            float charge = (float)TrajectoriesModule.minecraft.field_71439_g.func_71057_bx() / 20.0f;
            if ((charge = (charge * charge + charge * 2.0f) / 3.0f) < 0.1f) {
                return;
            }
            if (charge > 1.0f) {
                charge = 1.0f;
            }
            speed = charge * 3.0f;
        } else if (item instanceof ItemFishingRod && ((Boolean)this.projectiles.getValue()).booleanValue()) {
            gravity = 0.04f;
            radius = 0.25f;
            drag = 0.92f;
        } else if ((item instanceof ItemSnowball || item instanceof ItemEgg) && ((Boolean)this.projectiles.getValue()).booleanValue()) {
            gravity = 0.03f;
            radius = 0.25f;
        } else {
            if (!(item instanceof ItemEnderPearl) || !((Boolean)this.pearls.getValue()).booleanValue()) {
                return;
            }
            gravity = 0.03f;
            radius = 0.25f;
        }
        float yaw = TrajectoriesModule.minecraft.field_71439_g.field_70177_z;
        float pitch = TrajectoriesModule.minecraft.field_71439_g.field_70125_A;
        double x = ((AccessorRenderManager)renderManager).getRenderPosX() - (double)MathHelper.func_76134_b((float)(yaw / 180.0f * (float)Math.PI)) * 0.16;
        double y = ((AccessorRenderManager)renderManager).getRenderPosY() + (double)TrajectoriesModule.minecraft.field_71439_g.func_70047_e() - (double)0.1f;
        double z = ((AccessorRenderManager)renderManager).getRenderPosZ() - (double)MathHelper.func_76126_a((float)(yaw / 180.0f * (float)Math.PI)) * 0.16;
        double motionX = (double)(MathHelper.func_76126_a((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(pitch / 180.0f * (float)Math.PI))) * (arrow ? 1.0 : 0.4) * -1.0;
        double motionY = (double)MathHelper.func_76126_a((float)(pitch / 180.0f * (float)Math.PI)) * (arrow ? 1.0 : 0.4) * -1.0;
        double motionZ = (double)(MathHelper.func_76134_b((float)(yaw / 180.0f * (float)Math.PI)) * MathHelper.func_76134_b((float)(pitch / 180.0f * (float)Math.PI))) * (arrow ? 1.0 : 0.4);
        float magnitude = MathHelper.func_76133_a((double)(motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= (double)magnitude;
        motionY /= (double)magnitude;
        motionZ /= (double)magnitude;
        motionX *= (double)speed;
        motionY *= (double)speed;
        motionZ *= (double)speed;
        MovingObjectPosition hit = null;
        boolean collided = false;
        boolean hitEntity = false;
        WorldRenderer renderer = Tessellator.func_178181_a().func_178180_c();
        ArrayList<Vec3> points = new ArrayList<Vec3>();
        while (!collided && y > 0.0) {
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = new Vec3(x + motionX, y + motionY, z + motionZ);
            hit = TrajectoriesModule.minecraft.field_71441_e.func_147447_a(start, end, false, true, false);
            start = new Vec3(x, y, z);
            end = new Vec3(x + motionX, y + motionY, z + motionZ);
            AxisAlignedBB searchBox = new AxisAlignedBB(x - (double)radius, y - (double)radius, z - (double)radius, x + (double)radius, y + (double)radius, z + (double)radius).func_72321_a(motionX, motionY, motionZ).func_72314_b(1.0, 1.0, 1.0);
            int firstChunkX = MathHelper.func_76128_c((double)((searchBox.field_72340_a - 2.0) / 16.0));
            int lastChunkX = MathHelper.func_76128_c((double)((searchBox.field_72336_d + 2.0) / 16.0));
            int firstChunkZ = MathHelper.func_76128_c((double)((searchBox.field_72339_c - 2.0) / 16.0));
            int lastChunkZ = MathHelper.func_76128_c((double)((searchBox.field_72334_f + 2.0) / 16.0));
            ArrayList entities = new ArrayList();
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; ++chunkX) {
                for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; ++chunkZ) {
                    TrajectoriesModule.minecraft.field_71441_e.func_72964_e(chunkX, chunkZ).func_177414_a((Entity)TrajectoriesModule.minecraft.field_71439_g, searchBox, entities, null);
                }
            }
            for (Entity entity : entities) {
                AxisAlignedBB bounds;
                MovingObjectPosition intercept;
                if (!entity.func_70067_L() || entity == TrajectoriesModule.minecraft.field_71439_g || (intercept = (bounds = entity.func_174813_aQ().func_72314_b((double)radius, (double)radius, (double)radius)).func_72327_a(start, end)) == null) continue;
                hitEntity = true;
                collided = true;
                hit = intercept;
            }
            if (TrajectoriesModule.minecraft.field_71441_e.func_180495_p(new BlockPos(x += motionX, y += motionY, z += motionZ)).func_177230_c().func_149688_o() == Material.field_151586_h) {
                motionX *= 0.6;
                motionY *= 0.6;
                motionZ *= 0.6;
            } else {
                motionX *= (double)drag;
                motionY *= (double)drag;
                motionZ *= (double)drag;
            }
            motionY -= (double)gravity;
            points.add(new Vec3(x - ((AccessorRenderManager)renderManager).getRenderPosX(), y - ((AccessorRenderManager)renderManager).getRenderPosY(), z - ((AccessorRenderManager)renderManager).getRenderPosZ()));
        }
        if (points.size() <= 1) {
            return;
        }
        RenderUtils.beginOverlay();
        RenderUtils.setColor(new Color(hitEntity ? 85 : 255, 255, hitEntity ? 85 : 255, (int)(((Integer)this.opacity.getValue()).floatValue() / 100.0f * 255.0f)).getRGB());
        GL11.glLineWidth((float)1.5f);
        GL11.glEnable((int)2848);
        GL11.glHint((int)3154, (int)4354);
        renderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
        points.forEach(point -> renderer.func_181662_b(point.field_72450_a, point.field_72448_b, point.field_72449_c).func_181675_d());
        Tessellator.func_178181_a().func_78381_a();
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b((double)(x - ((AccessorRenderManager)renderManager).getRenderPosX()), (double)(y - ((AccessorRenderManager)renderManager).getRenderPosY()), (double)(z - ((AccessorRenderManager)renderManager).getRenderPosZ()));
        GlStateManager.func_179121_F();
        GL11.glDisable((int)2848);
        GL11.glLineWidth((float)2.0f);
        GlStateManager.func_179117_G();
        RenderUtils.endOverlay();
    }
}
