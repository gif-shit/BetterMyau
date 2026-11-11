package myau.mixin;

import myau.HotbarChangeEvent;
import myau.MyauClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value=Side.CLIENT)
@Mixin(value={KeyBinding.class})
public abstract class MixinKeyBinding {
    @Final
    @Shadow
    private String field_74515_c;

    @Inject(method={"isPressed"}, at={@At(value="RETURN")}, cancellable=true)
    private void onIsPressed(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (callbackInfo.getReturnValue().booleanValue() && MyauClient.eventBus != null) {
            Minecraft minecraft = Minecraft.func_71410_x();
            for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
                if (!minecraft.field_71474_y.field_151456_ac[hotbarSlot].func_151464_g().equals(this.field_74515_c)) continue;
                HotbarChangeEvent event = new HotbarChangeEvent(hotbarSlot, 0);
                MyauClient.eventBus.post(event);
                if (!event.isCancelled()) continue;
                callbackInfo.setReturnValue(false);
            }
        }
    }
}
