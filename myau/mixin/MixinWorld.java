package myau.mixin;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.List;
import myau.AntiObbyTrapModule;
import myau.JesusModule;
import myau.MyauClient;
import myau.PitModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { World.class })
public abstract class MixinWorld {

    @Inject(method = { "getEntitiesInAABBexcluding" }, at = @At("RETURN"), cancellable = true)
    private void filterPitEntities(Entity excluded, AxisAlignedBB bounds, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> callbackInfo) {
        filterPitPlayers(callbackInfo);
    }

    @Inject(method = { "getEntitiesWithinAABB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;" }, at = @At("RETURN"), cancellable = true)
    private void filterPitEntitiesByClass(Class<?> type, AxisAlignedBB bounds, Predicate<?> predicate, CallbackInfoReturnable<List<Entity>> callbackInfo) {
        filterPitPlayers(callbackInfo);
    }

    @Unique
    private void filterPitPlayers(CallbackInfoReturnable<List<Entity>> callbackInfo) {
        PitModule pit = PitModule.instance();
        if (!((World) (Object) this).field_72995_K
                || pit == null
                || !pit.isEnabled()
                || pit.hideFarPlayer.getValue() == 0
                && pit.hidePitSpawn.getValue() != 2)
            return;
        List<Entity> visible = new ArrayList<>(callbackInfo.getReturnValue());
        visible.removeIf(pit::hideEntity);
        callbackInfo.setReturnValue(visible);
    }

    @Redirect(method = { "handleMaterialAcceleration" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"))
    private boolean redirectHandleMaterialAcceleration(Entity entity) {
        JesusModule jesus;
        if (entity instanceof EntityPlayerSP
                && MyauClient.moduleManager != null
                && (jesus = (JesusModule) MyauClient.moduleManager.modules.get(JesusModule.class)).isEnabled()
                && jesus.noPush.getValue()) {
            return false;
        }
        return entity.func_96092_aw();
    }

    @Redirect(method = { "rayTraceBlocks(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;ZZZ)Lnet/minecraft/util/MovingObjectPosition;" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState redirectRayTraceBlocks(World world, BlockPos position) {
        if (MyauClient.moduleManager == null) {
            return world.func_180495_p(position);
        }
        AntiObbyTrapModule antiObbyTrap = (AntiObbyTrapModule) MyauClient.moduleManager.modules.get(AntiObbyTrapModule.class);
        if (antiObbyTrap.isEnabled() && antiObbyTrap.shouldIgnoreBlock(world, position)) {
            if (antiObbyTrap.setAir.getValue()) {
                world.func_175698_g(position);
            }
            return Blocks.field_150350_a.func_176223_P();
        }
        return world.func_180495_p(position);
    }
}
