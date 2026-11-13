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
        check(module.rotations.getModes()[4].equals("HYPIXEL") && module.rotations.getModes()[5].equals("SMOOTH"), "Hypixel retained and Stable replaced with Smooth");
        check(module.keepY.getModes().length == 4 && !module.towerOnPress.getValue(), "existing tower and keep-y controls retained");
        float[] limited = ScaffoldModule.smoothRotation(170, 0, -170, 80, true, 35.5f, 10);
        near(limited[0], 190, "Smooth takes the short path across the yaw boundary");
        near(limited[1], 80, "Smooth sets the placement pitch");
        limited = ScaffoldModule.smoothRotation(0, 0, 150, 80, true, 35.5f, 10);
        near(limited[0], 35.5f, "Smooth uses the supplied rotation limit");
        float yaw = 0;
        for (int tick = 0; tick < 6; tick++) {
            float next = ScaffoldModule.smoothRotation(yaw, 80, 150, 80, true, 35.5f, 10)[0];
            check(next >= yaw && next <= 150 && next - yaw <= 35.501f, "Smooth approaches the target without overshoot");
            yaw = next;
        }
        near(yaw, 150, "Smooth reaches the target within five limited turns");
        check(Math.abs(ScaffoldModule.smoothRotation(0, 0, 100, 80, false, 35, 0)[0] - 30) < 0.001, "Smooth eases the first rotation");
        module.rotations.setValue(5);
        module.setEnabled(true);
        float previousYaw = 0, previousPitch = 0;
        int start = controller.placed;
        for (int block = 1; block <= 48; ++block) {
            player.field_70161_v = block + 0.75;
            player.field_70166_s = player.field_70161_v - 1;
            player.field_70177_z = 0;
            BlockPos expected = new BlockPos(0, 63, block);
            for (int tick = 0; tick < 8 && !world.blocks.containsKey(expected); tick++) {
                RotationUpdateEvent rotation = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
                module.onRotationUpdate(rotation);
                previousYaw = rotation.getYaw();
                previousPitch = rotation.getPitch();
            }
            check(world.blocks.containsKey(expected), "Smooth places bridge block " + block);
            RotationUpdateEvent idle = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
            module.onRotationUpdate(idle);
            check(Math.abs(net.minecraft.util.MathHelper.func_76142_g(idle.getYaw() - 180)) < 45, "Smooth keeps looking toward the bridge between blocks: " + idle.getYaw());
            previousYaw = idle.getYaw(); previousPitch = idle.getPitch();
        }
        check(controller.placed - start == 48, "Smooth places a forty-eight block bridge without side placements");
        player.field_70177_z = -90;
        player.field_70161_v = player.field_70166_s = 48.5;
        for (int block = 1; block <= 8; ++block) {
            player.field_70165_t = block + 0.75;
            player.field_70169_q = player.field_70165_t - 1;
            BlockPos expected = new BlockPos(block, 63, 48);
            for (int tick = 0; tick < 8 && !world.blocks.containsKey(expected); tick++) {
                RotationUpdateEvent turn = new RotationUpdateEvent(previousYaw, previousPitch, player.field_70177_z, 0);
                module.onRotationUpdate(turn);
                previousYaw = turn.getYaw(); previousPitch = turn.getPitch();
            }
            check(world.blocks.containsKey(expected), "Smooth follows a deliberate direction change " + block);
        }
        RotationUpdateEvent competing = new RotationUpdateEvent(previousYaw, previousPitch, 25, 0);
        competing.setRotation(60, 20, 10);
        competing.setMovementYaw(60, 10);
        module.onRotationUpdate(competing);
        near(competing.getYaw(), 60, "higher-priority rotation is respected");
        module.onWorldLoad(new WorldLoadEvent());
        module.setEnabled(false);
        check(player.field_71071_by.field_70461_c == 3, "disable restores original hotbar slot");
        player.field_70161_v = player.field_70166_s = 0.5;
        player.field_70165_t = player.field_70169_q = 0.5;
        player.field_71071_by.field_70461_c = 2;
        player.field_71071_by.field_70462_a[2].field_77994_a = 64;
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
        for (int direction = 0; direction < 4; ++direction) {
            module.tower.setValue(2);
            module.rotations.setValue(5);
            module.towerOnPress.setValue(true);
            KeyBinding.func_74510_a(30, true);
            InputUtils.keys.add(-99);
            player.field_71158_b.field_78900_b = 1;
            player.field_71158_b.field_78902_a = 1;
            player.field_70177_z = direction * 90;
            player.field_70165_t = player.field_70161_v = 0.5;
            player.field_70163_u = 64;
            player.field_70181_x = 0;
            player.field_70122_E = true;
            MovementUtils.setHorizontalMotion(0.28, player.field_70177_z - 45);
            world.blocks.clear();
            world.blocks.put(new BlockPos(0, 63, 0), Blocks.field_150348_b.func_176223_P());
            module.setEnabled(true);
            RotationUpdateEvent diagonal = new RotationUpdateEvent(player.field_70177_z, 0, player.field_70177_z, 0);
            double peak = 64;
            for (int tick = 0; tick < 80; ++tick) {
                player.field_71071_by.field_70462_a[2].field_77994_a = 64;
                diagonal = new RotationUpdateEvent(diagonal.getYaw(), diagonal.getPitch(), player.field_70177_z, 0);
                module.onRotationUpdate(diagonal);
                module.onMoveFlying(new MoveFlyingEvent(1, 1, 0.02f));
                double oldY = player.field_70163_u;
                player.field_70165_t += player.field_70159_w;
                player.field_70161_v += player.field_70179_y;
                double nextY = oldY + player.field_70181_x;
                boolean landed = false;
                for (BlockPos pos : world.blocks.keySet()) {
                    double top = pos.func_177956_o() + 1;
                    if (player.field_70165_t + 0.3 <= pos.func_177958_n() || player.field_70165_t - 0.3 >= pos.func_177958_n() + 1
                            || player.field_70161_v + 0.3 <= pos.func_177952_p() || player.field_70161_v - 0.3 >= pos.func_177952_p() + 1) continue;
                    if (player.field_70181_x <= 0 && oldY >= top && nextY <= top) { nextY = top; landed = true; }
                }
                player.field_70163_u = nextY;
                player.field_70122_E = landed;
                module.onPreWalkingUpdate(new PreWalkingUpdateEvent());
                player.field_70181_x = ((landed ? 0 : player.field_70181_x) - 0.08) * 0.98;
                peak = Math.max(peak, nextY);
                check(nextY >= peak - 1.5, "diagonal Extra tower maintains support at direction " + direction + " tick " + tick + " y " + nextY + " peak " + peak);
            }
            check(peak > 72, "diagonal Extra tower continues climbing at direction " + direction);
            module.setEnabled(false);
        }
        near(HypixelScaffoldPacing.ticksToLevel(64, 0.42, 64), 12, "Hypixel jump prediction unchanged");
        near(HypixelScaffoldPacing.maxStep(180, 5, 25), 36, "Hypixel turn pacing unchanged");
        System.out.println("Scaffold integration checks passed: " + checks);
    }
}
