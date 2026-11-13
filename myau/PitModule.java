package myau;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import myau.modules.Pit;
import myau.modules.pit.PitMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.EnumChatFormatting;

public final class PitModule extends Module implements PitMinecraft.Bindings {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final PitMinecraft host = new PitMinecraft(minecraft, this);
    private final Pit engine = new Pit(host);
    private final Map<UUID, String> notified = new HashMap<>();
    private volatile boolean pitServer;
    private double spawnLevel = Double.NaN;
    private long nextDenick;
    private boolean scheduleNotice;
    public final BooleanSetting pitOnly = new BooleanSetting("pit-only", true);
    public final BooleanSetting playerList = new BooleanSetting("player-list", false);
    public final BooleanSetting playerListFriends = new BooleanSetting("player-list-friends", true, playerList::getValue);
    public final BooleanSetting playerListEnemies = new BooleanSetting("player-list-enemies", true, playerList::getValue);
    public final BooleanSetting playerListDarks = new BooleanSetting("player-list-darks", true, playerList::getValue);
    public final BooleanSetting playerListVenoms = new BooleanSetting("player-list-venoms", true, playerList::getValue);
    public final BooleanSetting playerListRegularity = new BooleanSetting("player-list-regularity", true, playerList::getValue);
    public final BooleanSetting playerListBountyHunters = new BooleanSetting("player-list-bounty-hunters", true, playerList::getValue);
    public final BooleanSetting playerListNicked = new BooleanSetting("player-list-nicked", true, playerList::getValue);
    public final BooleanSetting playerListEnchants = new BooleanSetting("player-list-enchants", false, playerList::getValue);
    public final IntegerSetting playerListMaxPlayers = new IntegerSetting("player-list-max-players", 10, 1, 80, playerList::getValue);
    public final ModeSetting playerListAlerts = new ModeSetting("player-list-alerts", 0, new String[] { "OFF", "JOIN", "LEAVE", "BOTH" }, playerList::getValue);
    public final ModeSetting playerListPositionX = new ModeSetting("player-list-position-x", 0, new String[] { "LEFT", "MIDDLE", "RIGHT" }, playerList::getValue);
    public final ModeSetting playerListPositionY = new ModeSetting("player-list-position-y", 1, new String[] { "TOP", "MIDDLE", "BOTTOM" }, playerList::getValue);
    public final IntegerSetting playerListOffsetX = new IntegerSetting("player-list-offset-x", 2, 0, 255, playerList::getValue);
    public final IntegerSetting playerListOffsetY = new IntegerSetting("player-list-offset-y", 0, 0, 255, playerList::getValue);
    public final FloatSetting playerListScale = new FloatSetting("player-list-scale", 1.0f, 0.5f, 1.5f, playerList::getValue);
    public final BooleanSetting denicker = new BooleanSetting("denicker", true);
    public final BooleanSetting denickerNotifications = new BooleanSetting("denicker-notifications", true, denicker::getValue);
    public final BooleanSetting autoMath = new BooleanSetting("auto-math", false);
    public final BooleanSetting chatFilter = new BooleanSetting("chat-filter", false);
    public final BooleanSetting chatFilterStreaks = new BooleanSetting("chat-filter-streaks", true, chatFilter::getValue);
    public final BooleanSetting chatFilterBounty = new BooleanSetting("chat-filter-bounty", false, chatFilter::getValue);
    public final BooleanSetting eventList = new BooleanSetting("event-list", false);
    public final ModeSetting eventListPositionX = new ModeSetting("event-list-position-x", 2, new String[] { "LEFT", "MIDDLE", "RIGHT" }, eventList::getValue);
    public final ModeSetting eventListPositionY = new ModeSetting("event-list-position-y", 2, new String[] { "TOP", "MIDDLE", "BOTTOM" }, eventList::getValue);
    public final IntegerSetting eventListOffsetX = new IntegerSetting("event-list-offset-x", 2, 0, 255, eventList::getValue);
    public final IntegerSetting eventListOffsetY = new IntegerSetting("event-list-offset-y", 2, 0, 255, eventList::getValue);
    public final FloatSetting eventListScale = new FloatSetting("event-list-scale", 1.0f, 0.5f, 1.5f, eventList::getValue);
    public final BooleanSetting inventoryCleaner = new BooleanSetting("inventory-cleaner", false);
    public final BooleanSetting throwIronArmor = new BooleanSetting("throw-iron-armor", true, inventoryCleaner::getValue);
    public final BooleanSetting throwIronSwords = new BooleanSetting("throw-iron-swords", true, inventoryCleaner::getValue);
    public final ModeSetting hideFarPlayer = new ModeSetting("hide-far-player", 0, new String[] { "NONE", "CLOSE", "EXTENDED", "SMART" });
    public final ModeSetting hidePitSpawn = new ModeSetting("hide-pit-spawn", 0, new String[] { "NONE", "VISUALS", "ENTITY" });
    public final BooleanSetting showInSpawn = new BooleanSetting("show-in-spawn", true, () -> hidePitSpawn.getValue() != 0);

    public PitModule() {
        super("Pit", false, 0);
    }

    @Override
    public void onSettingChanged(String name) {
        engine.settings.pitOnly = pitOnly.getValue();
        engine.settings.playerList = playerList.getValue();
        engine.settings.playerListFriends = playerListFriends.getValue();
        engine.settings.playerListEnemies = playerListEnemies.getValue();
        engine.settings.playerListDarks = playerListDarks.getValue();
        engine.settings.playerListVenoms = playerListVenoms.getValue();
        engine.settings.playerListRegularity = playerListRegularity.getValue();
        engine.settings.playerListBountyHunters = playerListBountyHunters.getValue();
        engine.settings.playerListNicked = playerListNicked.getValue();
        engine.settings.playerListEnchants = playerListEnchants.getValue();
        engine.settings.playerListMaxPlayers = playerListMaxPlayers.getValue();
        engine.settings.playerListAlerts = Pit.Alerts.values()[playerListAlerts.getValue()];
        engine.settings.playerListPositionX = Pit.Horizontal.values()[playerListPositionX.getValue()];
        engine.settings.playerListPositionY = Pit.Vertical.values()[playerListPositionY.getValue()];
        engine.settings.playerListOffsetX = playerListOffsetX.getValue();
        engine.settings.playerListOffsetY = playerListOffsetY.getValue();
        engine.settings.playerListScale = playerListScale.getValue();
        engine.settings.denicker = denicker.getValue();
        engine.settings.denickerNotifications = denickerNotifications.getValue();
        engine.settings.autoMath = autoMath.getValue();
        engine.settings.chatFilter = chatFilter.getValue();
        engine.settings.chatFilterStreaks = chatFilterStreaks.getValue();
        engine.settings.chatFilterBounty = chatFilterBounty.getValue();
        engine.settings.eventList = eventList.getValue();
        engine.settings.eventListPositionX = Pit.Horizontal.values()[eventListPositionX.getValue()];
        engine.settings.eventListPositionY = Pit.Vertical.values()[eventListPositionY.getValue()];
        engine.settings.eventListOffsetX = eventListOffsetX.getValue();
        engine.settings.eventListOffsetY = eventListOffsetY.getValue();
        engine.settings.eventListScale = eventListScale.getValue();
        engine.settings.inventoryCleaner = inventoryCleaner.getValue();
        engine.settings.throwIronArmor = throwIronArmor.getValue();
        engine.settings.throwIronSwords = throwIronSwords.getValue();
        engine.settings.hideFarPlayer = Pit.HideFar.values()[hideFarPlayer.getValue()];
        engine.settings.hidePitSpawn = Pit.HideSpawn.values()[hidePitSpawn.getValue()];
        engine.settings.showInSpawn = showInSpawn.getValue();
    }

    @Override
    public void onEnable() {
        onSettingChanged("");
        updateServer();
        engine.setEnabled(true);
    }

    @Override
    public void onDisable() {
        engine.setEnabled(false);
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        updateServer();
        engine.onTick();
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        engine.onRotation();
    }

    @EventHandler(priority = -50)
    public void onRenderOverlay(RenderOverlayEvent event) {
        engine.onRender();
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat packet = (S02PacketChat) event.getPacket();
            if (engine.onChat(packet.func_148915_c().func_150254_d(), packet.func_148915_c().func_150260_c(), packet.func_179841_c(), event.isCancelled()))
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        engine.onWorldChange();
        pitServer = false;
        spawnLevel = Double.NaN;
        nextDenick = 0;
        notified.clear();
        scheduleNotice = false;
    }

    public boolean hideEntity(Entity entity) {
        return isEnabled() && (hideFarPlayer.getValue() != 0 || hidePitSpawn.getValue() == 2) && host.hideEntity(engine, entity);
    }

    public boolean hideVisual(Entity entity) {
        return isEnabled() && (hideFarPlayer.getValue() != 0 || hidePitSpawn.getValue() != 0) && host.hideVisual(engine, entity);
    }

    public static PitModule instance() {
        return MyauClient.moduleManager == null ? null : (PitModule) MyauClient.moduleManager.modules.get(PitModule.class);
    }

    public static boolean isPitServer() {
        if (minecraft.field_71441_e == null)
            return false;
        ScoreObjective objective = minecraft.field_71441_e.func_96441_U().func_96539_a(1);
        if (objective == null)
            return false;
        String title = clean(objective.func_96678_d());
        return title.equalsIgnoreCase("THE HYPIXEL PIT") || title.equalsIgnoreCase("THE PIT") || title.equalsIgnoreCase("PIT");
    }

    private void updateServer() {
        pitServer = isPitServer();
        if (!pitServer || minecraft.field_71439_g == null || !minecraft.field_71439_g.field_70122_E)
            return;
        for (String line : ScoreboardUtils.sidebarLines()) {
            String status = clean(line);
            if (status.equalsIgnoreCase("Status: Idling") || status.equalsIgnoreCase("Status: Spawn")) {
                double level = Math.floor(minecraft.field_71439_g.field_70163_u);
                spawnLevel = Double.isNaN(spawnLevel) ? level : Math.min(spawnLevel, level);
            }
        }
    }

    public boolean inPit() {
        return pitServer;
    }

    public boolean hypixel() {
        return ScoreboardUtils.isHypixelScoreboard();
    }

    public boolean spawnHeight(double y, boolean onlyPit) {
        return (!onlyPit || pitServer) && !Double.isNaN(spawnLevel) && y >= spawnLevel;
    }

    public boolean inSpawn(Entity entity, boolean onlyPit) {
        return entity != null && spawnHeight(entity.field_70163_u, onlyPit);
    }

    private static String clean(String text) {
        return EnumChatFormatting.func_110646_a(text).trim();
    }

    static int loreCategory(String text) {
        String line = clean(text).toLowerCase(Locale.ROOT);
        if (line.matches("venom(?: [ivx0-9]+)?"))
            return 1 << 3;
        if (line.matches("regularity(?: [ivx0-9]+)?"))
            return 1 << 4;
        if (line.matches("bounty hunter(?: [ivx0-9]+)?"))
            return 1 << 5;
        return 0;
    }

    static String cachedProfileName(GameProfile profile) {
        if (profile == null || profile.getId() == null)
            return null;
        for (Property texture : profile.getProperties().get("textures")) {
            if (texture.getValue().length() > 32768)
                continue;
            try {
                String payload = new String(Base64.getDecoder().decode(texture.getValue()), StandardCharsets.UTF_8);
                try (JsonReader reader = new JsonReader(new StringReader(payload))) {
                    String name = null, id = null;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String key = reader.nextName();
                        if (key.equals("profileName") && reader.peek() == JsonToken.STRING)
                            name = reader.nextString();
                        else if (key.equals("profileId") && reader.peek() == JsonToken.STRING)
                            id = reader.nextString();
                        else
                            reader.skipValue();
                    }
                    reader.endObject();
                    if (reader.peek() == JsonToken.END_DOCUMENT
                            && name != null
                            && id != null
                            && name.matches("[a-zA-Z0-9_]{1,16}")
                            && id.replace("-", "").equalsIgnoreCase(profile.getId().toString().replace("-", "")))
                        return name;
                }
            } catch (IOException | RuntimeException invalid) {
            }
        }
        return null;
    }

    private String profileName(EntityPlayer player) {
        NetworkPlayerInfo info = minecraft.func_147114_u() == null ? null : minecraft.func_147114_u().func_175102_a(player.func_110124_au());
        return cachedProfileName(info == null ? player.func_146103_bH() : info.func_178845_a());
    }

    public PitMinecraft.Details playerDetails(EntityPlayer player) {
        String raw = player.func_70005_c_();
        String resolved = engine.settings.denicker ? profileName(player) : null;
        int categories = 0;
        if (MyauClient.friendList != null
                && (MyauClient.friendList.contains(raw)
                || resolved != null
                && MyauClient.friendList.contains(resolved)))
            categories |= 1;
        if (MyauClient.enemyList != null
                && (MyauClient.enemyList.contains(raw)
                || resolved != null
                && MyauClient.enemyList.contains(resolved)))
            categories |= 2;
        String name = EntityUtils.formattedName(player);
        if (resolved != null && !resolved.equalsIgnoreCase(raw)) {
            categories |= 1 << 6;
            name += " \u00a77[" + resolved + "]";
        }
        Set<String> enchants = new LinkedHashSet<>();
        for (int slot = 0; slot <= 4; ++slot) {
            ItemStack stack = player.func_71124_b(slot);
            if (stack == null)
                continue;
            if (clean(stack.func_82833_r()).equalsIgnoreCase("Dark Pants"))
                categories |= 1 << 2;
            NBTTagCompound tags = stack.func_77978_p();
            if (tags == null)
                continue;
            NBTTagList lore = tags.func_74775_l("display").func_150295_c("Lore", 8);
            for (int row = 0; row < lore.func_74745_c(); ++row) {
                String text = clean(lore.func_150307_f(row));
                int category = loreCategory(text);
                categories |= category;
                if (category != 0 || text.matches("[A-Za-z][A-Za-z '\\-]{0,40} [IVX0-9]+"))
                    enchants.add(text);
            }
        }
        return new PitMinecraft.Details(name, String.join(", ", enchants), categories);
    }

    public void updateDenicker(boolean active, boolean notifications, boolean onlyPit) {
        if (!active || !host.ready() || onlyPit && !pitServer) {
            notified.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextDenick)
            return;
        nextDenick = now + 250;
        Set<UUID> current = new HashSet<>();
        for (EntityPlayer player : minecraft.field_71441_e.field_73010_i) {
            current.add(player.func_110124_au());
            String resolved = profileName(player);
            if (resolved == null || resolved.equalsIgnoreCase(player.func_70005_c_()))
                continue;
            String previous = notified.put(player.func_110124_au(), resolved);
            if (notifications && !resolved.equals(previous))
                host.message(player.func_70005_c_() + " cached profile: " + resolved);
        }
        notified.keySet().retainAll(current);
    }

    public void updateEventSchedule(boolean active, boolean onlyPit) {
        if (!active) {
            scheduleNotice = false;
            return;
        }
        if (!scheduleNotice && host.ready() && (!onlyPit || pitServer)) {
            host.message("Event schedule unavailable: no schedule provider is configured.");
            scheduleNotice = true;
        }
    }

    public List<Pit.Event> events() {
        return Collections.emptyList();
    }

    public boolean eventsLoading() {
        return false;
    }

    private HUDModule hud() {
        return (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
    }

    public boolean hudShadow() {
        return hud() == null || hud().shadow.getValue();
    }

    public int hudColor(long millis, int row) {
        return hud() == null ? 0xffffffff : hud().getColorForRow(millis, row).getRGB();
    }

    public int hudBackgroundArgb() {
        return hud() == null ? 0x40000000 : ((int) (hud().background.getValue() * 2.55f) << 24);
    }

    public boolean hudSidebar() {
        return hud() != null && hud().bar.getValue();
    }
}
