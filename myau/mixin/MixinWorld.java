package myau.mixin;

import myau.AntiObbyTrapModule;
import myau.JesusModule;
import myau.MyauClient;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(value=Side.CLIENT)
@Mixin(value={World.class})
public abstract class MixinWorld {
    @Redirect(method={"handleMaterialAcceleration"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/Entity;isPushedByWater()Z"))
    private boolean redirectHandleMaterialAcceleration(Entity entity) {
        JesusModule jesus;
        if (entity instanceof EntityPlayerSP && MyauClient.moduleManager != null && (jesus = (JesusModule)MyauClient.moduleManager.modules.get(JesusModule.class)).isEnabled() && ((Boolean)jesus.noPush.getValue()).booleanValue()) {
            return false;
        }
        return entity.func_96092_aw();
    }

    @Redirect(method={"rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState redirectRayTraceBlocks(World world, BlockPos position) {
        if (MyauClient.moduleManager == null) {
            return world.func_180495_p(position);
        }
        AntiObbyTrapModule antiObbyTrap = (AntiObbyTrapModule)MyauClient.moduleManager.modules.get(AntiObbyTrapModule.class);
        if (antiObbyTrap.isEnabled() && antiObbyTrap.shouldIgnoreBlock(world, position)) {
            if (((Boolean)antiObbyTrap.setAir.getValue()).booleanValue()) {
                world.func_175698_g(position);
            }
            return Blocks.field_150350_a.func_176223_P();
        }
        return world.func_180495_p(position);
    }
}
