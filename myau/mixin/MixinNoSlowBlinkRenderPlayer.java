package myau.mixin;

import myau.NoSlowBlink;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RenderPlayer.class, remap = false)
public abstract class MixinNoSlowBlinkRenderPlayer {
    @Redirect(method = "func_177137_d", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/AbstractClientPlayer;func_71052_bv()I"))
    private int blinkThirdPersonUseCount(AbstractClientPlayer player) {
        int actual = player.func_71052_bv();
        return actual == 0 && NoSlowBlink.shouldRenderSwordBlock(player) ? 1 : actual;
    }
}
