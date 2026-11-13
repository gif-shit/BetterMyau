package myau;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TargetHUDModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat heartFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat differenceFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final ElapsedTimer targetTimer = new ElapsedTimer();
    private final ElapsedTimer animationTimer = new ElapsedTimer();
    private EntityLivingBase lastAttackedTarget;
    private EntityLivingBase displayedTarget;
    private ResourceLocation skin;
    private float previousHearts;
    private float targetHearts;
    private float maximumHearts;
    public final ModeSetting color = new ModeSetting("color", 0, new String[] { "DEFAULT", "HUD" });
    public final ModeSetting positionX = new ModeSetting("position-x", 1, new String[] { "LEFT", "MIDDLE", "RIGHT" });
    public final ModeSetting positionY = new ModeSetting("position-y", 1, new String[] { "TOP", "MIDDLE", "BOTTOM" });
    public final FloatSetting scale = new FloatSetting("scale", 1.0f, 0.5f, 1.5f);
    public final IntegerSetting offsetX = new IntegerSetting("offset-x", 0, -255, 255);
    public final IntegerSetting offsetY = new IntegerSetting("offset-y", 40, -255, 255);
    public final PercentSetting background = new PercentSetting("background", 25);
    public final BooleanSetting head = new BooleanSetting("head", true);
    public final BooleanSetting indicator = new BooleanSetting("indicator", true);
    public final BooleanSetting outline = new BooleanSetting("outline", false);
    public final BooleanSetting animations = new BooleanSetting("animations", true);
    public final BooleanSetting shadow = new BooleanSetting("shadow", true);
    public final BooleanSetting kaOnly = new BooleanSetting("ka-only", true);
    public final BooleanSetting chatPreview = new BooleanSetting("chat-preview", false);

    public TargetHUDModule() {
        super("TargetHUD", false, 0, true);
    }

    private EntityLivingBase selectTarget() {
        KillAuraModule combatModule = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        boolean combatEnabled = combatModule.isEnabled();
        if (combatEnabled && combatModule.isCombatInputAllowed() && EntityUtils.isLoaded((Entity) combatModule.getTargetEntity())) {
            return combatModule.getTargetEntity();
        }
        if (!this.kaOnly.getValue() && !this.targetTimer.hasElapsed(1500L) && EntityUtils.isLoaded((Entity) this.lastAttackedTarget)) {
            return this.lastAttackedTarget;
        }
        return this.chatPreview.getValue() && minecraft.field_71462_r instanceof GuiChat ? minecraft.field_71439_g : null;
    }

    private ResourceLocation skinFor(EntityLivingBase target) {
        NetworkPlayerInfo info;
        if (target instanceof EntityPlayer && (info = minecraft.func_147114_u().func_175104_a(target.func_70005_c_())) != null) {
            return info.func_178837_g();
        }
        return null;
    }

    private Color colorFor(EntityLivingBase target) {
        boolean player = target instanceof EntityPlayer;
        if (player) {
            if (EntityUtils.isFriend((EntityPlayer) target)) {
                return MyauClient.friendList.color();
            }
            if (EntityUtils.isEnemy((EntityPlayer) target)) {
                return MyauClient.enemyList.color();
            }
        }
        switch(this.color.getValue()) {
            case 0:
                {
                    return player ? EntityUtils.teamColor((EntityPlayer) target, 1.0f) : new Color(-1);
                }
            case 1:
                {
                    int hudColor = ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(hudColor);
                }
        }
        return new Color(-1);
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        ResourceLocation currentSkin;
        boolean animateHealth;
        if (!this.isEnabled()) {
            return;
        }
        if (minecraft.field_71439_g == null) {
            return;
        }
        EntityLivingBase previousTarget = this.displayedTarget;
        this.displayedTarget = this.selectTarget();
        if (this.displayedTarget == null) {
            return;
        }
        float playerHearts = (minecraft.field_71439_g.func_110143_aJ() + minecraft.field_71439_g.func_110139_bj()) / 2.0f;
        float absorptionHearts = this.displayedTarget.func_110139_bj() / 2.0f;
        float totalHearts = this.displayedTarget.func_110143_aJ() / 2.0f + absorptionHearts;
        if (this.displayedTarget != previousTarget) {
            this.skin = null;
            this.animationTimer.setLastResetMillis(0L);
            this.previousHearts = totalHearts;
            this.targetHearts = totalHearts;
        }
        if (!(animateHealth = this.animations.getValue().booleanValue()) || this.animationTimer.hasElapsed(150L)) {
            this.previousHearts = this.targetHearts;
            this.targetHearts = totalHearts;
            this.maximumHearts = this.displayedTarget.func_110138_aP() / 2.0f;
            if (this.previousHearts != this.targetHearts) {
                this.animationTimer.reset();
            }
        }
        if ((currentSkin = this.skinFor(this.displayedTarget)) != null) {
            this.skin = currentSkin;
        }
        float elapsed = Math.min(Math.max(this.animationTimer.elapsedMillis(), 0L), 150L);
        float interpolatedHearts = MathUtils.lerpFloat(this.previousHearts, this.targetHearts, elapsed / 150.0f);
        float healthFraction = Math.min(Math.max(interpolatedHearts / this.maximumHearts, 0.0f), 1.0f);
        Color targetColor = this.colorFor(this.displayedTarget);
        Color healthColor = this.color.getValue() == 0 ? ColorUtils.healthGradient(healthFraction) : targetColor;
        float advantage = Math.min(Math.max((playerHearts - totalHearts + 1.0f) / 2.0f, 0.0f), 1.0f);
        Color advantageColor = ColorUtils.healthGradient(advantage);
        ScaledResolution resolution = new ScaledResolution(minecraft);
        String nameLabel = ChatColor.formatText(String.format("&r%s&r", EntityUtils.formattedName((Entity) this.displayedTarget)));
        int nameWidth = minecraft.field_71466_p.func_78256_a(nameLabel);
        String healthLabel = ChatColor.formatText(String.format("&r&f%s%s\u2764&r", heartFormat.format(totalHearts), absorptionHearts > 0.0f ? "&6" : "&c"));
        int healthWidth = minecraft.field_71466_p.func_78256_a(healthLabel);
        String resultLabel = ChatColor.formatText(String.format("&r&l%s&r", totalHearts == playerHearts ? "D" : (totalHearts < playerHearts ? "W" : "L")));
        int resultWidth = minecraft.field_71466_p.func_78256_a(resultLabel);
        String differenceLabel = ChatColor.formatText(String.format("&r%s&r", totalHearts == playerHearts ? "0.0" : differenceFormat.format(playerHearts - totalHearts)));
        int differenceWidth = minecraft.field_71466_p.func_78256_a(differenceLabel);
        float firstRowWidth = (float) nameWidth + (this.indicator.getValue() ? 2.0f + (float) resultWidth + 2.0f : 0.0f);
        float secondRowWidth = (float) healthWidth + (this.indicator.getValue() ? 2.0f + (float) differenceWidth + 2.0f : 0.0f);
        float contentWidth = Math.max(firstRowWidth, secondRowWidth);
        float headWidth = this.head.getValue() && this.skin != null ? 25.0f : 0.0f;
        float width = Math.max(headWidth + 70.0f, headWidth + 2.0f + contentWidth + 2.0f);
        float height = 27.0f;
        float x = this.offsetX.getValue().floatValue() / this.scale.getValue();
        switch(this.positionX.getValue()) {
            case 1:
                {
                    x += (float) resolution.func_78326_a() / this.scale.getValue() / 2.0f - width / 2.0f;
                    break;
                }
            case 2:
                {
                    x *= -1.0f;
                    x += (float) resolution.func_78326_a() / this.scale.getValue() - width;
                }
        }
        float y = this.offsetY.getValue().floatValue() / this.scale.getValue();
        switch(this.positionY.getValue()) {
            case 1:
                {
                    y += (float) resolution.func_78328_b() / this.scale.getValue() / 2.0f - height / 2.0f;
                    break;
                }
            case 2:
                {
                    y *= -1.0f;
                    y += (float) resolution.func_78328_b() / this.scale.getValue() - height;
                }
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float) this.scale.getValue(), (float) this.scale.getValue(), (float) 0.0f);
        GlStateManager.func_179109_b((float) x, (float) y, (float) -450.0f);
        RenderUtils.beginOverlay();
        int backgroundColor = new Color(0.0f, 0.0f, 0.0f, (float) this.background.getValue() / 100.0f).getRGB();
        int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
        RenderUtils.drawBorderedRect(0.0f, 0.0f, width, height, 1.5f, backgroundColor, outlineColor);
        RenderUtils.fillRect(headWidth + 2.0f, 22.0f, width - 2.0f, 25.0f, ColorUtils.scaleBrightness(healthColor, 0.2f).getRGB());
        RenderUtils.fillRect(headWidth + 2.0f, 22.0f, headWidth + 2.0f + healthFraction * (width - 2.0f - headWidth - 2.0f), 25.0f, healthColor.getRGB());
        RenderUtils.endOverlay();
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        minecraft.field_71466_p.func_175065_a(nameLabel, headWidth + 2.0f, 2.0f, -1, this.shadow.getValue().booleanValue());
        minecraft.field_71466_p.func_175065_a(healthLabel, headWidth + 2.0f, 12.0f, -1, this.shadow.getValue().booleanValue());
        if (this.indicator.getValue()) {
            minecraft.field_71466_p.func_175065_a(resultLabel, width - 2.0f - (float) resultWidth, 2.0f, advantageColor.getRGB(), this.shadow.getValue().booleanValue());
            minecraft.field_71466_p.func_175065_a(differenceLabel, width - 2.0f - (float) differenceWidth, 12.0f, ColorUtils.scaleBrightness(advantageColor, 0.8f).getRGB(), this.shadow.getValue().booleanValue());
        }
        if (this.head.getValue() && this.skin != null) {
            GlStateManager.func_179124_c((float) 1.0f, (float) 1.0f, (float) 1.0f);
            minecraft.func_110434_K().func_110577_a(this.skin);
            Gui.func_152125_a((int) 2, (int) 2, (float) 8.0f, (float) 8.0f, (int) 8, (int) 8, (int) 23, (int) 23, (float) 64.0f, (float) 64.0f);
            Gui.func_152125_a((int) 2, (int) 2, (float) 40.0f, (float) 8.0f, (int) 8, (int) 8, (int) 23, (int) 23, (float) 64.0f, (float) 64.0f);
            GlStateManager.func_179124_c((float) 1.0f, (float) 1.0f, (float) 1.0f);
        }
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        boolean attack = this.isEnabled();
        if (!attack || event.isCancelled()) {
            return;
        }
        attack = event.getPacket() instanceof C02PacketUseEntity;
        if (!attack) {
            return;
        }
        Packet<?> raw = event.getPacket();
        C02PacketUseEntity packet = (C02PacketUseEntity) raw;
        if (packet.func_149565_c() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }
        Entity entity = packet.func_149564_a((World) minecraft.field_71441_e);
        if (!(entity instanceof EntityLivingBase) || entity instanceof EntityArmorStand) {
            return;
        }
        this.targetTimer.reset();
        this.lastAttackedTarget = (EntityLivingBase) entity;
    }
}
