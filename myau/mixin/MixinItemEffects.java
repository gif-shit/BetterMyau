package myau.mixin;

import myau.ItemEffectsModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = ItemRenderer.class, remap = false)
public abstract class MixinItemEffects {
    @ModifyArgs(method = {"func_178096_b", "func_178097_a"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;func_179152_a(FFF)V", ordinal = 0))
    private void resizeHeldItem(Args args) {
        ItemEffectsModule effects = ItemEffectsModule.current();
        if (effects == null) return;
        float size = effects.sizeMultiplier();
        if (size == 1) return;
        for (int axis = 0; axis < 3; axis++) args.set(axis, (Float) args.get(axis) * size);
    }

    @Redirect(method = "func_78440_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;func_70678_g(F)F"))
    private float renderSwingProgress(AbstractClientPlayer player, float partialTicks) {
        float vanilla = player.func_70678_g(partialTicks);
        ItemEffectsModule effects = ItemEffectsModule.current();
        return effects == null ? vanilla : effects.renderSwingProgress(player, vanilla, partialTicks);
    }
}
