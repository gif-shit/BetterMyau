package myau.render.raven;

import myau.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

final class RavenShapes {

    private static final Minecraft mc = Minecraft.func_71410_x();
    private static Shader mask;
    private static int width, height;
    private static boolean effectsFailed;

    static int downsample(int size, int level) {
        return Math.max(1, size >> level);
    }

    static boolean effectsAvailable() {
        if (effectsFailed || !OpenGlHelper.func_148822_b() || !GLContext.getCapabilities().OpenGL20)
            return false;
        if (mask == null) {
            mask = new Shader("#version 120\nuniform vec2 u_size;\nuniform float u_radius;\nuniform vec4 u_color;\nvoid main(){float d=length(max((abs(gl_TexCoord[0].st-0.5)+0.5)*u_size-u_size+u_radius,0.0))-u_radius+0.5;gl_FragColor=vec4(u_color.rgb,u_color.a*(1.0-smoothstep(0.0,1.0,d)));}");
        }
        effectsFailed = mask.program < 0 || !Blur.get().ready() || !GlowShader.get().ready();
        return !effectsFailed;
    }

    static void disableEffects() {
        effectsFailed = true;
    }

    static void updateResolution() {
        ScaledResolution resolution = new ScaledResolution(mc);
        width = resolution.func_78326_a();
        height = resolution.func_78328_b();
    }

    static void quad(double x, double y, double w, double h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2d(x, y + h);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2d(x + w, y + h);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2d(x + w, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2d(x, y);
        GL11.glEnd();
    }

    static void screenQuad() {
        quad(0, 0, width, height);
    }

    static void mask(float x, float y, float w, float h, float radius, int color) {
        mask.use();
        mask.uniformf("u_size", w, h);
        mask.uniformf("u_radius", radius);
        mask.uniformf("u_color", (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        quad(x, y, w, h);
        mask.stop();
        resetColor();
    }

    static void rounded(float left, float top, float right, float bottom, float radius, int leftColor, int rightColor) {
        if (right <= left || bottom <= top)
            return;
        radius = Math.min(radius / 2, Math.min(right - left, bottom - top) / 2);
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int corner = 0; corner < 4; corner++) {
            double cx = corner < 2 ? left + radius : right - radius;
            double cy = corner == 0 || corner == 3 ? top + radius : bottom - radius;
            RenderUtils.setColor(corner < 2 ? leftColor : rightColor);
            for (int step = 0; step <= 30; step++) {
                double angle = Math.toRadians(270 - corner * 90 - step * 3);
                GL11.glVertex2d(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
            }
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.func_179098_w();
        resetColor();
    }

    static int alpha(int color, int alpha) {
        return color & 0xffffff | Math.max(0, Math.min(255, alpha)) << 24;
    }

    static int blend(int first, int second, double fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        int red = (int) ((first >> 16 & 255) * fraction + (second >> 16 & 255) * (1 - fraction));
        int green = (int) ((first >> 8 & 255) * fraction + (second >> 8 & 255) * (1 - fraction));
        int blue = (int) ((first & 255) * fraction + (second & 255) * (1 - fraction));
        return 0xff000000 | red << 16 | green << 8 | blue;
    }

    static void resetColor() {
        GlStateManager.func_179117_G();
    }
}
