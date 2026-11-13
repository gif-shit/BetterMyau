package myau.openb4.module.dev;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.openb4.client.BotCheck;
import myau.openb4.client.Chat;
import myau.openb4.client.Hypixel;
import myau.openb4.module.Module;
import myau.openb4.util.BlockBreaker;
import myau.openb4.util.Executor;
import myau.openb4.util.MathUtils;
import myau.openb4.util.PacketUtils;
import myau.openb4.util.Render3D;
import myau.openb4.util.RotationUtils;
import myau.openb4.util.Strings;
import myau.openb4.util.TeleportPath;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TestModule extends Module {

    public static final BooleanSetting blockBreakTest = new BooleanSetting("block-break-test", false);
    public static final BooleanSetting breakTime = new BooleanSetting("break-time", false);
    public static final BooleanSetting limboPacket = new BooleanSetting("limbo-packet", false);
    public static final BooleanSetting printTicks = new BooleanSetting("print-ticks", false);
    public static final BooleanSetting bedwarsTp = new BooleanSetting("bedwars-tp", false);
    public static final BooleanSetting bedwarsLobbyTp = new BooleanSetting("bedwars-lobby-tp", false);
    public static final BooleanSetting killCounter = new BooleanSetting("kill-counter", false);
    public static final BooleanSetting killCounterCheck = new BooleanSetting("kill-counter-check", false);
    public static final BooleanSetting antiDcbot = new BooleanSetting("anti-dcbot", false);
    public static final BooleanSetting entityInfo = new BooleanSetting("entity-info", false);
    public static final BooleanSetting rejoin = new BooleanSetting("rejoin", false);
    public static final FloatSetting limboType = new FloatSetting("limbo-type", 0.0f, 0.0f, 2.0f, 1.0f);
    public static final FloatSetting bedwarsTpDelay = new FloatSetting("bedwars-tp-delay", 0.0f, 0.0f, 1000.0f, 50.0f);
    public static final myau.StringSetting dcName = new myau.StringSetting("dc-name", "");
    public static long breakStart;
    public static BlockPos breaking;
    public static BlockPos selected;
    public static BlockPos tpTarget;
    public static long lastKillMessage;

    public TestModule(int key) {
        super("Test", key);
    }

    @Override
    public void link() {
    }

    public static void onDig(C07PacketPlayerDigging p) {
        if (breakTime.getValue() && p.func_180762_c() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
            breakStart = System.currentTimeMillis();
            breaking = p.func_179715_a();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Chat.inWorld()) {
            return;
        }
        String s = Chat.strip(e.message.func_150260_c());
        if (s.isEmpty()) {
            return;
        }
        if (bedwarsTp.getValue()) {
            if (s.equals("You have respawned!") || tpTarget != null && s.charAt(0) == ' ' && s.contains("Protect your bed")) {
                Executor.later(() -> {
                    if (isEnabled() && Chat.inWorld())
                        tpDown();
                }, bedwarsTpDelay.getValue().longValue());
            } else if (tpTarget != null && s.equals("You will respawn because you still have a bed!")) {
                RotationUtils.setRotation(RotationUtils.toBlock(tpTarget));
                Chat.print("&7updated rotations, distance: &d" + Strings.round(mc.field_71439_g.func_70011_f(tpTarget.func_177958_n(), tpTarget.func_177956_o(), tpTarget.func_177952_p()), 1));
            }
        }
        if (bedwarsLobbyTp.getValue() && s.startsWith(mc.field_71439_g.func_70005_c_() + " has joined")) {
            Executor.later(() -> {
                if (isEnabled() && Chat.inWorld())
                    lobbyTp();
            }, 200);
        }
        if (killCounterCheck.getValue() && s.contains("current kills: ")) {
            lastKillMessage = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate || !Chat.inWorld()) {
            return;
        }
        if (blockBreakTest.getValue()) {
            if (e.button == 0) {
                MovingObjectPosition hit = BotCheck.rayTraceBlocks(25.0);
                if (hit == null || hit.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK) {
                    return;
                }
                e.setCanceled(true);
                if (selected != null) {
                    BlockBreaker.abort(selected);
                }
                selected = hit.func_178782_a();
                BlockBreaker.start(selected);
            } else if (e.button == 1) {
                if (selected != null) {
                    e.setCanceled(true);
                    BlockBreaker.stop(selected);
                    selected = null;
                } else {
                    Chat.print("&cno block selected");
                }
            }
        } else if (bedwarsTp.getValue() && e.button == 1) {
            MovingObjectPosition hit = BotCheck.rayTraceBlocks(150.0);
            if (hit == null || hit.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK) {
                return;
            }
            tpTarget = hit.func_178782_a();
            Chat.print("&7set target block: &d" + tpTarget.func_177958_n() + " " + tpTarget.func_177956_o() + " " + tpTarget.func_177952_p());
        }
    }

    @Override
    public void onDisable() {
        tpTarget = null;
        breaking = null;
        selected = null;
        lastKillMessage = 0L;
        breakStart = 0L;
    }

    @Override
    public void onUpdate() {
        if (breakTime.getValue() && breaking != null) {
            long now = System.currentTimeMillis();
            if (mc.field_71441_e.func_180495_p(breaking).func_177230_c() == Blocks.field_150350_a) {
                Chat.print("&7block break took &b" + (now - breakStart) + "ms");
                breaking = null;
            } else if (Math.abs(now - breakStart) >= 10000L) {
                Chat.print("&7timed out, break took too long");
                breaking = null;
            }
        }
        if (printTicks.getValue()) {
            Chat.print("&7ticks existed: &b" + mc.field_71439_g.field_70173_aa);
        }
        if (killCounter.getValue() && mc.field_71439_g.field_70173_aa % 60 == 0) {
            String kills = "unknown";
            for (String line : Hypixel.sidebarLines()) {
                String s = Chat.strip(line);
                if (!s.startsWith("Kills: "))
                    continue;
                try {
                    kills = s.split("Kills: ")[1];
                } catch (Exception ex) {
                    kills = "error";
                }
            }
            mc.field_71439_g.func_71165_d("/pc current kills: " + kills + " (" + Math.round(1000.0 * Math.random()) + ")");
        }
        if (killCounterCheck.getValue() && lastKillMessage != 0L) {
            if (Hypixel.bedwars(2)) {
                long l = Math.abs(System.currentTimeMillis() - lastKillMessage);
                if (l >= 5000L) {
                    Chat.print("&d" + l + "&7ms since last message, sending win message");
                    mc.field_71439_g.func_71165_d("/shout gg! " + MathUtils.randomString(10));
                    lastKillMessage = 0L;
                }
            } else {
                lastKillMessage = 0L;
                Chat.print("&7not in game, reset last received kill count");
            }
        }
    }

    @Override
    public void onEnable() {
        if (!Chat.inWorld() && !rejoin.getValue()) {
            this.disable();
            return;
        }
        if (rejoin.getValue()) {
            ServerData serverData = new ServerData("hypixel", "hypixel.net", false);
            mc.func_147108_a(new GuiConnecting(new GuiMainMenu(), mc, serverData));
            this.disable();
            return;
        }
        if (limboPacket.getValue()) {
            switch(limboType.getValue().intValue()) {
                case 0:
                    PacketUtils.sendThroughLag(new C09PacketHeldItemChange(-1));
                    break;
                case 1:
                    PacketUtils.sendThroughLag(new C07PacketPlayerDigging());
                    break;
                case 2:
                    PacketUtils.sendThroughLag(new C03PacketPlayer.C04PacketPlayerPosition(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !mc.field_71439_g.field_70122_E));
                    break;
            }
            this.disable();
        }
        if (antiDcbot.getValue()) {
            if (dcName.getValue().isEmpty()) {
                Chat.print("&7dcname not set, use &ddcname [name]");
            } else {
                EntityPlayer target = null;
                for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                    if (p == mc.field_71439_g || !p.func_70005_c_().equalsIgnoreCase(dcName.getValue()))
                        continue;
                    target = p;
                    break;
                }
                if (target == null) {
                    Chat.print("&7target not found: &d" + dcName.getValue());
                } else {
                    Chat.print("&7players within &b20 &7blocks of &d" + dcName.getValue());
                    for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
                        if (p == mc.field_71439_g || p.func_70005_c_().equalsIgnoreCase(dcName.getValue()) || target.func_70068_e(p) > 400.0)
                            continue;
                        ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + " &7- "));
                        ChatStyle style = new ChatStyle();
                        style.func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7click to paste username"))));
                        style.func_150241_a(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, p.func_70005_c_()));
                        line.func_150257_a(new ChatComponentText(Chat.color("&d" + p.func_70005_c_())).func_150255_a(style));
                        mc.field_71439_g.func_145747_a(line);
                    }
                }
            }
            this.disable();
        }
        if (entityInfo.getValue()) {
            Chat.print("entity players");
            List<EntityPlayer> players = mc.field_71441_e.field_73010_i;
            for (int i = 0; i < players.size(); ++i) {
                EntityPlayer p = players.get(i);
                ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + p.func_70005_c_()));
                ChatStyle style = new ChatStyle();
                style.func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7uuid: &r" + p.func_110124_au()))));
                line.func_150255_a(style);
                mc.field_71439_g.func_145747_a(line);
            }
            Chat.print("network players");
            ArrayList<NetworkPlayerInfo> infos = new ArrayList<NetworkPlayerInfo>(mc.func_147114_u().func_175106_d());
            MathUtils.dedupe(infos);
            for (NetworkPlayerInfo info : infos) {
                ScorePlayerTeam team = info.func_178850_i();
                ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + info.func_178845_a().getName()));
                ChatStyle style = new ChatStyle();
                style.func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color("&7uuid: &r" + info.func_178845_a().getId() + (team == null ? "&7no team found" : "\n&7team name: &r" + team.func_96669_c() + "\n&7registered: &r" + team.func_96661_b())))));
                line.func_150255_a(style);
                mc.field_71439_g.func_145747_a(line);
            }
            Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
            if (scoreboard != null) {
                Chat.print("scoreboard (score teams)");
                for (ScorePlayerTeam team : scoreboard.func_96525_g()) {
                    String members = "&emembers";
                    Collection<String> names = team.func_96670_d();
                    for (String name : names) {
                        members = members + "\n&r" + name;
                    }
                    ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + "&7" + team.func_96669_c()));
                    ChatStyle style = new ChatStyle();
                    style.func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(members))));
                    line.func_150255_a(style);
                    mc.field_71439_g.func_145747_a(line);
                }
            }
            this.disable();
        }
    }

    public void tpDown() {
        mc.func_147114_u().func_147297_a(new C03PacketPlayer(false));
        mc.func_147114_u().func_147297_a(new C03PacketPlayer.C04PacketPlayerPosition(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - 20.0, mc.field_71439_g.field_70161_v, true));
        mc.func_147114_u().func_147297_a(new C03PacketPlayer.C04PacketPlayerPosition(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70122_E));
        Chat.print("&7teleported, disabling");
        this.disable();
    }

    public static void lobbyTp() {
        try {
            TeleportPath.teleport(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 50.0, mc.field_71439_g.field_70161_v), true);
        } catch (Exception ex) {
            Chat.print("&7error teleporting");
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        if (blockBreakTest.getValue() && selected != null) {
            Render3D.block(selected, Color.orange.getRGB(), false, true);
        }
        if (bedwarsTp.getValue() && tpTarget != null) {
            for (int i = 0; i < 10; ++i) {
                Render3D.block(tpTarget.func_177981_b(i), i == 0 ? Color.orange.getRGB() : Color.yellow.getRGB(), false, true);
            }
        }
    }
}
