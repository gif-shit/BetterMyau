package myau;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import myau.mixin.AccessorRenderManager;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

public class BedTrackerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final Module owner;
    private final BooleanSetting tracking;
    private long scanAt;
    private String nearestName;
    private double nearestThreat = -1;
    private final LinkedHashMap<String, Long> lastAlertMillisByPlayer = new LinkedHashMap();
    private final LinkedHashSet<EntityEnderPearl> seenPearls = new LinkedHashSet();
    private final LinkedHashSet<String> ignoredTeammates = new LinkedHashSet();
    private BlockPos trackedBed = null;
    private long lastMacroMillis = -1L;
    private boolean waitingForSpawnTeleport = false;
    public final BooleanSetting alerts = new BooleanSetting("alerts", true, this::showTracker);
    public final IntegerSetting alertsRange = new IntegerSetting("alerts-range", 48, 8, 128, () -> showTracker() && alerts.getValue());
    public final BooleanSetting alertsOnPearl = new BooleanSetting("alerts-on-pearl", true, this::showTracker);
    public final ModeSetting alertsSound = new ModeSetting("alerts-sound", 1, new String[] { "NONE", "MEOW", "ANVIL" }, this::isAlertsSoundVisible);
    public final IntegerSetting alertsFrequency = new IntegerSetting("alerts-frequency", 5, 1, 30, this::isAlertsFrequencyVisible);
    public final BooleanSetting macro = new BooleanSetting("macro", false, this::showTracker);
    public final IntegerSetting macroRange = new IntegerSetting("macro-range", 24, 8, 128, () -> showTracker() && macro.getValue());
    public final BooleanSetting macroOnPearl = new BooleanSetting("macro-on-pearl", false, this::showTracker);
    public final StringSetting macroText = new StringSetting("macro-text", "/lobby", this::isMacroTextVisible);
    public final IntegerSetting macroDelay = new IntegerSetting("macro-delay", 1, 1, 10, this::isMacroDelayVisible);
    public final BooleanSetting marker = new BooleanSetting("bed-marker", true, this::showTracker);
    public final BooleanSetting hud = new BooleanSetting("hud", true, this::showTracker);
    public final ModeSetting hudPositionX = new ModeSetting("hud-position-x", 2, new String[] { "LEFT", "MIDDLE", "RIGHT" }, () -> showTracker() && hud.getValue());
    public final ModeSetting hudPositionY = new ModeSetting("hud-position-y", 0, new String[] { "TOP", "MIDDLE", "BOTTOM" }, () -> showTracker() && hud.getValue());
    public final IntegerSetting hudOffsetX = new IntegerSetting("hud-offset-x", 8, 0, 255, () -> showTracker() && hud.getValue());
    public final IntegerSetting hudOffsetY = new IntegerSetting("hud-offset-y", 8, 0, 255, () -> showTracker() && hud.getValue());
    public final FloatSetting hudScale = new FloatSetting("hud-scale", 1.0f, 0.5f, 1.5f, () -> showTracker() && hud.getValue());
    public final BooleanSetting hudShadow = new BooleanSetting("hud-shadow", true, () -> showTracker() && hud.getValue());

    public BedTrackerModule(Module owner, BooleanSetting tracking) {
        super("BedWars", false, 0, true);
        this.owner = owner;
        this.tracking = tracking;
    }

    @Override
    public boolean isEnabled() {
        return owner.isEnabled() && tracking.getValue();
    }

    private boolean showTracker() {
        return tracking.getValue();
    }

    public void trackBed(BlockPos bed) {
        this.trackedBed = bed;
        if (owner instanceof myau.openb4.module.minigames.BedWarsModule) myau.openb4.module.minigames.BedWarsModule.ownBed = bed;
        this.scanAt = 0;
    }

    public BlockPos getTrackedBed() {
        return this.trackedBed;
    }

    private void playAlertSound() {
        switch(this.alertsSound.getValue()) {
            case 1:
                {
                    SoundUtils.play("mob.cat.meow");
                    break;
                }
            case 2:
                {
                    SoundUtils.play("random.anvil_land");
                    break;
                }
        }
    }

    private boolean isBed(BlockPos position) {
        return position != null && minecraft.field_71441_e != null && minecraft.field_71441_e.func_180495_p(position).func_177230_c() == Blocks.field_150324_C;
    }

    private boolean claimPlayerAlert(String playerName, long nowMillis) {
        Long previousAlertMillis = this.lastAlertMillisByPlayer.get(playerName);
        if (previousAlertMillis != null && previousAlertMillis + (long) this.alertsFrequency.getValue() * 1000L > nowMillis) {
            return false;
        }
        this.lastAlertMillisByPlayer.put(playerName, nowMillis);
        return true;
    }

    private boolean claimMacro(long nowMillis) {
        if (this.lastMacroMillis + (long) this.macroDelay.getValue() * 1000L > nowMillis) {
            return false;
        }
        this.lastMacroMillis = nowMillis;
        return true;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled() || minecraft.field_71441_e == null || minecraft.field_71439_g == null) return;
        long nowMillis = System.currentTimeMillis();
        if (this.scanAt > 0 && nowMillis >= this.scanAt) {
            this.scanAt = 0;
            this.scanForNearestBed();
        }
        this.nearestThreat = -1;
        this.nearestName = null;
        if (!this.isBed(this.trackedBed)) return;
        boolean sentAlert = false;
        boolean runMacro = false;
        for (Entity entity : minecraft.field_71441_e.field_72996_f) {
            EntityEnderPearl pearl;
            if (!(entity instanceof EntityEnderPearl) || !this.seenPearls.add(pearl = (EntityEnderPearl) entity))
                continue;
            if (this.alertsOnPearl.getValue()) {
                ChatUtils.addFormattedMessage(String.format("%s%s: &fDetected &5Ender Pearl&r &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName()));
                sentAlert = true;
            }
            if (!this.macroOnPearl.getValue() || !this.claimMacro(nowMillis))
                continue;
            runMacro = true;
        }
        for (Entity entity : minecraft.field_71441_e.field_72996_f) {
            boolean macroThreat;
            boolean holdsEnderPearl;
            EntityPlayer player;
            if (!(entity instanceof EntityPlayer)
                    || entity == minecraft.field_71439_g
                    || !entity.func_70089_S()
                    || EntityUtils.isBot(player = (EntityPlayer) entity)
                    || this.ignoredTeammates.contains(player.func_70005_c_()))
                continue;
            if (EntityUtils.isTeammate(player)) {
                this.ignoredTeammates.add(player.func_70005_c_());
                continue;
            }
            double distance = player.func_70011_f((double) this.trackedBed.func_177958_n() + 0.5, (double) this.trackedBed.func_177956_o() + 0.5, (double) this.trackedBed.func_177952_p() + 0.5);
            if (this.nearestThreat < 0 || distance < this.nearestThreat) {
                this.nearestThreat = distance;
                this.nearestName = player.func_70005_c_();
            }
            String playerName = player.func_70005_c_();
            String displayName = player.func_145748_c_().func_150254_d();
            ItemStack heldItem = player.func_70694_bm();
            boolean bl = holdsEnderPearl = heldItem != null && heldItem.func_77973_b() instanceof ItemEnderPearl;
            if (this.alerts.getValue()
                    && distance < (double) this.alertsRange.getValue()
                    && this.claimPlayerAlert(playerName, nowMillis)) {
                ChatUtils.addFormattedMessage(String.format("%s%s: %s&r &fis %d blocks away from your bed &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName(), displayName, (int) distance + 1));
                sentAlert = true;
            }
            if (this.alertsOnPearl.getValue() && holdsEnderPearl && this.claimPlayerAlert(playerName, nowMillis)) {
                ChatUtils.addFormattedMessage(String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l\u26a0&r", "&7[&bMyau&7] &r", this.getName(), displayName));
                sentAlert = true;
            }
            if (!(macroThreat = this.macro.getValue()
                    && distance < (double) this.macroRange.getValue()
                    || this.macroOnPearl.getValue()
                    && holdsEnderPearl)
                    || !this.claimMacro(nowMillis))
                continue;
            runMacro = true;
        }
        if (sentAlert) {
            this.playAlertSound();
        }
        if (runMacro) {
            ChatUtils.addRawMessage(String.format(ChatColor.formatText("%s%s: &fRunning &6%s&r"), ChatColor.formatText("&7[&bMyau&7] &r"), this.getName(), this.macroText.getValue()));
            ChatUtils.sendMessage((String) this.macroText.getValue());
        }
    }

    @EventHandler(priority = -50)
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled()
                || !this.hud.getValue()
                || minecraft.field_71441_e == null
                || minecraft.field_71439_g == null
                || minecraft.field_71474_y.field_74330_P
                || minecraft.field_71462_r != null
                && !(minecraft.field_71462_r instanceof GuiChat)) {
            return;
        }
        boolean tracked = this.trackedBed != null;
        boolean loaded = tracked && minecraft.field_71441_e.func_175667_e(this.trackedBed);
        boolean intact = loaded && this.isBed(this.trackedBed);
        String status = BedTrackerHud.status(tracked, loaded, intact, this.nearestThreat, this.alertsRange.getValue());
        if (!tracked) status = BedTrackerHud.searching(System.currentTimeMillis());
        String distance = "";
        if (tracked) {
            int blocks = (int) Math.ceil(Math.sqrt(minecraft.field_71439_g.func_174831_c(this.trackedBed)));
            distance = blocks + "m from your bed";
        }
        String threat = this.nearestName == null ? "No enemy in sight" : this.nearestName + "  " + (int) Math.ceil(this.nearestThreat) + "m to bed";
        if (!tracked) threat = "";
        else if (!loaded) threat = "Return closer to check your bed";
        else if (!intact) threat = "Your bed has been destroyed";
        int width = BedTrackerHud.width(threat, tracked);
        float scale = this.hudScale.getValue();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        float x = BedTrackerHud.anchor(this.hudPositionX.getValue(), resolution.func_78326_a() / scale, width, this.hudOffsetX.getValue() / scale);
        float y = BedTrackerHud.anchor(this.hudPositionY.getValue(), resolution.func_78328_b() / scale, BedTrackerHud.height(tracked), this.hudOffsetY.getValue() / scale);
        GlStateManager.func_179094_E();
        try {
            GlStateManager.func_179152_a(scale, scale, 1);
            GlStateManager.func_179109_b(x, y, 0);
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b(770, 771);
            BedTrackerHud.draw(width, status, distance, threat,
                    intact ? BedTrackerHud.meter(this.nearestThreat, this.alertsRange.getValue()) : 0,
                    BedTrackerHud.accent(tracked, loaded, intact, this.nearestThreat, this.alertsRange.getValue()), this.hudShadow.getValue(), tracked);
        } finally {
            GlStateManager.func_179117_G();
            GlStateManager.func_179084_k();
            GlStateManager.func_179126_j();
            GlStateManager.func_179121_F();
        }
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled() || !this.marker.getValue() || !this.isBed(this.trackedBed)) return;
        BlockPos first = this.trackedBed;
        BlockPos second = first;
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            BlockPos adjacent = first.func_177972_a(side);
            if (this.isBed(adjacent)) {
                second = adjacent;
                break;
            }
        }
        AccessorRenderManager camera = (AccessorRenderManager) minecraft.func_175598_ae();
        AxisAlignedBB bounds = new AxisAlignedBB(Math.min(first.func_177958_n(), second.func_177958_n()), first.func_177956_o(), Math.min(first.func_177952_p(), second.func_177952_p()),
                Math.max(first.func_177958_n(), second.func_177958_n()) + 1, first.func_177956_o() + 0.57, Math.max(first.func_177952_p(), second.func_177952_p()) + 1)
                .func_72317_d(-camera.getRenderPosX(), -camera.getRenderPosY(), -camera.getRenderPosZ());
        int color = BedTrackerHud.accent(true, true, true, this.nearestThreat, this.alertsRange.getValue());
        int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
        RenderUtils.beginOverlay();
        try {
            RenderUtils.fillBounds(bounds, red, green, blue, 34);
            RenderUtils.outlineBounds(bounds, red, green, blue, 200, 1);
        } finally {
            RenderUtils.endOverlay();
            GlStateManager.func_179117_G();
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.onDisable();
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        String message;
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat
                && isBedWarsStartMessage(message = ((S02PacketChat) event.getPacket()).func_148915_c().func_150254_d())) {
            this.lastAlertMillisByPlayer.clear();
            this.seenPearls.clear();
            this.ignoredTeammates.clear();
            this.trackedBed = null;
            this.waitingForSpawnTeleport = true;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waitingForSpawnTeleport) {
            this.waitingForSpawnTeleport = false;
            this.scanAt = System.currentTimeMillis() + 3000;
        }
    }

    private static boolean isBedWarsStartMessage(String message) {
        return message.contains("\u00a7e\u00a7lProtect your bed and destroy the enemy bed")
                || message.contains("\u00a7e\u00a7lDestroy the enemy bed and then eliminate them")
                || message.contains("\u00a7e\u00a7lEvery few seconds brings a new surprise");
    }

    private void scanForNearestBed() {
        if (minecraft.field_71441_e == null || minecraft.field_71439_g == null) {
            return;
        }
        int playerX = MathHelper.func_76128_c((double) minecraft.field_71439_g.field_70165_t);
        int playerY = MathHelper.func_76128_c((double) (minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e()));
        int playerZ = MathHelper.func_76128_c((double) minecraft.field_71439_g.field_70161_v);
        BlockPos nearestBed = null;
        int nearestDistance = -1;
        for (int x = playerX - 25; x <= playerX + 25; ++x) {
            for (int y = playerY - 25; y <= playerY + 25; ++y) {
                for (int z = playerZ - 25; z <= playerZ + 25; ++z) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (!this.isBed(candidate))
                        continue;
                    int distance = Math.abs(x - playerX) + Math.abs(y - playerY) + Math.abs(z - playerZ);
                    if (nearestDistance >= 0 && nearestDistance <= distance)
                        continue;
                    nearestBed = candidate;
                    nearestDistance = distance;
                }
            }
        }
        if (nearestBed != null) {
            this.trackBed(nearestBed);
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
        this.scanAt = 0;
        this.waitingForSpawnTeleport = false;
        this.nearestThreat = -1;
        this.nearestName = null;
    }

    private boolean isMacroDelayVisible() {
        return showTracker() && (this.macro.getValue() || this.macroOnPearl.getValue());
    }

    private boolean isMacroTextVisible() {
        return showTracker() && (this.macro.getValue() || this.macroOnPearl.getValue());
    }

    private boolean isAlertsFrequencyVisible() {
        return showTracker() && (this.alerts.getValue() || this.alertsOnPearl.getValue());
    }

    private boolean isAlertsSoundVisible() {
        return showTracker() && (this.alerts.getValue() || this.alertsOnPearl.getValue());
    }
}
