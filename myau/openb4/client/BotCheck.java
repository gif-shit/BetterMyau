package myau.openb4.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class BotCheck {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private BotCheck() {
    }

    public static boolean isBot(Entity entity, boolean spawnDelay) {
        return isBot(entity);
    }

    public static boolean isBot(Entity entity) {
        return entity instanceof EntityPlayer && myau.EntityUtils.isBot((EntityPlayer) entity);
    }

    public static MovingObjectPosition rayTraceBlocks(double d) {
        Vec3 eyes = new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + (double) mc.field_71439_g.func_70047_e(), mc.field_71439_g.field_70161_v);
        Vec3 dir = look(mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A);
        Vec3 end = eyes.func_72441_c(dir.field_72450_a * d, dir.field_72448_b * d, dir.field_72449_c * d);
        return mc.field_71441_e.func_147447_a(eyes, end, false, false, false);
    }

    public static Vec3 look(float yaw, float pitch) {
        float f3 = MathHelper.func_76134_b((yaw *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.func_76126_a(yaw - (float) Math.PI);
        float f5 = -MathHelper.func_76134_b(pitch *= -((float) Math.PI / 180));
        float f6 = MathHelper.func_76126_a(pitch);
        return new Vec3(f4 * f5, f6, f3 * f5);
    }

    public static MovingObjectPosition rayTrace(Entity only, double d, double x, double y, double z, float yaw, float pitch) {
        Entity hit = null;
        Vec3 eyes = new Vec3(x, y + (double) mc.field_71439_g.func_70047_e(), z);
        Vec3 dir = look(yaw, pitch);
        Vec3 end = eyes.func_72441_c(dir.field_72450_a * d, dir.field_72448_b * d, dir.field_72449_c * d);
        Vec3 hitVec = null;
        Entity view = mc.func_175606_aa();
        List<Entity> list = mc.field_71441_e.func_72839_b(view, view.func_174813_aQ().func_72321_a(dir.field_72450_a * d, dir.field_72448_b * d, dir.field_72449_c * d).func_72314_b(1.0, 1.0, 1.0));
        double best = d;
        for (Entity e : list) {
            if (!e.func_70067_L())
                continue;
            if (only != null && e != only)
                continue;
            float border = e.func_70111_Y();
            AxisAlignedBB bb = e.func_174813_aQ().func_72314_b(border, border, border);
            MovingObjectPosition mop = bb.func_72327_a(eyes, end);
            if (bb.func_72318_a(eyes)) {
                if (!(best >= 0.0))
                    continue;
                hit = e;
                hitVec = mop == null ? eyes : mop.field_72307_f;
                best = 0.0;
                continue;
            }
            double dist;
            if (mop == null || !((dist = eyes.func_72438_d(mop.field_72307_f)) < best) && best != 0.0)
                continue;
            if (e == view.field_70154_o && !e.canRiderInteract()) {
                if (best != 0.0)
                    continue;
                hit = e;
                hitVec = mop.field_72307_f;
                continue;
            }
            hit = e;
            hitVec = mop.field_72307_f;
            best = dist;
        }
        return hit == null ? null : new MovingObjectPosition(hit, hitVec);
    }
}
