package myau;

import myau.BooleanSetting;
import myau.EntityUtils;
import myau.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;

public class ChamsModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final BooleanSetting players = new BooleanSetting("players", true);
    public final BooleanSetting friends = new BooleanSetting("friends", true);
    public final BooleanSetting enemies = new BooleanSetting("enemies", true);
    public final BooleanSetting bosses = new BooleanSetting("bosses", false);
    public final BooleanSetting mobs = new BooleanSetting("mobs", false);
    public final BooleanSetting creepers = new BooleanSetting("creepers", false);
    public final BooleanSetting endermen = new BooleanSetting("endermen", false);
    public final BooleanSetting blazes = new BooleanSetting("blazes", false);
    public final BooleanSetting animals = new BooleanSetting("animals", false);
    public final BooleanSetting self = new BooleanSetting("self", false);
    public final BooleanSetting bots = new BooleanSetting("bots", false);

    public ChamsModule() {
        super("Chams", false, 0);
    }

    public boolean shouldRenderEntity(EntityLivingBase entity) {
        int deathTime = entity.field_70725_aQ;
        if (deathTime > 0) {
            return false;
        }
        if (minecraft.func_175606_aa().func_70032_d((Entity)entity) > 512.0f) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            if (EntityUtils.isLocalOrViewPlayer(player)) {
                return (Boolean)this.self.getValue() != false && ChamsModule.minecraft.field_71474_y.field_74320_O != 0;
            }
            if (EntityUtils.isFriend(player)) {
                return (Boolean)this.friends.getValue();
            }
            if (EntityUtils.isEnemy(player)) {
                return (Boolean)this.enemies.getValue();
            }
            if (EntityUtils.isBot(player)) {
                return (Boolean)this.bots.getValue();
            }
            return (Boolean)this.players.getValue();
        }
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return !entity.func_82150_aj() && (Boolean)this.bosses.getValue() != false;
        }
        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntityCreeper) {
                return (Boolean)this.creepers.getValue();
            }
            if (entity instanceof EntityEnderman) {
                return (Boolean)this.endermen.getValue();
            }
            if (entity instanceof EntityBlaze) {
                return (Boolean)this.blazes.getValue();
            }
            return (Boolean)this.mobs.getValue();
        }
        if (entity instanceof EntityAnimal || entity instanceof EntityBat || entity instanceof EntitySquid || entity instanceof EntityVillager) {
            return (Boolean)this.animals.getValue();
        }
        return false;
    }
}
