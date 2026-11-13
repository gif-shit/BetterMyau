package myau.openb4.module.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.ModeSetting;
import myau.openb4.client.BotCheck;
import myau.openb4.client.Chat;
import myau.openb4.client.Hypixel;
import myau.openb4.module.Module;
import myau.openb4.util.RenderUtils;
import myau.openb4.util.Strings;
import myau.openb4.util.Theme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class DamageTagsModule extends Module {

    private static final Random RANDOM = new Random();
    public static final FloatSetting length = new FloatSetting("length", 1.0f, 0.3f, 5.0f, 0.1f);
    public static final FloatSetting scale = new FloatSetting("scale", 1.0f, 0.3f, 3.0f, 0.1f);
    public static final ModeSetting themeSetting = new ModeSetting("theme", 0, new String[] { "DEFAULT", "RAINBOW", "AURORA", "CHERRY", "COTTON CANDY", "FLARE", "FLOWER", "FOREST", "FROST", "GOLD", "GRAYSCALE", "INFERNO", "ROYAL", "SANDSTORM", "SKY", "VINE" });
    public static final BooleanSetting showHeal = new BooleanSetting("show-heal", true);
    public static final BooleanSetting showHurt = new BooleanSetting("show-hurt", true);
    public static final BooleanSetting showInvis = new BooleanSetting("show-invis", true);
    public static final BooleanSetting showMobs = new BooleanSetting("show-mobs", true);
    public static final BooleanSetting traditionalHealthColor = new BooleanSetting("traditional-health-color", true);
    public static Theme theme = Theme.DEFAULT;
    public static final BooleanSetting healthAsHearts = new BooleanSetting("health-as-hearts", false);
    public static final BooleanSetting heartSymbol = new BooleanSetting("heart-symbol", false);
    private long lifetime = 1000L;
    private final long fadeTime = 200L;
    private final Map<Integer, Float> lastHealth = new HashMap<Integer, Float>();
    private final List<Tag> tags = new ArrayList<Tag>();

    public DamageTagsModule(int key) {
        super("DamageTags", key);
    }

    public void link() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Chat.inWorld()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Tag t : this.tags) {
            if (!t.visible())
                continue;
            long age = now - t.createdAt;
            if (age < 0L || age > this.lifetime)
                continue;
            double p = Math.max(0.0, Math.min(1.0, (double) age / (double) this.lifetime));
            double rise = t.rise * (1.0 - Math.pow(1.0 - p, 2.0));
            RenderManager rm = mc.func_175598_ae();
            double dx = t.x - rm.field_78730_l;
            double dy = t.y + rise - rm.field_78731_m;
            double dz = t.z - rm.field_78728_n;
            int alpha = 255;
            long fadeStart = this.lifetime - this.fadeTime;
            if (age > fadeStart && this.fadeTime > 0L) {
                double d9 = Math.max(0.0, Math.min(1.0, (double) (age - fadeStart) / (double) this.fadeTime));
                alpha = (int) (255.0 * (1.0 - d9));
            }
            if (alpha <= 4)
                continue;
            float sc = (float) scale.getValue();
            GlStateManager.func_179094_E();
            GlStateManager.func_179109_b((float) dx, (float) dy, (float) dz);
            GlStateManager.func_179114_b(-rm.field_78735_i, 0.0f, 1.0f, 0.0f);
            GlStateManager.func_179114_b(rm.field_78732_j, 1.0f, 0.0f, 0.0f);
            float f2 = -0.02266667f * sc;
            GlStateManager.func_179152_a(f2, f2, f2);
            GlStateManager.func_179132_a(false);
            GlStateManager.func_179097_i();
            GlStateManager.func_179147_l();
            GlStateManager.func_179120_a(770, 771, 1, 0);
            int x = mc.field_71466_p.func_78256_a(t.text) / -2;
            int color = t.color() & 0xFFFFFF | alpha << 24;
            mc.field_71466_p.func_175065_a(t.text, (float) x, -3.0f, color, true);
            GlStateManager.func_179084_k();
            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            RenderUtils.resetColor();
            GlStateManager.func_179121_F();
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.onDisable();
        } else {
            this.lastHealth.remove(e.entity.func_145782_y());
        }
    }

    @Override
    public void onDisable() {
        this.lastHealth.clear();
        this.tags.clear();
    }

    @Override
    public void onUpdate() {
        final long now = System.currentTimeMillis();
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (Entity entity : mc.field_71441_e.field_72996_f) {
            if (!(entity instanceof EntityLivingBase))
                continue;
            EntityLivingBase e = (EntityLivingBase) entity;
            if (e == mc.field_71439_g)
                continue;
            if (!showInvis.getValue() && e.func_82150_aj())
                continue;
            if (mc.field_71439_g.func_70068_e(e) > 2500.0)
                continue;
            if (e instanceof EntityPlayer) {
                if (BotCheck.isBot(e))
                    continue;
            } else if (!(showMobs.getValue() && isMob(e) && !Hypixel.sameTeam(e))) {
                continue;
            }
            seen.add(e.func_145782_y());
            float hp = e.func_110143_aJ() + e.func_110139_bj();
            float last = this.lastHealth.getOrDefault(e.func_145782_y(), hp);
            if (hp != last) {
                float delta = hp - last;
                this.tags.add(new Tag(delta, e.field_70165_t, e.field_70163_u + (double) e.func_70047_e(), e.field_70161_v, delta < 0.0f && e.field_70725_aQ != 0, now));
            }
            if (e.field_70725_aQ <= 0) {
                this.lastHealth.put(e.func_145782_y(), hp);
            } else {
                this.lastHealth.remove(e.func_145782_y());
            }
        }
        this.lastHealth.keySet().removeIf(id -> !seen.contains(id));
        if (!this.tags.isEmpty()) {
            this.tags.removeIf(t -> now - t.createdAt > this.lifetime);
        }
    }

    public static boolean isMob(Entity e) {
        return e instanceof EntityMob || e instanceof EntityGolem || e instanceof EntitySlime;
    }

    @Override
    public void guiUpdate() {
        this.lifetime = (long) (length.getValue() * 1000.0);
        theme = Theme.values()[themeSetting.getValue().intValue()];
    }

    private static double range(double d, double d2) {
        return d + (d2 - d) * Math.random();
    }

    private static int sign() {
        return RANDOM.nextBoolean() ? 1 : -1;
    }

    private static final class Tag {

        final long createdAt;

        final boolean fatal;

        final String text;

        final double rise;

        final double delta;

        final double x, y, z;

        Tag(double d, double x, double y, double z, boolean fatal, long now) {
            this.delta = d;
            double spread = 1.0;
            double ox = (double) sign() * range(0.15, 0.65) * spread;
            double oy = -range(0.1, 0.45);
            double oz = (double) sign() * range(0.15, 0.65) * spread;
            this.x = x + ox;
            this.y = y + oy;
            this.z = z + oz;
            this.fatal = fatal;
            this.createdAt = now;
            this.rise = range(0.35, 0.7);
            if (healthAsHearts.getValue()) {
                d /= 2.0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(Strings.number(Strings.round(Math.abs(d), 1)));
            if (heartSymbol.getValue()) {
                Strings.heart(sb);
            }
            this.text = sb.toString();
        }

        boolean visible() {
            return (this.delta > 0.0 ? showHeal : showHurt).getValue();
        }

        int color() {
            if (traditionalHealthColor.getValue()) {
                if (this.delta > 0.0) {
                    return -14221530;
                }
                if (this.delta < -6.0 || this.fatal) {
                    return -55770;
                }
                if (this.delta < -3.0) {
                    return -12250;
                }
                return -218;
            }
            return theme.getRGB(0.0);
        }
    }
}
