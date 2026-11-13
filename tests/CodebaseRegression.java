package myau;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import myau.source.SourceExceptions;

public final class CodebaseRegression {
    private static int checks;

    private static void check(boolean value, String message) {
        ++checks;
        if (!value) throw new AssertionError(message);
    }

    private static final class ExampleModule extends Module {
        final BooleanSetting flag = new BooleanSetting("flag", true);
        final IntegerSetting delay = new IntegerSetting("delay", 3, 0, 9);
        final FloatSetting range = new FloatSetting("range", 1f, 0.5f, 2f);
        final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"FIRST", "SECOND"});
        int delayWhenEnabled;

        ExampleModule() {
            super("CleanupTest", false, 0);
            for (Setting<?> setting : settings()) setting.setModule(this);
        }

        List<Setting<?>> settings() {
            return Arrays.<Setting<?>>asList(flag, delay, range, mode);
        }

        public void onEnable() {
            delayWhenEnabled = delay.getValue();
        }
    }

    private static class TestEvent extends CancellableEvent { }
    private static final class ChildEvent extends TestEvent { }

    private static void save(Path file, JsonObject json) throws IOException {
        Files.write(file, json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        check(Paths.get("").toAbsolutePath().toString().contains("codebase-tests-"), "config tests require an isolated build directory");
        ExampleModule module = new ExampleModule();
        MyauClient.moduleManager.modules.clear();
        MyauClient.moduleManager.modules.put(ExampleModule.class, module);
        MyauClient.settingManager.settingsByModule.clear();
        MyauClient.settingManager.settingsByModule.put(ExampleModule.class, new ArrayList<>(module.settings()));
        ConfigProfile profile = new ConfigProfile("cleanup-test");
        Path file = Paths.get("config", "Myau", profile.fileName());
        check(new ConfigProfile(null).fileName().equals("predac1.json"), "default profile name remains compatible");
        check(!module.delay.setValue(-1) && !module.delay.setValue(10), "integer setting bounds remain enforced");
        check(!module.range.setValue(Float.NaN) && !module.range.setValue(Float.POSITIVE_INFINITY), "float settings reject invalid numbers");
        module.flag.setValue(false);
        module.delay.setValue(7);
        module.range.setValue(1.5f);
        module.mode.setValue(1);
        module.setKeyCode(42);
        module.setHidden(true);
        module.setEnabled(true);
        check(profile.save(), "config save succeeds");
        JsonObject saved = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject state = saved.getAsJsonObject("CleanupTest");
        check(state.get("mode").getAsString().equals("SECOND"), "config schema stores mode names");
        check(state.get("key").getAsInt() == 42 && state.get("hidden").getAsBoolean(), "config schema preserves key and hidden flags");
        for (Setting<?> setting : module.settings()) setting.reset();
        module.setEnabled(false);
        module.setHidden(false);
        module.setKeyCode(0);
        check(profile.load(), "saved config reloads");
        check(!module.flag.getValue() && module.delay.getValue() == 7 && module.range.getValue() == 1.5f, "setting values survive save and reload");
        check(module.mode.getValue() == 1 && module.isEnabled() && module.isHidden() && module.getKeyCode() == 42, "module state survives save and reload");
        check(module.delayWhenEnabled == 7, "settings load before module enable hooks");
        check("cleanup-test".equals(ConfigProfile.activeProfileName), "successful load records the active profile");
        state.addProperty("delay", 1000);
        state.addProperty("mode", "MISSING");
        state.remove("flag");
        save(file, saved);
        check(profile.load(), "invalid individual settings fall back without rejecting the profile");
        check(module.delay.getValue() == 3 && module.mode.getValue() == 0 && module.flag.getValue(), "invalid and missing settings reset to their defaults");
        check(module.range.getValue() == 1.5f && module.getKeyCode() == 42, "valid settings and keys remain intact");
        state.remove("key");
        state.remove("hidden");
        state.remove("toggled");
        save(file, saved);
        check(profile.load(), "missing module flags retain fallback behavior");
        check(!module.isEnabled() && !module.isHidden() && module.getKeyCode() == 0, "missing flags reset independently");
        module.flag.setValue(false);
        module.delay.setValue(8);
        module.setEnabled(true);
        save(file, new JsonObject());
        check(profile.load(), "profiles without a module still load");
        check(module.flag.getValue() && module.delay.getValue() == 3 && !module.isEnabled(), "missing modules reset settings and state");
        Files.write(file, "{".getBytes(StandardCharsets.UTF_8));
        check(!profile.load(), "malformed JSON returns failure");
        check("cleanup-test".equals(ConfigProfile.activeProfileName), "failed loads preserve the active profile");

        EventBus bus = new EventBus();
        List<String> calls = new ArrayList<>();
        bus.register(new Object() {
            @EventHandler(priority = 10)
            private void first(TestEvent event) { calls.add("first"); event.setCancelled(true); }
        });
        bus.register(new Object() {
            @EventHandler(priority = 10)
            private void second(TestEvent event) { calls.add("second"); check(event.isCancelled(), "cancellation remains visible to later handlers"); }
        });
        bus.register(new Object() {
            @EventHandler(priority = 5)
            private void broken(TestEvent event) { calls.add("broken"); throw new IllegalStateException("fixture failure"); }
        });
        bus.register(new Object() {
            @EventHandler(priority = -10)
            private void last(TestEvent event) { calls.add("last"); }
            @EventHandler
            private void invalid() { throw new AssertionError("invalid handler called"); }
            private void unannotated(TestEvent event) { throw new AssertionError("unannotated handler called"); }
        });
        TestEvent event = new TestEvent();
        check(bus.post(event) == event, "event dispatch returns the same event");
        check(calls.equals(Arrays.asList("first", "second", "broken", "last")), "priority, equal-priority ordering, and exception isolation remain intact");
        calls.clear();
        bus.post(new ChildEvent());
        check(calls.isEmpty(), "dispatch continues to match exact event classes");
        IOException original = new IOException("fixture exception");
        try {
            throw SourceExceptions.propagate(original);
        } catch (Throwable failure) {
            check(failure == original, "checked exception propagation preserves the original instance");
        }
        System.out.println("Codebase regression checks passed: " + checks);
    }
}
