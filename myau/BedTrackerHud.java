package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

final class BedTrackerHud {
    static final int height = 68;

    static String status(boolean tracked, boolean loaded, boolean intact, double threat, int range) {
        if (!tracked) return "SEARCHING";
        if (!loaded) return "OUT OF RANGE";
        if (!intact) return "BED LOST";
        return threat >= 0 && threat < range ? "THREAT NEARBY" : "SECURE";
    }

    static int accent(boolean tracked, boolean loaded, boolean intact, double threat, int range) {
        if (tracked && loaded && (!intact || threat >= 0 && threat < range)) return 0xfff1787e;
        return tracked && loaded ? 0xff79d7b2 : 0xffb0b8cc;
    }

    static float meter(double threat, int range) {
        return threat < 0 || range <= 0 ? 0 : (float) Math.max(0, Math.min(1, 1 - threat / range));
    }

    static float anchor(int mode, float available, float size, float offset) {
        if (mode == 1) return (available - size) / 2 + offset;
        return mode == 2 ? available - size - offset - 1 : offset + 1;
    }

    static int width(String threat) {
        return Math.max(186, Minecraft.func_71410_x().field_71466_p.func_78256_a(threat) + 22);
    }

    static void draw(int width, String status, String distance, String threat, float danger, int accent, boolean shadow) {
        FontRenderer font = Minecraft.func_71410_x().field_71466_p;
        RenderUtils.roundedRect(0, 0, width, height, 6, 0xe51a1d26);
        RenderUtils.roundedRect(0, 0, 3, height, 1.5f, accent);
        font.func_175065_a("BED GUARD", 11, 9, 0xfff0f2f8, shadow);
        int statuswidth = font.func_78256_a(status);
        RenderUtils.roundedRect(width - statuswidth - 19, 7, width - 8, 20, 4, accent & 0xffffff | 0x22000000);
        font.func_175065_a(status, width - statuswidth - 13, 10, accent, false);
        font.func_175065_a(distance, 11, 27, 0xfff0f2f8, shadow);
        font.func_175065_a(threat, 11, 43, 0xffb5bdcd, shadow);
        RenderUtils.roundedRect(11, 58, width - 11, 61, 1.5f, 0xff343947);
        if (danger > 0) {
            RenderUtils.roundedRect(11, 58, 11 + (width - 22) * danger, 61, 1.5f, accent);
        }
    }
}
