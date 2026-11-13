package myau.mixin;

import java.util.List;
import myau.AntiDebuffModule;
import myau.BlockReachEvent;
import myau.EntityReachEvent;
import myau.GhostHandModule;
import myau.KillAuraModule;
import myau.MutableValue;
import myau.MyauClient;
import myau.NoHurtCamModule;
import myau.ScaffoldModule;
import myau.ViewClipModule;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { EntityRenderer.class })
public abstract class MixinEntityRenderer {

    @Unique
    private MutableValue<Integer> spoofedHotbarSlot = null;

    @Unique
    private MutableValue<ItemStack> previousItemInUse = null;

    @Unique
    private MutableValue<Integer> previousItemUseCount = null;

    @Shadow
    private Minecraft field_78531_r;

    @Shadow
    private float field_78490_B;

    @Inject(method = { "updateCameraAndRender" }, at = { @At(value = "HEAD") })
    private void beforeUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
        if (this.field_78531_r.field_71439_g != null) {
            KillAuraModule killAura;
            int spoofedSlot;
            ScaffoldModule scaffold = (ScaffoldModule) MyauClient.moduleManager.modules.get(ScaffoldModule.class);
            if (scaffold.isEnabled() && scaffold.itemSpoof.getValue() && (spoofedSlot = scaffold.getPreviousHotbarSlot()) >= 0) {
                this.spoofedHotbarSlot = new MutableValue<Integer>(this.field_78531_r.field_71439_g.field_71071_by.field_70461_c);
                this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = spoofedSlot;
            }
            if ((killAura = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class)).isEnabled()
                    && killAura.shouldRenderBlocking()) {
                this.previousItemInUse = new MutableValue<ItemStack>(((AccessorEntityPlayer) this.field_78531_r.field_71439_g).getItemInUse());
                ((AccessorEntityPlayer) this.field_78531_r.field_71439_g).setItemInUse(this.field_78531_r.field_71439_g.field_71071_by.func_70448_g());
                this.previousItemUseCount = new MutableValue<Integer>(((AccessorEntityPlayer) this.field_78531_r.field_71439_g).getItemInUseCount());
                ((AccessorEntityPlayer) this.field_78531_r.field_71439_g).setItemInUseCount(69000);
            }
        }
    }

    @Inject(method = { "updateCameraAndRender" }, at = { @At(value = "RETURN") })
    private void afterUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
        if (this.spoofedHotbarSlot != null) {
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = (Integer) this.spoofedHotbarSlot.value;
            this.spoofedHotbarSlot = null;
        }
        if (this.previousItemInUse != null) {
            ((AccessorEntityPlayer) this.field_78531_r.field_71439_g).setItemInUse((ItemStack) this.previousItemInUse.value);
            this.previousItemInUse = null;
        }
        if (this.previousItemUseCount != null) {
            ((AccessorEntityPlayer) this.field_78531_r.field_71439_g).setItemInUseCount((Integer) this.previousItemUseCount.value);
            this.previousItemUseCount = null;
        }
    }

    @Inject(method = { "updateRenderer" }, at = { @At(value = "HEAD") })
    private void beforeUpdateRenderer(CallbackInfo callbackInfo) {
        int spoofedSlot;
        ScaffoldModule scaffold = (ScaffoldModule) MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue() && (spoofedSlot = scaffold.getPreviousHotbarSlot()) >= 0) {
            this.spoofedHotbarSlot = new MutableValue<Integer>(this.field_78531_r.field_71439_g.field_71071_by.field_70461_c);
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = spoofedSlot;
        }
    }

    @Inject(method = { "updateRenderer" }, at = { @At(value = "RETURN") })
    private void afterUpdateRenderer(CallbackInfo callbackInfo) {
        if (this.spoofedHotbarSlot != null) {
            this.field_78531_r.field_71439_g.field_71071_by.field_70461_c = (Integer) this.spoofedHotbarSlot.value;
            this.spoofedHotbarSlot = null;
        }
    }

    @ModifyConstant(method = { "hurtCameraEffect" }, constant = { @Constant(floatValue = 14.0f, ordinal = 0) })
    private float modifyHurtCameraStrength(float strength) {
        if (MyauClient.moduleManager == null) {
            return strength;
        }
        NoHurtCamModule noHurtCam = (NoHurtCamModule) MyauClient.moduleManager.modules.get(NoHurtCamModule.class);
        return noHurtCam.isEnabled() ? strength * (float) ((Integer) noHurtCam.multiplier.getValue()).intValue() / 100.0f : strength;
    }

    @ModifyConstant(method = { "getMouseOver" }, constant = { @Constant(doubleValue = 3.0, ordinal = 1) })
    private double modifyEntityReach(double reachDistance) {
        if (MyauClient.eventBus != null) {
            EntityReachEvent event = new EntityReachEvent(reachDistance);
            MyauClient.eventBus.post(event);
            return event.getReachDistance();
        }
        return reachDistance;
    }

    @ModifyVariable(method = { "getMouseOver" }, at = @At(value = "STORE"), name = { "d0" })
    private double modifyBlockReach(double reachDistance) {
        if (MyauClient.eventBus != null) {
            BlockReachEvent blockReachEvent = new BlockReachEvent(reachDistance);
            MyauClient.eventBus.post(blockReachEvent);
            return blockReachEvent.getReachDistance();
        }
        return reachDistance;
    }

    @Redirect(method = { "getMouseOver" }, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0, remap = false))
    private int filterMouseOverEntities(List<Entity> entities) {
        GhostHandModule ghostHand;
        if (MyauClient.moduleManager != null
                && (ghostHand = (GhostHandModule) MyauClient.moduleManager.modules.get(GhostHandModule.class)).isEnabled()) {
            entities.removeIf(ghostHand::shouldIgnoreEntity);
        }
        return entities.size();
    }

    @Redirect(method = { "orientCamera" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    private double redirectOrientCamera(Vec3 cameraPosition, Vec3 collisionPosition) {
        if (MyauClient.moduleManager == null) {
            return cameraPosition.func_72438_d(collisionPosition);
        }
        ViewClipModule viewClip = (ViewClipModule) MyauClient.moduleManager.modules.get(ViewClipModule.class);
        return viewClip.isEnabled() ? (double) this.field_78490_B : cameraPosition.func_72438_d(collisionPosition);
    }

    @Redirect(method = { "setupFog" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"))
    private Material redirectCameraBlockMaterial(Block block) {
        if (MyauClient.moduleManager == null) {
            return block.func_149688_o();
        }
        ViewClipModule viewClip = (ViewClipModule) MyauClient.moduleManager.modules.get(ViewClipModule.class);
        return viewClip.isEnabled() ? Material.field_151579_a : block.func_149688_o();
    }

    @Redirect(method = { "updateFogColor" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectBlindnessForFogColor(EntityLivingBase target, Potion potion) {
        AntiDebuffModule antiDebuff;
        if (potion == Potion.field_76440_q
                && MyauClient.moduleManager != null
                && (antiDebuff = (AntiDebuffModule) MyauClient.moduleManager.modules.get(AntiDebuffModule.class)).isEnabled()
                && antiDebuff.blindness.getValue()) {
            return false;
        }
        return ((AccessorEntityLivingBase) target).getActivePotionsMap().containsKey(potion.field_76415_H);
    }

    @Redirect(method = { "setupFog" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectBlindnessForFogSetup(EntityLivingBase livingEntity, Potion potion) {
        AntiDebuffModule antiDebuff;
        if (potion == Potion.field_76440_q
                && MyauClient.moduleManager != null
                && (antiDebuff = (AntiDebuffModule) MyauClient.moduleManager.modules.get(AntiDebuffModule.class)).isEnabled()
                && antiDebuff.blindness.getValue()) {
            return false;
        }
        return ((AccessorEntityLivingBase) livingEntity).getActivePotionsMap().containsKey(potion.field_76415_H);
    }

    @Redirect(method = { "setupCameraTransform" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectNauseaCheck(EntityPlayerSP player, Potion potion) {
        AntiDebuffModule antiDebuff;
        if (potion == Potion.field_76431_k
                && MyauClient.moduleManager != null
                && (antiDebuff = (AntiDebuffModule) MyauClient.moduleManager.modules.get(AntiDebuffModule.class)).isEnabled()
                && antiDebuff.nausea.getValue()) {
            return false;
        }
        return ((AccessorEntityLivingBase) player).getActivePotionsMap().containsKey(potion.field_76415_H);
    }
}
