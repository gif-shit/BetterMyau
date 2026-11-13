package myau;

import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import myau.openb4.ModuleEvents;
import myau.openb4.module.dev.DebugAcModule;
import myau.openb4.module.dev.TestModule;
import myau.openb4.module.dev.ViewPacketsModule;
import myau.openb4.module.minigames.BedWarsModule;
import myau.openb4.module.minigames.SkyWarsModule;
import myau.openb4.module.player.AutoBlockinModule;
import myau.openb4.module.player.AutoLadderModule;
import myau.openb4.module.render.DamageTagsModule;
import myau.openb4.module.world.WeatherModule;
import myau.openb4.util.Executor;
import myau.render.raven.RavenHudState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import sun.misc.Unsafe;

public final class Update60Regression {
    private static int checks;
    private static Unsafe unsafe;

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.001, message + ": " + actual);
    }

    private static void enabled(Module module, boolean value) throws Exception {
        Field field = Module.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.setBoolean(module, value);
    }

    private static final class FakeWorld extends WorldClient {
        long time;
        WorldInfo info;
        FakeWorld() { super(null, null, 0, null, null); }
        public long func_72820_D() { return time; }
        public void func_72877_b(long value) { time = value; }
        public WorldInfo func_72912_H() { return info; }
        public boolean func_72896_J() { return info.func_76059_o(); }
    }

    private static final class Mob extends EntityMob {
        Mob() { super(null); }
        public float func_110139_bj() { return 0; }
        public float func_70047_e() { return 1.6f; }
        public int func_145782_y() { return 27; }
    }

    private static final class Actions extends TestModule {
        int starts, stops;
        Actions() { super(0); }
        public void onEnable() { ++starts; }
        public void onDisable() { ++stops; }
    }

    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        unsafe = (Unsafe) access.get(null);
        Field registered = Bootstrap.class.getDeclaredField("field_151355_a");
        registered.setAccessible(true);
        registered.setBoolean(null, true);
        net.minecraft.block.Block.func_149671_p();
        net.minecraft.item.Item.func_150900_l();
        colors();
        raven();
        bedTracker();
        modules();
        weatherAndDamage();
        System.out.println("Update 6.0 regression checks passed: " + checks);
    }

    private static void colors() {
        ColorWheel wheel = new ColorWheel();
        wheel.sync(0xff0000);
        check(wheel.select(30, 0, 30) == 0xff0000, "wheel selects red at zero degrees");
        check(wheel.select(0, 0, 30) == 0xffffff, "wheel center selects white");
        check(wheel.select(60, 0, 30) == 0xff0000, "wheel clamps dragging beyond its edge");
        near(wheel.saturation, 1, "saturation stays within range");
        check(wheel.brightness(-1) == 0, "brightness clamps to black");
        wheel.select(-15, (float) (Math.sqrt(3) * 15), 30);
        wheel.sync(0);
        check(wheel.brightness(1) == 0x00ff00, "black retains the selected hue when brightness returns");
        wheel.sync(0x808080);
        near(wheel.saturation, 0, "external grey color clears saturation");
        check(wheel.brightness(2) == 0xffffff, "brightness clamps to white for grey");
        FloatSetting stepped = new FloatSetting("step", 0f, 0f, 24f, 0.3f);
        check(stepped.setValue(1f), "stepped slider accepts values in range");
        near(stepped.getValue(), 0.9, "slider preserves its original increment");
        check(!stepped.setValue(Float.NaN) && !stepped.setValue(Float.POSITIVE_INFINITY), "slider rejects nonfinite values");
    }

    private static void raven() {
        RavenHudState hud = new RavenHudState();
        Object target = new Object();
        hud.update(target, "Target", 20, 20, 1000, true);
        near(hud.shown(1000), 1, "new Raven target starts at its own health");
        hud.update(target, "Target", 10, 20, 1100, true);
        near(hud.front(1100), 0.5, "damage immediately updates the front bar");
        near(hud.shown(1100), 1, "damage keeps a trailing bar");
        near(hud.shown(1500), 0.515625, "damage animation uses the original quintic easing");
        near(hud.shown(1900), 0.5, "damage animation completes after eight hundred milliseconds");
        check(hud.alpha(1350) == 255 && hud.alpha(1550) == 128 && hud.alpha(1750) == 0, "Raven holds then fades for four hundred milliseconds");
        String text = hud.text(20, 20, true);
        check(text.contains("+10") && text.endsWith("W"), "Raven shows health difference and win indicator");
        check(!hud.text(20, 20, false).contains("+"), "Raven respects the indicator setting");
        hud.update(new Object(), "Other", 3, 0, 2000, false);
        near(hud.shown(2000), 0, "zero maximum health remains finite");
        hud.reset();
        check(hud.alpha(2000) == 0, "Raven reset clears stale target display");
    }

    private static void bedTracker() {
        check(BedTrackerHud.status(false, false, false, -1, 48).equals("searching"), "untracked bed shows searching");
        check(BedTrackerHud.status(true, false, false, -1, 48).equals("OUT OF RANGE"), "unloaded bed is not reported destroyed");
        check(BedTrackerHud.status(true, true, false, -1, 48).equals("BED LOST"), "destroyed bed has its own status");
        check(BedTrackerHud.status(true, true, true, 20, 48).equals("THREAT NEARBY"), "nearby threat changes bed status");
        check(BedTrackerHud.status(true, true, true, 80, 48).equals("Secure"), "distant enemy does not trigger danger status");
        check(BedTrackerHud.searching(0).equals("searching"), "searching starts lowercase without dots");
        check(BedTrackerHud.searching(400).equals("searching."), "searching animation advances by elapsed time");
        check(BedTrackerHud.searching(800).equals("searching.."), "searching animation has a second dot");
        check(BedTrackerHud.searching(1200).equals("searching..."), "searching animation has a third dot");
        check(BedTrackerHud.searching(1600).equals("searching"), "searching animation loops");
        check(BedTrackerHud.height(false) == 22 && BedTrackerHud.height(true) == 68, "searching uses only the compact status pill");
        near(BedTrackerHud.meter(-1, 48), 0, "absent enemies leave danger meter empty");
        near(BedTrackerHud.meter(12, 48), 0.75, "danger meter reflects proximity to bed");
        near(BedTrackerHud.anchor(2, 600, 200, 4), 395, "right HUD anchoring includes the whole card");
        JsonObject config = new JsonObject(), legacy = new JsonObject();
        legacy.addProperty("toggled", true);
        legacy.addProperty("key", 29);
        legacy.addProperty("alerts-range", 64);
        legacy.addProperty("macro-text", "/lobby");
        config.add("BedTracker", legacy);
        ConfigProfile.migrateBedTracker(config);
        JsonObject migrated = config.getAsJsonObject("BedWars");
        check(migrated.get("toggled").getAsBoolean() && migrated.get("bed-tracker").getAsBoolean(), "migration preserves an active BedTracker");
        check(migrated.get("key").getAsInt() == 29 && migrated.get("alerts-range").getAsInt() == 64, "migration keeps bindings and tracker preferences");
        migrated.addProperty("key", 33);
        ConfigProfile.migrateBedTracker(config);
        check(migrated.get("key").getAsInt() == 33, "legacy tracker cannot overwrite an existing BedWars binding");
    }

    private static void modules() throws Exception {
        Module[] modules = {new DamageTagsModule(0), new DebugAcModule(0), new ViewPacketsModule(0), new TestModule(0),
                new BedWarsModule(0), new SkyWarsModule(0), new AutoBlockinModule(0), new AutoLadderModule(0), new WeatherModule(0)};
        for (Module module : modules) {
            check(!module.isEnabled(), module.getName() + " defaults off");
            ((myau.openb4.module.Module) module).link();
            ((myau.openb4.module.Module) module).guiUpdate();
            ModuleCategory expected = module instanceof AutoBlockinModule ? ModuleCategory.PLAYER
                    : module instanceof BedWarsModule || module instanceof SkyWarsModule || module instanceof AutoLadderModule || module instanceof WeatherModule || module instanceof DebugAcModule || module instanceof ViewPacketsModule || module instanceof TestModule ? ModuleCategory.MISC : ModuleCategory.RENDER;
            check(ModuleCategory.of(module) == expected, module.getName() + " appears in its requested category");
        }
        BedWarsModule bedwars = (BedWarsModule) modules[4];
        check(!bedwars.bedGuard.isEnabled(), "tracker follows the disabled BedWars module");
        enabled(bedwars, true);
        check(bedwars.bedGuard.isEnabled(), "BedWars owns tracker activation");
        net.minecraft.util.BlockPos bed = new net.minecraft.util.BlockPos(1, 2, 3);
        bedwars.bedGuard.trackBed(bed);
        check(bed.equals(BedWarsModule.ownBed), "tracker and whitelist share the same bed");
        bedwars.bedTracker.setValue(false);
        check(!bedwars.bedGuard.isEnabled() && !bedwars.bedGuard.hud.isVisible(), "tracker switch controls behavior and setting visibility");
        MyauClient.moduleManager.modules.clear();
        check(ModuleCategory.visibleCategories().length == ModuleCategory.values().length - 1, "Misc is hidden by default");
        ClickGuiModule gui = new ClickGuiModule();
        MyauClient.moduleManager.modules.put(ClickGuiModule.class, gui);
        gui.showMisc.setValue(true);
        check(ModuleCategory.visibleCategories().length == ModuleCategory.values().length, "ClickGUI setting reveals Misc");
        Actions actions = new Actions();
        enabled(actions, true);
        MyauClient.moduleManager.modules.clear();
        MyauClient.moduleManager.modules.put(Actions.class, actions);
        new ModuleEvents().onWorldLoad(new WorldLoadEvent());
        check(actions.starts == 0 && actions.stops == 1, "world changes never rerun Test actions");
        final int[] calls = {0};
        Executor.later(() -> ++calls[0], 0);
        Executor.tick(); Executor.tick();
        check(calls[0] == 1, "scheduled game-thread action runs once");
        Executor.later(() -> ++calls[0], 0);
        Executor.clear(); Executor.tick();
        check(calls[0] == 1, "world-change cleanup drops queued actions");
    }

    private static void weatherAndDamage() throws Exception {
        Minecraft mc = Minecraft.func_71410_x();
        FakeWorld world = (FakeWorld) unsafe.allocateInstance(FakeWorld.class);
        world.info = (WorldInfo) unsafe.allocateInstance(WorldInfo.class);
        world.time = 7000;
        Field entities = World.class.getDeclaredField("field_72996_f");
        entities.setAccessible(true);
        entities.set(world, new ArrayList<Entity>());
        mc.field_71441_e = world;
        mc.field_71439_g = (EntityPlayerSP) unsafe.allocateInstance(EntityPlayerSP.class);
        WeatherModule weather = new WeatherModule(0);
        enabled(weather, true);
        WeatherModule.time.setValue(6f);
        WeatherModule.rain.setValue(true);
        weather.onUpdate();
        check(world.time == -22000 && world.info.func_76059_o(), "Weather changes time and rain without an integrated server");
        check(weather.onReceivePacket(new S08PacketPlayerPosLook(), null), "Weather passes unrelated packets safely");
        check(!weather.onReceivePacket(new S03PacketTimeUpdate(0, 9000, true), null), "custom time intercepts only time updates");
        MyauClient.moduleManager.modules.clear();
        MyauClient.moduleManager.modules.put(WeatherModule.class, weather);
        PacketReceiveEvent timeUpdate = new PacketReceiveEvent(new S03PacketTimeUpdate(0, 12000, true));
        new ModuleEvents().onPacketReceive(timeUpdate);
        check(timeUpdate.isCancelled(), "weather packet filtering is connected to the host event bus");
        enabled(weather, false);
        weather.afterDisable();
        check(world.time == 12000 && !world.info.func_76059_o(), "disabling Weather restores the latest server time and original rain");
        DamageTagsModule damage = new DamageTagsModule(0);
        damage.link(); damage.guiUpdate();
        myau.openb4.client.Hypixel.state = myau.openb4.client.Hypixel.State.NOT_HYPIXEL;
        Mob mob = (Mob) unsafe.allocateInstance(Mob.class);
        net.minecraft.entity.DataWatcher watcher = new net.minecraft.entity.DataWatcher(mob);
        watcher.func_75682_a(6, 20f);
        Field data = Entity.class.getDeclaredField("field_70180_af");
        data.setAccessible(true);
        data.set(mob, watcher);
        world.field_72996_f.add(mob);
        damage.onUpdate();
        Field tags = DamageTagsModule.class.getDeclaredField("tags");
        tags.setAccessible(true);
        check(((List<?>) tags.get(damage)).isEmpty(), "first health observation creates no false damage tag");
        watcher.func_75692_b(6, 14f);
        damage.onUpdate();
        check(((List<?>) tags.get(damage)).size() == 1, "health loss creates one damage tag");
        damage.onUpdate();
        check(((List<?>) tags.get(damage)).size() == 1, "unchanged health does not duplicate tags");
        watcher.func_75692_b(6, 17f);
        damage.onUpdate();
        check(((List<?>) tags.get(damage)).size() == 2, "health gain creates a heal tag");
        world.field_72996_f.clear();
        damage.onUpdate();
        world.field_72996_f.add(mob);
        watcher.func_75692_b(6, 20f);
        damage.onUpdate();
        check(((List<?>) tags.get(damage)).size() == 2, "returning entities do not inherit stale health changes");
        damage.onDisable();
        check(((List<?>) tags.get(damage)).isEmpty(), "disabling damage tags clears world state");
        mc.field_71441_e = null; mc.field_71439_g = null;
        AutoLadderModule.reset();
    }
}
