package myau.mixin;

import myau.RotationState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value=Side.CLIENT)
@Mixin(value={RenderManager.class})
public abstract class MixinRenderManager {
    @Unique
    private float originalPrevRenderYawOffset;
    @Unique
    private float originalRenderYawOffset;
    @Unique
    private float originalPrevYawHead;
    @Unique
    private float originalYawHead;
    @Unique
    private float originalPrevPitch;
    @Unique
    private float originalPitch;

    @Inject(method={"renderEntityStatic"}, at={@At(value="HEAD")})
    private void beforeRenderEntityStatic(Entity entity, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (entity instanceof EntityPlayerSP && RotationState.isActiveWithinTicks(1)) {
            EntityPlayerSP player = (EntityPlayerSP)entity;
            this.originalPrevRenderYawOffset = player.field_70760_ar;
            this.originalRenderYawOffset = player.field_70761_aq;
            this.originalPrevYawHead = player.field_70758_at;
            this.originalYawHead = player.field_70759_as;
            this.originalPrevPitch = player.field_70127_C;
            this.originalPitch = player.field_70125_A;
            player.field_70760_ar = RotationState.previousBodyYaw();
            player.field_70761_aq = RotationState.bodyYaw();
            player.field_70758_at = RotationState.previousHeadYaw();
            player.field_70759_as = RotationState.headYaw();
            player.field_70127_C = RotationState.previousPitch();
            player.field_70125_A = RotationState.pitch();
        }
    }

    @Inject(method={"renderEntityStatic"}, at={@At(value="RETURN")})
    private void afterRenderEntityStatic(Entity entity, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (entity instanceof EntityPlayerSP && RotationState.isActiveWithinTicks(1)) {
            EntityPlayerSP player = (EntityPlayerSP)entity;
            player.field_70760_ar = this.originalPrevRenderYawOffset;
            player.field_70761_aq = this.originalRenderYawOffset;
            player.field_70758_at = this.originalPrevYawHead;
            player.field_70759_as = this.originalYawHead;
            player.field_70127_C = this.originalPrevPitch;
            player.field_70125_A = this.originalPitch;
        }
    }
}
