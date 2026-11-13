package myau.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { EntityRenderer.class })
public interface AccessorEntityRenderer {

    @Invoker
    public void callSetupCameraTransform(float var1, int var2);
}
