package myau;

import java.util.ArrayList;
import net.minecraft.network.Packet;

class PacketUtils {
    static final ArrayList<Packet<?>> sent = new ArrayList<>();
    public static void send(Packet<?> packet) { sent.add(packet); }
}

class ChatUtils {
    public static void addFormattedMessage(String message) { }
}
