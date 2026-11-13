package myau.openb4.util;

import net.minecraft.client.Minecraft;

public final class Strings {

    private static final Minecraft mc = Minecraft.func_71410_x();
    public static final char SECTION = '§';
    public static final char HEART = '❤';
    public static final String RED = "§c", GOLD = "§6", YELLOW = "§e", GREEN = "§a", DARK_GREEN = "§2";

    public static String number(double value) {
        return value == Math.floor(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private Strings() {
    }

    public static double round(double d, int n) {
        if (n == 0) {
            return Math.round(d);
        }
        double d2 = Math.pow(10.0, n);
        return (double) Math.round(d * d2) / d2;
    }

    public static void heart(StringBuilder sb) {
        sb.append(SECTION);
        sb.append('c');
        sb.append(HEART);
    }
}
