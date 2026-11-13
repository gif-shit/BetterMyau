package myau.mixin;

import myau.NoSlowBlink;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerControllerMP.class, remap = false)
public abstract class MixinNoSlowBlinkController {

    @Inject(method = "func_78765_e", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;func_78750_j()V"))
    private void beforeBlinkSync(CallbackInfo ci) {
        NoSlowBlink blink = NoSlowBlink.current();
        if (blink != null)
            blink.beforeSync();
    }
}
