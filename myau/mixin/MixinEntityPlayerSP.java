package myau.mixin;

import myau.AntiDebuffModule;
import myau.KillAuraModule;
import myau.LivingUpdateEvent;
import myau.MovementInputEvent;
import myau.MyauClient;
import myau.NoSlowModule;
import myau.PostPlayerUpdateEvent;
import myau.PreWalkingUpdateEvent;
import myau.RotationState;
import myau.RotationUpdateEvent;
import myau.RotationUtils;
import myau.mixin.AccessorEntityLivingBase;
import myau.mixin.MixinEntityPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value=Side.CLIENT)
@Mixin(value={EntityPlayerSP.class})
public abstract class MixinEntityPlayerSP
extends MixinEntityPlayer {
    @Unique
    private float spoofedYaw = Float.NaN;
    @Unique
    private float spoofedPitch = Float.NaN;
    @Unique
    private float originalYaw;
    @Unique
    private float originalPitch;
    @Shadow
    private float field_175164_bL;
    @Shadow
    private float field_175165_bM;
    @Shadow
    public float field_71154_f;
    @Shadow
    public float field_71163_h;

    @Inject(method={"onUpdate"}, at={@At(value="HEAD")})
    private void beforePlayerUpdate(CallbackInfo callbackInfo) {
        if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v)) && MyauClient.eventBus != null) {
            RotationUpdateEvent event = new RotationUpdateEvent(this.field_175164_bL, this.field_175165_bM, this.field_70177_z, this.field_70125_A);
            MyauClient.eventBus.post(event);
            RotationState.update(event.isModified() && !this.func_70115_ae(), event.getYaw(), event.getPitch(), event.getMovementYaw(), event.getMovementPriority());
            if (event.isModified()) {
                this.originalYaw = this.field_70177_z;
                this.originalPitch = this.field_70125_A;
                this.spoofedYaw = event.getYaw();
                this.spoofedPitch = event.getPitch();
            } else {
                this.originalYaw = Float.NaN;
                this.originalPitch = Float.NaN;
                this.spoofedYaw = Float.NaN;
                this.spoofedPitch = Float.NaN;
            }
        }
    }

    @Inject(method={"onUpdate"}, at={@At(value="RETURN")})
    private void afterPlayerUpdate(CallbackInfo callbackInfo) {
        if (this.field_70170_p.func_175667_e(new BlockPos(this.field_70165_t, 0.0, this.field_70161_v))) {
            if (!Float.isNaN(this.originalYaw) && !Float.isNaN(this.originalPitch)) {
                this.field_175164_bL = this.field_70177_z;
                this.field_175165_bM = this.field_70125_A;
                this.field_70177_z = RotationUtils.nearestEquivalentAngle(this.originalYaw, this.field_70177_z);
                this.field_70125_A = this.originalPitch;
                this.field_70126_B = this.field_70177_z;
                this.field_70127_C = this.field_70125_A;
                this.field_71163_h = this.field_70177_z - (this.field_71154_f - this.field_71163_h) * 2.0f;
                this.field_71154_f = this.field_70177_z;
            }
            if (MyauClient.eventBus != null) {
                MyauClient.eventBus.post(new PostPlayerUpdateEvent());
            }
        }
    }

    @Redirect(method={"onUpdate"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/entity/EntityPlayerSP;isRiding()Z"))
    private boolean applySpoofedRotationBeforeRidingCheck(EntityPlayerSP player) {
        if (!Float.isNaN(this.spoofedYaw) && !Float.isNaN(this.spoofedPitch)) {
            this.field_70177_z = this.spoofedYaw;
            this.field_70125_A = this.spoofedPitch;
        }
        return player.func_70115_ae();
    }

    @Inject(method={"onUpdate"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V")})
    private void beforeWalkingPlayerUpdate(CallbackInfo callbackInfo) {
        if (MyauClient.eventBus != null) {
            MyauClient.eventBus.post(new PreWalkingUpdateEvent());
        }
    }

    @Inject(method={"onLivingUpdate"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V")})
    private void beforeLivingUpdate(CallbackInfo callbackInfo) {
        if (MyauClient.eventBus != null) {
            MyauClient.eventBus.post(new LivingUpdateEvent());
        }
    }

    @Inject(method={"onLivingUpdate"}, at={@At(value="INVOKE", target="Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V", shift=At.Shift.AFTER)})
    private void afterMovementInputUpdate(CallbackInfo callbackInfo) {
        if (MyauClient.eventBus != null) {
            MyauClient.eventBus.post(new MovementInputEvent());
        }
    }

    @Redirect(method={"onLivingUpdate"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"))
    private boolean redirectUsingItemForNoSlow(EntityPlayerSP player) {
        KillAuraModule killAura = (KillAuraModule)MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (killAura.isAutoBlockActive() && ((Boolean)killAura.autoBlockNoSlow.getValue()).booleanValue()) {
            return false;
        }
        NoSlowModule noSlow = (NoSlowModule)MyauClient.moduleManager.modules.get(NoSlowModule.class);
        return noSlow.isEnabled() && noSlow.shouldSuppressItemSlowdown() ? false : player.func_71039_bw();
    }

    @Redirect(method={"onLivingUpdate"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectNauseaCheck(EntityPlayerSP player, Potion potion) {
        AntiDebuffModule antiDebuff;
        if (potion == Potion.field_76431_k && MyauClient.moduleManager != null && (antiDebuff = (AntiDebuffModule)MyauClient.moduleManager.modules.get(AntiDebuffModule.class)).isEnabled() && ((Boolean)antiDebuff.nausea.getValue()).booleanValue()) {
            return false;
        }
        return ((AccessorEntityLivingBase)player).getActivePotionsMap().containsKey(potion.field_76415_H);
    }
}
