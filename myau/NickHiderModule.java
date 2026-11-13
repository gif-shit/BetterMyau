package myau;

import java.util.regex.Matcher;
import net.minecraft.client.Minecraft;

public class NickHiderModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final StringSetting nameSetting = new StringSetting("name", "You");
    public final BooleanSetting scoreboard = new BooleanSetting("scoreboard", true);
    public final BooleanSetting level = new BooleanSetting("level", true);

    public NickHiderModule() {
        super("NickHider", false, 0, true);
    }

    public String formatText(String text) {
        if (text == null || minecraft.field_71439_g == null) {
            return text;
        }
        if (this.scoreboard.getValue() && text.matches("\u00a77\\d{2}/\\d{2}/\\d{2}(?:\\d{2})?  ?\u00a78.*")) {
            text = text.replaceAll("\u00a78", "\u00a78\u00a7k").replaceAll("[^\\x00-\\x7F\u00a7]", "?");
        }
        return text.replaceAll(minecraft.field_71439_g.func_70005_c_(), Matcher.quoteReplacement(ChatColor.formatText((String) this.nameSetting.getValue())));
    }
}
