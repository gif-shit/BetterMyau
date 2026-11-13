package myau;

import net.minecraft.network.Packet;

public class PacketReceiveEvent extends CancellableEvent {

    private final Packet<?> packet;
    private final Object connection;

    public PacketReceiveEvent(Packet<?> packet) {
        this(packet, null);
    }

    public PacketReceiveEvent(Packet<?> packet, Object connection) {
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public Object getConnection() {
        return this.connection;
    }
}
