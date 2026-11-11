package myau;

import net.minecraft.util.MovingObjectPosition;

class HitTypeSwitchMap {
    static final int[] n = new int[MovingObjectPosition.MovingObjectType.values().length];

    HitTypeSwitchMap() {
    }

    static {
        try {
            HitTypeSwitchMap.n[MovingObjectPosition.MovingObjectType.BLOCK.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            HitTypeSwitchMap.n[MovingObjectPosition.MovingObjectType.ENTITY.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
    }
}
