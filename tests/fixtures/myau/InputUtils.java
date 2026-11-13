package myau;

import java.util.HashSet;
import net.minecraft.client.settings.KeyBinding;

public class InputUtils {
    public static final HashSet<Integer> keys = new HashSet<>();
    public static boolean isKeyDown(int key) { return keys.contains(key); }
    public static void setKeyState(int key, boolean down) { KeyBinding.func_74510_a(key, down); }
    public static void updateKeyState(int key) { setKeyState(key, isKeyDown(key)); }
}
