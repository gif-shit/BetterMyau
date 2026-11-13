package myau.mixin;

import myau.NoSlowBlink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, remap = false)
public abstract class MixinNoSlowBlinkInput {

    @Inject(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;func_71039_bw()Z", ordinal = 0))
    private void beginBlinkInput(CallbackInfo ci) {
        NoSlowBlink blink = NoSlowBlink.current();
        if (blink != null)
            blink.beginInput();
    }

    @Inject(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;func_147115_a(Z)V", shift = At.Shift.AFTER))
    private void endBlinkInput(CallbackInfo ci) {
        NoSlowBlink blink = NoSlowBlink.current();
        if (blink != null)
            blink.endInput();
    }

    @Redirect(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;func_151470_d()Z"))
    private boolean readHeld(KeyBinding key) {
        NoSlowBlink blink = NoSlowBlink.current();
        return blink == null ? key.func_151470_d() : blink.held(key);
    }

    @Redirect(method = "func_71407_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;func_151468_f()Z"))
    private boolean readPress(KeyBinding key) {
        NoSlowBlink blink = NoSlowBlink.current();
        return blink == null ? key.func_151468_f() : blink.pressed(key);
    }
}
