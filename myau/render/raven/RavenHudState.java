package myau.render.raven;

import java.util.Locale;

public final class RavenHudState {

    private Object target;
    private String name;
    private float health, ratio, from, goal;
    private long lastSeen, changedAt, duration;

    public void reset() {
        target = null;
    }

    public void update(Object target, String name, float health, float maximum, long now, boolean animate) {
        float ratio = maximum <= 0 ? 0 : Math.max(0, Math.min(1, health / maximum));
        float current = shown(now);
        if (this.target != target) {
            from = ratio;
            goal = ratio;
            changedAt = now;
            duration = 0;
        } else if (goal != ratio) {
            from = current;
            duration = ratio > goal ? 500 : 800;
            changedAt = now;
            goal = ratio;
        }
        if (!animate) {
            from = goal;
            duration = 0;
        }
        this.target = target;
        this.name = name;
        this.health = Math.max(0, health);
        this.ratio = ratio;
        lastSeen = now;
    }

    public float shown(long now) {
        double time = duration <= 0 ? 1 : Math.max(0, Math.min(1, (double) (now - changedAt) / duration));
        return (float) (from + (goal - from) * (1 - Math.pow(1 - time, 5)));
    }

    public float front(long now) {
        return Math.min(ratio, shown(now));
    }

    public int alpha(long now) {
        if (target == null || now - lastSeen >= 650)
            return 0;
        double time = Math.max(0, (now - lastSeen - 250) / 400.0);
        double eased = time < 0.5 ? 4 * time * time * time : 1 + 4 * Math.pow(time - 1, 3);
        return (int) Math.round(255 * (1 - eased));
    }

    public String text(float ownHealth, float ownMaximum, boolean indicator) {
        char color = ratio < 0.3 ? 'c' : ratio < 0.5 ? '6' : ratio < 0.7 ? 'e' : 'a';
        String result = name + " \u00a7" + color + number(health);
        if (indicator) {
            float difference = ownHealth - health;
            char differenceColor = difference >= 8 ? '2' : difference > 2 ? 'a' : difference > -2 ? 'e' : difference > -6 ? '6' : 'c';
            result += " \u00a7" + differenceColor + (difference >= 0 ? "+" : "") + number(difference);
            result += ratio <= (ownMaximum <= 0 ? 0 : ownHealth / ownMaximum) ? " \u00a7aW" : " \u00a7cL";
        }
        String plain = result.replaceAll("\u00a7.", "");
        if (plain.length() < 14) {
            int spaces = (17 - plain.length()) / 2;
            String padding = String.format("%" + spaces + "s", "");
            result = padding + result + padding;
        }
        return result;
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.1f", value).replaceFirst("\\.0$", "");
    }
}
