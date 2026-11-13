package myau;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import myau.mixin.AccessorEntityRenderer;
import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.util.vector.Vector4f;

public class ESPModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private OutlineShader outlineShader;
    private ColorMaskShader colorShader;
    private Framebuffer outlineFramebuffer;
    private boolean nameTagsAllowed = true;
    private boolean enchantmentGlintAllowed = true;
    public final ModeSetting mode = new ModeSetting("mode", 2, new String[] { "NONE", "2D", "3D", "OUTLINE" });
    public final ModeSetting color = new ModeSetting("color", 0, new String[] { "DEFAULT", "TEAMS", "HUD" });
    public final ModeSetting healthBar = new ModeSetting("health-bar", 0, new String[] { "NONE", "2D", "3D" }) {
        @Override
        public boolean parseValue(String input) {
            return super.parseValue("RAVEN".equalsIgnoreCase(input) ? "3D" : input);
        }
    };
    public final ModeSetting healthBarStyle = new ModeSetting("health-bar-style", 0, new String[] { "MYAU", "RAVEN" }, () -> healthBar.getValue() != 0);
    public final BooleanSetting players = new BooleanSetting("players", true);
    public final BooleanSetting friends = new BooleanSetting("friends", true);
    public final BooleanSetting enemies = new BooleanSetting("enemies", true);
    public final BooleanSetting self = new BooleanSetting("self", false);
    public final BooleanSetting bots = new BooleanSetting("bots", false);

    public ESPModule() {
        super("ESP", false, 0);
    }

    public boolean areNameTagsAllowed() {
        return this.nameTagsAllowed;
    }

    public boolean isEnchantmentGlintAllowed() {
        return this.enchantmentGlintAllowed;
    }

    private boolean isEligible(EntityPlayer player) {
        if (EntityUtils.isLocalOrViewPlayer(player)) {
            return this.self.getValue() && minecraft.field_71474_y.field_74320_O != 0;
        }
        if (EntityUtils.isFriend(player)) {
            return this.friends.getValue();
        }
        if (EntityUtils.isEnemy(player)) {
            return this.enemies.getValue();
        }
        if (EntityUtils.isBot(player)) {
            return this.bots.getValue();
        }
        return this.players.getValue();
    }

    private boolean shouldRender(EntityPlayer player) {
        return this.isEligible(player) && RenderUtils.isEntityInFrustum((Entity) player, 0.1f);
    }

    private Color colorFor(EntityPlayer player) {
        if (EntityUtils.isFriend(player)) {
            return MyauClient.friendList.color();
        }
        if (EntityUtils.isEnemy(player)) {
            return MyauClient.enemyList.color();
        }
        switch(this.color.getValue()) {
            case 0:
                {
                    return EntityUtils.teamColor(player, 1.0f);
                }
            case 1:
                {
                    boolean teammate = EntityUtils.isTeammate(player);
                    byte teamColor = (byte) (teammate ? ChatColor.BLUE.getRgb() : ChatColor.RED.getRgb());
                    return new Color(teamColor);
                }
            case 2:
                {
                    return ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis());
                }
        }
        return new Color(-1);
    }

    private void reportFramebufferUnavailable() {
        this.setEnabled(false);
        ChatUtils.addFormattedMessage(String.format("%s%s: &cFramebuffer is not enabled!&r", "&7[&bMyau&7] &r", this.getName()));
    }

    boolean hasOverlayHealthBar() {
        return healthBar.getValue() == 1 && healthBarStyle.getValue() == 0;
    }

    boolean hasWorldHealthBar() {
        return healthBar.getValue() != 0 && (healthBar.getValue() == 2 || healthBarStyle.getValue() == 1);
    }

    @EventHandler
    public void onFramebufferResize(FramebufferResizeEvent event) {
        if (this.outlineFramebuffer != null) {
            this.outlineFramebuffer.func_147608_a();
        }
        this.outlineFramebuffer = new Framebuffer(minecraft.field_71443_c, minecraft.field_71440_d, false);
    }

    @EventHandler(priority = 50)
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int selectedMode = this.mode.getValue();
        if (selectedMode != 1 && selectedMode != 3 && !this.hasOverlayHealthBar()) {
            return;
        }
        if (selectedMode == 3 && !OpenGlHelper.func_148822_b()) {
            this.reportFramebufferUnavailable();
            return;
        }
        List<EntityPlayer> targets = EntityUtils.entitiesBackToFront().stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).filter(this::shouldRender).collect(Collectors.toList());
        if (targets.isEmpty()) {
            return;
        }
        if (selectedMode == 3) {
            this.drawOutlines(event, targets);
        }
        if (selectedMode != 1 && !this.hasOverlayHealthBar()) {
            return;
        }
        HashMap<EntityPlayer, Vector4f> screenBounds = new HashMap<EntityPlayer, Vector4f>();
        ((AccessorEntityRenderer) minecraft.field_71460_t).callSetupCameraTransform(event.getPartialTicks(), 0);
        for (EntityPlayer player : targets) {
            screenBounds.put(player, RenderUtils.projectEntityBounds((Entity) player, 0.1f));
        }
        minecraft.field_71460_t.func_78478_c();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        GlStateManager.func_179090_x();
        GlStateManager.func_179118_c();
        for (EntityPlayer player : targets) {
            Vector4f bounds = (Vector4f) screenBounds.get(player);
            if (bounds == null)
                continue;
            float left = bounds.x;
            float top = bounds.y;
            float right = bounds.z;
            float bottom = bounds.w;
            if (selectedMode == 1) {
                int rgb = this.colorFor(player).getRGB();
                RenderUtils.drawBorderedRect(left, top, right, bottom, 3.0f, 0, (rgb & 0xFCFCFC) >> 2 | rgb & 0xFF000000);
                RenderUtils.drawBorderedRect(left, top, right, bottom, 1.5f, 0, rgb);
            }
            if (!this.hasOverlayHealthBar())
                continue;
            float health = player.func_110143_aJ() + player.func_110139_bj();
            float fraction = Math.min(Math.max(health / player.func_110138_aP(), 0.0f), 1.0f);
            float offset = (right - left) * 0.08f;
            int rgb = ColorUtils.healthGradient(fraction).getRGB();
            RenderUtils.drawLine2D(left - offset, top, left - offset, bottom, 3.0f, (rgb & 0xFCFCFC) >> 2 | rgb & 0xFF000000);
            RenderUtils.drawLine2D(left - offset, bottom, left - offset, bottom + (top - bottom) * fraction, 1.5f, rgb);
        }
        GlStateManager.func_179141_d();
        GlStateManager.func_179098_w();
        GlStateManager.func_179084_k();
    }

    private void drawOutlines(RenderOverlayEvent event, List<EntityPlayer> targets) {
        if (this.outlineShader == null) this.outlineShader = new OutlineShader();
        if (this.colorShader == null) this.colorShader = new ColorMaskShader();
        GlStateManager.func_179094_E();
        GlStateManager.func_179123_a();
        if (this.outlineFramebuffer == null) {
            this.outlineFramebuffer = new Framebuffer(minecraft.field_71443_c, minecraft.field_71440_d, false);
        } else {
            this.outlineFramebuffer.func_147614_f();
        }
        this.outlineFramebuffer.func_147610_a(false);
        ((AccessorEntityRenderer) minecraft.field_71460_t).callSetupCameraTransform(event.getPartialTicks(), 0);
        boolean shadows = minecraft.field_71474_y.field_181151_V;
        minecraft.field_71474_y.field_181151_V = false;
        this.nameTagsAllowed = false;
        this.enchantmentGlintAllowed = false;
        this.colorShader.use();
        for (EntityPlayer player : targets) {
            Color tint = this.colorFor(player);
            this.colorShader.setColor(tint);
            boolean invisible = player.func_82150_aj();
            player.func_82142_c(false);
            minecraft.func_175598_ae().func_147936_a((Entity) player, event.getPartialTicks(), true);
            player.func_82142_c(invisible);
        }
        this.colorShader.stop();
        this.enchantmentGlintAllowed = true;
        this.nameTagsAllowed = true;
        minecraft.field_71474_y.field_181151_V = shadows;
        minecraft.field_71460_t.func_78478_c();
        minecraft.func_147110_a().func_147610_a(false);
        this.outlineShader.use();
        RenderUtils.drawFramebuffer(this.outlineFramebuffer);
        this.outlineShader.stop();
        GlStateManager.func_179099_b();
        GlStateManager.func_179121_F();
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getValue() != 2 && !this.hasWorldHealthBar()) {
            return;
        }
        if (this.mode.getValue() == 3 && !OpenGlHelper.func_148822_b()) {
            this.reportFramebufferUnavailable();
            return;
        }
        RenderUtils.beginOverlay();
        for (EntityPlayer player : EntityUtils.entitiesBackToFront().stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).filter(this::shouldRender).collect(Collectors.toList())) {
            if (this.mode.getValue() == 2) {
                Color tint = this.colorFor(player);
                RenderUtils.outlineEntityBounds((Entity) player, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha(), 1.5f, 0.1f);
            }
            if (!this.hasWorldHealthBar())
                continue;
            double x = MathUtils.lerpDouble(player.field_70142_S, player.field_70165_t, event.getPartialTicks()) - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosX();
            double y = MathUtils.lerpDouble(player.field_70137_T, player.field_70163_u, event.getPartialTicks()) - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosY() - (double) 0.1f;
            double z = MathUtils.lerpDouble(player.field_70136_U, player.field_70161_v, event.getPartialTicks()) - ((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosZ();
            GlStateManager.func_179094_E();
            GlStateManager.func_179137_b((double) x, (double) y, (double) z);
            GlStateManager.func_179114_b((float) (minecraft.func_175598_ae().field_78735_i * -1.0f), (float) 0.0f, (float) 1.0f, (float) 0.0f);
            if (this.healthBarStyle.getValue() == 1) {
                RavenHealthBar.draw(player);
                GlStateManager.func_179121_F();
                continue;
            }
            float currentHealth = player.func_110143_aJ() + player.func_110139_bj();
            float healthFraction = Math.min(Math.max(currentHealth / player.func_110138_aP(), 0.0f), 1.0f);
            Color tint = ColorUtils.healthGradient(healthFraction);
            float left = 0.6f;
            float height = player.field_70131_O + 0.2f;
            float width = 0.1f;
            float border = width * 0.275f;
            RenderUtils.fillSmoothRect(left - border, -border, left + width + border, height + border, Color.black.getRGB());
            RenderUtils.fillSmoothRect(left, 0.0f, left + width, height, Color.darkGray.getRGB());
            RenderUtils.fillSmoothRect(left, 0.0f, left + width, height * healthFraction, tint.getRGB());
            GlStateManager.func_179121_F();
        }
        RenderUtils.endOverlay();
    }

    @Override
    public void onEnable() {
        if (this.mode.getValue() == 3 && !OpenGlHelper.func_148822_b()) {
            this.reportFramebufferUnavailable();
        }
    }
}
