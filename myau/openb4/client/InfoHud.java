package myau.openb4.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public final class InfoHud {

    public static final class Line {

        final String left, right;

        final ItemStack item;

        Line(String left, String right, ItemStack item) {
            this.left = Chat.color(left);
            this.right = right == null ? null : Chat.color(right);
            this.item = item;
        }
    }

    private static final List<Line> lines = new ArrayList<>();

    public static Line line(String left, String right, ItemStack item) {
        return new Line(left, right, item);
    }

    public static void add(Line line) {
        if (line != null)
            lines.add(line);
    }

    public static void add(String text) {
        add(line(text, null, null));
    }

    public static void clear() {
        lines.clear();
    }

    public static void render() {
        try {
            if (!Overlay.canRender() || lines.isEmpty())
                return;
            Minecraft mc = Minecraft.func_71410_x();
            int width = 0;
            for (Line line : lines) width = Math.max(width, mc.field_71466_p.func_78256_a(line.left) + (line.right == null ? 0 : mc.field_71466_p.func_78256_a(line.right) + 10) + (line.item == null ? 0 : 20));
            myau.RenderUtils.roundedRect(3, 82, width + 19, 92 + lines.size() * 18, 5, 0xe51a1d26);
            int y = 88;
            for (Line line : lines) {
                mc.field_71466_p.func_175065_a(line.left, 10, y, 0xffeeeeee, true);
                if (line.right != null)
                    mc.field_71466_p.func_175065_a(line.right, width + 10 - (line.item == null ? 0 : 20) - mc.field_71466_p.func_78256_a(line.right), y, 0xffeeeeee, true);
                if (line.item != null)
                    mc.func_175599_af().func_180450_b(line.item, width - 6, y - 3);
                y += 18;
            }
        } finally {
            clear();
        }
    }
}
