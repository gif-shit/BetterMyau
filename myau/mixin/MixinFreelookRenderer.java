package myau.mixin;

import myau.FreelookModule;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { EntityRenderer.class })
public abstract class MixinFreelookRenderer {

    @Inject(method = { "func_78467_g" }, at = { @At(value = "HEAD", remap = false) }, remap = false)
    private void freelookBeforeOrientCamera(float f, CallbackInfo callbackInfo) {
        FreelookModule.beginCamera();
    }

    @Inject(method = { "func_78467_g" }, at = { @At(value = "RETURN", remap = false) }, remap = false)
    private void freelookAfterOrientCamera(float f, CallbackInfo callbackInfo) {
        FreelookModule.endCamera();
    }
}
