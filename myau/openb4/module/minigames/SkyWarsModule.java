package myau.openb4.module.minigames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.openb4.client.BotCheck;
import myau.openb4.client.Chat;
import myau.openb4.client.ChatCommands;
import myau.openb4.client.Hypixel;
import myau.openb4.client.InfoHud;
import myau.openb4.client.Overlay;
import myau.openb4.module.Module;
import myau.openb4.util.Render3D;
import myau.openb4.util.RenderUtils;
import myau.openb4.util.Strings;
import myau.openb4.util.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SkyWarsModule extends Module {

    public static final FloatSetting closestEnemy = new FloatSetting("closest-enemy", 0.0f, 0.0f, 10.0f, 1.0f);
    public static final BooleanSetting strengthIndicator = new BooleanSetting("strength-indicator", true);
    public static final BooleanSetting timeWarpPearl = new BooleanSetting("time-warp-pearl", true);
    private static boolean inGame;
    private static List<InfoHud.Line> enemies;
    public static final HashMap<String, Long> strength = new HashMap<String, Long>();
    private static final HashMap<EntityOtherPlayerMP, Long> ghosts = new HashMap<EntityOtherPlayerMP, Long>();
    public static long pearlAt;
    private static long countdownAt;
    private static final String TIME_WARP = "§b§l";

    public SkyWarsModule(int key) {
        super("SkyWars", key);
    }

    private static Minecraft mc() {
        return Minecraft.func_71410_x();
    }

    public void link() {
    }

    @Override
    public void onDisable() {
        reset();
        enemies = null;
    }

    @Override
    public void onEnable() {
        enemies = new ArrayList<InfoHud.Line>((int) 10);
    }

    private static int byDistance(EntityPlayer a, EntityPlayer b) {
        return Double.compare(a.func_70068_e(mc().field_71439_g), b.func_70068_e(mc().field_71439_g));
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        Minecraft mc = mc();
        if (!Chat.inWorld()) {
            return;
        }
        if (strengthIndicator.getValue() && !strength.isEmpty()) {
            for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                if (p == mc.field_71439_g || p.field_70725_aQ != 0)
                    continue;
                Long until = strength.get(p.func_70005_c_());
                if (until == null)
                    continue;
                long left = until - System.currentTimeMillis();
                if (left <= 0L) {
                    strength.remove(p.func_70005_c_());
                    continue;
                }
                int alpha = left > 150L ? 112 : (int) ((double) left / 150.0 * 112.0);
                Render3D.shadedBox(p, RenderUtils.withAlpha(Theme.FLARE.first.getRGB(), alpha), e.partialTicks);
            }
        }
        if (timeWarpPearl.getValue() && !ghosts.isEmpty()) {
            Iterator<Map.Entry<EntityOtherPlayerMP, Long>> it = ghosts.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<EntityOtherPlayerMP, Long> entry = it.next();
                long until = entry.getValue();
                long left = until - System.currentTimeMillis();
                if (left <= 0L) {
                    it.remove();
                    continue;
                }
                double d = left > 150L ? 1.0 : (double) left / 150.0;
                int alpha = (int) (112.0 * d);
                int textAlpha = (int) (255.0 * d);
                EntityOtherPlayerMP ghost = entry.getKey();
                Render3D.shadedBox(ghost, RenderUtils.withAlpha(13082111, alpha), e.partialTicks);
                this.renderTimer(ghost, until, RenderUtils.withAlpha(-1, RenderUtils.clampAlpha(textAlpha)), e.partialTicks);
            }
        }
    }

    private static void reset() {
        strength.clear();
        countdownAt = 0L;
        pearlAt = 0L;
        ghosts.clear();
        if (enemies != null) {
            enemies.clear();
        }
    }

    public static void onSetback() {
        if (System.currentTimeMillis() - pearlAt < 7000L) {
            addGhost(mc().field_71439_g);
            countdownAt = System.currentTimeMillis();
        }
        pearlAt = 0L;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        Minecraft mc = mc();
        if (!inGame || e.phase == TickEvent.Phase.START || !Overlay.canRender()) {
            return;
        }
        if (enemies != null) {
            for (InfoHud.Line line : enemies) {
                InfoHud.add(line);
            }
        }
        if (countdownAt != 0L) {
            long now = System.currentTimeMillis();
            long elapsed = now - countdownAt;
            long left = 3000L - elapsed;
            if (left <= 0L) {
                countdownAt = 0L;
                return;
            }
            int alpha = elapsed < 125L ? (int) ((float) elapsed / 125.0f * 240.0f) : (left < 125L ? (int) ((float) left / 125.0f * 240.0f) : 240);
            if (alpha <= 0) {
                return;
            }
            String s = Strings.round((double) left / 1000.0, 1) + "s";
            float half = (float) mc.field_71466_p.func_78256_a(s) / 2.0f - 0.5f;
            float x = (float) RenderUtils.scaledWidth / 2.0f - half;
            float y = (float) RenderUtils.scaledHeight / 2.0f - 11.0f;
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            mc.field_71466_p.func_175065_a(s, x, y, RenderUtils.withAlpha(13082111, RenderUtils.clampAlpha(alpha)), true);
            GlStateManager.func_179084_k();
        }
    }

    @Override
    public void onChat(ClientChatReceivedEvent e, String s) {
        if (!inGame) {
            return;
        }
        if (s.endsWith(".")) {
            HashSet<String> names = new HashSet<String>();
            for (NetworkPlayerInfo info : tabList()) {
                names.add(info.func_178845_a().getName());
            }
            String[] words = s.split(" ");
            if (words.length > 2) {
                for (int i = words.length - 1; i >= 1; --i) {
                    String w = words[i];
                    if (w.endsWith(".")) {
                        w = w.replace(".", "");
                    } else if (w.endsWith("'s")) {
                        w = w.replace("'s", "");
                    }
                    if (!names.contains(w))
                        continue;
                    long l = Hypixel.skywars(3) ? 5000L : 2000L;
                    strength.put(w, System.currentTimeMillis() + l);
                    break;
                }
            }
        }
    }

    private static List<NetworkPlayerInfo> tabList() {
        Minecraft mc = mc();
        if (mc.func_147114_u() == null) {
            return new ArrayList<NetworkPlayerInfo>();
        }
        ArrayList<NetworkPlayerInfo> list = new ArrayList<NetworkPlayerInfo>(mc.func_147114_u().func_175106_d());
        HashSet<NetworkPlayerInfo> set = new HashSet<NetworkPlayerInfo>(list);
        list.clear();
        list.addAll(set);
        list.remove(mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au()));
        return list;
    }

    @Override
    public void onPearlThrown(ItemStack stack) {
        if (timeWarpPearl.getValue() && stack.func_82833_r().startsWith(TIME_WARP)) {
            pearlAt = System.currentTimeMillis();
        }
    }

    private static void addGhost(EntityPlayer p) {
        EntityOtherPlayerMP ghost = new EntityOtherPlayerMP(mc().field_71441_e, p.func_146103_bH());
        ghost.func_145769_d(-170);
        ghost.func_82149_j(p);
        ghosts.put(ghost, System.currentTimeMillis() + 3000L);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = mc();
        inGame = Hypixel.skywars(2);
        if (!inGame) {
            return;
        }
        if (mc.field_71439_g.field_70173_aa % 3 == 0) {
            HashSet<Integer> ids = new HashSet<Integer>();
            ArrayList<EntityPlayer> list = new ArrayList<EntityPlayer>();
            for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                if (p == mc.field_71439_g || p.field_70725_aQ != 0 || Hypixel.info(p) == null)
                    continue;
                if (!ids.add(p.func_145782_y()))
                    continue;
                if (Hypixel.sameTeam(p))
                    continue;
                if (BotCheck.isBot(p, false))
                    continue;
                list.add(p);
            }
            enemies.clear();
            if (!list.isEmpty()) {
                list.sort(SkyWarsModule::byDistance);
                int n = Math.min(closestEnemy.getValue().intValue(), list.size());
                for (int i = 0; i < n; ++i) {
                    EntityPlayer p = list.get(i);
                    double d = p.func_70032_d(mc.field_71439_g);
                    enemies.add(InfoHud.line(p.func_145748_c_().func_150260_c(), ChatCommands.distance(d, false), p.func_70694_bm()));
                }
            }
        }
    }

    private void renderTimer(EntityOtherPlayerMP ghost, long until, int color, float partialTicks) {
        Minecraft mc = mc();
        long left = until - System.currentTimeMillis();
        double d = Math.max(0.0, (double) left / 1000.0);
        String s = Strings.round(d, 1) + "s";
        RenderManager rm = mc.func_175598_ae();
        double x = ghost.field_70142_S + (ghost.field_70165_t - ghost.field_70142_S) * (double) partialTicks - rm.field_78730_l;
        double y = ghost.field_70137_T + (ghost.field_70163_u - ghost.field_70137_T) * (double) partialTicks + (double) ghost.field_70131_O * 0.6 - rm.field_78731_m;
        double z = ghost.field_70136_U + (ghost.field_70161_v - ghost.field_70136_U) * (double) partialTicks - rm.field_78728_n;
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b(x, y, z);
        GlStateManager.func_179114_b(-rm.field_78735_i, 0.0f, 1.0f, 0.0f);
        GlStateManager.func_179114_b(rm.field_78732_j, 1.0f, 0.0f, 0.0f);
        double dist = mc.field_71439_g.func_70068_e(ghost);
        float scale = (float) Math.min(0.025 * (1.0 + dist / 500.0), 0.1);
        GlStateManager.func_179152_a(-scale, -scale, scale);
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b(770, 771);
        int w = mc.field_71466_p.func_78256_a(s);
        mc.field_71466_p.func_175065_a(s, (float) (-w / 2), -4.0f, color, true);
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        RenderUtils.resetColor();
        GlStateManager.func_179121_F();
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc().field_71439_g) {
            reset();
        }
    }
}
