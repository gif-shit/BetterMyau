package myau.mixin;

import myau.FreelookModule;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value=Side.CLIENT)
@Mixin(value={Entity.class})
public abstract class MixinFreelookEntity {
    @Inject(method={"func_70082_c"}, at={@At(value="HEAD", remap=false)}, cancellable=true, remap=false)
    private void freelookSetAngles(float f, float f2, CallbackInfo callbackInfo) {
        if (this instanceof EntityPlayerSP && FreelookModule.handleAngles(f, f2)) {
            callbackInfo.cancel();
        }
    }
}
