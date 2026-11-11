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
import java.nio.file.OpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.List;
import myau.Module;
import myau.MyauClient;
import myau.Setting;
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
            Files.createDirectories(this.file.toPath().getParent(), new FileAttribute[0]);
        }
        catch (Exception exception) {
            ((AccessorMinecraft)minecraft).getLogger().error(exception.getMessage());
        }
    }

    public String fileName() {
        return this.file.getName();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public boolean load() {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(this.file.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
            JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
            Iterator<Module> iterator = MyauClient.moduleManager.modules.values().iterator();
            while (true) {
                JsonObject state;
                Module module;
                block26: {
                    Iterator iterator2;
                    block27: {
                        block25: {
                            if (!iterator.hasNext()) break block25;
                            module = iterator.next();
                            List settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
                            JsonElement entry = config.get(module.getName());
                            if (entry == null) {
                                ConfigProfile.resetModule(module, settings);
                                continue;
                            }
                            state = entry.getAsJsonObject();
                            if (settings == null) break block26;
                            iterator2 = settings.iterator();
                            break block27;
                        }
                        activeProfileName = this.profileName;
                        boolean bl = true;
                        return bl;
                    }
                    while (iterator2.hasNext()) {
                        Setting setting = (Setting)iterator2.next();
                        try {
                            if (setting.readJson(state)) continue;
                            throw new IllegalArgumentException("Invalid setting value");
                        }
                        catch (Exception exception) {
                            setting.reset();
                        }
                    }
                }
                try {
                    module.setHidden(state.get("hidden").getAsBoolean());
                }
                catch (Exception exception) {
                    module.resetHidden();
                }
                try {
                    module.setKeyCode(state.get("key").getAsInt());
                }
                catch (Exception exception) {
                    module.resetKeyCode();
                }
                try {
                    module.setEnabled(state.get("toggled").getAsBoolean());
                }
                catch (Exception exception) {
                    module.resetEnabled();
                }
            }
        }
        catch (Exception exception) {
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
                List settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
                if (settings != null) {
                    for (Setting setting : settings) {
                        setting.writeJson(state);
                    }
                }
                config.add(module.getName(), state);
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(this.file.toPath(), new OpenOption[0]), StandardCharsets.UTF_8);){
                GSON.toJson((JsonElement)config, (Appendable)writer);
            }
            activeProfileName = this.profileName;
            return true;
        }
        catch (Exception exception) {
            return false;
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
