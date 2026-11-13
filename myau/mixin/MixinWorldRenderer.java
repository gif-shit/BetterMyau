package myau.mixin;

import java.nio.IntBuffer;
import myau.MyauClient;
import myau.XrayModule;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { WorldRenderer.class })
public abstract class MixinWorldRenderer {

    @Redirect(method = { "putColorMultiplier" }, at = @At(value = "INVOKE", target = "Ljava/nio/IntBuffer;put(II)Ljava/nio/IntBuffer;", remap = false))
    private IntBuffer redirectColorBufferWrite(IntBuffer intBuffer, int index, int packedColor) {
        if (MyauClient.moduleManager == null) {
            return intBuffer.put(index, packedColor);
        }
        XrayModule xray = (XrayModule) MyauClient.moduleManager.modules.get(XrayModule.class);
        return xray.isEnabled() ? intBuffer.put(index, packedColor & 0xFFFFFF | (int) ((float) xray.opacity.getValue() * 255.0f / 100.0f) << 24) : intBuffer.put(index, packedColor);
    }
}
