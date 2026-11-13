package myau;

import java.awt.Color;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

final class ColorWheel {
    float hue, saturation, brightness;
    private int rgb = -1;

    void sync(int value) {
        if (rgb == value) return;
        float[] hsb = Color.RGBtoHSB(value >> 16 & 255, value >> 8 & 255, value & 255, null);
        if (hsb[1] > 0) hue = hsb[0];
        if (hsb[2] > 0) saturation = hsb[1];
        brightness = hsb[2];
        rgb = value;
    }

    int select(float x, float y, float radius) {
        hue = (float) ((Math.atan2(y, x) / (Math.PI * 2) + 1) % 1);
        saturation = Math.max(0, Math.min(1, (float) Math.hypot(x, y) / radius));
        return color();
    }

    int brightness(float fraction) {
        brightness = Math.max(0, Math.min(1, fraction));
        return color();
    }

    private int color() {
        rgb = Color.HSBtoRGB(hue, saturation, brightness) & 0xffffff;
        return rgb;
    }

    void draw(float x, float y, float radius, float sliderx) {
        GlStateManager.func_179090_x();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        RenderUtils.setColor(Color.HSBtoRGB(0, 0, brightness));
        GL11.glVertex2f(x, y);
        for (int segment = 0; segment <= 96; segment++) {
            float hue = segment / 96f;
            double angle = hue * Math.PI * 2;
            RenderUtils.setColor(Color.HSBtoRGB(hue, 1, brightness));
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }
        GL11.glEnd();
        float top = y - radius, bottom = y + radius;
        GL11.glBegin(GL11.GL_QUADS);
        RenderUtils.setColor(Color.HSBtoRGB(hue, saturation, 1));
        GL11.glVertex2f(sliderx, top);
        GL11.glVertex2f(sliderx + 8, top);
        RenderUtils.setColor(0xff000000);
        GL11.glVertex2f(sliderx + 8, bottom);
        GL11.glVertex2f(sliderx, bottom);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        float thumbx = x + (float) Math.cos(hue * Math.PI * 2) * saturation * radius;
        float thumby = y + (float) Math.sin(hue * Math.PI * 2) * saturation * radius;
        RenderUtils.roundedRect(thumbx - 3, thumby - 3, thumbx + 3, thumby + 3, 3, 0xff121218);
        RenderUtils.roundedRect(thumbx - 2, thumby - 2, thumbx + 2, thumby + 2, 2, 0xfff8f8ff);
        GlStateManager.func_179090_x();
        float slidery = bottom - brightness * radius * 2;
        RenderUtils.fillRect(sliderx - 2, slidery - 1, sliderx + 10, slidery + 1, 0xffffffff);
        GlStateManager.func_179098_w();
        GlStateManager.func_179117_G();
    }
}
