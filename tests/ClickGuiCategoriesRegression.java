package myau;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ClickGuiCategoriesRegression {
    private static int checks;
    private static final String[] titles = {"Combat", "Movement", "Player", "Render", "Exploits", "Misc", "Configs"};
    private static class Dummy extends Module { Dummy(String name) { super(name, false, 0); } }
    private static final class Combat extends Dummy { Combat() { super("AimAssist"); } }
    private static final class Movement extends Dummy { Movement() { super("Fly"); } }
    private static final class Player extends Dummy { Player() { super("AutoTool"); } }
    private static final class Exploits extends Dummy { Exploits() { super("Pit"); } }
    private static final class Misc extends Dummy { Misc() { super("Weather"); } }

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static List<String> headers(ClickGuiScreen screen) throws Exception {
        Method layout = ClickGuiScreen.class.getDeclaredMethod("layout");
        layout.setAccessible(true);
        layout.invoke(screen);
        List<String> titles = new ArrayList<>();
        for (Object row : (List<?>) field(screen, "rows")) {
            if ((Integer) field(row, "kind") == 0) titles.add((String) field(field(row, "panel"), "title"));
        }
        return titles;
    }

    public static void main(String[] args) throws Exception {
        check(Paths.get("").toAbsolutePath().toString().contains("codebase-tests-"), "category config tests require an isolated directory");
        MyauClient.moduleManager.modules.clear();
        MyauClient.settingManager.settingsByModule.clear();
        ClickGuiModule gui = new ClickGuiModule();
        MyauClient.moduleManager.modules.put(ClickGuiModule.class, gui);
        ArrayList<Setting<?>> settings = new ArrayList<>();
        for (Field field : ClickGuiModule.class.getFields()) {
            if (!Setting.class.isAssignableFrom(field.getType())) continue;
            Setting<?> setting = (Setting<?>) field.get(gui);
            setting.setModule(gui);
            settings.add(setting);
        }
        MyauClient.settingManager.settingsByModule.put(ClickGuiModule.class, settings);
        for (Module module : new Module[] {new Combat(), new Movement(), new Player(), new Exploits(), new Misc()}) {
            MyauClient.moduleManager.modules.put(module.getClass(), module);
            module.setEnabled(true);
        }
        Path directory = Paths.get("config", "Myau");
        Files.createDirectories(directory);
        Files.write(directory.resolve("category-fixture.json"), "{}".getBytes(StandardCharsets.UTF_8));
        ClickGuiScreen screen = new ClickGuiScreen();
        screen.field_146294_l = 640;
        screen.field_146295_m = 360;
        List<String> visible = headers(screen);
        for (String title : titles) {
            boolean expected = !title.equals("Misc");
            check(gui.categorySetting(title).getValue() == expected, title + " has the requested default");
            check(visible.contains(title) == expected, title + " panel follows its default visibility");
        }
        screen.func_73864_a(600, 8, 0);
        for (int index = 0; index < titles.length; index++) {
            if (gui.categorySetting(titles[index]).getValue()) screen.func_73864_a(500, 36 + index * 18, 0);
        }
        check(headers(screen).isEmpty(), "every panel, including Render and Configs, can be hidden");
        for (Module module : MyauClient.moduleManager.modules.values()) {
            if (module instanceof Dummy) check(module.isEnabled(), "hiding a category keeps " + module.getName() + " running");
        }
        screen.func_73869_a('\0', 1);
        check(!(Boolean) field(screen, "categoriesOpen"), "Escape closes the category menu");
        screen.func_73864_a(600, 8, 0);
        screen.func_73864_a(500, 36 + 3 * 18, 0);
        check(headers(screen).contains("Render"), "the Categories button restores panels when all are hidden");
        ConfigProfile profile = new ConfigProfile("category-visibility");
        check(profile.save(), "category visibility saves with the profile");
        for (String title : titles) gui.categorySetting(title).setValue(true);
        check(profile.load(), "category visibility profile loads");
        for (String title : titles) check(gui.categorySetting(title).getValue() == title.equals("Render"), title + " visibility survives a profile round trip");
        Path file = directory.resolve(profile.fileName());
        JsonObject old = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject state = old.getAsJsonObject("ClickGui");
        for (String title : titles) if (!title.equals("Misc")) state.remove(gui.categorySetting(title).getName());
        state.addProperty("show-misc", true);
        Files.write(file, old.toString().getBytes(StandardCharsets.UTF_8));
        check(profile.load(), "older profiles without the new settings still load");
        for (String title : titles) check(gui.categorySetting(title).getValue(), title + " defaults on while preserving an existing Misc preference");
        System.out.println("ClickGUI category checks passed: " + checks);
    }
}
