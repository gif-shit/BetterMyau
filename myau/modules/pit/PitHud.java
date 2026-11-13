package myau.modules.pit;

import java.util.*;
import myau.modules.Pit;

public final class PitHud {

    private PitHud() {
    }

    public static final class Panel {

        public final List<String> rows;
        public final float x, y, width, height, scale, lineHeight;
        public final boolean rightAligned;

        Panel(List<String> rows, float x, float y, float width, float height, float scale, float lineHeight, boolean right) {
            this.rows = Collections.unmodifiableList(rows);
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
            this.lineHeight = lineHeight;
            rightAligned = right;
        }
    }

    public static Panel layout(List<String> source, Pit.Horizontal horizontal, Pit.Vertical vertical, int offsetX, int offsetY, float scale, Pit.Host host, boolean truncateRows) {
        float screenWidth = host.screenWidth() / scale, screenHeight = host.screenHeight() / scale;
        float lineHeight = host.fontHeight() - 1 + (host.textShadow() ? 1 : 0);
        int count = truncateRows ? Math.min(source.size(), Math.max(1, (int) ((screenHeight - 4) / lineHeight))) : source.size();
        List<String> rows = new ArrayList<>();
        float width = 0;
        for (int i = 0; i < count; i++) {
            String row = i == count - 1 && source.size() > count ? "\u00a77+" + (source.size() - count + 1) + " more rows" : source.get(i);
            rows.add(row);
            width = Math.max(width, host.textWidth(row));
        }
        width = Math.min(width + 6, screenWidth);
        float height = count * lineHeight + 1;
        int available = Math.max(1, (int) (width - 6));
        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i);
            if (host.textWidth(row) <= available)
                continue;
            while (!row.isEmpty() && host.textWidth(row) > available) row = row.substring(0, row.length() - 1);
            if (row.endsWith("\u00a7"))
                row = row.substring(0, row.length() - 1);
            if (row.lastIndexOf('[') > row.lastIndexOf(']'))
                row = row.substring(0, row.lastIndexOf('[')).trim();
            rows.set(i, row);
        }
        float x = offsetX / scale, y = offsetY / scale;
        if (horizontal == Pit.Horizontal.MIDDLE)
            x += screenWidth / 2 - width / 2;
        else if (horizontal == Pit.Horizontal.RIGHT)
            x = screenWidth - width - x;
        if (vertical == Pit.Vertical.MIDDLE)
            y += screenHeight / 2 - height / 2;
        else if (vertical == Pit.Vertical.BOTTOM)
            y = screenHeight - height - y;
        x = Math.max(0, Math.min(x, screenWidth - width));
        y = Math.max(0, Math.min(y, screenHeight - height));
        return new Panel(rows, x, y, width, height, scale, lineHeight, horizontal == Pit.Horizontal.RIGHT);
    }
}
