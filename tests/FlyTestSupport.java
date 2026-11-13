package myau;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;

class MyauClient {
    public static ModuleManager moduleManager = new ModuleManager();
}

class InputUtils {
    static final HashSet<Integer> keys = new HashSet<>();
    public static boolean isKeyDown(int key) { return keys.contains(key); }
    public static void setKeyState(int key, boolean down) { KeyBinding.func_74510_a(key, down); }
    public static void updateKeyState(int key) { setKeyState(key, isKeyDown(key)); }
}

class PacketUtils {
    static final ArrayList<Packet<?>> sent = new ArrayList<>();
    public static void send(Packet<?> packet) { sent.add(packet); }
}

class ChatUtils {
    public static void addFormattedMessage(String message) { }
}
