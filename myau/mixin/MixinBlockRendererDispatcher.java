package myau.mixin;

import myau.BedESPModule;
import myau.MyauClient;
import myau.XrayModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value=Side.CLIENT)
@Mixin(value={BlockRendererDispatcher.class})
public abstract class MixinBlockRendererDispatcher {
    @Inject(method={"renderBlock"}, at={@At(value="HEAD")})
    private void onRenderBlock(IBlockState blockState, BlockPos position, IBlockAccess iBlockAccess, WorldRenderer worldRenderer, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (MyauClient.moduleManager != null) {
            XrayModule xray;
            BedESPModule bedESP = (BedESPModule)MyauClient.moduleManager.modules.get(BedESPModule.class);
            if (bedESP.isEnabled() && blockState.func_177230_c() instanceof BlockBed && blockState.func_177229_b((IProperty)BlockBed.field_176472_a) == BlockBed.EnumPartType.HEAD) {
                bedESP.bedHeads.add(new BlockPos((Vec3i)position));
            }
            if ((xray = (XrayModule)MyauClient.moduleManager.modules.get(XrayModule.class)).isEnabled() && xray.isBlockEnabled(Block.func_149682_b((Block)blockState.func_177230_c()))) {
                if (xray.passesCaveCheck(position)) {
                    xray.discoveredBlocks.add(new BlockPos((Vec3i)position));
                } else {
                    xray.discoveredBlocks.remove(position);
                }
            }
        }
    }
}
