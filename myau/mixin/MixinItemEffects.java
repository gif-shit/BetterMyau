package myau.mixin;

import myau.ItemEffectsModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = ItemRenderer.class, remap = false)
public abstract class MixinItemEffects {
    @Inject(method = "func_178099_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;func_179094_E()V", shift = At.Shift.AFTER), require = 1)
    private void resizeHeldItem(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, CallbackInfo callbackInfo) {
        if (transform != ItemCameraTransforms.TransformType.FIRST_PERSON) return;
        ItemEffectsModule effects = ItemEffectsModule.current();
        if (effects == null) return;
        float size = effects.sizeMultiplier();
        if (size != 1) GlStateManager.func_179152_a(size, size, size);
    }

    @ModifyArgs(method = "func_178097_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;func_179152_a(FFF)V", ordinal = 0))
    private void resizeMap(Args args) {
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
