package myau.mixin;

import myau.JesusModule;
import myau.MoveFlyingEvent;
import myau.MyauClient;
import myau.RotationState;
import myau.mixin.MixinEntity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(value=Side.CLIENT)
@Mixin(value={EntityLivingBase.class})
public abstract class MixinEntityLivingBase
extends MixinEntity {
    @ModifyVariable(method={"jump"}, at=@At(value="STORE"), ordinal=0)
    private float modifyJumpYawRadians(float yawRadians) {
        return (Entity)this instanceof EntityPlayerSP && RotationState.isActive() ? RotationState.movementYaw() * ((float)Math.PI / 180) : yawRadians;
    }

    @Redirect(method={"moveEntityWithHeading"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/EntityLivingBase;moveFlying(FFF)V"))
    private void redirectMoveFlying(EntityLivingBase target, float strafe, float forward, float friction) {
        if ((Entity)this instanceof EntityPlayerSP && MyauClient.eventBus != null) {
            MoveFlyingEvent event = new MoveFlyingEvent(strafe, forward, friction);
            MyauClient.eventBus.post(event);
            strafe = event.getStrafe();
            forward = event.getForward();
            friction = event.getFriction();
            boolean rotationSpoofActive = RotationState.isActive();
            float yaw = this.field_70177_z;
            if (rotationSpoofActive) {
                this.field_70177_z = RotationState.movementYaw();
            }
            target.func_70060_a(strafe, forward, friction);
            if (rotationSpoofActive) {
                this.field_70177_z = yaw;
            }
        } else {
            target.func_70060_a(strafe, forward, friction);
        }
    }

    @ModifyVariable(method={"moveEntityWithHeading"}, name={"f3"}, at=@At(value="STORE"))
    private float modifyDepthStriderLevel(float depthStriderLevel) {
        if ((EntityLivingBase)this instanceof EntityPlayerSP && depthStriderLevel == (float)EnchantmentHelper.func_180318_b((Entity)((EntityLivingBase)this))) {
            if (MyauClient.moduleManager == null) {
                return depthStriderLevel;
            }
            JesusModule jesus = (JesusModule)MyauClient.moduleManager.modules.get(JesusModule.class);
            if (jesus.isEnabled() && (!((Boolean)jesus.groundOnly.getValue()).booleanValue() || this.field_70122_E)) {
                return Math.max(depthStriderLevel, ((Float)jesus.speed.getValue()).floatValue());
            }
        }
        return depthStriderLevel;
    }
}
