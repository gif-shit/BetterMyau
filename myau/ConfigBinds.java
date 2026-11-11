package myau;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import myau.ChatUtils;
import myau.ConfigProfile;
import myau.InputUtils;
import net.minecraft.client.Minecraft;

public final class ConfigBinds {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final File FILE = new File("./config/Myau/", "config_binds.txt");
    private static final Map<String, Integer> BINDS = new LinkedHashMap<String, Integer>();
    private static final Map<String, Boolean> DOWN = new HashMap<String, Boolean>();
    private static boolean loaded;

    private ConfigBinds() {
    }

    public static synchronized void load() {
        loaded = true;
        BINDS.clear();
        try {
            if (!FILE.isFile()) {
                return;
            }
            for (String string : Files.readAllLines(FILE.toPath(), StandardCharsets.UTF_8)) {
                int n = string.lastIndexOf(61);
                if (n <= 0) continue;
                String string2 = string.substring(0, n).trim().toLowerCase(Locale.ROOT);
                int n2 = Integer.parseInt(string.substring(n + 1).trim());
                if (n2 == 0) continue;
                BINDS.put(string2, n2);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void save() {
        try {
            ArrayList<String> arrayList = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : BINDS.entrySet()) {
                arrayList.add(entry.getKey() + "=" + entry.getValue());
            }
            File file = FILE.getParentFile();
            if (file != null) {
                file.mkdirs();
            }
            Files.write(FILE.toPath(), arrayList, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static synchronized int get(String string) {
        Integer n;
        if (!loaded) {
            ConfigBinds.load();
        }
        return (n = BINDS.get(string.toLowerCase(Locale.ROOT))) == null ? 0 : n;
    }

    public static synchronized void set(String string, int n) {
        if (!loaded) {
            ConfigBinds.load();
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        if (n == 0) {
            BINDS.remove(string2);
        } else {
            BINDS.put(string2, n);
        }
        DOWN.put(string2, n != 0 && InputUtils.isKeyDown(n));
        ConfigBinds.save();
    }

    public static boolean loadConfig(String string) {
        ConfigProfile configProfile = new ConfigProfile(string);
        boolean bl = configProfile.load();
        ChatUtils.addFormattedMessage(String.format(bl ? "%sConfig has been loaded (&a&o%s&r)&r" : "%sConfig couldn't be loaded (&c&o%s&r)&r", "&7[&bMyau&7] &r", configProfile.fileName()));
        return bl;
    }

    public static synchronized void tick() {
        if (!loaded) {
            ConfigBinds.load();
        }
        if (BINDS.isEmpty()) {
            return;
        }
        boolean bl = ConfigBinds.minecraft.field_71462_r != null || ConfigBinds.minecraft.field_71439_g == null;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (Map.Entry<String, Integer> object : BINDS.entrySet()) {
            boolean bl2 = InputUtils.isKeyDown(object.getValue());
            Boolean bl3 = DOWN.get(object.getKey());
            DOWN.put(object.getKey(), bl2);
            if (bl || bl3 == null || bl3.booleanValue() || !bl2) continue;
            arrayList.add(object.getKey());
        }
        for (String string : arrayList) {
            ConfigBinds.loadConfig(string);
        }
    }
}
