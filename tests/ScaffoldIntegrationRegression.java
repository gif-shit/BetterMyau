package myau;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import myau.mixin.AccessorEntity;
import myau.mixin.AccessorEntityLivingBase;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.client.gui.GuiScreen;
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
import sun.misc.Unsafe;

public final class ScaffoldIntegrationRegression {
    private static Unsafe unsafe;
    private static int checks;

    private static class Screen extends GuiScreen { }

    private static class Player extends EntityPlayerSP implements AccessorEntity, AccessorEntityLivingBase {
        boolean sprint;
        Player() { super(null, null, null, null); }
        public boolean getIsInWeb() { return false; }
        public boolean func_70644_a(Potion potion) { return false; }
        public Map<Integer, PotionEffect> getActivePotionsMap() { return Collections.emptyMap(); }
        public AttributeModifier getSprintingSpeedBoostModifier() { return null; }
        public int getJumpTicks() { return 0; }
        public void setJumpTicks(int ticks) { }
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
        public List<AxisAlignedBB> func_72945_a(Entity entity, AxisAlignedBB bounds) {
            List<AxisAlignedBB> hits = new ArrayList<>();
            for (BlockPos pos : blocks.keySet()) {
                AxisAlignedBB block = new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1, pos.func_177956_o() + 1, pos.func_177952_p() + 1);
                if (block.func_72326_a(bounds)) hits.add(block);
            }
            return hits;
        }
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

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-6, message + ": " + actual);
    }

    private static int phase(ScaffoldModule module) throws Exception {
        Field field = ScaffoldModule.class.getDeclaredField("towerPhase");
        field.setAccessible(true);
        return field.getInt(module);
    }

    private static void bind(ScaffoldModule module) throws Exception {
        for (Field field : ScaffoldModule.class.getFields()) {
            if (Setting.class.isAssignableFrom(field.getType())) ((Setting<?>)field.get(module)).setModule(module);
        }
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
        world.blocks.put(new BlockPos(0, 63, 0), Blocks.field_150348_b.func_176223_P());
        Controller controller = (Controller)unsafe.allocateInstance(Controller.class);
        game.field_71439_g = player;
        game.field_71441_e = world;
        game.field_71442_b = controller;
        game.field_71474_y = (GameSettings)unsafe.allocateInstance(GameSettings.class);
        game.field_71474_y.field_74314_A = new KeyBinding("jump", 57, "test");
        game.field_71474_y.field_74311_E = new KeyBinding("sneak", 42, "test");
        game.field_71474_y.field_74313_G = new KeyBinding("use", -99, "test");
        game.field_71474_y.field_74351_w = new KeyBinding("forward", 17, "test");
        game.field_71474_y.field_74368_y = new KeyBinding("back", 31, "test");
        game.field_71474_y.field_74370_x = new KeyBinding("left", 30, "test");
        game.field_71474_y.field_74366_z = new KeyBinding("right", 32, "test");
        player.field_71158_b = new MovementInput();
        player.field_71158_b.field_78900_b = 1;
        player.field_71071_by = new InventoryPlayer(player);
        player.field_71071_by.field_70461_c = controller.slot = 3;
        player.field_71071_by.field_70462_a[0] = new ItemStack(new ItemBlock(Blocks.field_150348_b), 64);
        player.field_71071_by.field_70462_a[2] = new ItemStack(new ItemBlock(Blocks.field_150348_b), 64);
        player.field_70165_t = player.field_70169_q = 0.5;
        player.field_70163_u = player.field_70167_r = 64;
        player.field_70122_E = true;
        MyauClient.moduleManager.modules.put(BedNukerModule.class, new BedNukerModule());
        MyauClient.moduleManager.modules.put(LongJumpModule.class, new LongJumpModule());
        KeyBinding.func_74510_a(17, true);
        ScaffoldModule module = new ScaffoldModule();
        bind(module);
        check(module.rotations.getModes()[4].equals("HYPIXEL") && module.rotations.getModes()[5].equals("STABLE"), "Hypixel retained and Stable added");
        check(module.keepY.getModes().length == 4 && !module.towerOnPress.getValue(), "previous Scaffold settings and jump trigger restored");
        check(!module.stableSmoothing.isVisible() && module.stableSmoothing.getValue() == 0, "Stable smoothing is hidden outside Stable and defaults to fastest");
        module.rotations.setValue(5);
        check(module.stableSmoothing.isVisible(), "Stable smoothing slider is visible in Stable");
        check(!module.stableSmoothing.setValue(-1) && !module.stableSmoothing.setValue(101), "Stable smoothing is limited to zero through one hundred percent");
        module.setEnabled(true);
        float previousYaw = 0, previousPitch = 0;
        float lockedYaw = Float.NaN;
        int start = controller.placed;
        for (int block = 1; block <= 48; ++block) {
            player.field_70161_v = block + 0.75;
            player.field_70166_s = player.field_70161_v - 1;
            player.field_70177_z = 0;
            RotationUpdateEvent rotation = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
            module.onRotationUpdate(rotation);
            if (block == 1) lockedYaw = rotation.getYaw();
            near(rotation.getYaw(), lockedYaw, "Stable yaw stays fixed after block " + block);
            check(world.blocks.containsKey(new BlockPos(0, 63, block)), "Stable places straight bridge block " + block);
            previousYaw = rotation.getYaw();
            previousPitch = rotation.getPitch();
            for (int gap = 0; gap < 2; ++gap) {
                RotationUpdateEvent idle = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
                module.onRotationUpdate(idle);
                near(idle.getYaw(), lockedYaw, "Stable holds yaw between placements");
                previousYaw = idle.getYaw(); previousPitch = idle.getPitch();
            }
        }
        check(controller.placed - start == 48, "forty-eight blocks placed without side placements");
        player.field_70177_z = -90;
        player.field_70161_v = player.field_70166_s = 48.5;
        for (int block = 1; block <= 8; ++block) {
            player.field_70165_t = block + 0.75;
            player.field_70169_q = player.field_70165_t - 1;
            RotationUpdateEvent turn = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
            module.onRotationUpdate(turn);
            check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(turn.getYaw() - 90)) < 0.01, "Stable follows deliberate camera turn");
            check(world.blocks.containsKey(new BlockPos(block, 63, 48)), "Stable places along the new camera direction");
            previousYaw = turn.getYaw(); previousPitch = turn.getPitch();
        }
        KeyBinding.func_74510_a(17, false);
        KeyBinding.func_74510_a(30, true);
        player.field_70165_t = player.field_70169_q = 8.5;
        for (int block = 1; block <= 8; ++block) {
            player.field_70161_v = 48.5 - block;
            player.field_70166_s = player.field_70161_v + 1;
            RotationUpdateEvent strafe = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
            module.onRotationUpdate(strafe);
            check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(strafe.getYaw())) < 0.01, "Stable follows deliberate WASD direction changes");
            check(world.blocks.containsKey(new BlockPos(8, 63, 48 - block)), "Stable places along the new strafe direction");
            previousYaw = strafe.getYaw(); previousPitch = strafe.getPitch();
        }
        check(controller.placed - start == 64, "straight bridge and two controlled turns have no side placements");
        KeyBinding.func_74510_a(30, false);
        KeyBinding.func_74510_a(17, true);
        RotationUpdateEvent competing = new RotationUpdateEvent(previousYaw, previousPitch, 25, 0);
        competing.setRotation(60, 20, 10);
        competing.setMovementYaw(60, 10);
        module.onRotationUpdate(competing);
        near(competing.getYaw(), 60, "higher-priority rotation is respected");
        module.onWorldLoad(new WorldLoadEvent());
        RotationUpdateEvent reset = new RotationUpdateEvent(previousYaw, previousPitch, 90, 0);
        player.field_70177_z = 90;
        module.onRotationUpdate(reset);
        check(Math.abs(reset.getYaw() - lockedYaw) > 10, "Stable accepts the current direction after a world change");
        module.setEnabled(false);
        check(player.field_71071_by.field_70461_c == 3, "disable restores original hotbar slot");

        player.field_70161_v = player.field_70166_s = 0.5;
        player.field_70165_t = player.field_70169_q = 0.5;
        player.field_71071_by.field_70461_c = 2;
        player.field_71071_by.field_70462_a[2].field_77994_a = 64;
        player.field_70177_z = 0;
        Field randomField = RandomUtils.class.getDeclaredField("RANDOM");
        randomField.setAccessible(true);
        java.util.Random random = (java.util.Random)randomField.get(null);
        Field targetPitch = ScaffoldModule.class.getDeclaredField("placementPitch");
        targetPitch.setAccessible(true);
        float[] yawSteps = new float[3], pitchSteps = new float[3];
        int[] smoothing = {0, 35, 100};
        for (int preset = 0; preset < smoothing.length; ++preset) {
            module.stableSmoothing.setValue(smoothing[preset]);
            module.setEnabled(true);
            random.setSeed(42);
            RotationUpdateEvent turn = new RotationUpdateEvent(0, 0, 0, 0);
            module.onRotationUpdate(turn);
            yawSteps[preset] = Math.abs(net.minecraft.util.MathHelper.func_76142_g(turn.getYaw()));
            pitchSteps[preset] = turn.getPitch();
            float error = Math.abs(net.minecraft.util.MathHelper.func_76142_g(-180 - turn.getYaw()));
            float pitch = targetPitch.getFloat(module);
            for (int tick = 0; tick < 8; ++tick) {
                RotationUpdateEvent next = new RotationUpdateEvent(turn.getYaw(), turn.getPitch(), 0, 0);
                module.onRotationUpdate(next);
                float remaining = Math.abs(net.minecraft.util.MathHelper.func_76142_g(-180 - next.getYaw()));
                check(remaining <= error + 0.01, "Stable smoothing approaches the requested direction without reversing");
                near(next.getMovementYaw(), next.getYaw(), "movement correction follows the applied smoothed yaw");
                error = remaining;
                turn = next;
            }
            check(error < 0.01, "Stable smoothing reaches the requested yaw");
            near(turn.getPitch(), pitch, "Stable smoothing reaches the placement pitch");
            module.setEnabled(false);
        }
        check(yawSteps[0] > 179.99, "zero smoothing gives the fastest full turn in one tick");
        check(yawSteps[2] < yawSteps[1] && yawSteps[1] < yawSteps[0], "higher smoothing makes yaw turns progressively gentler");
        check(pitchSteps[2] < pitchSteps[1] && pitchSteps[1] <= pitchSteps[0], "higher smoothing makes pitch turns progressively gentler");
        module.stableSmoothing.setValue(0);
        for (int mode = 1; mode <= 2; ++mode) {
            module.tower.setValue(mode);
            module.towerOnPress.setValue(true);
            module.setEnabled(true);
            InputUtils.keys.clear();
            KeyBinding.func_74510_a(57, true);
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            check(phase(module) == 0, "space alone does not activate tower-on-press");
            KeyBinding.func_74510_a(57, false);
            InputUtils.keys.add(-99);
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            check(phase(module) == 1, "right-click activates selected tower mode " + mode);
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            near(player.field_70181_x, (double)0.42f, "right-click tower jumps without space");
            InputUtils.keys.clear();
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            check(phase(module) == 0, "releasing right-click stops tower");
            InputUtils.keys.add(-99);
            game.field_71462_r = new Screen();
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            check(phase(module) == 0, "GUI clicks do not activate tower");
            game.field_71462_r = null;
            InputUtils.keys.clear();
            module.towerOnPress.setValue(false);
            KeyBinding.func_74510_a(57, true);
            module.onMoveFlying(new MoveFlyingEvent(0, 1, 0.02f));
            check(phase(module) == 1, "default tower still uses space");
            KeyBinding.func_74510_a(57, false);
            module.setEnabled(false);
        }
        module.tower.setValue(3);
        module.towerOnPress.setValue(true);
        module.setEnabled(true);
        player.field_71158_b.field_78901_c = false;
        module.onMovementInput(new MovementInputEvent());
        check(!player.field_71158_b.field_78901_c, "Telly tower waits for right-click");
        InputUtils.keys.add(-99);
        module.onMovementInput(new MovementInputEvent());
        check(player.field_71158_b.field_78901_c, "right-click activates Telly jump input");
        module.setEnabled(false);
        near(HypixelScaffoldPacing.ticksToLevel(64, 0.42, 64), 12, "Hypixel jump prediction unchanged");
        near(HypixelScaffoldPacing.maxStep(180, 5, 25), 36, "Hypixel turn pacing unchanged");
        System.out.println("Scaffold integration checks passed: " + checks);
    }
}
