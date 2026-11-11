package myau;

import myau.BooleanSetting;
import myau.Module;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AntiObbyTrapModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final BooleanSetting setAir = new BooleanSetting("set-air", true);

    public AntiObbyTrapModule() {
        super("AntiObbyTrap", false, 0);
    }

    public boolean shouldIgnoreBlock(World world, BlockPos position) {
        IBlockState state = world.func_180495_p(position);
        Block block = state.func_177230_c();
        boolean solid = block.func_149688_o().func_76220_a();
        if (!solid || !block.func_149730_j()) {
            return false;
        }
        Vec3 eyes = new Vec3(AntiObbyTrapModule.minecraft.field_71439_g.field_70165_t, AntiObbyTrapModule.minecraft.field_71439_g.field_70163_u + (double)AntiObbyTrapModule.minecraft.field_71439_g.func_70047_e(), AntiObbyTrapModule.minecraft.field_71439_g.field_70161_v);
        return block.func_180640_a((World)AntiObbyTrapModule.minecraft.field_71441_e, position, state).func_72318_a(eyes);
    }
}
