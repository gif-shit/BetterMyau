package myau;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

final class TabGui {
    static final int LEFT = 5;
    static final int TOP = 5;
    static final int WIDTH = 78;
    static final int DEFAULT_ACCENT = 0xC38DE4;

    private static final int ROW_HEIGHT = 13;
    private static final int LOGO_HEIGHT = 16;
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int BACKGROUND = 0xD8111014;
    private static final int BORDER = 0xFF302A37;
    private static final ModuleCategory[] CATEGORIES = ModuleCategory.values();
    private static final int[] KEYS = {
        Keyboard.KEY_UP, Keyboard.KEY_DOWN, Keyboard.KEY_LEFT, Keyboard.KEY_RIGHT,
        Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER
    };

    private int category;
    private int selection;
    private int previousKeys;
    private List<Module> modules = Collections.emptyList();

    void tick(Minecraft minecraft, boolean enabled) {
        if (!Keyboard.isCreated()) return;
        int keys = 0;
        for (int i = 0; i < KEYS.length; i++) {
            if (Keyboard.isKeyDown(KEYS[i])) keys |= 1 << i;
        }
        int pressed = keys & ~previousKeys;
        previousKeys = keys;
        if (!enabled || minecraft.field_71462_r != null || !minecraft.field_71415_G
                || minecraft.field_71474_y.field_74330_P) return;
        for (int i = 0; i < KEYS.length; i++) {
            if ((pressed & (1 << i)) != 0) handleKey(KEYS[i]);
        }
    }

    void handleKey(int key) {
        switch (key) {
            case Keyboard.KEY_UP:
            case Keyboard.KEY_DOWN:
                int step = key == Keyboard.KEY_UP ? -1 : 1;
                if (modules.isEmpty()) category = Math.floorMod(category + step, CATEGORIES.length);
                else selection = Math.floorMod(selection + step, modules.size());
                break;
            case Keyboard.KEY_LEFT:
                modules = Collections.emptyList();
                break;
            case Keyboard.KEY_RIGHT:
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (modules.isEmpty()) {
                    modules = CATEGORIES[category].modules();
                    selection = 0;
                } else {
                    MyauClient.moduleManager.toggle(modules.get(selection));
                }
                break;
            default:
                break;
        }
    }

    int bottom(boolean watermark, boolean tabs) {
        return TOP + (watermark ? LOGO_HEIGHT : 0) + (tabs ? 2 + CATEGORIES.length * ROW_HEIGHT : 0);
    }

    void draw(Minecraft minecraft, boolean watermark, boolean tabs, int rgb) {
        FontRenderer font = minecraft.field_71466_p;
        int accent = 0xFF000000 | rgb;
        int top = TOP;
        if (watermark) {
            GlStateManager.func_179094_E();
            try {
                GlStateManager.func_179109_b(LEFT, top, 0.0f);
                GlStateManager.func_179152_a(1.4f, 1.4f, 1.0f);
                font.func_175063_a("m", 0, 0, accent);
                font.func_175063_a("yau", font.func_78256_a("m"), 0, 0xFFF1EDF4);
            } finally {
                GlStateManager.func_179121_F();
            }
            top += LOGO_HEIGHT;
        }
        if (!tabs) return;

        panel(LEFT, top, WIDTH, CATEGORIES.length);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int y = top + 1 + i * ROW_HEIGHT;
            if (i == category) highlight(LEFT + 1, y, WIDTH - 2, accent);
            font.func_175063_a(CATEGORIES[i].label, LEFT + 4, y + 3, i == category ? 0xFFFFFFFF : 0xFFC1BCC7);
        }
        if (modules.isEmpty()) return;

        ScaledResolution screen = new ScaledResolution(minecraft);
        int x = LEFT + WIDTH + 3;
        int width = Math.min(112, screen.func_78326_a() - x - LEFT);
        if (width < 24) return;
        int available = (screen.func_78328_b() - top - 7) / ROW_HEIGHT;
        int visible = Math.min(modules.size(), Math.min(MAX_VISIBLE_ROWS, available));
        if (visible < 1) return;
        int first = Math.max(0, Math.min(selection - visible / 2, modules.size() - visible));
        panel(x, top, width, visible);
        for (int i = 0; i < visible; i++) {
            int index = first + i;
            Module module = modules.get(index);
            int y = top + 1 + i * ROW_HEIGHT;
            if (index == selection) highlight(x + 1, y, width - 2, accent);
            String name = font.func_78269_a(module.getName(), width - 15);
            font.func_175063_a(name, x + 4, y + 3,
                    index == selection || module.isEnabled() ? 0xFFFFFFFF : 0xFFAAA4B0);
            if (module.isEnabled()) Gui.func_73734_a(x + width - 7, y + 5, x + width - 4, y + 8, accent);
        }
    }

    private static void panel(int x, int y, int width, int rows) {
        int bottom = y + rows * ROW_HEIGHT + 2;
        Gui.func_73734_a(x, y, x + width, bottom, BORDER);
        Gui.func_73734_a(x + 1, y + 1, x + width - 1, bottom - 1, BACKGROUND);
    }

    private static void highlight(int x, int y, int width, int accent) {
        Gui.func_73734_a(x, y, x + width, y + ROW_HEIGHT, (accent & 0xFFFFFF) | 0x9E000000);
        Gui.func_73734_a(x, y, x + 1, y + ROW_HEIGHT, accent);
    }
}
