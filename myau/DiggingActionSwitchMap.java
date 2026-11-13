package myau;

import net.minecraft.network.play.client.C07PacketPlayerDigging;

class DiggingActionSwitchMap {

    static final int[] J = new int[C07PacketPlayerDigging.Action.values().length];

    DiggingActionSwitchMap() {
    }

    static {
        try {
            J[C07PacketPlayerDigging.Action.START_DESTROY_BLOCK.ordinal()] = 1;
        } catch (NoSuchFieldError noSuchFieldError) {
        }
        try {
            J[C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK.ordinal()] = 2;
        } catch (NoSuchFieldError noSuchFieldError) {
        }
    }
}
