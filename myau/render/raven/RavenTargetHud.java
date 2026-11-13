package myau.render.raven;

import myau.HUDModule;
import myau.MyauClient;
import myau.TargetHUDModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public final class RavenTargetHud {

    private final RavenHudState state = new RavenHudState();

    public void reset() {
        state.reset();
    }

    public void render(TargetHUDModule settings, EntityLivingBase target) {
        Minecraft mc = Minecraft.func_71410_x();
        long now = System.currentTimeMillis();
        if (target != null) {
            state.update(target, target.func_70005_c_(), target.func_110143_aJ() + target.func_110139_bj(), target.func_110138_aP(), now, settings.animations.getValue());
        }
        int alpha = state.alpha(now);
        if (alpha < 4)
            return;
        String text = state.text(mc.field_71439_g.func_110143_aJ() + mc.field_71439_g.func_110139_bj(), mc.field_71439_g.func_110138_aP(), settings.indicator.getValue());
        float scale = settings.scale.getValue();
        float width = (mc.field_71466_p.func_78256_a(text) + 16) * scale;
        float height = (mc.field_71466_p.field_78288_b + 28) * scale;
        ScaledResolution resolution = new ScaledResolution(mc);
        float x = position(settings.positionX.getValue(), resolution.func_78326_a(), width, settings.offsetX.getValue());
        float y = position(settings.positionY.getValue(), resolution.func_78328_b(), height, settings.offsetY.getValue());
        int first = 0xffffc8c8, second = 0xfff33a6a;
        if (settings.color.getValue() == 1) {
            HUDModule hud = (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
            if (hud != null) {
                first = hud.getColorForRow(now, 0).getRGB();
                second = hud.getColorForRow(now, 1).getRGB();
            }
        }
        GlStateManager.func_179094_E();
        try {
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            boolean effects = false;
            try {
                if (RavenShapes.effectsAvailable()) {
                    RavenShapes.updateResolution();
                    GlowShader.get().startCapture();
                    RavenShapes.mask(x, y, width, height, 5 * scale, RavenShapes.alpha(0, Math.min(alpha, 210)));
                    GlowShader.get().render(2, 1.3f * Math.min(alpha, 210) / 210);
                    Blur.get().startStencil();
                    RavenShapes.mask(x, y, width, height, 5 * scale, 0xffffffff);
                    Blur.get().render(2, 2f * Math.min(alpha, 210) / 210);
                    effects = true;
                }
            } catch (RuntimeException | LinkageError failure) {
                RavenShapes.disableEffects();
            } finally {
                mc.func_147110_a().func_147610_a(true);
                if (org.lwjgl.opengl.GLContext.getCapabilities().OpenGL20)
                    GL20.glUseProgram(0);
                GlStateManager.func_179138_g(GL13.GL_TEXTURE0);
                GlStateManager.func_179144_i(0);
                GlStateManager.func_179098_w();
                GlStateManager.func_179141_d();
                GlStateManager.func_179092_a(GL11.GL_GREATER, 0.1f);
            }
            if (!effects)
                RavenShapes.rounded(x, y, x + width, y + height, 10 * scale, RavenShapes.alpha(0x16161c, Math.min(alpha, 210)), RavenShapes.alpha(0x16161c, Math.min(alpha, 210)));
            float barleft = x + 6 * scale, barright = x + width - 6 * scale;
            float bartop = y + (mc.field_71466_p.field_78288_b + 15) * scale;
            int back = RavenShapes.alpha(0, Math.min(alpha, 100));
            RavenShapes.rounded(barleft, bartop, barright, bartop + 5 * scale, 4 * scale, back, back);
            float trail = Math.max(state.shown(now), state.front(now));
            RavenShapes.rounded(barleft, bartop, barleft + (barright - barleft) * trail, bartop + 5 * scale, 4 * scale, RavenShapes.alpha(first, Math.min(alpha, 100)), RavenShapes.alpha(second, Math.min(alpha, 100)));
            float front = state.front(now);
            RavenShapes.rounded(barleft, bartop, barleft + (barright - barleft) * front, bartop + 5 * scale, 4 * scale, RavenShapes.alpha(first, Math.min(alpha, 210)), RavenShapes.alpha(RavenShapes.blend(first, second, 1 - front), Math.min(alpha, 210)));
            GlStateManager.func_179152_a(scale, scale, 1);
            mc.field_71466_p.func_175065_a(text, x / scale + 8, y / scale + 8, RavenShapes.alpha(0xffffff, alpha), settings.shadow.getValue());
        } finally {
            GlStateManager.func_179117_G();
            GlStateManager.func_179084_k();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }

    static float position(int mode, float screen, float size, float offset) {
        return mode == 1 ? (screen - size) / 2 + offset : mode == 2 ? screen - size - offset : offset;
    }
}
