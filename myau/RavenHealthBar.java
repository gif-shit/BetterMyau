package myau;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;

final class RavenHealthBar {
    static float fraction(float health, float maximum) {
        return maximum <= 0 || !Float.isFinite(health) || !Float.isFinite(maximum) ? 0 : Math.max(0, Math.min(1, health / maximum));
    }

    static float height(float entityHeight, boolean child) {
        return entityHeight < 1.1f || child ? 40 : 70;
    }

    static int color(float fraction) {
        return fraction < 0.3f ? 0xffff2626 : fraction < 0.5f ? 0xffffd026 : fraction < 0.7f ? 0xffffff26 : 0xff26ff26;
    }

    static void draw(EntityLivingBase entity) {
        GlStateManager.func_179152_a(0.03f, 0.03f, 0.03f);
        float fraction = fraction(entity.func_110143_aJ(), entity.func_110138_aP());
        float height = height(entity.field_70131_O, entity.func_70631_g_());
        float filled = height * fraction;
        int left = entity.func_70631_g_() ? 16 : 21;
        RenderUtils.fillRect(left, -1, left + 4, height + 1, 0xff0c0c0c);
        RenderUtils.fillRect(left + 1, filled, left + 3, height, 0xff494949);
        RenderUtils.fillRect(left + 1, 0, left + 3, filled, color(fraction));
    }
}
