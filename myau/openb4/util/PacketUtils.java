package myau.openb4.util;

import net.minecraft.network.Packet;

public final class PacketUtils {

    public static volatile long lastPosLook;
    public static Packet<?> sending;
    public static boolean sentSwing, sentUseEntity, sentAnyDigging;

    public static boolean noUseOrDig() {
        return !sentUseEntity && !sentAnyDigging;
    }

    public static void send(Packet<?> packet) {
        Packet<?> previous = sending;
        sending = packet;
        try {
            myau.PacketUtils.send(packet);
        } finally {
            sending = previous;
        }
    }

    public static void sendThroughLag(Packet<?> packet) {
        sending = packet;
        try {
            myau.PacketUtils.sendWithoutSendEvent(packet);
        } finally {
            sending = null;
        }
    }
}
