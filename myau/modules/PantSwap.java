package myau.modules;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PantSwap {

    public static final class Settings {

        public int delay = 100;
        public boolean inventory;

        public void validate() {
            if (delay < 0 || delay > 1000)
                throw new IllegalArgumentException("delay: 0..1000ms");
        }
    }

    public interface Host {

        boolean ready();

        Object worldIdentity();

        Object playerIdentity();

        boolean playerContainerOpen();

        boolean inventoryScreenOpen();

        boolean otherScreenOpen();

        boolean survivalOrAdventure();

        Object screenIdentity();

        void openInventory();

        void closeInventory();

        Object stack(int containerSlot);

        Object cursor();

        Object copy(Object stack);

        boolean equal(Object first, Object second);

        boolean leggings(Object stack);

        boolean canEquipLeggings(Object stack);

        long nowMillis();

        void click(int containerSlot);

        void message(String text);
    }

    private static final int NO_TRANSACTION = Integer.MIN_VALUE;
    private static final long CONFIRM_TIMEOUT = 2000;

    private static final class Confirmation {

        final int action;

        final boolean accepted;

        Confirmation(int action, boolean accepted) {
            this.action = action;
            this.accepted = accepted;
        }
    }

    public final Settings settings = new Settings();
    private final Host host;
    private final Queue<Confirmation> confirmations = new ConcurrentLinkedQueue<>();
    private volatile boolean enabled, issuingClick;
    private volatile int pendingTransaction = NO_TRANSACTION;
    private Object world, player, screen, expectedCursor;
    private final Map<Integer, Object> expected = new LinkedHashMap<>();
    private boolean openedScreen, waiting;
    private int[] plan = new int[0];
    private int step;
    private long lastClick;

    public PantSwap(Host host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int pendingTransaction() {
        return pendingTransaction;
    }

    public boolean issuingClick() {
        return issuingClick;
    }

    public void setEnabled(boolean value) {
        settings.validate();
        if (value == enabled)
            return;
        if (!value) {
            finish(null);
            return;
        }
        enabled = true;
        if (!host.ready() || !host.playerContainerOpen() || !host.survivalOrAdventure()) {
            finish("Canceled: another container is open or the player is unavailable.");
            return;
        }
        if (host.cursor() != null) {
            finish("Canceled: an item is on the cursor.");
            return;
        }
        if (host.otherScreenOpen()) {
            finish("Canceled: another screen is open.");
            return;
        }
        int source = -1;
        for (int slot = 36; slot < 45 && source < 0; slot++) if (host.leggings(host.stack(slot)) && host.canEquipLeggings(host.stack(slot)))
            source = slot;
        if (source < 0 && settings.inventory)
            for (int slot = 9; slot < 36 && source < 0; slot++) if (host.leggings(host.stack(slot)) && host.canEquipLeggings(host.stack(slot)))
                source = slot;
        if (source < 0) {
            finish("No spare leggings found" + (settings.inventory ? "." : " in your hotbar."));
            return;
        }
        world = host.worldIdentity();
        player = host.playerIdentity();
        if (!host.inventoryScreenOpen()) {
            openedScreen = true;
            try {
                host.openInventory();
            } catch (RuntimeException | Error failure) {
                finish("Canceled: inventory screen could not open.");
                throw failure;
            }
        }
        screen = host.screenIdentity();
        if (!host.inventoryScreenOpen() || !sameContext()) {
            finish("Canceled: inventory screen changed.");
            return;
        }
        if (host.cursor() != null) {
            finish("Canceled: an item appeared on the cursor.");
            return;
        }
        for (int slot = 0; slot < 45; slot++) expected.put(slot, host.copy(host.stack(slot)));
        expectedCursor = host.copy(host.cursor());
        plan = expected.get(7) == null ? new int[] { source, 7 } : new int[] { source, 7, source };
        step = 0;
        waiting = false;
        confirmations.clear();
        pendingTransaction = NO_TRANSACTION;
        lastClick = host.nowMillis() - settings.delay;
    }

    public void onPacketSend(int windowId, int actionNumber) {
        if (enabled && issuingClick && windowId == 0)
            pendingTransaction = actionNumber;
    }

    public void onPacketReceive(int windowId, int actionNumber, boolean accepted, boolean cancelled) {
        if (enabled && !cancelled && windowId == 0 && actionNumber == pendingTransaction)
            confirmations.offer(new Confirmation(actionNumber, accepted));
    }

    public boolean cancelInventoryAction() {
        return enabled && !issuingClick;
    }

    public void onTick() {
        settings.validate();
        if (!enabled)
            return;
        if (!sameContext()) {
            finish("Canceled: inventory screen or player changed.");
            return;
        }
        if (!matches()) {
            finish("Canceled: inventory contents changed. Check your cursor.");
            return;
        }
        long now = host.nowMillis();
        if (waiting) {
            Confirmation response;
            while ((response = confirmations.poll()) != null) {
                if (response.action != pendingTransaction)
                    continue;
                if (!response.accepted) {
                    finish("Canceled: the server rejected the swap. Check your inventory.");
                    return;
                }
                waiting = false;
                pendingTransaction = NO_TRANSACTION;
                break;
            }
            if (waiting) {
                if (now < lastClick || now - lastClick >= CONFIRM_TIMEOUT)
                    finish("Canceled: no server confirmation. Check your inventory.");
                return;
            }
        }
        if (step == plan.length) {
            finish("Swapped leggings.");
            return;
        }
        if (now < lastClick || now - lastClick < settings.delay)
            return;
        int slot = plan[step];
        Object old = expected.get(slot);
        expected.put(slot, expectedCursor);
        expectedCursor = old;
        pendingTransaction = NO_TRANSACTION;
        waiting = true;
        issuingClick = true;
        lastClick = now;
        try {
            host.click(slot);
        } catch (RuntimeException | Error failure) {
            finish("Canceled: inventory click failed. Check your cursor.");
            throw failure;
        } finally {
            issuingClick = false;
        }
        if (pendingTransaction == NO_TRANSACTION) {
            finish("Canceled: inventory click was not observed.");
            return;
        }
        step++;
    }

    private boolean sameContext() {
        return host.ready()
                && world == host.worldIdentity()
                && player == host.playerIdentity()
                && host.playerContainerOpen()
                && host.inventoryScreenOpen()
                && screen == host.screenIdentity();
    }

    private boolean matches() {
        if (!host.equal(expectedCursor, host.cursor()))
            return false;
        for (Map.Entry<Integer, Object> entry : expected.entrySet()) if (!host.equal(entry.getValue(), host.stack(entry.getKey())))
            return false;
        return true;
    }

    private void finish(String message) {
        boolean close = openedScreen && sameContext() && host.cursor() == null;
        enabled = false;
        issuingClick = false;
        waiting = false;
        pendingTransaction = NO_TRANSACTION;
        confirmations.clear();
        expected.clear();
        expectedCursor = null;
        plan = new int[0];
        step = 0;
        openedScreen = false;
        world = null;
        player = null;
        screen = null;
        if (close)
            host.closeInventory();
        if (message != null)
            host.message(message);
    }

    public void onWorldChange() {
        finish(null);
    }
}
