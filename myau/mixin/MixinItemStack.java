package myau.mixin;

import myau.ESPModule;
import myau.MyauClient;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value=Side.CLIENT)
@Mixin(value={ItemStack.class})
public abstract class MixinItemStack {
    @Inject(method={"hasEffect"}, at={@At(value="HEAD")}, cancellable=true)
    private void onHasEffect(CallbackInfoReturnable<Boolean> callbackInfo) {
        ESPModule eSP;
        if (MyauClient.moduleManager != null && (eSP = (ESPModule)MyauClient.moduleManager.modules.get(ESPModule.class)).isEnabled() && !eSP.isEnchantmentGlintAllowed()) {
            callbackInfo.setReturnValue(false);
        }
    }
}
