package myau.mixin;

import myau.MyauClient;
import myau.XrayModule;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value=Side.CLIENT)
@Mixin(value={Block.class})
public abstract class MixinBlock {
    @Inject(method={"shouldSideBeRendered"}, at={@At(value="HEAD")}, cancellable=true)
    private void onShouldSideBeRendered(IBlockAccess iBlockAccess, BlockPos position, EnumFacing facing, CallbackInfoReturnable<Boolean> callbackInfo) {
        int neighborZ;
        int neighborY;
        int neighborX;
        XrayModule xray;
        if (MyauClient.moduleManager != null && (xray = (XrayModule)MyauClient.moduleManager.modules.get(XrayModule.class)).isEnabled() && (Integer)xray.mode.getValue() == 1 && xray.isSupportedBlock(Block.func_149682_b((Block)((Block)this))) && xray.passesCaveCheck(new BlockPos(neighborX = position.func_177958_n() - facing.func_176730_m().func_177958_n(), neighborY = position.func_177956_o() - facing.func_176730_m().func_177956_o(), neighborZ = position.func_177952_p() - facing.func_176730_m().func_177952_p()))) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method={"getBlockLayer"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfo) {
        int blockId;
        XrayModule xray;
        if (MyauClient.moduleManager != null && (xray = (XrayModule)MyauClient.moduleManager.modules.get(XrayModule.class)).isEnabled() && (!xray.isSupportedBlock(blockId = Block.func_149682_b((Block)((Block)this))) || (Integer)xray.mode.getValue() == 0 && !xray.isBlockEnabled(blockId))) {
            callbackInfo.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
        }
    }
}
