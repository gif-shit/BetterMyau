package myau;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import myau.mixin.AccessorEntity;
import myau.mixin.AccessorPlayerControllerMP;
import myau.mixin.MixinEntityPlayerSP;
import myau.modules.Scaffold;
import myau.modules.scaffold.ScaffoldMinecraft;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sun.misc.Unsafe;

public final class ScaffoldIntegrationRegression {
    private static Unsafe unsafe;
    private static int checks;

    private static class Player extends EntityPlayerSP implements AccessorEntity {
        boolean sprint;
        Player() { super(null, null, null, null); }
        public boolean getIsInWeb() { return false; }
        public Vec3 callGetVectorForRotation(float pitch, float yaw) {
            double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
            return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        }
        public float func_70047_e() { return 1.62f; }
        public Vec3 func_174824_e(float partial) { return new Vec3(field_70165_t, field_70163_u + func_70047_e(), field_70161_v); }
        public AxisAlignedBB func_174813_aQ() { return new AxisAlignedBB(field_70165_t - 0.3, field_70163_u, field_70161_v - 0.3, field_70165_t + 0.3, field_70163_u + 1.8, field_70161_v + 0.3); }
        public void func_70031_b(boolean value) { sprint = value; }
        public void func_71038_i() { }
    }

    private static class World extends WorldClient {
        Map<BlockPos, IBlockState> blocks;
        World() { super(null, null, 0, null, null); }
        public boolean func_175667_e(BlockPos pos) { return true; }
        public IBlockState func_180495_p(BlockPos pos) { return blocks.containsKey(pos) ? blocks.get(pos) : Blocks.field_150350_a.func_176223_P(); }
        public MovingObjectPosition func_72933_a(Vec3 from, Vec3 to) { return func_147447_a(from, to, false, false, false); }
        public MovingObjectPosition func_147447_a(Vec3 from, Vec3 to, boolean liquid, boolean ignore, boolean last) {
            MovingObjectPosition nearest = null;
            for (BlockPos pos : blocks.keySet()) {
                AxisAlignedBB bounds = new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1, pos.func_177956_o() + 1, pos.func_177952_p() + 1);
                MovingObjectPosition hit = bounds.func_72327_a(from, to);
                if (hit != null && (nearest == null || from.func_72438_d(hit.field_72307_f) < from.func_72438_d(nearest.field_72307_f))) nearest = new MovingObjectPosition(hit.field_72307_f, hit.field_178784_b, pos);
            }
            return nearest;
        }
    }

    private static class Controller extends PlayerControllerMP implements AccessorPlayerControllerMP {
        int slot, syncs, placed, observedSlot;
        float observedYaw, observedPitch;
        boolean fail;
        Controller() { super(null, null); }
        public float func_78757_d() { return 4.5f; }
        public boolean func_178890_a(EntityPlayerSP player, WorldClient world, ItemStack stack, BlockPos pos, EnumFacing face, Vec3 hit) {
            if (fail) throw new IllegalStateException("controller failure");
            observedSlot = player.field_71071_by.field_70461_c;
            observedYaw = player.field_70177_z;
            observedPitch = player.field_70125_A;
            ((World)world).blocks.put(pos.func_177972_a(face), Blocks.field_150348_b.func_176223_P());
            --stack.field_77994_a;
            ++placed;
            return true;
        }
        public float getCurBlockDamageMP() { return 0; }
        public void setCurBlockDamageMP(float value) { }
        public int getBlockHitDelay() { return 0; }
        public void setBlockHitDelay(int value) { }
        public boolean getIsHittingBlock() { return false; }
        public int getCurrentPlayerItem() { return slot; }
        public void setCurrentPlayerItem(int value) { slot = value; }
        public void callSyncCurrentPlayItem() {
            int current = Minecraft.func_71410_x().field_71439_g.field_71071_by.field_70461_c;
            if (current != slot) { slot = current; ++syncs; }
        }
    }

    private static class Hook extends MixinEntityPlayerSP { }

    private static void check(boolean value, String message) {
        ++checks;
        if (!value) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-6, message + ": " + actual);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void callback(Hook hook, String name) throws Exception {
        Method method = MixinEntityPlayerSP.class.getDeclaredMethod(name, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(hook, new CallbackInfo("test", false));
    }

    public static void main(String[] args) throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        unsafe = (Unsafe)access.get(null);
        Field registered = Bootstrap.class.getDeclaredField("field_151355_a");
        registered.setAccessible(true);
        registered.setBoolean(null, true);
        Block.func_149671_p();
        Minecraft game = Minecraft.func_71410_x();
        Player player = (Player)unsafe.allocateInstance(Player.class);
        World world = (World)unsafe.allocateInstance(World.class);
        world.blocks = new HashMap<>();
        world.blocks.put(new BlockPos(0, 62, 0), Blocks.field_150348_b.func_176223_P());
        Controller controller = (Controller)unsafe.allocateInstance(Controller.class);
        game.field_71439_g = player;
        game.field_71441_e = world;
        game.field_71442_b = controller;
        game.field_71474_y = (GameSettings)unsafe.allocateInstance(GameSettings.class);
        game.field_71474_y.field_74314_A = new KeyBinding("jump", 57, "test");
        game.field_71474_y.field_74311_E = new KeyBinding("sneak", 42, "test");
        game.field_71474_y.field_74351_w = new KeyBinding("forward", 17, "test");
        game.field_71474_y.field_74368_y = new KeyBinding("back", 31, "test");
        game.field_71474_y.field_74370_x = new KeyBinding("left", 30, "test");
        game.field_71474_y.field_74366_z = new KeyBinding("right", 32, "test");
        player.field_71158_b = new MovementInput();
        player.field_71158_b.field_78900_b = 1;
        player.field_71071_by = new InventoryPlayer(player);
        player.field_71071_by.field_70461_c = controller.slot = 3;
        player.field_71071_by.field_70462_a[0] = new ItemStack(new ItemBlock(Blocks.field_150348_b), 4);
        player.field_71071_by.field_70462_a[2] = new ItemStack(new ItemBlock(Blocks.field_150348_b), 32);
        player.field_70165_t = player.field_70169_q = player.field_70161_v = player.field_70166_s = 0.5;
        player.field_70163_u = player.field_70167_r = 64;
        player.field_70122_E = true;
        player.field_70177_z = 30;
        player.field_70125_A = 20;
        ScaffoldModule module = new ScaffoldModule();
        for (Field setting : ScaffoldModule.class.getFields()) if (Setting.class.isAssignableFrom(setting.getType())) ((Setting<?>)setting.get(module)).setModule(module);
        check(module.rotations.getModes()[4].equals("HYPIXEL") && module.rotations.getModes()[5].equals("STABLE"), "Hypixel config index retained and Stable added");
        check(module.keepY.getModes()[4].equals("OLDNCP") && module.verticalTower.getModes()[1].equals("PREDICTION"), "all source modes exposed");
        check(!module.sneak.setValue(8) && !module.sneakTicks.setValue(0), "source setting bounds enforced");
        module.itemSpoof.setValue(true);
        module.setEnabled(true);
        MyauClient.eventBus.register(module);
        module.onMovementInput(new MovementInputEvent());
        SafeWalkEvent walk = new SafeWalkEvent(false);
        module.onSafeWalk(walk);
        check(walk.isSafeWalk(), "source safe-walk request reaches movement event");
        Hook hook = new Hook();
        hook.field_70170_p = world;
        hook.field_70165_t = hook.field_70161_v = 0.5;
        hook.field_70163_u = 64;
        hook.field_70177_z = 30;
        hook.field_70125_A = 20;
        callback(hook, "beforePlayerUpdate");
        callback(hook, "beforeWalkingPlayerUpdate");
        check(controller.placed == 1 && controller.observedSlot == 2, "pre-walking placement uses largest block stack");
        check(player.field_71071_by.field_70461_c == 3 && controller.slot == 3 && controller.syncs == 2, "spoof transaction restores inventory and controller cache");
        near(controller.observedYaw, hook.field_70177_z, "send handlers and walking packet observe same yaw");
        near(controller.observedPitch, hook.field_70125_A, "send handlers and walking packet observe same pitch");
        callback(hook, "afterPlayerUpdate");
        near(hook.field_70177_z, 30, "late rotation restores camera yaw");
        near(hook.field_70125_A, 20, "late rotation restores camera pitch");
        callback(hook, "beforeWalkingPlayerUpdate");
        check(controller.placed == 1, "same tick is not placed twice");
        ScaffoldMinecraft adapter = (ScaffoldMinecraft)field(module, "adapter");
        adapter.selectSlot(2, false);
        player.field_71071_by.field_70461_c = 4;
        adapter.restoreSlot();
        check(player.field_71071_by.field_70461_c == 4, "manual slot changes survive cleanup");
        player.field_71071_by.field_70461_c = 3;
        world.blocks.remove(new BlockPos(0, 63, 0));
        ++player.field_70173_aa;
        RotationUpdateEvent competing = new RotationUpdateEvent(0, 0, 0, 0);
        competing.setRotation(10, 10, 5);
        PreWalkingUpdateEvent denied = new PreWalkingUpdateEvent(competing);
        module.onPreWalkingUpdate(denied);
        check(controller.placed == 1 && denied.getRotation() == competing, "higher priority rotation prevents placement and remains owned");
        ++player.field_70173_aa;
        controller.fail = true;
        boolean threw = false;
        try { module.onPreWalkingUpdate(new PreWalkingUpdateEvent(new RotationUpdateEvent(0, 0, 0, 0))); }
        catch (IllegalStateException expected) { threw = true; }
        check(threw && player.field_71071_by.field_70461_c == 3 && controller.slot == 3, "controller failure restores spoof slot");
        controller.fail = false;
        module.rotations.setValue(4);
        Field reportedYaw = MixinEntityPlayerSP.class.getDeclaredField("field_175164_bL");
        Field reportedPitch = MixinEntityPlayerSP.class.getDeclaredField("field_175165_bM");
        reportedYaw.setAccessible(true);
        reportedPitch.setAccessible(true);
        reportedYaw.setFloat(hook, 30);
        reportedPitch.setFloat(hook, -60);
        boolean turned = false;
        int before = controller.placed;
        for (int tick = 0; tick < 20 && controller.placed == before; ++tick) {
            ++player.field_70173_aa;
            player.field_70177_z = hook.field_70177_z = 30;
            player.field_70125_A = hook.field_70125_A = 20;
            module.onMovementInput(new MovementInputEvent());
            callback(hook, "beforePlayerUpdate");
            float previous = RotationState.previousHeadYaw();
            callback(hook, "beforeWalkingPlayerUpdate");
            if (tick == 0) {
                turned = hook.field_70177_z != 30 || hook.field_70125_A != 20;
                check(controller.placed == before, "Hypixel waits while the smoothed aim is outside the face");
            }
            near(RotationState.previousHeadYaw(), previous, "late updates preserve previous-frame rotation history");
            callback(hook, "afterPlayerUpdate");
        }
        check(turned && controller.placed == before + 1, "Hypixel keeps turning across rejected placements until aim reaches the face");
        module.setEnabled(false);
        near(HypixelScaffoldPacing.ticksToLevel(64, 0.42, 64), 12, "Hypixel jump prediction preserved");
        check(HypixelScaffoldPacing.turnTicks(12, false) == 11 && HypixelScaffoldPacing.turnTicks(12, true) == 5, "Hypixel diagonal placement budget preserved");
        near(HypixelScaffoldPacing.maxStep(180, 5, 25), 36, "Hypixel required turn rate preserved");
        near(HypixelScaffoldPacing.maxStep(180, 1, 25), 90, "Hypixel turn cap preserved");
        check(HypixelScaffoldPacing.smoothing(0, false) == 80 && HypixelScaffoldPacing.smoothing(0, true) == 65, "Hypixel smoothing floors preserved");
        System.out.println("Scaffold integration checks passed: " + checks);
    }
}
