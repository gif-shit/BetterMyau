package myau;

import myau.Module;

public class AntiObfuscateModule
extends Module {
    public AntiObfuscateModule() {
        super("AntiObfuscate", false, 0, true);
    }

    public String formatText(String text) {
        return text.replaceAll("\u00a7k", "");
    }
}
