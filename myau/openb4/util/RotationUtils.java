package myau.openb4.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class RotationUtils {

    private static final Minecraft mc = Minecraft.func_71410_x();
    public static float lastYawStep, lastPitchStep;

    private RotationUtils() {
    }

    public static final class AimPoint {

        public final float yaw;
        public final float pitch;
        public final Vec3 point;

        public AimPoint(float yaw, float pitch, Vec3 point) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.point = point;
        }
    }

    public enum Precision {

        Low(40.0f), Medium(20.0f), High(5.0f), Very_high(1.5f);

        public final float offsetYaw;

        Precision(float offsetYaw) {
            this.offsetYaw = offsetYaw;
        }

        public float getOffsetYaw() {
            return this.offsetYaw;
        }

        public float getOffsetPitch(float pitch) {
            if (pitch > 75.0f) {
                return 1.0f;
            }
            if (pitch > 60.0f) {
                return 2.0f;
            }
            return 5.0f;
        }
    }

    private enum Axis {

        X, Y, Z
    }

    public static float[] toBlockFixed(BlockPos pos, EnumFacing facing, float lastYaw, float lastPitch) {
        float[] r = toBlock(pos, facing);
        return fixSensitivity(r[0], r[1], lastYaw, lastPitch);
    }

    public static float clampPitch(float pitch) {
        return MathHelper.func_76131_a(pitch, -90.0f, 90.0f);
    }

    public static void setRotation(float yaw, float pitch) {
        mc.field_71439_g.field_70177_z = yaw;
        mc.field_71439_g.field_70125_A = pitch;
    }

    public static void setRotation(float[] rotations) {
        if (rotations != null) {
            setRotation(rotations[0], rotations[1]);
        }
    }

    public static float[] playerRotation() {
        return new float[] { mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A };
    }

    public static float[] currentRotation() {
        if (RotationManager.rotating && RotationManager.wasRotating) {
            return new float[] { RotationManager.serverYaw, RotationManager.serverPitch };
        }
        return playerRotation();
    }

    public static float[] toBlock(BlockPos pos) {
        return toBlock(pos, null);
    }

    public static float[] toBlock(BlockPos pos, EnumFacing facing) {
        return toBlock(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), facing);
    }

    public static float[] toBlock(double x, double y, double z, EnumFacing facing) {
        double d4 = x + 0.45 - mc.field_71439_g.field_70165_t;
        double d5 = y + 0.45 - (mc.field_71439_g.field_70163_u + (double) mc.field_71439_g.func_70047_e());
        double d6 = z + 0.45 - mc.field_71439_g.field_70161_v;
        if (facing != null) {
            d4 += (double) facing.func_176730_m().func_177958_n() * 0.5;
            d5 += (double) facing.func_176730_m().func_177956_o() * 0.5;
            d6 += (double) facing.func_176730_m().func_177952_p() * 0.5;
        }
        return fromDelta(d4, d5, d6);
    }

    public static float[] toPosition(double x, double y, double z) {
        return fromDelta(x - mc.field_71439_g.field_70165_t, y - (mc.field_71439_g.field_70163_u + (double) mc.field_71439_g.func_70047_e()), z - mc.field_71439_g.field_70161_v);
    }

    public static float[] fixSensitivity(float yaw, float pitch, float lastYaw, float lastPitch) {
        float f5 = yaw - lastYaw;
        float f7 = pitch - lastPitch;
        float f8 = mc.field_71474_y.field_74341_c * 0.6f + 0.2f;
        double d = (double) (f8 * f8 * f8) * 1.2;
        float f9 = lastYawStep = (float) ((double) Math.round((double) f5 / d) * d);
        float f10 = lastPitchStep = (float) ((double) Math.round((double) f7 / d) * d);
        yaw = lastYaw + f9;
        pitch = lastPitch + f10;
        return new float[] { yaw, clampPitch(pitch) };
    }

    public static float[] fromDelta(double dx, double dy, double dz) {
        double d4 = Math.sqrt(dx * dx + dz * dz);
        float f = (float) (Math.atan2(dz, dx) * (double) 57.29578f) - 90.0f;
        float f2 = (float) (-(Math.atan2(dy, d4) * (double) 57.29578f));
        float[] current = currentRotation();
        f = current[0] + MathHelper.func_76142_g(f - current[0]);
        f2 = clampPitch(current[1] + MathHelper.func_76142_g(f2 - current[1]));
        return new float[] { f, f2 };
    }

    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        float f3 = MathHelper.func_76134_b((yaw *= -((float) Math.PI / 180)) - (float) Math.PI);
        float f4 = MathHelper.func_76126_a(yaw - (float) Math.PI);
        float f5 = -MathHelper.func_76134_b(pitch *= -((float) Math.PI / 180));
        float f6 = MathHelper.func_76126_a(pitch);
        return new Vec3(f4 * f5, f6, f3 * f5);
    }

    public static AimPoint aimAtFace(BlockPos pos, EnumFacing facing) {
        double reach = mc.field_71442_b.func_78757_d();
        for (Vec3 p : facePoints(pos, facing)) {
            float[] r = toPosition(p.field_72450_a, p.field_72448_b, p.field_72449_c);
            if (PlayerUtils.rayTraceBlock(pos, facing, reach, r[0], r[1]) == null)
                continue;
            return new AimPoint(r[0], r[1], p);
        }
        return null;
    }

    public static List<Vec3> facePoints(BlockPos pos, EnumFacing facing) {
        Axis a;
        Axis b;
        ArrayList<Vec3> list = new ArrayList<Vec3>((2 * 2 + 1) * (2 * 2 + 1) + 8);
        double x = (double) pos.func_177958_n() + 0.5 + 0.5 * (double) facing.func_82601_c();
        double y = (double) pos.func_177956_o() + 0.5 + 0.5 * (double) facing.func_96559_d();
        double z = (double) pos.func_177952_p() + 0.5 + 0.5 * (double) facing.func_82599_e();
        if (Math.abs(facing.func_96559_d()) == 1) {
            a = Axis.X;
            b = Axis.Z;
        } else if (Math.abs(facing.func_82599_e()) == 1) {
            a = Axis.X;
            b = Axis.Y;
        } else {
            a = Axis.Z;
            b = Axis.Y;
        }
        double step = 0.49 / (double) 2;
        list.add(jitter(x, y, z));
        for (int i = 1; i <= 2; ++i) {
            list.add(offset(x, y, z, a, (double) i * step, b, 0.0));
            list.add(offset(x, y, z, a, (double) (-i) * step, b, 0.0));
            list.add(offset(x, y, z, a, 0.0, b, (double) i * step));
            list.add(offset(x, y, z, a, 0.0, b, (double) (-i) * step));
            for (int j = -i; j <= i; ++j) {
                for (int k = -i; k <= i; ++k) {
                    int ak;
                    int aj = Math.abs(j);
                    if (Math.max(aj, ak = Math.abs(k)) != i || j == 0 && ak == i || k == 0 && aj == i)
                        continue;
                    list.add(offset(x, y, z, a, (double) j * step, b, (double) k * step));
                }
            }
        }
        return list;
    }

    private static Vec3 jitter(double x, double y, double z) {
        double d6 = 0.04;
        double d7 = 0.01;
        double lo;
        double hi;
        if (x % 1.0 != 0.0) {
            lo = Math.floor(x += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(x);
            x = Math.max(lo + d7, Math.min(hi - d7, x));
        }
        if (y % 1.0 != 0.0) {
            lo = Math.floor(y += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(y);
            y = Math.max(lo + d7, Math.min(hi - d7, y));
        }
        if (z % 1.0 != 0.0) {
            lo = Math.floor(z += MathUtils.randomBetween(-d6, d6));
            hi = Math.ceil(z);
            z = Math.max(lo + d7, Math.min(hi - d7, z));
        }
        return new Vec3(x, y, z);
    }

    private static Vec3 offset(double x, double y, double z, Axis a, double da, Axis b, double db) {
        if (a == Axis.X) {
            x += da;
        } else if (a == Axis.Y) {
            y += da;
        } else {
            z += da;
        }
        if (b == Axis.X) {
            x += db;
        } else if (b == Axis.Y) {
            y += db;
        } else {
            z += db;
        }
        return jitter(x, y, z);
    }
}
