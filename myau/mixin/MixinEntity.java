package myau.mixin;

import myau.MyauClient;
import myau.SafeWalkEvent;
import myau.VelocityEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value=Side.CLIENT)
@Mixin(value={Entity.class})
public abstract class MixinEntity {
    @Shadow
    public World field_70170_p;
    @Shadow
    public double field_70165_t;
    @Shadow
    public double field_70163_u;
    @Shadow
    public double field_70161_v;
    @Shadow
    public double field_70159_w;
    @Shadow
    public double field_70181_x;
    @Shadow
    public double field_70179_y;
    @Shadow
    public float field_70177_z;
    @Shadow
    public float field_70125_A;
    @Shadow
    public float field_70126_B;
    @Shadow
    public float field_70127_C;
    @Shadow
    public boolean field_70122_E;

    @Shadow
    public boolean func_70115_ae() {
        return false;
    }

    @Inject(method={"setVelocity"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeSetVelocity(double velocityX, double velocityY, double velocityZ, CallbackInfo callbackInfo) {
        if ((Entity)this instanceof EntityPlayerSP && MyauClient.eventBus != null) {
            VelocityEvent event = new VelocityEvent(velocityX, velocityY, velocityZ);
            MyauClient.eventBus.post(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                this.field_70159_w = event.getVelocityX();
                this.field_70181_x = event.getVelocityY();
                this.field_70179_y = event.getVelocityZ();
            }
        }
    }

    @Inject(method={"setAngles"}, at={@At(value="HEAD")}, cancellable=true)
    private void beforeSetAngles(CallbackInfo callbackInfo) {
        if ((Entity)this instanceof EntityPlayerSP && MyauClient.rotationSmoother != null && MyauClient.rotationSmoother.isRequestActive()) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(method={"moveEntity"}, ordinal=0, at=@At(value="STORE"), name={"flag"})
    private boolean modifySafeWalkFlag(boolean safeWalk) {
        if ((Entity)this instanceof EntityPlayerSP && MyauClient.eventBus != null) {
            SafeWalkEvent safeWalkEvent = new SafeWalkEvent(safeWalk);
            MyauClient.eventBus.post(safeWalkEvent);
            return safeWalkEvent.isSafeWalk();
        }
        return safeWalk;
    }
}
