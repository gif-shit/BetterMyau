package myau;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import myau.BooleanSetting;
import myau.ChatColor;
import myau.ChatUtils;
import myau.ColorUtils;
import myau.EntityUtils;
import myau.EventHandler;
import myau.FloatSetting;
import myau.IntegerSetting;
import myau.ModeSetting;
import myau.Module;
import myau.PacketReceiveEvent;
import myau.PostTickEvent;
import myau.RenderOverlayEvent;
import myau.SoundUtils;
import myau.StringSetting;
import myau.WorldLoadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

public class BedTrackerModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ScheduledExecutorService scannerExecutor = Executors.newScheduledThreadPool(1);
    private final LinkedHashMap<String, Long> lastAlertMillisByPlayer = new LinkedHashMap();
    private final LinkedHashSet<EntityEnderPearl> seenPearls = new LinkedHashSet();
    private final LinkedHashSet<String> ignoredTeammates = new LinkedHashSet();
    private final Color inactiveColor = new Color(ChatColor.WHITE.getRgb());
    private final Color farColor = new Color(ChatColor.RED.getRgb());
    private final Color warningColor = new Color(ChatColor.YELLOW.getRgb());
    private final Color safeColor = new Color(ChatColor.GREEN.getRgb());
    private BlockPos trackedBed = null;
    private long lastMacroMillis = -1L;
    private boolean waitingForSpawnTeleport = false;
    public final BooleanSetting alerts = new BooleanSetting("alerts", true);
    public final IntegerSetting alertsRange = new IntegerSetting("alerts-range", 48, 8, 128, this.alerts::getValue);
    public final BooleanSetting alertsOnPearl = new BooleanSetting("alerts-on-pearl", true);
    public final ModeSetting alertsSound = new ModeSetting("alerts-sound", 1, new String[]{"NONE", "MEOW", "ANVIL"}, this::isAlertsSoundVisible);
    public final IntegerSetting alertsFrequency = new IntegerSetting("alerts-frequency", 5, 1, 30, this::isAlertsFrequencyVisible);
    public final BooleanSetting macro = new BooleanSetting("macro", false);
    public final IntegerSetting macroRange = new IntegerSetting("macro-range", 24, 8, 128, this.macro::getValue);
    public final BooleanSetting macroOnPearl = new BooleanSetting("macro-on-pearl", false);
    public final StringSetting macroText = new StringSetting("macro-text", "/lobby", this::isMacroTextVisible);
    public final IntegerSetting macroDelay = new IntegerSetting("macro-delay", 1, 1, 10, this::isMacroDelayVisible);
    public final BooleanSetting hud = new BooleanSetting("hud", true);
    public final ModeSetting hudPositionX = new ModeSetting("hud-position-x", 0, new String[]{"LEFT", "MIDDLE", "RIGHT"}, this.hud::getValue);
    public final ModeSetting hudPositionY = new ModeSetting("hud-position-y", 0, new String[]{"TOP", "MIDDLE", "BOTTOM"}, this.hud::getValue);
    public final IntegerSetting hudOffsetX = new IntegerSetting("hud-offset-x", 2, 0, 255, this.hud::getValue);
    public final IntegerSetting hudOffsetY = new IntegerSetting("hud-offset-y", 2, 0, 255, this.hud::getValue);
    public final FloatSetting hudScale = new FloatSetting("hud-scale", Float.valueOf(1.0f), Float.valueOf(0.5f), Float.valueOf(1.5f), this.hud::getValue);
    public final BooleanSetting hudShadow = new BooleanSetting("hud-shadow", true, this.hud::getValue);

    public BedTrackerModule() {
        super("BedTracker", false, 0, true);
    }

    private void playAlertSound() {
        switch ((Integer)this.alertsSound.getValue()) {
            case 1: {
                SoundUtils.play("mob.cat.meow");
                break;
            }
            case 2: {
                SoundUtils.play("random.anvil_land");
                break;
            }
        }
    }

    private Color getDistanceColor(int distance) {
        if (distance < 0) {
            return this.inactiveColor;
        }
        if (distance <= 100) {
            return this.safeColor;
        }
        if (distance <= 114) {
            return ColorUtils.interpolate((float)(114 - distance) / 14.0f, this.warningColor, this.safeColor);
        }
        if (distance > 128) {
            return this.farColor;
        }
        return ColorUtils.interpolate((float)(128 - distance) / 14.0f, this.farColor, this.warningColor);
    }

    private boolean isBed(BlockPos position) {
        return position != null && BedTrackerModule.minecraft.field_71441_e.func_180495_p(position).func_177230_c() == Blocks.field_150324_C;
    }

    private boolean claimPlayerAlert(String playerName, long nowMillis) {
        Long previousAlertMillis = this.lastAlertMillisByPlayer.get(playerName);
        if (previousAlertMillis != null && previousAlertMillis + (long)((Integer)this.alertsFrequency.getValue()).intValue() * 1000L > nowMillis) {
            return false;
        }
        this.lastAlertMillisByPlayer.put(playerName, nowMillis);
        return true;
    }

    private boolean claimMacro(long nowMillis) {
        if (this.lastMacroMillis + (long)((Integer)this.macroDelay.getValue()).intValue() * 1000L > nowMillis) {
            return false;
        }
        this.lastMacroMillis = nowMillis;
        return true;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || !this.isBed(this.trackedBed)) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        boolean sentAlert = false;
        boolean runMacro = false;
        for (Entity entity : BedTrackerModule.minecraft.field_71441_e.field_72996_f) {
            EntityEnderPearl pearl;
            if (!(entity instanceof EntityEnderPearl) || !this.seenPearls.add(pearl = (EntityEnderPearl)entity)) continue;
            if (((Boolean)this.alertsOnPearl.getValue()).booleanValue()) {
                ChatUtils.addFormattedMessage(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName()));
                sentAlert = true;
            }
            if (!((Boolean)this.macroOnPearl.getValue()).booleanValue() || !this.claimMacro(nowMillis)) continue;
            runMacro = true;
        }
        for (Entity entity : BedTrackerModule.minecraft.field_71441_e.field_72996_f) {
            boolean macroThreat;
            boolean holdsEnderPearl;
            EntityPlayer player;
            if (!(entity instanceof EntityPlayer) || EntityUtils.isBot(player = (EntityPlayer)entity) || this.ignoredTeammates.contains(player.func_70005_c_())) continue;
            if (EntityUtils.isTeammate(player)) {
                this.ignoredTeammates.add(player.func_70005_c_());
                continue;
            }
            double distance = player.func_70011_f((double)this.trackedBed.func_177958_n() + 0.5, (double)this.trackedBed.func_177956_o() + 0.5, (double)this.trackedBed.func_177952_p() + 0.5);
            String playerName = player.func_70005_c_();
            String displayName = player.func_145748_c_().func_150254_d();
            ItemStack heldItem = player.func_70694_bm();
            boolean bl = holdsEnderPearl = heldItem != null && heldItem.func_77973_b() instanceof ItemEnderPearl;
            if (((Boolean)this.alerts.getValue()).booleanValue() && distance < (double)((Integer)this.alertsRange.getValue()).intValue() && this.claimPlayerAlert(playerName, nowMillis)) {
                ChatUtils.addFormattedMessage(String.format("%s%s: %s&r &fis %d blocks away from your bed &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName(), displayName, (int)distance + 1));
                sentAlert = true;
            }
            if (((Boolean)this.alertsOnPearl.getValue()).booleanValue() && holdsEnderPearl && this.claimPlayerAlert(playerName, nowMillis)) {
                ChatUtils.addFormattedMessage(String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName(), displayName));
                sentAlert = true;
            }
            if (!(macroThreat = (Boolean)this.macro.getValue() != false && distance < (double)((Integer)this.macroRange.getValue()).intValue() || (Boolean)this.macroOnPearl.getValue() != false && holdsEnderPearl) || !this.claimMacro(nowMillis)) continue;
            runMacro = true;
        }
        if (sentAlert) {
            this.playAlertSound();
        }
        if (runMacro) {
            ChatUtils.addRawMessage(String.format(ChatColor.formatText("%s%s: &fRunning &6%s&r"), ChatColor.formatText("&7[&bMyau&7] &r"), this.getName(), this.macroText.getValue()));
            ChatUtils.sendMessage((String)this.macroText.getValue());
        }
    }

    @EventHandler(priority=-50)
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled() || !((Boolean)this.hud.getValue()).booleanValue() || BedTrackerModule.minecraft.field_71441_e == null || BedTrackerModule.minecraft.field_71439_g == null || BedTrackerModule.minecraft.field_71474_y.field_74330_P || BedTrackerModule.minecraft.field_71462_r != null && !(BedTrackerModule.minecraft.field_71462_r instanceof GuiChat)) {
            return;
        }
        boolean hasBed = this.isBed(this.trackedBed);
        int distance = 0;
        if (hasBed) {
            double deltaX = BedTrackerModule.minecraft.field_71439_g.field_70165_t - (double)this.trackedBed.func_177958_n();
            double deltaZ = BedTrackerModule.minecraft.field_71439_g.field_70161_v - (double)this.trackedBed.func_177952_p();
            distance = (int)Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) + 1;
        }
        String status = hasBed ? "&atrue&r" : "&cfalse&r";
        String distanceText = "";
        if (hasBed) {
            String warning = distance >= 128 ? " &c&l\u26a0&r" : "";
            distanceText = String.format(" &7| &fDistance: &r%d%s", distance, warning);
        }
        String message = ChatColor.formatText(String.format("&fBed: %s%s", status, distanceText));
        ScaledResolution resolution = new ScaledResolution(minecraft);
        float textWidth = BedTrackerModule.minecraft.field_71466_p.func_78256_a(message);
        float textHeight = (float)BedTrackerModule.minecraft.field_71466_p.field_78288_b - 1.0f;
        float scale = ((Float)this.hudScale.getValue()).floatValue();
        float x = (float)((Integer)this.hudOffsetX.getValue()).intValue() / scale;
        float y = (float)((Integer)this.hudOffsetY.getValue()).intValue() / scale;
        switch ((Integer)this.hudPositionX.getValue()) {
            case 0: {
                x += 1.0f;
                break;
            }
            case 1: {
                x += (float)resolution.func_78326_a() / scale / 2.0f - textWidth / 2.0f;
                break;
            }
            case 2: {
                x = (float)resolution.func_78326_a() / scale - textWidth - x - 1.0f;
                break;
            }
        }
        switch ((Integer)this.hudPositionY.getValue()) {
            case 0: {
                y += 1.0f;
                break;
            }
            case 1: {
                y += (float)resolution.func_78328_b() / scale / 2.0f - textHeight / 2.0f;
                break;
            }
            case 2: {
                y = (float)resolution.func_78328_b() / scale - textHeight - y - 1.0f;
                break;
            }
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float)scale, (float)scale, (float)1.0f);
        GlStateManager.func_179109_b((float)x, (float)y, (float)0.0f);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int)770, (int)771);
        BedTrackerModule.minecraft.field_71466_p.func_175065_a(message, 0.0f, 0.0f, this.getDistanceColor(distance).getRGB(), ((Boolean)this.hudShadow.getValue()).booleanValue());
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.waitingForSpawnTeleport = false;
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        String message;
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat && BedTrackerModule.isBedWarsStartMessage(message = ((S02PacketChat)event.getPacket()).func_148915_c().func_150254_d())) {
            this.lastAlertMillisByPlayer.clear();
            this.seenPearls.clear();
            this.ignoredTeammates.clear();
            this.trackedBed = null;
            this.waitingForSpawnTeleport = true;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForSpawnTeleport) {
            this.waitingForSpawnTeleport = false;
            this.scannerExecutor.schedule(this::scanForNearestBed, 3L, TimeUnit.SECONDS);
        }
    }

    private static boolean isBedWarsStartMessage(String message) {
        return message.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy bed") || message.contains("\u00a7e\u00a7lDestroy the enemy bed and then eliminate them") || message.contains("\u00a7e\u00a7lEvery few seconds brings a new surprise");
    }

    private void scanForNearestBed() {
        if (BedTrackerModule.minecraft.field_71441_e == null || BedTrackerModule.minecraft.field_71439_g == null) {
            return;
        }
        int playerX = MathHelper.func_76128_c((double)BedTrackerModule.minecraft.field_71439_g.field_70165_t);
        int playerY = MathHelper.func_76128_c((double)(BedTrackerModule.minecraft.field_71439_g.field_70163_u + (double)BedTrackerModule.minecraft.field_71439_g.func_70047_e()));
        int playerZ = MathHelper.func_76128_c((double)BedTrackerModule.minecraft.field_71439_g.field_70161_v);
        BlockPos nearestBed = null;
        int nearestDistance = -1;
        for (int x = playerX - 25; x <= playerX + 25; ++x) {
            for (int y = playerY - 25; y <= playerY + 25; ++y) {
                for (int z = playerZ - 25; z <= playerZ + 25; ++z) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!this.isBed(candidate)) continue;
                    int distance = Math.abs(x - playerX) + Math.abs(y - playerY) + Math.abs(z - playerZ);
                    if (nearestDistance >= 0 && nearestDistance <= distance) continue;
                    nearestBed = candidate;
                    nearestDistance = distance;
                }
            }
        }
        if (nearestBed != null) {
            this.trackedBed = nearestBed;
            ChatUtils.addFormattedMessage(String.format("%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l\u2714&r", "&7[&bMyau&7] &r", this.getName(), nearestBed.func_177958_n(), nearestBed.func_177956_o(), nearestBed.func_177952_p()));
            SoundUtils.play("note.pling");
        }
    }

    @Override
    public void onDisable() {
        this.lastAlertMillisByPlayer.clear();
        this.seenPearls.clear();
        this.ignoredTeammates.clear();
        this.trackedBed = null;
    }

    private boolean isMacroDelayVisible() {
        return (Boolean)this.macro.getValue() != false || (Boolean)this.macroOnPearl.getValue() != false;
    }

    private boolean isMacroTextVisible() {
        return (Boolean)this.macro.getValue() != false || (Boolean)this.macroOnPearl.getValue() != false;
    }

    private boolean isAlertsFrequencyVisible() {
        return (Boolean)this.alerts.getValue() != false || (Boolean)this.alertsOnPearl.getValue() != false;
    }

    private boolean isAlertsSoundVisible() {
        return (Boolean)this.alerts.getValue() != false || (Boolean)this.alertsOnPearl.getValue() != false;
    }
}
