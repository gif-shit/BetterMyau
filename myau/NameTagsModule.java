package myau;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.BooleanSetting;
import myau.ChatColor;
import myau.ColorUtils;
import myau.EntityUtils;
import myau.EventHandler;
import myau.FloatSetting;
import myau.MathUtils;
import myau.ModeSetting;
import myau.Module;
import myau.MyauClient;
import myau.PercentSetting;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

public class NameTagsModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat heartFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    public final FloatSetting scale = new FloatSetting("scale", Float.valueOf(1.0f), Float.valueOf(0.5f), Float.valueOf(2.0f));
    public final BooleanSetting autoScale = new BooleanSetting("auto-scale", true);
    public final PercentSetting background = new PercentSetting("background", 25);
    public final BooleanSetting shadow = new BooleanSetting("shadow", true);
    public final ModeSetting distance = new ModeSetting("distance", 0, new String[]{"NONE", "DEFAULT", "VAPE"});
    public final ModeSetting health = new ModeSetting("health", 2, new String[]{"NONE", "HP", "HEARTS", "TAB"});
    public final BooleanSetting armor = new BooleanSetting("armor", true);
    public final BooleanSetting effects = new BooleanSetting("effects", true);
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

    public NameTagsModule() {
        super("NameTags", false, 0);
    }

    public boolean shouldRenderNameTag(EntityLivingBase entity) {
        if (entity.field_70725_aQ > 0 || minecraft.func_175606_aa().func_70032_d((Entity)entity) > 512.0f) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            if (EntityUtils.isLocalOrViewPlayer(player)) {
                return (Boolean)this.self.getValue() != false && NameTagsModule.minecraft.field_71474_y.field_74320_O != 0;
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

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        for (Entity entity : EntityUtils.entitiesBackToFront()) {
            String displayName;
            if (!(entity instanceof EntityLivingBase) || !this.shouldRenderNameTag((EntityLivingBase)entity) || !entity.field_70158_ak && !RenderUtils.isEntityInFrustum(entity, 10.0) || StringUtils.isBlank((CharSequence)EnumChatFormatting.func_110646_a((String)(displayName = EntityUtils.formattedName(entity))))) continue;
            double x = MathUtils.lerpDouble(entity.field_70142_S, entity.field_70165_t, event.getPartialTicks()) - ((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosX();
            double y = MathUtils.lerpDouble(entity.field_70137_T, entity.field_70163_u, event.getPartialTicks()) - ((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosY() + (double)entity.func_70047_e();
            double z = MathUtils.lerpDouble(entity.field_70136_U, entity.field_70161_v, event.getPartialTicks()) - ((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosZ();
            double entityDistance = minecraft.func_175606_aa().func_70032_d(entity);
            GlStateManager.func_179094_E();
            GlStateManager.func_179137_b((double)x, (double)(y + (entity.func_70093_af() ? 0.225 : 0.4)), (double)z);
            float rotation = NameTagsModule.minecraft.func_175598_ae().field_78735_i * -1.0f;
            float axisX = 0.0f;
            GlStateManager.func_179114_b((float)rotation, (float)0.0f, (float)1.0f, (float)0.0f);
            rotation = NameTagsModule.minecraft.func_175598_ae().field_78732_j;
            axisX = NameTagsModule.minecraft.field_71474_y.field_74320_O == 2 ? -1.0f : 1.0f;
            GlStateManager.func_179114_b((float)rotation, (float)axisX, (float)0.0f, (float)0.0f);
            double labelScale = Math.pow(Math.min(Math.max((Boolean)this.autoScale.getValue() != false ? entityDistance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
            GlStateManager.func_179139_a((double)(-labelScale * (double)((Float)this.scale.getValue()).floatValue()), (double)(-labelScale * (double)((Float)this.scale.getValue()).floatValue()), (double)1.0);
            String distancePrefix = "";
            switch ((Integer)this.distance.getValue()) {
                case 1: {
                    distancePrefix = String.format("&7%dm&r ", (int)entityDistance);
                    break;
                }
                case 2: {
                    distancePrefix = String.format("&a[&f%d&a]&r ", (int)entityDistance);
                }
            }
            float currentHealth = ((EntityLivingBase)entity).func_110143_aJ();
            float absorption = ((EntityLivingBase)entity).func_110139_bj();
            float maxHealth = ((EntityLivingBase)entity).func_110138_aP();
            float healthFraction = Math.min(Math.max((currentHealth + absorption) / maxHealth, 0.0f), 1.0f);
            String healthSuffix = "";
            switch ((Integer)this.health.getValue()) {
                case 1: {
                    healthSuffix = String.format(" %d%s", (int)currentHealth, absorption > 0.0f ? String.format(" &6%d&r", (int)absorption) : "&r");
                    break;
                }
                case 2: {
                    String hearts = heartFormat.format((double)currentHealth / 2.0);
                    String absorptionHearts = absorption > 0.0f ? String.format(" &6%s&r", heartFormat.format((double)absorption / 2.0)) : "&r";
                    healthSuffix = String.format(" %s%s", hearts, absorptionHearts);
                    break;
                }
                case 3: {
                    Score score;
                    ScoreObjective objective;
                    Scoreboard scoreboard;
                    if (!(entity instanceof EntityPlayer) || (scoreboard = NameTagsModule.minecraft.field_71441_e.func_96441_U()) == null || (objective = scoreboard.func_96539_a(2)) == null || (score = scoreboard.func_96529_a(entity.func_70005_c_(), objective)) == null) break;
                    healthSuffix = String.format(" &e%d&r", score.func_96652_c());
                }
            }
            String label = ChatColor.formatText(String.format("%s&f%s&r%s", distancePrefix, displayName, healthSuffix));
            int width = NameTagsModule.minecraft.field_71466_p.func_78256_a(label);
            if ((Integer)this.background.getValue() > 0) {
                Color backgroundColor = !entity.func_70093_af() && !entity.func_82150_aj() ? new Color(0.0f, 0.0f, 0.0f, (float)((Integer)this.background.getValue()).intValue() / 100.0f) : new Color(0.33f, 0.0f, 0.33f, (float)((Integer)this.background.getValue()).intValue() / 100.0f);
                RenderUtils.beginOverlay();
                RenderUtils.fillRect((float)(-width) / 2.0f - 1.0f, (float)(-NameTagsModule.minecraft.field_71466_p.field_78288_b) - 1.0f, (float)width / 2.0f + ((Boolean)this.shadow.getValue() != false ? 1.0f : 0.0f), (Boolean)this.shadow.getValue() != false ? 0.0f : -1.0f, backgroundColor.getRGB());
                RenderUtils.endOverlay();
            }
            GlStateManager.func_179097_i();
            NameTagsModule.minecraft.field_71466_p.func_175065_a(label, (float)(-width) / 2.0f, (float)(-NameTagsModule.minecraft.field_71466_p.field_78288_b), ColorUtils.healthGradient(healthFraction).getRGB(), ((Boolean)this.shadow.getValue()).booleanValue());
            GlStateManager.func_179126_j();
            if (entity instanceof EntityPlayer) {
                this.drawPlayerDetails((EntityPlayer)entity, width);
            }
            GlStateManager.func_179121_F();
        }
    }

    private void drawPlayerDetails(EntityPlayer player, int labelWidth) {
        List visibleEffects;
        int left;
        int rowOffset = NameTagsModule.minecraft.field_71466_p.field_78288_b + 2;
        if (((Boolean)this.armor.getValue()).booleanValue()) {
            ArrayList<ItemStack> equipment = new ArrayList<ItemStack>();
            for (int slot = 4; slot >= 0; --slot) {
                ItemStack stack;
                ItemStack itemStack = stack = slot == 0 ? player.func_70694_bm() : player.field_71071_by.field_70460_b[slot - 1];
                if (stack == null) continue;
                equipment.add(stack);
            }
            if (!equipment.isEmpty()) {
                left = equipment.size() * -8;
                for (int index = 0; index < equipment.size(); ++index) {
                    RenderUtils.drawItemWithEnchantments((ItemStack)equipment.get(index), left + index * 16, -rowOffset - 16);
                }
                rowOffset += 16;
            }
        }
        if (((Boolean)this.effects.getValue()).booleanValue() && !(visibleEffects = player.func_70651_bq().stream().filter(NameTagsModule::hasStatusIcon).collect(Collectors.toList())).isEmpty()) {
            GlStateManager.func_179094_E();
            GlStateManager.func_179152_a((float)0.5f, (float)0.5f, (float)1.0f);
            left = visibleEffects.size() * -9;
            for (int index = 0; index < visibleEffects.size(); ++index) {
                RenderUtils.drawPotionIcon((PotionEffect)visibleEffects.get(index), left + index * 18, -(rowOffset * 2) - 18);
            }
            GlStateManager.func_179121_F();
        }
        if (EntityUtils.isFriend(player)) {
            RenderUtils.beginOverlay();
            RenderUtils.drawBorderedRect((float)(-labelWidth) / 2.0f - 1.0f, (float)(-NameTagsModule.minecraft.field_71466_p.field_78288_b) - 1.0f, (float)labelWidth / 2.0f + 1.0f, (Boolean)this.shadow.getValue() != false ? 0.0f : -1.0f, 1.5f, 0, MyauClient.friendList.color().getRGB());
            RenderUtils.endOverlay();
        } else if (EntityUtils.isEnemy(player)) {
            RenderUtils.beginOverlay();
            RenderUtils.drawBorderedRect((float)(-labelWidth) / 2.0f - 1.0f, (float)(-NameTagsModule.minecraft.field_71466_p.field_78288_b) - 1.0f, (float)labelWidth / 2.0f + 1.0f, (Boolean)this.shadow.getValue() != false ? 0.0f : -1.0f, 1.5f, 0, MyauClient.enemyList.color().getRGB());
            RenderUtils.endOverlay();
        }
    }

    private static boolean hasStatusIcon(PotionEffect effect) {
        return Potion.field_76425_a[effect.func_76456_a()].func_76400_d();
    }
}
