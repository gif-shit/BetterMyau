package myau.openb4.module.dev;

import myau.BooleanSetting;
import myau.openb4.client.Chat;
import myau.openb4.module.Module;
import myau.openb4.util.ItemUtils;
import myau.openb4.util.PacketUtils;
import myau.openb4.util.Strings;
import net.minecraft.entity.Entity;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ViewPacketsModule extends Module {

    public static final BooleanSetting includeCancelled = new BooleanSetting("include-cancelled", false);
    public static final BooleanSetting logsOnly = new BooleanSetting("logs-only", false);
    public static final BooleanSetting roundValues = new BooleanSetting("round-values", true);
    public static final BooleanSetting sent = new BooleanSetting("sent", true);
    public static final BooleanSetting ignoreC00 = new BooleanSetting("ignore-c00", true);
    public static final BooleanSetting ignoreC03 = new BooleanSetting("ignore-c03", false);
    public static final BooleanSetting compactC03 = new BooleanSetting("compact-c03", true);
    public static final BooleanSetting ignoreC08 = new BooleanSetting("ignore-c08", false);
    public static final BooleanSetting ignoreC0A = new BooleanSetting("ignore-c0a", false);
    public static final BooleanSetting ignoreC0F = new BooleanSetting("ignore-c0f", true);
    public static final BooleanSetting received = new BooleanSetting("received", false);
    private static final Logger LOGGER = LogManager.getLogger();
    public static Packet<?> last;
    public static long ticks;

    public ViewPacketsModule(int key) {
        super("ViewPackets", key);
    }

    @Override
    public void link() {
    }

    public static void onSent(Packet<?> packet) {
        if (mc.func_71356_B()) {
            if (packet.getClass().getSimpleName().charAt(0) == 'S') {
                return;
            }
            if (packet instanceof FMLProxyPacket) {
                return;
            }
        }
        if (ignoreC00.getValue() && packet instanceof C00PacketKeepAlive) {
            return;
        }
        if (ignoreC0A.getValue() && packet instanceof C0APacketAnimation) {
            return;
        }
        if (ignoreC08.getValue() && packet instanceof C08PacketPlayerBlockPlacement) {
            return;
        }
        if (ignoreC0F.getValue() && packet instanceof C0FPacketConfirmTransaction) {
            return;
        }
        if (packet instanceof C03PacketPlayer) {
            if (ignoreC03.getValue()) {
                return;
            }
            if (compactC03.getValue() && (last == null || last instanceof C03PacketPlayer)) {
                return;
            }
        }
        last = packet;
        show(last, true);
    }

    public static void onReceived(Packet<?> packet) {
        if (mc.func_71356_B() && packet.getClass().getSimpleName().charAt(0) == 'C') {
            return;
        }
        show(packet, false);
    }

    public static double round(double d) {
        return roundValues.getValue() ? Strings.round(d, 3) : d;
    }

    private static String bool(boolean b) {
        return (b ? "&atrue" : "&cfalse") + "&r";
    }

    private static String pos(BlockPos pos) {
        return pos == null ? "null" : pos.func_177958_n() + "&7, &b" + pos.func_177956_o() + "&7, &b" + pos.func_177952_p();
    }

    public static String receivedInfo(Packet<?> packet) {
        String s = "&a" + packet.getClass().getSimpleName();
        if (packet instanceof S0BPacketAnimation) {
            Entity entity = mc.field_71441_e.func_73045_a(((S0BPacketAnimation) packet).func_148978_c());
            s = s + "\n&7Target: &b" + (entity == null ? "null" : entity.func_70005_c_());
            s = s + "\n&7Animation: &b" + ((S0BPacketAnimation) packet).func_148977_d();
        }
        return s;
    }

    public static String sentInfo(Packet<?> packet) {
        String s = (PacketUtils.sending == packet ? "&c" : "&a") + packet.getClass().getSimpleName();
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging p = (C07PacketPlayerDigging) packet;
            s = s + "\n&7Status: &b" + (p.func_180762_c() == null ? "null" : p.func_180762_c().name());
            s = s + "\n&7Facing: &b" + (p.func_179714_b() == null ? "null" : p.func_179714_b().ordinal() + " " + p.func_179714_b().name().toLowerCase());
            s = s + "\n&7Position: &b" + pos(p.func_179715_a());
        } else if (packet instanceof C09PacketHeldItemChange) {
            s = s + "\n&7Swap to slot: &b" + ((C09PacketHeldItemChange) packet).func_149614_c();
        } else if (packet instanceof C0BPacketEntityAction) {
            s = s + "\n&7Action: &b" + ((C0BPacketEntityAction) packet).func_180764_b().name();
            s = s + "\n&7Aux data: &b" + ((C0BPacketEntityAction) packet).func_149512_e();
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement) packet;
            s = s + "\n&7Item: &b" + ItemUtils.name(p.func_149574_g());
            int n = p.func_149568_f();
            s = s + "\n&7Direction: &b" + n + (n != 255 ? " " + EnumFacing.func_82600_a(n) : "");
            s = s + "\n&7Position: &b" + pos(p.func_179724_a());
            s = s + "\n&7Offset: &b" + round(p.func_149573_h()) + "&7, &b" + round(p.func_149569_i()) + "&7, &b" + round(p.func_149575_j());
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity p = (C02PacketUseEntity) packet;
            s = s + "\n&7Action: &b" + p.func_149565_c().name();
            Entity entity = p.func_149564_a(mc.field_71441_e);
            s = s + "\n&7Target: &b" + (entity == null ? "null" : entity.func_70005_c_() + " " + entity.func_145782_y());
            Vec3 v = p.func_179712_b();
            s = v == null ? s + "\n&7Hit vec: &bnull" : s + "\n&7Hit vec: &b" + round(v.field_72450_a) + "&7, &b" + round(v.field_72448_b) + "&7, &b" + round(v.field_72449_c);
        } else if (packet instanceof C01PacketChatMessage) {
            s = s + "\n&7Length: &b" + ((C01PacketChatMessage) packet).func_149439_c().length();
        } else if (packet instanceof C14PacketTabComplete) {
            s = s + "\n&7Message: &b" + ((C14PacketTabComplete) packet).func_149419_c();
            s = s + "\n&7Target block: &b" + pos(((C14PacketTabComplete) packet).func_179709_b());
        } else if (packet instanceof C17PacketCustomPayload) {
            s = s + "\n&7Channel: &b" + ((C17PacketCustomPayload) packet).func_149559_c();
        } else if (packet instanceof C15PacketClientSettings) {
            s = s + "\n&7Language: &b" + ((C15PacketClientSettings) packet).func_149524_c();
            s = s + "\n&7Chat visibility: &b" + ((C15PacketClientSettings) packet).func_149523_e().name();
        } else if (packet instanceof C00PacketKeepAlive) {
            s = s + "\n&7Key: &b" + ((C00PacketKeepAlive) packet).func_149460_c();
        } else if (packet instanceof C16PacketClientStatus) {
            s = s + "\n&7Status: &b" + ((C16PacketClientStatus) packet).func_149435_c().name();
        } else if (packet instanceof C10PacketCreativeInventoryAction) {
            s = s + "\n&7Slot: &b" + ((C10PacketCreativeInventoryAction) packet).func_149627_c();
            s = s + "\n&7Item: &b" + ItemUtils.name(((C10PacketCreativeInventoryAction) packet).func_149625_d());
        } else if (packet instanceof C0DPacketCloseWindow) {
            String window = "error";
            try {
                window = String.valueOf((int) (Integer) ReflectionHelper.getPrivateValue(C0DPacketCloseWindow.class, (C0DPacketCloseWindow) packet, "windowId", "field_149556_a"));
            } catch (Exception ignored) {
            }
            s = s + "\n&7Window: &b" + window;
        } else if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow p = (C0EPacketClickWindow) packet;
            s = s + "\n&7Window: &b" + p.func_149548_c();
            s = s + "\n&7Slot: &b" + p.func_149544_d();
            s = s + "\n&7Button: &b" + p.func_149543_e();
            s = s + "\n&7Mode: &b" + p.func_149542_h();
            s = s + "\n&7Action: &b" + p.func_149547_f();
            s = s + "\n&7Item: &b" + ItemUtils.name(p.func_149546_g());
        } else if (packet instanceof C0FPacketConfirmTransaction) {
            s = s + "\n&7Window: &b" + ((C0FPacketConfirmTransaction) packet).func_149532_c();
            s = s + "\n&7Uid: &b" + ((C0FPacketConfirmTransaction) packet).func_149533_d();
        } else if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer p = (C03PacketPlayer) packet;
            s = s + "\n&7X: &b" + p.func_149464_c();
            s = s + "\n&7Y: &b" + p.func_149467_d();
            s = s + "\n&7Z: &b" + p.func_149472_e();
            s = s + "\n&7Yaw: &b" + p.func_149462_g();
            s = s + "\n&7Pitch: &b" + p.func_149470_h();
            s = s + "\n&7Ground: " + bool(p.func_149465_i());
            s = s + "\n&7Moving: " + bool(p.func_149466_j());
            s = s + "\n&7Rotating: " + bool(p.func_149463_k());
        }
        s = s + "\n&7Client tick: &e" + ticks + " " + System.currentTimeMillis() % 1000000L;
        return s;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.START) {
            ++ticks;
        }
    }

    public static void show(Packet<?> packet, boolean isSent) {
        String head = "&7" + (isSent ? "Sent" : "Received") + " packet (t:&b" + ticks + " " + (int) (System.currentTimeMillis() % 10000L) + "&7): ";
        String details = isSent ? sentInfo(packet) : receivedInfo(packet);
        String name = (packet instanceof C03PacketPlayer ? "&6" : "&d") + packet.getClass().getSimpleName();
        ChatComponentText line = new ChatComponentText(Chat.color(Chat.PREFIX + head));
        if (!Chat.inWorld() || logsOnly.getValue()) {
            String[] lines = details.split("\n");
            for (int i = 0; i < lines.length; ++i) {
                String l = Chat.strip(Chat.color(lines[i]));
                if (i == 0) {
                    LOGGER.info(Chat.strip(Chat.color(head)) + l);
                    continue;
                }
                if (i == lines.length - 1) {
                    l = l + " " + (PacketUtils.sending == packet ? "Client" : "Vanilla");
                }
                LOGGER.info("  " + l);
            }
            return;
        }
        ChatStyle style = new ChatStyle();
        style.func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Chat.color(details))));
        line.func_150257_a(new ChatComponentText(Chat.color(name)).func_150255_a(style));
        mc.field_71439_g.func_145747_a(line);
    }

    @Override
    public void onDisable() {
        last = null;
        ticks = 0L;
    }
}
