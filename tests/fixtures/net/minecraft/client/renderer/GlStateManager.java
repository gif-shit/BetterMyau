package net.minecraft.client.renderer;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GlStateManager {
    public static float[] scale = {1, 1, 1};
    private static final Deque<float[]> matrices = new ArrayDeque<>();

    public static void func_179094_E() { matrices.push(scale.clone()); }
    public static void func_179121_F() { scale = matrices.pop(); }
    public static void func_179152_a(float x, float y, float z) {
        scale[0] *= x;
        scale[1] *= y;
        scale[2] *= z;
    }
}
