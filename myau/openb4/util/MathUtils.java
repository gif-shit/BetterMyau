package myau.openb4.util;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import myau.FloatSetting;

public final class MathUtils {

    public static final Random RANDOM = new Random();

    private MathUtils() {
    }

    public static int randomInt(int min, int max) {
        return RANDOM.nextInt(max - min + 1) + min;
    }

    public static int randomInt(int n) {
        return (int) ((double) (n + 1) * Math.random());
    }

    public static double randomBetween(double min, double max) {
        return min + (max - min) * Math.random();
    }

    public static double randomBetween(FloatSetting min, FloatSetting max) {
        return min.getValue() == max.getValue() ? min.getValue() : min.getValue() + RANDOM.nextDouble() * (max.getValue() - min.getValue());
    }

    public static <T> void dedupe(List<T> list) {
        HashSet<T> set = new HashSet<T>(list);
        list.clear();
        list.addAll(set);
    }

    public static String randomString(int n) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            sb.append(chars[RANDOM.nextInt(chars.length)]);
        }
        return sb.toString();
    }
}
