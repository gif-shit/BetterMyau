package myau.openb4.client;

import myau.BooleanSetting;
import myau.openb4.module.ModuleManager;

public final class ChatCommands {

    private ChatCommands() {
    }

    public static String distance(double d, boolean brackets) {
        char c = d <= 10.0 ? 'c' : (d <= 25.0 ? '6' : (d <= 50.0 ? 'e' : (d <= 100.0 ? 'a' : '2')));
        String s = "&" + c + (int) d + "m";
        if (brackets
                && ((BooleanSetting) ModuleManager.setting(ModuleManager.get("Settings"), "Add brackets to distance")).getValue()) {
            s = "&7(" + s + "&7)";
        }
        return s;
    }
}
