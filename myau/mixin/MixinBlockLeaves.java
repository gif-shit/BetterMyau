package myau.mixin;

import myau.MyauClient;
import myau.XrayModule;
import net.minecraft.block.BlockLeaves;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { BlockLeaves.class })
public abstract class MixinBlockLeaves {

    @Inject(method = { "getBlockLayer" }, at = { @At(value = "HEAD") }, cancellable = true)
    private void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfo) {
        XrayModule xray;
        if (MyauClient.moduleManager != null
                && (xray = (XrayModule) MyauClient.moduleManager.modules.get(XrayModule.class)).isEnabled()) {
            callbackInfo.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
        }
    }
}
