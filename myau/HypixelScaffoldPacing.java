package myau;

/**
 * Turn pacing for Scaffold's Hypixel rotation. It turns at a smooth, slow rate unless the time left before the feet
 * reach the placement level needs more, so the aim always arrives with a tick to spare for the placement itself.
 */
final class HypixelScaffoldPacing {
    /** 0.2 seconds of normal walking between jumps. */
    static final int JUMP_DELAY_TICKS = 4;
    static final int STRAIGHT_SMOOTHING = 80;
    static final int DIAGONAL_SMOOTHING = 65;
    private static final int MAX_TICKS = 30;
    private static final float MAX_STEP = 90f;

    private HypixelScaffoldPacing() {}

    /**
     * Ticks until the feet come back down to levelY, using vanilla gravity. At least 1. A rising player can start below
     * the level, so only the falling part counts. If the jump never gets there, the time is already up.
     */
    static int ticksToLevel(double y, double motionY, double levelY) {
        boolean reached = y > levelY;
        for (int ticks = 1; ticks <= MAX_TICKS; ticks++) {
            y += motionY;
            motionY = (motionY - 0.08) * 0.98;
            if (y > levelY) reached = true;
            if (motionY < 0.0 && y <= levelY) return reached ? ticks : 1;
        }
        return MAX_TICKS;
    }

    /** Ticks that can be spent turning. One is kept for the placement, and a diagonal needs about twice the placements. */
    static int turnTicks(int ticksToLevel, boolean diagonal) {
        int ticks = Math.max(1, ticksToLevel - 1);
        return diagonal ? Math.max(1, ticks / 2) : ticks;
    }

    /** Tower has to place the whole step before the feet drop past it, about three blocks straight and five diagonally. */
    static int towerTurnTicks(int ticksToLevel, boolean diagonal) {
        return Math.max(1, ticksToLevel - (diagonal ? 5 : 3));
    }

    /** The largest step for this tick: the comfortable rate, or whatever the remaining turn needs. */
    static float maxStep(float remainingYaw, int turnTicks, float comfortableStep) {
        float needed = Math.abs(remainingYaw) / Math.max(1, turnTicks);
        return Math.min(MAX_STEP, Math.max(comfortableStep, needed));
    }

    static int smoothing(int configured, boolean diagonal) {
        return Math.max(configured, diagonal ? DIAGONAL_SMOOTHING : STRAIGHT_SMOOTHING);
    }
}
