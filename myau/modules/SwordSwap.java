package myau.modules;

import java.util.Objects;

public final class SwordSwap {

    public enum Spoof {

        NONE, ANIMATIONS, FULL
    }

    public static final class Settings {

        public boolean pitOnly = true, killauraOnly = true;
        public int slot1 = 1, slot2 = 3;
        public Spoof spoof = Spoof.NONE;

        public void validate() {
            Objects.requireNonNull(spoof, "spoof");
            if (slot1 < 1 || slot1 > 9 || slot2 < 1 || slot2 > 9)
                throw new IllegalArgumentException("slots: 1..9");
        }
    }

    public interface Host {

        boolean ready();

        Object playerIdentity();

        Object worldIdentity();

        boolean screenOpen();

        boolean inPit();

        boolean auraEnabledWithTarget();

        boolean attackKeyDown();

        boolean swinging();

        boolean autoHealing();

        boolean consuming();

        int currentSlot();

        Object stack(int slot);

        boolean allowedDestination(Object stack);

        void selectSlot(int slot);

        void syncHeldItem();

        void showItem(Object stack, int selectedSlot);
    }

    public final Settings settings = new Settings();
    private final Host host;
    private boolean enabled, swappedThisTick;
    private Object savedPlayer, visualStack;
    private int restoreSlot = -1, visualSlot = -1, lastSelected = -1, lastSlot1 = -1, lastSlot2 = -1;

    public SwordSwap(Host host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        settings.validate();
        if (enabled == value)
            return;
        enabled = value;
        if (!value) {
            swappedThisTick = false;
            restore();
        }
    }

    public void onTick() {
        settings.validate();
        swappedThisTick = false;
        if (!enabled || !host.ready() || host.screenOpen() || settings.pitOnly && !host.inPit())
            return;
        if (settings.killauraOnly ? !host.auraEnabledWithTarget() : !host.attackKeyDown() && !host.swinging())
            return;
        if (host.autoHealing() || host.consuming())
            return;
        int first = settings.slot1 - 1, second = settings.slot2 - 1, current = host.currentSlot();
        int next = current == first ? second : first;
        Object stack = host.stack(next);
        if (first == second || current != first && current != second || stack == null || !host.allowedDestination(stack))
            return;
        if (restoreSlot == -1
                || savedPlayer != host.playerIdentity()
                || current != lastSelected
                || first != lastSlot1
                || second != lastSlot2) {
            restoreSlot = current;
            savedPlayer = host.playerIdentity();
            visualSlot = -1;
        }
        if (settings.spoof == Spoof.FULL) {
            if (visualSlot == -1 || current != lastSelected || first != lastSlot1 || second != lastSlot2)
                visualSlot = current;
            visualStack = host.stack(visualSlot);
        } else {
            visualSlot = -1;
            visualStack = stack;
        }
        lastSelected = next;
        lastSlot1 = first;
        lastSlot2 = second;
        swappedThisTick = true;
        host.selectSlot(next);
        host.syncHeldItem();
        if (settings.spoof != Spoof.NONE)
            host.showItem(visualStack, next);
    }

    public void onPostTick() {
        if (enabled
                && swappedThisTick
                && settings.spoof != Spoof.NONE
                && host.playerIdentity() != null
                && host.currentSlot() == lastSelected)
            host.showItem(visualStack, lastSelected);
        else if (!swappedThisTick || !enabled)
            restore();
    }

    private void restore() {
        if (host.autoHealing())
            return;
        if (restoreSlot >= 0
                && savedPlayer == host.playerIdentity()
                && host.worldIdentity() != null
                && host.currentSlot() == lastSelected) {
            host.selectSlot(restoreSlot);
            host.syncHeldItem();
        }
        restoreSlot = -1;
        savedPlayer = null;
        clearVisualState();
    }

    private void clearVisualState() {
        visualStack = null;
        visualSlot = -1;
        lastSelected = -1;
        swappedThisTick = false;
    }

    public void onWorldChange() {
        restoreSlot = -1;
        savedPlayer = null;
        clearVisualState();
    }
}
