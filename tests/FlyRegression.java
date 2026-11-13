package myau;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import myau.mixin.AccessorC03PacketPlayer;
import myau.mixin.AccessorEntity;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import sun.misc.Unsafe;

public final class FlyRegression {
    private static Unsafe unsafe;

    private static class Player extends EntityPlayerSP implements AccessorEntity {
        boolean web, ladder;
        Player() { super(null, null, null, null); }
        public boolean getIsInWeb() { return web; }
        public Vec3 callGetVectorForRotation(float pitch, float yaw) { return null; }
        public boolean func_70617_f_() { return ladder; }
        public boolean func_70644_a(Potion potion) { return false; }
        public boolean func_70051_ag() { return false; }
        public AxisAlignedBB func_174813_aQ() { return new AxisAlignedBB(0, field_70163_u, 0, 1, field_70163_u + 2, 1); }
        public void func_70107_b(double x, double y, double z) { field_70165_t = x; field_70163_u = y; field_70161_v = z; }
    }

    private static class World extends WorldClient {
        boolean liquid;
        World() { super(null, null, 0, null, null); }
        public boolean func_72953_d(AxisAlignedBB bounds) { return liquid; }
    }

    private static class Position extends C03PacketPlayer.C04PacketPlayerPosition implements AccessorC03PacketPlayer {
        Position(boolean ground) { super(0, 1, 0, ground); }
        public void setOnGround(boolean ground) { field_149474_g = ground; }
        public void setY(double y) { field_149477_b = y; }
    }

    private static class Controller extends PlayerControllerMP implements AccessorPlayerControllerMP {
        int slot;
        Controller() { super(null, null); }
        public float getCurBlockDamageMP() { return 0; }
        public void setCurBlockDamageMP(float value) { }
        public int getBlockHitDelay() { return 0; }
        public void setBlockHitDelay(int value) { }
        public boolean getIsHittingBlock() { return false; }
        public int getCurrentPlayerItem() { return slot; }
        public void setCurrentPlayerItem(int value) { slot = value; }
        public void callSyncCurrentPlayItem() { }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-7, message + ": " + actual);
    }

    private static void set(FlyModule fly, String name, Object value) throws Exception {
        Field field = FlyModule.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(fly, value);
    }

    private static Object get(FlyModule fly, String name) throws Exception {
        Field field = FlyModule.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(fly);
    }

    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        unsafe = (Unsafe)access.get(null);
        Minecraft game = Minecraft.func_71410_x();
        Player player = (Player)unsafe.allocateInstance(Player.class);
        game.field_71439_g = player;
        World world = (World)unsafe.allocateInstance(World.class);
        game.field_71441_e = world;
        game.field_71474_y = (GameSettings)unsafe.allocateInstance(GameSettings.class);
        GameSettings settings = game.field_71474_y;
        settings.field_74314_A = new KeyBinding("jump", 57, "test");
        settings.field_74311_E = new KeyBinding("sneak", 42, "test");
        settings.field_74351_w = new KeyBinding("forward", 17, "test");
        settings.field_74368_y = new KeyBinding("back", 31, "test");
        settings.field_74370_x = new KeyBinding("left", 30, "test");
        settings.field_74366_z = new KeyBinding("right", 32, "test");
        Timer clock = (Timer)unsafe.allocateInstance(Timer.class);
        clock.field_74278_d = 1.0f;
        game.setTimer(clock);
        player.field_71158_b = new MovementInput();
        player.field_71071_by = new InventoryPlayer(player);
        player.field_71075_bZ = new PlayerCapabilities();
        game.field_71442_b = (Controller)unsafe.allocateInstance(Controller.class);
        FlyModule fly = new FlyModule();
        for (Field field : FlyModule.class.getFields()) {
            if (Setting.class.isAssignableFrom(field.getType())) ((Setting<?>)field.get(fly)).setModule(fly);
        }
        check(fly.getName().equals("Fly") && !fly.isEnabled(), "existing module identity and disabled default");
        check(fly.mode.getValue() == 1 && fly.oldNcpMode.getValue() == 1 && fly.speed.getValue() == 4.25f, "Ivory defaults");
        check(!fly.decay.setValue(19) && !fly.activationTimeV.setValue(61), "source setting bounds");
        fly.mode.setValue(0);
        fly.setEnabled(true);
        InputUtils.keys.add(57);
        KeyBinding.func_74510_a(17, true);
        fly.onRotationUpdate(new RotationUpdateEvent(0, 0, 0, 0));
        MoveFlyingEvent move = new MoveFlyingEvent(0, 1, 0.02f);
        fly.onMoveFlying(move);
        near(player.field_70181_x, (double)0.42f, "Vanilla vertical motion");
        near(move.getFriction(), 0.28015f, "Vanilla horizontal acceleration");
        fly.setEnabled(false);
        InputUtils.keys.clear();
        fly.mode.setValue(1);
        fly.oldNcpMode.setValue(0);
        fly.timerBoost.setValue(true);
        clock.field_74278_d = 1.25f;
        fly.setEnabled(true);
        near(clock.field_74278_d, 2, "Slow timer boost");
        fly.onMoveFlying(move);
        near(MovementUtils.horizontalSpeed(), 0.27, "Slow base speed");
        player.web = true;
        fly.onMoveFlying(move);
        near(MovementUtils.horizontalSpeed(), 0.27 * 0.4072398190045249, "web multiplier");
        player.web = false;
        Position packet = new Position(false);
        player.field_70173_aa = 20;
        fly.onPacketSend(new PacketSendEvent(packet));
        check(packet.func_149465_i() && packet.func_149467_d() > 1.00001, "even packet offset and ground");
        packet = new Position(true);
        player.field_70173_aa = 21;
        fly.onPacketSend(new PacketSendEvent(packet));
        check(!packet.func_149465_i() && packet.func_149467_d() >= 1 && packet.func_149467_d() < 1.00001, "odd packet offset and ground");
        set(fly, "boostStartedAt", System.currentTimeMillis() - 2000);
        fly.onPreTick(new PreTickEvent());
        near(clock.field_74278_d, 1.25, "expired timer restores previous speed");
        fly.setEnabled(false);
        fly.setEnabled(true);
        TimerModule timer = new TimerModule();
        MyauClient.moduleManager.modules.put(TimerModule.class, timer);
        timer.setEnabled(true);
        clock.field_74278_d = 1.6f;
        fly.onPreTick(new PreTickEvent());
        near(clock.field_74278_d, 1.6f, "Timer module retains ownership");
        timer.setEnabled(false);
        fly.oldNcpMode.setValue(1);
        player.field_70122_E = true;
        fly.setEnabled(false);
        PacketUtils.sent.clear();
        fly.setEnabled(true);
        check(PacketUtils.sent.size() == 123, "Instant damage packet sequence");
        check(((C03PacketPlayer)PacketUtils.sent.get(122)).func_149465_i(), "damage setup keeps its final ground flag");
        player.field_70172_ad = 19;
        fly.onMoveFlying(move);
        near(MovementUtils.horizontalSpeed(), (double)0.223f * 4.25f, "acknowledged damage launch");
        set(fly, "airTicks", 5);
        check(!fly.getPacketGround(true), "damage ground flag before tick six");
        set(fly, "airTicks", 6);
        check(fly.getPacketGround(false), "damage ground flag on tick six");
        PacketReceiveEvent correction = new PacketReceiveEvent(new S08PacketPlayerPosLook());
        correction.setCancelled(true);
        fly.onPacketReceive(correction);
        check(fly.isEnabled(), "cancelled corrections are ignored");
        correction.setCancelled(false);
        fly.onPacketReceive(correction);
        check(!fly.isEnabled() && !correction.isCancelled(), "OldNCP disables and permits correction");
        fly.damageMode.setValue(1);
        fly.setEnabled(true);
        check(!fly.isEnabled(), "missing bow disables safely");
        player.field_71071_by.field_70461_c = 3;
        player.field_71071_by.field_70462_a[2] = new ItemStack(new ItemBow());
        player.field_71075_bZ.field_75098_d = true;
        fly.setEnabled(true);
        check(fly.isEnabled() && player.field_71071_by.field_70461_c == 2, "bow selects hotbar slot");
        for (int tick = 0; tick < 5; ++tick) fly.onPreTick(new PreTickEvent());
        check(player.field_71071_by.field_70461_c == 3 && (Integer)get(fly, "bowStage") == 3, "bow release restores slot after four draw ticks");
        fly.onPacketReceive(new PacketReceiveEvent(new S12PacketEntityVelocity(1, 0, 0, 0)));
        check(!(Boolean)get(fly, "receivedVelocity"), "other players' velocity does not acknowledge bow damage");
        fly.onPacketReceive(new PacketReceiveEvent(new S12PacketEntityVelocity(player.func_145782_y(), 0, 0, 0)));
        check((Boolean)get(fly, "receivedVelocity"), "local velocity acknowledges bow damage");
        fly.setEnabled(false);
        fly.setEnabled(true);
        for (int tick = 0; tick < 35; ++tick) fly.onPreTick(new PreTickEvent());
        check(!fly.isEnabled() && player.field_71071_by.field_70461_c == 3, "bow timeout disables and restores slot");
        Method decay = FlyModule.class.getDeclaredMethod("updateDecay", double.class, double.class);
        decay.setAccessible(true);
        fly.decay.setValue(20);
        clock.field_74278_d = 1;
        double value = 2;
        for (int tick = 0; tick < 90; ++tick) value = (Double)decay.invoke(fly, value, 0.223);
        near(value, (Double)get(fly, "baselineSpeed"), "decay converges at ninety steps");
        set(fly, "decayAccumulator", 0.0);
        set(fly, "decayTicks", 0);
        clock.field_74278_d = 2;
        decay.invoke(fly, 2.0, 0.223);
        check((Integer)get(fly, "decayTicks") == 0, "decay respects inverse timer speed");
        fly.flightMode.setValue(1);
        fly.setEnabled(true);
        player.field_70181_x = 0.8;
        fly.onLivingUpdate(new LivingUpdateEvent());
        near(player.field_70181_x, 0.8, "Rewound B first sample");
        player.field_70181_x = 0.9;
        player.field_70177_z = 90;
        fly.onLivingUpdate(new LivingUpdateEvent());
        near(player.field_70181_x, 2.54, "Rewound B rising trigger");
        near(player.field_70159_w, -1.63, "Rewound B view yaw");
        for (int tick = 1; tick < 45; ++tick) {
            player.field_70181_x = 2.41;
            fly.onLivingUpdate(new LivingUpdateEvent());
            near(player.field_70181_x, 2.54, "Rewound B configured vertical window");
        }
        player.field_70181_x = 2.41;
        fly.onLivingUpdate(new LivingUpdateEvent());
        near(player.field_70181_x, 2.41, "Rewound B expires after forty-five updates");
        fly.onWorldLoad(new WorldLoadEvent());
        check(!(Boolean)get(fly, "hasPreviousMotion"), "world change resets motion history");
        fly.setEnabled(false);
        near(player.field_70181_x, 2.41, "Rewound B disable preserves motion");
        System.out.println("Fly regression checks passed");
    }
}
