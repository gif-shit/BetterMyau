package myau;

import java.lang.reflect.Field;
import java.util.*;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.chunk.Chunk;
import sun.misc.Unsafe;

public final class TrajectoriesRegression {
    private static int checks;
    private static void check(boolean value, String message) { ++checks; if (!value) throw new AssertionError(message); }
    private static void near(double value, double expected, String message) { check(Math.abs(value - expected) < 1e-6, message + ": " + value); }
    private static class Player extends EntityPlayerSP {
        ItemStack held;
        boolean using;
        int charge;
        Player() { super(null, null, null, null); }
        public ItemStack func_70694_bm() { return held; }
        public float func_70047_e() { return 1.62f; }
        public boolean func_71039_bw() { return using; }
        public int func_71057_bx() { return charge; }
    }
    private static class Target extends Entity {
        AxisAlignedBB bounds;
        Target() { super(null); }
        protected void func_70088_a() { }
        protected void func_70037_a(NBTTagCompound tags) { }
        protected void func_70014_b(NBTTagCompound tags) { }
        public boolean func_70067_L() { return true; }
        public AxisAlignedBB func_174813_aQ() { return bounds; }
    }
    private static class LoadedChunk extends Chunk {
        List<Entity> entities;
        LoadedChunk() { super(null, 0, 0); }
        public void func_177414_a(Entity excluded, AxisAlignedBB bounds, List<Entity> output, Predicate<? super Entity> predicate) {
            for (Entity entity : entities) if (entity != excluded && entity.func_174813_aQ().func_72326_a(bounds)) output.add(entity);
        }
    }
    private static class World extends WorldClient {
        Map<BlockPos, IBlockState> blocks;
        LoadedChunk chunk;
        boolean water, ignoreNoBox;
        World() { super(null, null, 0, null, null); }
        public Chunk func_72964_e(int x, int z) { return chunk; }
        public IBlockState func_180495_p(BlockPos pos) {
            return blocks.containsKey(pos) ? blocks.get(pos) : (water ? Blocks.field_150355_j : Blocks.field_150350_a).func_176223_P();
        }
        public MovingObjectPosition func_147447_a(Vec3 from, Vec3 to, boolean liquid, boolean ignore, boolean last) {
            ignoreNoBox = ignore;
            MovingObjectPosition nearest = null;
            for (BlockPos pos : blocks.keySet()) {
                AxisAlignedBB bounds = new AxisAlignedBB(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), pos.func_177958_n() + 1, pos.func_177956_o() + 1, pos.func_177952_p() + 1);
                MovingObjectPosition hit = bounds.func_72327_a(from, to);
                if (hit != null && (nearest == null || from.func_72438_d(hit.field_72307_f) < from.func_72438_d(nearest.field_72307_f))) nearest = new MovingObjectPosition(hit.field_72307_f, hit.field_178784_b, pos);
            }
            return nearest;
        }
    }
    private static Vec3 last(TrajectoriesModule.Prediction prediction) { return prediction.points.get(prediction.points.size() - 1); }
    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Unsafe unsafe = (Unsafe)field.get(null);
        Field registered = Bootstrap.class.getDeclaredField("field_151355_a"); registered.setAccessible(true); registered.setBoolean(null, true);
        Block.func_149671_p();
        Minecraft game = Minecraft.func_71410_x();
        Player player = (Player)unsafe.allocateInstance(Player.class);
        World world = (World)unsafe.allocateInstance(World.class);
        world.blocks = new HashMap<>();
        world.chunk = (LoadedChunk)unsafe.allocateInstance(LoadedChunk.class);
        world.chunk.entities = new ArrayList<>();
        game.field_71439_g = player; game.field_71441_e = world;
        game.field_71474_y = (GameSettings)unsafe.allocateInstance(GameSettings.class);
        player.field_70165_t = player.field_70161_v = 10.5;
        player.field_70163_u = 64;
        player.held = new ItemStack(new ItemEnderPearl());
        TrajectoriesModule module = new TrajectoriesModule();
        check(module.predictPath() == null, "disabled Trajectories has no prediction");
        module.setEnabled(true);
        BlockPos wall = new BlockPos(10, 65, 11);
        world.blocks.put(wall, Blocks.field_150348_b.func_176223_P());
        TrajectoriesModule.Prediction path = module.predictPath();
        check(path.hit != null && path.hit.func_178782_a().equals(wall), "pearl prediction stops at the targeted block");
        check(path.points.size() == 2, "an immediate block impact still produces a visible segment");
        near(last(path).field_72449_c, 11, "path terminates exactly on the block face");
        near(last(path).field_72448_b, 65.52, "impact height matches the launch height");
        near(path.points.get(0).field_72450_a, 10.5 - (double)0.16f, "launch origin uses the player's hand offset");
        check(path.hit.field_178784_b == EnumFacing.NORTH, "impact identifies the actual block face");
        check(!world.ignoreNoBox, "throwable ray tracing uses vanilla block collision policy");
        Target behind = (Target)unsafe.allocateInstance(Target.class);
        behind.bounds = new AxisAlignedBB(10, 65, 11.5, 11, 66, 11.7);
        world.chunk.entities.add(behind);
        path = module.predictPath();
        check(path.hit.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK, "an entity behind the wall cannot replace its collision");
        world.blocks.clear();
        Target farther = (Target)unsafe.allocateInstance(Target.class);
        farther.bounds = new AxisAlignedBB(10, 65, 11.8, 11, 66, 12);
        Target closer = (Target)unsafe.allocateInstance(Target.class);
        closer.bounds = new AxisAlignedBB(10, 65, 11.2, 11, 66, 11.4);
        world.chunk.entities.clear(); world.chunk.entities.add(closer); world.chunk.entities.add(farther);
        path = module.predictPath();
        check(path.hit.field_72308_g == closer, "nearest entity wins even when a farther entity is scanned last");
        near(last(path).field_72449_c, 10.9, "entity impact uses vanilla collision expansion");
        Collections.reverse(world.chunk.entities);
        check(module.predictPath().hit.field_72308_g == closer, "collision is independent of entity iteration order");
        world.chunk.entities.clear();
        for (Item item : new Item[]{new ItemEnderPearl(), new ItemSnowball(), new ItemEgg(), new ItemFishingRod(), new ItemBow()}) {
            module.projectiles.setValue(true);
            player.held = new ItemStack(item); player.using = true; player.charge = 20;
            path = module.predictPath();
            double speed = item instanceof ItemBow ? 3 : 1.5;
            Vec3 first = path.points.get(0), second = path.points.get(1), third = path.points.get(2);
            near(first.func_72438_d(second), speed, "launch speed for " + item.getClass().getSimpleName());
            near(second.field_72449_c - first.field_72449_c, speed, "forward direction for " + item.getClass().getSimpleName());
            double drag = item instanceof ItemFishingRod ? (double)0.92f : (double)0.99f;
            near(third.field_72449_c - second.field_72449_c, speed * drag, "air drag for " + item.getClass().getSimpleName());
            double fall = item instanceof ItemBow ? (double)0.05f : item instanceof ItemFishingRod ? (double)0.04f * drag : (double)0.03f;
            near(third.field_72448_b - second.field_72448_b, -fall, "gravity order for " + item.getClass().getSimpleName());
        }
        check(world.ignoreNoBox, "arrow ray tracing ignores blocks without collision boxes");
        player.charge = 10;
        path = module.predictPath();
        near(path.points.get(0).func_72438_d(path.points.get(1)), 1.25, "half-drawn bow uses its actual power");
        player.charge = 1;
        check(module.predictPath() == null, "an insufficient bow draw has no launch preview");
        player.charge = 20; player.using = false;
        check(module.predictPath() == null, "bow preview requires drawing the bow");
        player.using = true; world.water = true;
        path = module.predictPath();
        near(path.points.get(2).field_72449_c - path.points.get(1).field_72449_c, 3 * (double)0.6f, "arrows use vanilla water drag");
        player.held = new ItemStack(new ItemEnderPearl());
        path = module.predictPath();
        near(path.points.get(2).field_72449_c - path.points.get(1).field_72449_c, 1.5 * (double)0.8f, "pearls use vanilla water drag");
        world.water = false;
        for (int x = 5; x <= 15; ++x) for (int z = 5; z <= 20; ++z) world.blocks.put(new BlockPos(x, 63, z), Blocks.field_150348_b.func_176223_P());
        player.field_70125_A = 60;
        path = module.predictPath();
        check(path.hit != null && path.hit.field_178784_b == EnumFacing.UP, "downward aim lands on the top of the block");
        near(last(path).field_72448_b, 64, "downward impact stops exactly at the block surface");
        world.blocks.clear(); player.field_70125_A = 0;
        RotationState.update(true, 90, 30, 90, 3);
        path = module.predictPath();
        check(path.points.get(1).field_72450_a < path.points.get(0).field_72450_a - 1, "preview follows the active launch yaw");
        check(Math.abs(path.points.get(1).field_72448_b - path.points.get(0).field_72448_b + 0.75) < 1e-4, "preview follows the active launch pitch with vanilla angle precision");
        near(player.field_70177_z, 0, "preview never changes the player's aim");
        for (int tick = 0; tick < 3; ++tick) RotationState.update(false, 0, 0, 0, 0);
        player.field_70163_u = 100000;
        path = module.predictPath();
        check(path.hit == null && path.points.size() == 512, "empty high-altitude paths have a bounded render cost");
        game.field_71474_y.field_74320_O = 1;
        check(module.predictPath() == null, "third-person view retains the existing visibility setting");
        game.field_71474_y.field_74320_O = 0; game.field_71441_e = null;
        check(module.predictPath() == null, "world unload is safe");
        System.out.println("Trajectories regression checks passed: " + checks);
    }
}
