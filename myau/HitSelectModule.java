package myau;

import java.util.HashMap;
import java.util.Map;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.IntegerSetting;
import myau.LeftClickEvent;
import myau.Module;
import myau.PlayerUtils;
import myau.PreTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

public class HitSelectModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    Map<EntityLivingBase, Integer> remainingHurtTicks = new HashMap<EntityLivingBase, Integer>();
    Map<EntityLivingBase, Float> previousAttackDamage = new HashMap<EntityLivingBase, Float>();
    public final IntegerSetting hurtTime = new IntegerSetting("hurt-time", 4, 0, 10);
    public final BooleanSetting swing = new BooleanSetting("swing", true);

    public HitSelectModule() {
        super("HitSelect", false, 0);
    }

    public boolean shouldDelayAttack(EntityLivingBase target) {
        float attackDamage = PlayerUtils.estimateAttackDamage((Entity)target);
        int remaining = this.remainingHurtTicks.getOrDefault(target, 0);
        if (remaining <= 0) {
            this.remainingHurtTicks.put(target, target.field_70737_aN);
            this.previousAttackDamage.put(target, Float.valueOf(attackDamage));
            return false;
        }
        remaining = this.remainingHurtTicks.getOrDefault(target, 0);
        if (remaining <= (Integer)this.hurtTime.getValue()) {
            return false;
        }
        if (this.previousAttackDamage.getOrDefault(target, Float.valueOf(0.0f)).floatValue() > attackDamage) {
            this.previousAttackDamage.put(target, Float.valueOf(attackDamage));
            return false;
        }
        if (HitSelectModule.minecraft.field_71439_g.field_70737_aN <= 0 && HitSelectModule.minecraft.field_71439_g.func_70051_ag()) {
            if (((Boolean)this.swing.getValue()).booleanValue()) {
                PlayerUtils.swingLocally();
            }
            return true;
        }
        return false;
    }

    @EventHandler(priority=-100)
    public void onLeftClick(LeftClickEvent event) {
        boolean enabled = this.isEnabled();
        if (!enabled || event.isCancelled()) {
            return;
        }
        if (HitSelectModule.minecraft.field_71476_x == null) {
            return;
        }
        if (HitSelectModule.minecraft.field_71476_x.field_72313_a != MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }
        if (!(HitSelectModule.minecraft.field_71476_x.field_72308_g instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase target = (EntityLivingBase)HitSelectModule.minecraft.field_71476_x.field_72308_g;
        if (this.shouldDelayAttack(target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        this.remainingHurtTicks.replaceAll(HitSelectModule::decrementHurtTicks);
    }

    private static Integer decrementHurtTicks(EntityLivingBase target, Integer remaining) {
        if (remaining > 0) {
            return remaining - 1;
        }
        return remaining;
    }

    @Override
    public void onDisable() {
        this.remainingHurtTicks.clear();
        this.previousAttackDamage.clear();
    }
}
