package myau;

import java.lang.reflect.Field;
import java.util.ArrayList;
import myau.AimAssistModule;
import myau.AntiDebuffModule;
import myau.AntiFireballModule;
import myau.AntiObbyTrapModule;
import myau.AntiObfuscateModule;
import myau.AntiVoidModule;
import myau.AutoClickerModule;
import myau.AutoHealModule;
import myau.AutoToolModule;
import myau.BacktrackModule;
import myau.BedESPModule;
import myau.BedNukerModule;
import myau.BedTrackerModule;
import myau.BindCommand;
import myau.BlinkModule;
import myau.ChamsModule;
import myau.ChestESPModule;
import myau.ChestStealerModule;
import myau.ClickGuiModule;
import myau.CommandManager;
import myau.ConfigCommand;
import myau.DenickCommand;
import myau.DisplaceModule;
import myau.ESPModule;
import myau.EagleModule;
import myau.EnemyCommand;
import myau.EnemyList;
import myau.EventBus;
import myau.FastPlaceModule;
import myau.FlyModule;
import myau.FreelookModule;
import myau.FriendCommand;
import myau.FriendList;
import myau.FullbrightModule;
import myau.GhostHandModule;
import myau.GroundSpoofManager;
import myau.HUDModule;
import myau.HelpCommand;
import myau.HideCommand;
import myau.HitSelectModule;
import myau.HurtTimeTracker;
import myau.IncomingPacketBuffer;
import myau.IndicatorsModule;
import myau.InvManagerModule;
import myau.InvWalkModule;
import myau.ItemESPModule;
import myau.ItemNameCommand;
import myau.JesusModule;
import myau.KeepSprintModule;
import myau.KillAuraModule;
import myau.LagRangeModule;
import myau.LightningTrackerModule;
import myau.LongJumpModule;
import myau.MCFModule;
import myau.Module;
import myau.ModuleListCommand;
import myau.ModuleManager;
import myau.ModuleSettingsCommand;
import myau.NameTagsModule;
import myau.NickHiderModule;
import myau.NoFallModule;
import myau.NoHitDelayModule;
import myau.NoHurtCamModule;
import myau.NoJumpDelayModule;
import myau.NoRotateModule;
import myau.NoSlowModule;
import myau.OutgoingPacketBuffer;
import myau.PacketActionTracker;
import myau.PacketDelayManager;
import myau.PlayerListCommand;
import myau.ReachModule;
import myau.RotationSmoother;
import myau.SafeWalkModule;
import myau.ScaffoldModule;
import myau.Setting;
import myau.SettingManager;
import myau.ShowCommand;
import myau.SpammerModule;
import myau.SpeedMineModule;
import myau.SpeedModule;
import myau.SprintModule;
import myau.TargetHUDModule;
import myau.TargetStrafeModule;
import myau.TimerModule;
import myau.ToggleCommand;
import myau.TracersModule;
import myau.TrajectoriesModule;
import myau.UsernameCommand;
import myau.VClipCommand;
import myau.VelocityModule;
import myau.ViewClipModule;
import myau.WTapModule;
import myau.XrayModule;

public final class MyauClient {
    public static final String CHAT_PREFIX = "&7[&bMyau&7] &r";
    public static EventBus eventBus;
    public static RotationSmoother rotationSmoother;
    public static GroundSpoofManager groundSpoofManager;
    public static HurtTimeTracker hurtTimeTracker;
    public static OutgoingPacketBuffer outgoingPacketBuffer;
    public static IncomingPacketBuffer incomingPacketBuffer;
    public static PacketDelayManager packetDelayManager;
    public static PacketActionTracker packetActionTracker;
    public static FriendList friendList;
    public static EnemyList enemyList;
    public static SettingManager settingManager;
    public static ModuleManager moduleManager;
    public static CommandManager commandManager;
    private static boolean initialized;

    private MyauClient() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        eventBus = new EventBus();
        rotationSmoother = new RotationSmoother();
        groundSpoofManager = new GroundSpoofManager();
        hurtTimeTracker = new HurtTimeTracker();
        outgoingPacketBuffer = new OutgoingPacketBuffer();
        incomingPacketBuffer = new IncomingPacketBuffer();
        packetDelayManager = new PacketDelayManager();
        packetActionTracker = new PacketActionTracker();
        friendList = new FriendList();
        enemyList = new EnemyList();
        settingManager = new SettingManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        MyauClient.registerInfrastructureListeners();
        MyauClient.registerModules();
        MyauClient.registerCommands();
        friendList.load();
        enemyList.load();
        initialized = true;
    }

    private static void registerInfrastructureListeners() {
        eventBus.register(rotationSmoother);
        eventBus.register(groundSpoofManager);
        eventBus.register(hurtTimeTracker);
        eventBus.register(outgoingPacketBuffer);
        eventBus.register(incomingPacketBuffer);
        eventBus.register(packetDelayManager);
        eventBus.register(commandManager);
    }

    private static void registerModules() {
        MyauClient.registerModule(new AimAssistModule());
        MyauClient.registerModule(new AntiDebuffModule());
        MyauClient.registerModule(new AntiFireballModule());
        MyauClient.registerModule(new AntiObbyTrapModule());
        MyauClient.registerModule(new AntiObfuscateModule());
        MyauClient.registerModule(new AntiVoidModule());
        MyauClient.registerModule(new AutoClickerModule());
        MyauClient.registerModule(new AutoHealModule());
        MyauClient.registerModule(new AutoToolModule());
        MyauClient.registerModule(new BedESPModule());
        MyauClient.registerModule(new BedNukerModule());
        MyauClient.registerModule(new BacktrackModule());
        MyauClient.registerModule(new BedTrackerModule());
        MyauClient.registerModule(new BlinkModule());
        MyauClient.registerModule(new ChamsModule());
        MyauClient.registerModule(new ClickGuiModule());
        MyauClient.registerModule(new ChestESPModule());
        MyauClient.registerModule(new ChestStealerModule());
        MyauClient.registerModule(new DisplaceModule());
        MyauClient.registerModule(new EagleModule());
        MyauClient.registerModule(new ESPModule());
        MyauClient.registerModule(new FastPlaceModule());
        MyauClient.registerModule(new FlyModule());
        MyauClient.registerModule(new FreelookModule());
        MyauClient.registerModule(new FullbrightModule());
        MyauClient.registerModule(new GhostHandModule());
        MyauClient.registerModule(new HitSelectModule());
        MyauClient.registerModule(new HUDModule());
        MyauClient.registerModule(new IndicatorsModule());
        MyauClient.registerModule(new InvManagerModule());
        MyauClient.registerModule(new InvWalkModule());
        MyauClient.registerModule(new ItemESPModule());
        MyauClient.registerModule(new JesusModule());
        MyauClient.registerModule(new KeepSprintModule());
        MyauClient.registerModule(new KillAuraModule());
        MyauClient.registerModule(new LagRangeModule());
        MyauClient.registerModule(new LightningTrackerModule());
        MyauClient.registerModule(new LongJumpModule());
        MyauClient.registerModule(new MCFModule());
        MyauClient.registerModule(new NameTagsModule());
        MyauClient.registerModule(new NickHiderModule());
        MyauClient.registerModule(new NoFallModule());
        MyauClient.registerModule(new NoHitDelayModule());
        MyauClient.registerModule(new NoHurtCamModule());
        MyauClient.registerModule(new NoJumpDelayModule());
        MyauClient.registerModule(new NoRotateModule());
        MyauClient.registerModule(new NoSlowModule());
        MyauClient.registerModule(new ReachModule());
        MyauClient.registerModule(new SafeWalkModule());
        MyauClient.registerModule(new ScaffoldModule());
        MyauClient.registerModule(new SpammerModule());
        MyauClient.registerModule(new SpeedModule());
        MyauClient.registerModule(new SpeedMineModule());
        MyauClient.registerModule(new SprintModule());
        MyauClient.registerModule(new TargetHUDModule());
        MyauClient.registerModule(new TargetStrafeModule());
        MyauClient.registerModule(new TimerModule());
        MyauClient.registerModule(new TracersModule());
        MyauClient.registerModule(new TrajectoriesModule());
        MyauClient.registerModule(new VelocityModule());
        MyauClient.registerModule(new ViewClipModule());
        MyauClient.registerModule(new WTapModule());
        MyauClient.registerModule(new XrayModule());
        try {
            MyauClient.registerModule((Module)Class.forName("myau.BpsModule").newInstance());
        }
        catch (ReflectiveOperationException reflectiveOperationException) {
            // empty catch block
        }
    }

    private static void registerModule(Module module) {
        MyauClient.moduleManager.modules.put(module.getClass(), module);
        eventBus.register(module);
        ArrayList<Setting> arrayList = new ArrayList<Setting>();
        for (Field field : module.getClass().getFields()) {
            if (!Setting.class.isAssignableFrom(field.getType())) continue;
            try {
                Setting setting = (Setting)field.get(module);
                if (setting == null) continue;
                setting.setModule(module);
                arrayList.add(setting);
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new IllegalStateException("Unable to register setting " + field.getName(), illegalAccessException);
            }
        }
        MyauClient.settingManager.settingsByModule.put(module.getClass(), arrayList);
    }

    private static void registerCommands() {
        MyauClient.commandManager.commands.add(new BindCommand());
        MyauClient.commandManager.commands.add(new ConfigCommand());
        MyauClient.commandManager.commands.add(new DenickCommand());
        MyauClient.commandManager.commands.add(new EnemyCommand());
        MyauClient.commandManager.commands.add(new FriendCommand());
        MyauClient.commandManager.commands.add(new HelpCommand());
        MyauClient.commandManager.commands.add(new HideCommand());
        MyauClient.commandManager.commands.add(new ItemNameCommand());
        MyauClient.commandManager.commands.add(new ModuleListCommand());
        MyauClient.commandManager.commands.add(new ModuleSettingsCommand());
        MyauClient.commandManager.commands.add(new PlayerListCommand());
        MyauClient.commandManager.commands.add(new ShowCommand());
        MyauClient.commandManager.commands.add(new ToggleCommand());
        MyauClient.commandManager.commands.add(new UsernameCommand());
        MyauClient.commandManager.commands.add(new VClipCommand());
    }
}
