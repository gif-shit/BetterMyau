package myau;

import java.awt.Color;
import java.util.ArrayList;
import myau.mixin.AccessorRenderManager;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
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

public class TrajectoriesModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final PercentSetting opacity = new PercentSetting("opacity", 100);
    public final BooleanSetting bow = new BooleanSetting("bow", true);
    public final BooleanSetting projectiles = new BooleanSetting("projectiles", false);
    public final BooleanSetting pearls = new BooleanSetting("pearls", true);

    public TrajectoriesModule() {
        super("Trajectories", false, 0, true);
    }

    static final class Prediction {

        final ArrayList<Vec3> points = new ArrayList<>();

        MovingObjectPosition hit;
    }

    Prediction predictPath() {
        float radius;
        float gravity;
        if (!this.isEnabled() || minecraft.field_71439_g == null || minecraft.field_71441_e == null) {
            return null;
        }
        if (minecraft.field_71439_g.func_70694_bm() == null) {
            return null;
        }
        if (minecraft.field_71474_y.field_74320_O != 0) {
            return null;
        }
        Item item = minecraft.field_71439_g.func_70694_bm().func_77973_b();
        boolean arrow = false;
        float speed = 1.5f;
        float drag = 0.99f;
        if (item instanceof ItemBow && this.bow.getValue()) {
            if (!minecraft.field_71439_g.func_71039_bw()) {
                return null;
            }
            arrow = true;
            gravity = 0.05f;
            radius = 0.25f;
            float charge = (float) minecraft.field_71439_g.func_71057_bx() / 20.0f;
            if ((charge = (charge * charge + charge * 2.0f) / 3.0f) < 0.1f) {
                return null;
            }
            if (charge > 1.0f) {
                charge = 1.0f;
            }
            speed = charge * 3.0f;
        } else if (item instanceof ItemFishingRod && this.projectiles.getValue()) {
            gravity = 0.04f;
            radius = 0.125f;
            drag = 0.92f;
        } else if ((item instanceof ItemSnowball || item instanceof ItemEgg) && this.projectiles.getValue()) {
            gravity = 0.03f;
            radius = 0.125f;
        } else {
            if (!(item instanceof ItemEnderPearl) || !this.pearls.getValue()) {
                return null;
            }
            gravity = 0.03f;
            radius = 0.125f;
        }
        float yaw = RotationState.isActiveWithinTicks(1) ? RotationState.headYaw() : minecraft.field_71439_g.field_70177_z;
        float pitch = RotationState.isActiveWithinTicks(1) ? RotationState.pitch() : minecraft.field_71439_g.field_70125_A;
        double x = minecraft.field_71439_g.field_70165_t - (double) MathHelper.func_76134_b((float) (yaw / 180.0f * (float) Math.PI)) * (double) 0.16f;
        double y = minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e() - (double) 0.1f;
        double z = minecraft.field_71439_g.field_70161_v - (double) MathHelper.func_76126_a((float) (yaw / 180.0f * (float) Math.PI)) * (double) 0.16f;
        double motionX = (double) (MathHelper.func_76126_a((float) (yaw / 180.0f * (float) Math.PI)) * MathHelper.func_76134_b((float) (pitch / 180.0f * (float) Math.PI))) * (arrow ? 1.0 : 0.4) * -1.0;
        double motionY = (double) MathHelper.func_76126_a((float) (pitch / 180.0f * (float) Math.PI)) * (arrow ? 1.0 : 0.4) * -1.0;
        double motionZ = (double) (MathHelper.func_76134_b((float) (yaw / 180.0f * (float) Math.PI)) * MathHelper.func_76134_b((float) (pitch / 180.0f * (float) Math.PI))) * (arrow ? 1.0 : 0.4);
        float magnitude = MathHelper.func_76133_a((double) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= (double) magnitude;
        motionY /= (double) magnitude;
        motionZ /= (double) magnitude;
        motionX *= (double) speed;
        motionY *= (double) speed;
        motionZ *= (double) speed;
        Prediction prediction = new Prediction();
        prediction.points.add(new Vec3(x, y, z));
        while (prediction.points.size() < 512 && y > 0.0) {
            Vec3 start = new Vec3(x, y, z);
            Vec3 end = new Vec3(x + motionX, y + motionY, z + motionZ);
            MovingObjectPosition hit = minecraft.field_71441_e.func_147447_a(start, end, false, arrow, false);
            if (hit != null)
                end = hit.field_72307_f;
            double nearest = start.func_72438_d(end);
            AxisAlignedBB searchBox = new AxisAlignedBB(x - (double) radius, y - (double) radius, z - (double) radius, x + (double) radius, y + (double) radius, z + (double) radius).func_72321_a(motionX, motionY, motionZ).func_72314_b(1.0, 1.0, 1.0);
            int firstChunkX = MathHelper.func_76128_c((double) ((searchBox.field_72340_a - 2.0) / 16.0));
            int lastChunkX = MathHelper.func_76128_c((double) ((searchBox.field_72336_d + 2.0) / 16.0));
            int firstChunkZ = MathHelper.func_76128_c((double) ((searchBox.field_72339_c - 2.0) / 16.0));
            int lastChunkZ = MathHelper.func_76128_c((double) ((searchBox.field_72334_f + 2.0) / 16.0));
            ArrayList<Entity> entities = new ArrayList<>();
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; ++chunkX) {
                for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; ++chunkZ) {
                    minecraft.field_71441_e.func_72964_e(chunkX, chunkZ).func_177414_a((Entity) minecraft.field_71439_g, searchBox, entities, null);
                }
            }
            for (Entity entity : entities) {
                if (!entity.func_70067_L() || entity == minecraft.field_71439_g)
                    continue;
                MovingObjectPosition intercept = entity.func_174813_aQ().func_72314_b(0.3, 0.3, 0.3).func_72327_a(start, end);
                if (intercept == null)
                    continue;
                double distance = start.func_72438_d(intercept.field_72307_f);
                if (distance >= nearest)
                    continue;
                nearest = distance;
                hit = new MovingObjectPosition(entity, intercept.field_72307_f);
            }
            if (hit != null) {
                prediction.hit = hit;
                prediction.points.add(hit.field_72307_f);
                return prediction;
            }
            x += motionX;
            y += motionY;
            z += motionZ;
            prediction.points.add(new Vec3(x, y, z));
            boolean water = minecraft.field_71441_e.func_180495_p(new BlockPos(x, y, z)).func_177230_c().func_149688_o() == Material.field_151586_h;
            float slowdown = water ? (arrow ? 0.6f : 0.8f) : drag;
            if (item instanceof ItemFishingRod)
                motionY -= gravity;
            motionX *= slowdown;
            motionY *= slowdown;
            motionZ *= slowdown;
            if (!(item instanceof ItemFishingRod))
                motionY -= gravity;
        }
        return prediction;
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        Prediction prediction = predictPath();
        if (prediction == null || prediction.points.size() < 2)
            return;
        AccessorRenderManager camera = (AccessorRenderManager) minecraft.func_175598_ae();
        boolean hitEntity = prediction.hit != null && prediction.hit.field_72313_a == MovingObjectPosition.MovingObjectType.ENTITY;
        Color color = new Color(hitEntity ? 85 : 255, 255, hitEntity ? 85 : 255, (int) (opacity.getValue() / 100.0f * 255.0f));
        RenderUtils.beginOverlay();
        try {
            RenderUtils.setColor(color.getRGB());
            GL11.glLineWidth(1.5f);
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);
            WorldRenderer renderer = Tessellator.func_178181_a().func_178180_c();
            renderer.func_181668_a(3, DefaultVertexFormats.field_181705_e);
            for (Vec3 point : prediction.points) {
                renderer.func_181662_b(point.field_72450_a - camera.getRenderPosX(), point.field_72448_b - camera.getRenderPosY(), point.field_72449_c - camera.getRenderPosZ()).func_181675_d();
            }
            Tessellator.func_178181_a().func_78381_a();
            if (prediction.hit != null) {
                Vec3 hit = prediction.hit.field_72307_f;
                double x = hit.field_72450_a - camera.getRenderPosX();
                double y = hit.field_72448_b - camera.getRenderPosY();
                double z = hit.field_72449_c - camera.getRenderPosZ();
                RenderUtils.outlineBounds(new AxisAlignedBB(x - 0.08, y - 0.08, z - 0.08, x + 0.08, y + 0.08, z + 0.08), color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.5f);
            }
        } finally {
            GL11.glDisable(2848);
            GL11.glLineWidth(2.0f);
            GlStateManager.func_179117_G();
            RenderUtils.endOverlay();
        }
    }
}
