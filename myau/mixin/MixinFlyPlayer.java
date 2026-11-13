package myau.mixin;

import myau.FlyModule;
import myau.MyauClient;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityPlayerSP.class)
public abstract class MixinFlyPlayer {

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z"), require = 1)
    private boolean packetGround(EntityPlayerSP player) {
        FlyModule fly = MyauClient.moduleManager == null ? null : (FlyModule) MyauClient.moduleManager.modules.get(FlyModule.class);
        return fly == null ? player.field_70122_E : fly.getPacketGround(player.field_70122_E);
    }
}
