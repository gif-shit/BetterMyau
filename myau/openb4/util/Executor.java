package myau.openb4.util;

public final class Executor {

    private static final java.util.concurrent.ConcurrentLinkedQueue<Runnable> pending = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public static void later(Runnable task, long delay) {
        long deadline = System.currentTimeMillis() + delay;
        pending.add(new Runnable() {

            public void run() {
                if (System.currentTimeMillis() >= deadline) {
                    pending.remove(this);
                    task.run();
                }
            }
        });
    }

    public static void tick() {
        for (Runnable task : pending) task.run();
    }

    public static void clear() {
        pending.clear();
    }
}
