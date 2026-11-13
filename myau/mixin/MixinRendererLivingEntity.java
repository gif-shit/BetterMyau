package myau.mixin;

import myau.ChamsModule;
import myau.ESPModule;
import myau.MyauClient;
import myau.NameTagsModule;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { RendererLivingEntity.class }, priority = 991)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {

    @Unique
    private boolean chamsPolygonOffsetActive = false;

    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = { "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V" }, at = { @At(value = "HEAD") })
    private void beforeDoRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo callbackInfo) {
        ChamsModule chams;
        if (MyauClient.moduleManager != null
                && (chams = (ChamsModule) MyauClient.moduleManager.modules.get(ChamsModule.class)).isEnabled()
                && chams.shouldRenderEntity((EntityLivingBase) entity)) {
            GL11.glEnable((int) 32823);
            GL11.glPolygonOffset((float) 1.0f, (float) -2500000.0f);
            this.chamsPolygonOffsetActive = true;
        }
    }

    @Inject(method = { "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V" }, at = { @At(value = "RETURN") })
    private void afterDoRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo callbackInfo) {
        if (this.chamsPolygonOffsetActive) {
            GL11.glPolygonOffset((float) 1.0f, (float) 2500000.0f);
            GL11.glDisable((int) 32823);
            this.chamsPolygonOffsetActive = false;
        }
    }

    @Inject(method = { "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z" }, at = { @At(value = "HEAD") }, cancellable = true)
    private void onCanRenderName(T entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (MyauClient.moduleManager != null) {
            NameTagsModule nameTags = (NameTagsModule) MyauClient.moduleManager.modules.get(NameTagsModule.class);
            if (nameTags.isEnabled() && nameTags.shouldRenderNameTag((EntityLivingBase) entity)) {
                callbackInfo.setReturnValue(false);
            } else {
                ESPModule eSP = (ESPModule) MyauClient.moduleManager.modules.get(ESPModule.class);
                if (eSP.isEnabled() && !eSP.areNameTagsAllowed()) {
                    callbackInfo.setReturnValue(false);
                }
            }
        }
    }
}
