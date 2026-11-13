package myau;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import myau.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;

public class ConfigProfile {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final String profileName;
    private final File file;
    public static String activeProfileName;

    public ConfigProfile(String name) {
        this.profileName = name != null && !name.isEmpty() && !name.equals("!") ? name : "predac1";
        this.file = new File("./config/Myau/", String.format("%s.json", this.profileName));
        try {
            Files.createDirectories(this.file.toPath().getParent());
        } catch (Exception exception) {
            ((AccessorMinecraft) minecraft).getLogger().error(exception.getMessage());
        }
    }

    public String fileName() {
        return this.file.getName();
    }

    public boolean load() {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.file.toPath()), StandardCharsets.UTF_8)) {
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            migrateBedTracker(config);
            for (Module module : MyauClient.moduleManager.modules.values()) {
                List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
                JsonElement entry = config.get(module.getName());
                if (entry == null) {
                    resetModule(module, settings);
                    continue;
                }
                JsonObject state = entry.getAsJsonObject();
                if (module instanceof ScaffoldModule && state.has("rotations") && state.get("rotations").isJsonPrimitive() && "STABLE".equalsIgnoreCase(state.get("rotations").getAsString())) {
                    state.addProperty("rotations", "SMOOTH");
                }
                if (settings != null) {
                    for (Setting<?> setting : settings) {
                        try {
                            if (!setting.readJson(state)) {
                                throw new IllegalArgumentException("Invalid setting value");
                            }
                        } catch (Exception invalidSetting) {
                            setting.reset();
                        }
                    }
                }
                try {
                    module.setHidden(state.get("hidden").getAsBoolean());
                } catch (Exception exception) {
                    module.resetHidden();
                }
                try {
                    module.setKeyCode(state.get("key").getAsInt());
                } catch (Exception exception) {
                    module.resetKeyCode();
                }
                try {
                    module.setEnabled(state.get("toggled").getAsBoolean());
                } catch (Exception exception) {
                    module.resetEnabled();
                }
            }
            activeProfileName = this.profileName;
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean save() {
        try {
            JsonObject config = new JsonObject();
            for (Module module : MyauClient.moduleManager.modules.values()) {
                JsonObject state = new JsonObject();
                state.addProperty("toggled", module.isEnabled());
                state.addProperty("key", module.getKeyCode());
                state.addProperty("hidden", module.isHidden());
                List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
                if (settings != null) {
                    for (Setting<?> setting : settings) {
                        setting.writeJson(state);
                    }
                }
                config.add(module.getName(), state);
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(this.file.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            activeProfileName = this.profileName;
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    static void migrateBedTracker(JsonObject config) {
        JsonElement legacy = config.get("BedTracker");
        if (legacy == null || !legacy.isJsonObject()) return;
        JsonObject old = legacy.getAsJsonObject();
        JsonObject bedwars;
        if (!config.has("BedWars")) {
            bedwars = new JsonParser().parse(old.toString()).getAsJsonObject();
            bedwars.addProperty("bed-tracker", true);
            config.add("BedWars", bedwars);
        } else {
            bedwars = config.getAsJsonObject("BedWars");
            for (java.util.Map.Entry<String, JsonElement> entry : old.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("toggled") && !key.equals("key") && !key.equals("hidden") && !bedwars.has(key)) bedwars.add(key, entry.getValue());
            }
        }
    }

    private static void resetModule(Module module, List<Setting<?>> settings) {
        if (settings != null) {
            for (Setting<?> setting : settings) {
                setting.reset();
            }
        }
        module.resetHidden();
        module.resetKeyCode();
        module.resetEnabled();
    }
}
