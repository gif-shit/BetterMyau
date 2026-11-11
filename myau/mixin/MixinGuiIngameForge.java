package myau.mixin;

import myau.MyauClient;
import myau.NickHiderModule;
import myau.RenderOverlayEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value=Side.CLIENT)
@Mixin(value={GuiIngameForge.class}, remap=false)
public abstract class MixinGuiIngameForge {
    @Inject(method={"renderGameOverlay(F)V", "func_175180_a(F)V"}, at={@At(value="INVOKE", target="Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V", shift=At.Shift.AFTER, remap=false)}, remap=false)
    private void afterRenderTitle(float partialTicks, CallbackInfo callbackInfo) {
        if (MyauClient.eventBus != null) {
            MyauClient.eventBus.post(new RenderOverlayEvent(partialTicks));
        }
    }

    @Redirect(method={"renderExperience(II)V"}, at=@At(value="FIELD", target="Lnet/minecraft/client/entity/EntityPlayerSP;experience:F", remap=true), remap=false)
    private float redirectRenderExperience(EntityPlayerSP player) {
        if (MyauClient.moduleManager == null) {
            return player.field_71106_cc;
        }
        NickHiderModule nickHider = (NickHiderModule)MyauClient.moduleManager.modules.get(NickHiderModule.class);
        return nickHider.isEnabled() && (Boolean)nickHider.level.getValue() != false ? 0.0f : player.field_71106_cc;
    }

    @Redirect(method={"renderExperience(II)V"}, at=@At(value="FIELD", target="Lnet/minecraft/client/entity/EntityPlayerSP;experienceLevel:I", remap=true), remap=false)
    private int redirectExperienceLevel(EntityPlayerSP player) {
        if (MyauClient.moduleManager == null) {
            return player.field_71068_ca;
        }
        NickHiderModule nickHider = (NickHiderModule)MyauClient.moduleManager.modules.get(NickHiderModule.class);
        return nickHider.isEnabled() && (Boolean)nickHider.level.getValue() != false ? 0 : player.field_71068_ca;
    }
}
