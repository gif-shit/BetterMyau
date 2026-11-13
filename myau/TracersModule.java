package myau;

import java.awt.Color;
import java.util.stream.Collectors;
import myau.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class TracersModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final ModeSetting color = new ModeSetting("color", 0, new String[] { "DEFAULT", "TEAMS", "HUD" });
    public final BooleanSetting lines = new BooleanSetting("lines", true);
    public final BooleanSetting arrows = new BooleanSetting("arrows", false);
    public final PercentSetting opacity = new PercentSetting("opacity", 100);
    public final BooleanSetting players = new BooleanSetting("players", true);
    public final BooleanSetting friends = new BooleanSetting("friends", true);
    public final BooleanSetting enemies = new BooleanSetting("enemies", true);
    public final BooleanSetting bots = new BooleanSetting("bots", false);

    public TracersModule() {
        super("Tracers", false, 0);
    }

    private boolean shouldTrackPlayer(EntityPlayer player) {
        if (player.field_70725_aQ > 0) {
            return false;
        }
        if (minecraft.func_175606_aa().func_70032_d((Entity) player) > 512.0f) {
            return false;
        }
        if (EntityUtils.isLocalOrViewPlayer(player)) {
            return false;
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

    private boolean shouldTrackEntity(Entity entity) {
        return entity instanceof EntityPlayer && this.shouldTrackPlayer((EntityPlayer) entity);
    }

    private Color colorFor(EntityPlayer player, float alpha) {
        if (EntityUtils.isFriend(player)) {
            return withAlpha(MyauClient.friendList.color(), alpha);
        }
        if (EntityUtils.isEnemy(player)) {
            return withAlpha(MyauClient.enemyList.color(), alpha);
        }
        switch(this.color.getValue()) {
            case 0:
                {
                    return EntityUtils.teamColor(player, alpha);
                }
            case 1:
                {
                    boolean teammate = EntityUtils.isTeammate(player);
                    byte teamColor = (byte) (teammate ? ChatColor.BLUE.getRgb() : ChatColor.RED.getRgb());
                    return new Color(teamColor & 0xFFFFFF | (int) (alpha * 255.0f) << 24, true);
                }
            case 2:
                {
                    int hudColor = ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(hudColor & 0xFFFFFF | (int) (alpha * 255.0f) << 24, true);
                }
        }
        return new Color(1.0f, 1.0f, 1.0f, alpha);
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color((float) color.getRed() / 255.0f, (float) color.getGreen() / 255.0f, (float) color.getBlue() / 255.0f, alpha);
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled() || !this.lines.getValue()) {
            return;
        }
        RenderUtils.beginOverlay();
        int thirdPersonView = minecraft.field_71474_y.field_74320_O;
        Vec3 direction = thirdPersonView == 0 ? new Vec3(0.0, 0.0, 1.0).func_178789_a((float) (-Math.toRadians(MathUtils.lerpFloat(minecraft.func_175606_aa().field_70127_C, minecraft.func_175606_aa().field_70125_A, ((AccessorMinecraft) minecraft).getTimer().field_74281_c)))).func_178785_b((float) (-Math.toRadians(MathUtils.lerpFloat(minecraft.func_175606_aa().field_70126_B, minecraft.func_175606_aa().field_70177_z, ((AccessorMinecraft) minecraft).getTimer().field_74281_c)))) : new Vec3(0.0, 0.0, 0.0).func_178789_a((float) (-Math.toRadians(MathUtils.lerpFloat(minecraft.field_71439_g.field_70727_aS, minecraft.field_71439_g.field_70726_aT, ((AccessorMinecraft) minecraft).getTimer().field_74281_c)))).func_178785_b((float) (-Math.toRadians(MathUtils.lerpFloat(minecraft.field_71439_g.field_71107_bF, minecraft.field_71439_g.field_71109_bG, ((AccessorMinecraft) minecraft).getTimer().field_74281_c))));
        Vec3 start = new Vec3(direction.field_72450_a, direction.field_72448_b + (double) minecraft.func_175606_aa().func_70047_e(), direction.field_72449_c);
        for (EntityPlayer player : EntityUtils.entitiesBackToFront().stream().filter(this::shouldTrackEntity).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            Color lineColor = this.colorFor(player, (float) this.opacity.getValue() / 100.0f);
            double x = MathUtils.lerpDouble(player.field_70142_S, player.field_70165_t, event.getPartialTicks());
            double y = MathUtils.lerpDouble(player.field_70137_T, player.field_70163_u, event.getPartialTicks()) - (player.func_70093_af() ? 0.125 : 0.0);
            double z = MathUtils.lerpDouble(player.field_70136_U, player.field_70161_v, event.getPartialTicks());
            RenderUtils.drawTracer(start, x, y + (double) player.func_70047_e(), z, (float) lineColor.getRed() / 255.0f, (float) lineColor.getGreen() / 255.0f, (float) lineColor.getBlue() / 255.0f, (float) lineColor.getAlpha() / 255.0f, 1.5f);
        }
        RenderUtils.endOverlay();
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        boolean drawArrows = this.isEnabled();
        if (!drawArrows) {
            return;
        }
        drawArrows = this.arrows.getValue();
        if (!drawArrows) {
            return;
        }
        for (EntityPlayer player : EntityUtils.entitiesBackToFront().stream().filter(this::shouldTrackEntity).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            float radius = 40.0f;
            float angle = RotationUtils.horizontalYawError(MathUtils.lerpDouble(minecraft.field_71439_g.field_70169_q, minecraft.field_71439_g.field_70165_t, event.getPartialTicks()), MathUtils.lerpDouble(minecraft.field_71439_g.field_70166_s, minecraft.field_71439_g.field_70161_v, event.getPartialTicks()), MathUtils.lerpDouble(player.field_70169_q, player.field_70165_t, event.getPartialTicks()), MathUtils.lerpDouble(player.field_70166_s, player.field_70161_v, event.getPartialTicks()));
            if (minecraft.field_71474_y.field_74320_O == 2) {
                angle += 180.0f;
            }
            float directionX = (float) Math.sin(Math.toRadians(angle));
            float directionY = (float) Math.cos(Math.toRadians(angle)) * -1.0f;
            float alpha = this.opacity.getValue().floatValue() / 100.0f;
            float angleFromCenter = Math.abs(MathHelper.func_76142_g((float) angle));
            if (angleFromCenter < 30.0f) {
                alpha = 0.0f;
            } else if (angleFromCenter < 60.0f) {
                alpha *= (angleFromCenter - 30.0f) / 30.0f;
            }
            HUDModule hud = (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a((float) hud.scale.getValue(), (float) hud.scale.getValue(), (float) 0.0f);
            GlStateManager.func_179109_b((float) ((float) new ScaledResolution(minecraft).func_78326_a() / 2.0f / hud.scale.getValue()), (float) ((float) new ScaledResolution(minecraft).func_78328_b() / 2.0f / hud.scale.getValue()), (float) 0.0f);
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) ((radius + 15.0f) * directionX + 1.0f), (float) ((radius + 15.0f) * directionY + 1.0f), (float) -100.0f);
            RenderUtils.beginOverlay();
            RenderUtils.fillArrow(0.0f, 0.0f, (float) (Math.atan2(directionY, directionX) + Math.PI), 10.0f, this.colorFor(player, alpha).getRGB());
            RenderUtils.endOverlay();
            GlStateManager.func_179121_F();
            GlStateManager.func_179121_F();
        }
    }
}
