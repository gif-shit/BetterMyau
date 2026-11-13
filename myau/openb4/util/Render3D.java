package myau.openb4.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

public final class Render3D {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private Render3D() {
    }

    public static void filledBox(AxisAlignedBB a, float r, float g, float b, float alpha) {
        Tessellator t = Tessellator.func_178181_a();
        WorldRenderer w = t.func_178180_c();
        w.func_181668_a(7, DefaultVertexFormats.field_181706_f);
        double[][] v = { { a.field_72340_a, a.field_72338_b, a.field_72339_c }, { a.field_72340_a, a.field_72337_e, a.field_72339_c }, { a.field_72336_d, a.field_72337_e, a.field_72339_c }, { a.field_72336_d, a.field_72338_b, a.field_72339_c }, { a.field_72340_a, a.field_72338_b, a.field_72334_f }, { a.field_72336_d, a.field_72338_b, a.field_72334_f }, { a.field_72336_d, a.field_72337_e, a.field_72334_f }, { a.field_72340_a, a.field_72337_e, a.field_72334_f }, { a.field_72340_a, a.field_72338_b, a.field_72339_c }, { a.field_72340_a, a.field_72338_b, a.field_72334_f }, { a.field_72340_a, a.field_72337_e, a.field_72334_f }, { a.field_72340_a, a.field_72337_e, a.field_72339_c }, { a.field_72336_d, a.field_72338_b, a.field_72339_c }, { a.field_72336_d, a.field_72337_e, a.field_72339_c }, { a.field_72336_d, a.field_72337_e, a.field_72334_f }, { a.field_72336_d, a.field_72338_b, a.field_72334_f }, { a.field_72340_a, a.field_72338_b, a.field_72339_c }, { a.field_72336_d, a.field_72338_b, a.field_72339_c }, { a.field_72336_d, a.field_72338_b, a.field_72334_f }, { a.field_72340_a, a.field_72338_b, a.field_72334_f }, { a.field_72340_a, a.field_72337_e, a.field_72339_c }, { a.field_72340_a, a.field_72337_e, a.field_72334_f }, { a.field_72336_d, a.field_72337_e, a.field_72334_f }, { a.field_72336_d, a.field_72337_e, a.field_72339_c } };
        for (double[] p : v) {
            w.func_181662_b(p[0], p[1], p[2]).func_181666_a(r, g, b, alpha).func_181675_d();
        }
        t.func_78381_a();
    }

    public static void shadedBox(EntityLivingBase e, int n, float f) {
        RenderManager rm = mc.func_175598_ae();
        double d = e.field_70142_S + (e.field_70165_t - e.field_70142_S) * (double) f - rm.field_78730_l;
        double d2 = e.field_70137_T + (e.field_70163_u - e.field_70137_T) * (double) f - rm.field_78731_m;
        double d3 = e.field_70136_U + (e.field_70161_v - e.field_70136_U) * (double) f - rm.field_78728_n;
        AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(0.1, 0.1, 0.1);
        AxisAlignedBB bb3 = new AxisAlignedBB(bb.field_72340_a - e.field_70165_t + d, bb.field_72338_b - e.field_70163_u + d2, bb.field_72339_c - e.field_70161_v + d3, bb.field_72336_d - e.field_70165_t + d, bb.field_72337_e - e.field_70163_u + d2, bb.field_72334_f - e.field_70161_v + d3);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (n >> 24 & 0xFF) / 255.0f;
        float r = (float) (n >> 16 & 0xFF) / 255.0f;
        float g = (float) (n >> 8 & 0xFF) / 255.0f;
        float b = (float) (n & 0xFF) / 255.0f;
        filledBox(bb3, r, g, b, a);
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    public static void blockBox(int n, int n2, int n3, float f, float f2, float f3, float f4, float f5, float f6, int n4, boolean outline, boolean shade) {
        RenderManager rm = mc.func_175598_ae();
        double d = (double) n - rm.field_78730_l;
        double d2 = (double) n2 - rm.field_78731_m;
        double d3 = (double) n3 - rm.field_78728_n;
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        float a = (float) (n4 >> 24 & 0xFF) / 255.0f;
        float r = (float) (n4 >> 16 & 0xFF) / 255.0f;
        float g = (float) (n4 >> 8 & 0xFF) / 255.0f;
        float b = (float) (n4 & 0xFF) / 255.0f;
        AxisAlignedBB bb = new AxisAlignedBB(d + (double) f, d2 + (double) f2, d3 + (double) f3, d + (double) f4, d2 + (double) f5, d3 + (double) f6);
        if (outline) {
            GlStateManager.func_179131_c(r, g, b, a);
            GL11.glEnable(2848);
            GL11.glLineWidth(2.0f);
            RenderGlobal.func_181561_a(bb);
            GL11.glDisable(2848);
        }
        if (shade) {
            filledBox(bb, r, g, b, 0.42f);
        }
        RenderUtils.resetColor();
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
    }

    public static void block(BlockPos pos, int n, boolean outline, boolean shade) {
        blockBox(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, n, outline, shade);
    }
}
