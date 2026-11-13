package myau.openb4.util;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public final class RenderUtils {

    private static final Minecraft mc = Minecraft.func_71410_x();
    public static int scaledWidth, scaledWidthD, scaledHeight, scaledHeightD, scaleFactor = 1;

    private RenderUtils() {
    }

    public static int rainbow(long speed, long... offset) {
        long l2 = System.currentTimeMillis() + (offset.length > 0 ? offset[0] : 0L);
        return Color.getHSBColor((float) (l2 % (15000L / speed)) / (15000.0f / (float) speed), 0.75f, 1.0f).getRGB();
    }

    public static int blend(Color color, Color color2, double d) {
        int n = (int) (d * 255.0);
        int n2 = 255 - n;
        int r = color.getRed() * n + color2.getRed() * n2 >> 8;
        int g = color.getGreen() * n + color2.getGreen() * n2 >> 8;
        int b = color.getBlue() * n + color2.getBlue() * n2 >> 8;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public static int withAlpha(int rgb, int alpha) {
        return rgb & 0xFFFFFF | alpha << 24;
    }

    public static int clampAlpha(int n) {
        if (n > 255)
            return 255;
        if (n < 4)
            return 4;
        return n;
    }

    public static void resetColor() {
        GlStateManager.func_179131_c(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
