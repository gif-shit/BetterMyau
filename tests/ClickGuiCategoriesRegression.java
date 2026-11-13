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
import java.util.Arrays;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import sun.misc.Unsafe;

public final class ClickGuiCategoriesRegression {
    private static int checks;
    private static final String[] titles = {"Combat", "Movement", "Player", "Render", "Exploits", "Misc", "Configs"};
    private static class Dummy extends Module { Dummy(String name) { super(name, false, 0); } }
    private static final class Combat extends Dummy { Combat() { super("AimAssist"); } }
    private static final class Movement extends Dummy { Movement() { super("Fly"); } }
    private static final class Player extends Dummy { Player() { super("AutoTool"); } }
    private static final class Exploits extends Dummy { Exploits() { super("Pit"); } }
    private static final class Misc extends Dummy { Misc() { super("Weather"); } }
    private static final class Font extends FontRenderer {
        Font() { super(null, null, null, false); }
        public int func_78256_a(String text) { return text.length() * 6; }
        public String func_78269_a(String text, int width) { return text.substring(0, Math.min(text.length(), Math.max(0, width / 6))); }
        public int func_175063_a(String text, float x, float y, int color) { return func_78256_a(text); }
        public List<String> func_78271_c(String text, int width) {
            List<String> lines = new ArrayList<>();
            String line = "";
            for (String word : text.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && func_78256_a(next) > width) {
                    lines.add(line);
                    line = word;
                } else line = next;
            }
            lines.add(line);
            return lines;
        }
    }

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
        check(gui.tooltips.getValue(), "tooltips default on");
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
        gui.tooltips.setValue(false);
        check(profile.save(), "category visibility saves with the profile");
        for (String title : titles) gui.categorySetting(title).setValue(true);
        gui.tooltips.setValue(true);
        check(profile.load(), "category visibility profile loads");
        check(!gui.tooltips.getValue(), "tooltip preference survives a profile round trip");
        for (String title : titles) check(gui.categorySetting(title).getValue() == title.equals("Render"), title + " visibility survives a profile round trip");
        Path file = directory.resolve(profile.fileName());
        JsonObject old = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject state = old.getAsJsonObject("ClickGui");
        for (String title : titles) if (!title.equals("Misc")) state.remove(gui.categorySetting(title).getName());
        state.addProperty("show-misc", true);
        state.remove("tooltips");
        Files.write(file, old.toString().getBytes(StandardCharsets.UTF_8));
        check(profile.load(), "older profiles without the new settings still load");
        for (String title : titles) check(gui.categorySetting(title).getValue(), title + " defaults on while preserving an existing Misc preference");
        check(gui.tooltips.getValue(), "older profiles enable tooltips by default");
        tooltipChecks(gui);
        System.out.println("ClickGUI category checks passed: " + checks);
    }

    private static Object row(ClickGuiScreen screen, int kind, Module module, Setting<?> setting) throws Exception {
        headers(screen);
        for (Object row : (List<?>) field(screen, "rows"))
            if ((Integer) field(row, "kind") == kind && field(row, "module") == module && field(row, "setting") == setting) return row;
        return null;
    }

    private static void tooltipChecks(ClickGuiModule gui) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Font font = (Font) ((Unsafe) access.get(null)).allocateInstance(Font.class);
        Minecraft.func_71410_x().field_71466_p = font;
        Field names = ModuleCategory.class.getDeclaredField("moduleNames");
        names.setAccessible(true);
        for (ModuleCategory category : ModuleCategory.values())
            for (String name : (String[]) names.get(category))
                check(!new Dummy(name).getDescription().isEmpty(), name + " has a description");
        for (String name : new String[] {"MCF", "Spammer", "AntiObfuscate"})
            check(!new Dummy(name).getDescription().isEmpty(), name + " has a description");
        Module module = MyauClient.moduleManager.getModule("AimAssist");
        BooleanSetting longName = new BooleanSetting("only-while-middle-clicking", true);
        BooleanSetting shortName = new BooleanSetting("short", true);
        longName.setModule(module);
        shortName.setModule(module);
        MyauClient.settingManager.settingsByModule.put(module.getClass(), new ArrayList<Setting<?>>(Arrays.<Setting<?>>asList(longName, shortName)));
        ClickGuiScreen screen = new ClickGuiScreen();
        screen.field_146294_l = 640;
        screen.field_146295_m = 800;
        Field expandedField = ClickGuiScreen.class.getDeclaredField("EXPANDED");
        expandedField.setAccessible(true);
        Set<Module> expanded = (Set<Module>) expandedField.get(null);
        expanded.add(module);
        Object title = row(screen, 1, module, null);
        Object description = row(screen, 4, module, null);
        Object setting = row(screen, 2, module, longName);
        check((Integer) field(description, "y") == (Integer) field(title, "y") + (Integer) field(title, "h"), "description sits directly below the module name");
        check((Integer) field(setting, "y") == (Integer) field(description, "y") + (Integer) field(description, "h"), "settings start after the description");
        int height = (Integer) field(description, "h");
        screen.func_73864_a((Integer) field(description, "x") + 12, (Integer) field(description, "y") + 3, 0);
        check(module.isEnabled() && longName.getValue(), "description clicks leave module and settings unchanged");
        gui.tooltips.setValue(false);
        check(row(screen, 4, module, null) == null, "disabling tooltips removes description rows");
        check((Integer) field(row(screen, 2, module, longName), "y") == (Integer) field(setting, "y") - height, "disabling tooltips removes the extra row height");
        gui.tooltips.setValue(true);
        Module noSettings = MyauClient.moduleManager.getModule("Fly");
        Object plain = row(screen, 1, noSettings, null);
        screen.func_73864_a((Integer) field(plain, "x") + 10, (Integer) field(plain, "y") + 4, 1);
        check(row(screen, 4, noSettings, null) != null, "modules without settings can expand to show descriptions");
        Method lines = ClickGuiScreen.class.getDeclaredMethod("descriptionLines", Module.class, int.class);
        lines.setAccessible(true);
        for (String line : (List<String>) lines.invoke(null, module, 90))
            check(font.func_78256_a(line) <= 72, "descriptions wrap inside narrow panels");
        setting = row(screen, 2, module, longName);
        Method drawLabel = ClickGuiScreen.class.getDeclaredMethod("drawSettingLabel", FontRenderer.class, setting.getClass(), int.class);
        drawLabel.setAccessible(true);
        drawLabel.invoke(null, font, setting, (Integer) field(setting, "w") - 30);
        check("only while middle clicking".equals(field(setting, "tooltip")), "shortened labels retain their full readable text");
        Method hover = ClickGuiScreen.class.getDeclaredMethod("tooltipAt", int.class, int.class);
        hover.setAccessible(true);
        int x = (Integer) field(setting, "x") + 12, y = (Integer) field(setting, "y") + 4;
        check("only while middle clicking".equals(hover.invoke(screen, x, y)), "hovering a shortened label shows the full name");
        check(hover.invoke(screen, (Integer) field(setting, "x") + (Integer) field(setting, "w") - 7, y) == null, "hover text stays away from switches");
        gui.tooltips.setValue(false);
        check(hover.invoke(screen, x, y) == null, "the tooltip option also controls hover text");
        gui.tooltips.setValue(true);
        Field dragging = ClickGuiScreen.class.getDeclaredField("draggingSlider");
        dragging.setAccessible(true);
        dragging.set(screen, setting);
        check(hover.invoke(screen, x, y) == null, "hover text is hidden while dragging a slider");
        dragging.set(screen, null);
        Field menu = ClickGuiScreen.class.getDeclaredField("categoriesOpen");
        menu.setAccessible(true);
        menu.setBoolean(screen, true);
        check(hover.invoke(screen, x, y) == null, "the category menu stays above tooltips");
        menu.setBoolean(screen, false);
        Object shortRow = row(screen, 2, module, shortName);
        drawLabel.invoke(null, font, shortRow, 100);
        check(field(shortRow, "tooltip") == null, "labels that already fit do not show redundant hover text");
        expanded.clear();
    }
}
