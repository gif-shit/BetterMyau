package myau;

import java.lang.invoke.MethodHandles;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import myau.source.SourceExceptions;
import net.minecraft.util.EnumFacing;

class EnumFacingSwitchMap {

    static final int[] g;

    EnumFacingSwitchMap() {
    }

    static {
        try {
            Cipher var2;
            long var7 = KeySchedule.a(-4446868390293276978L, 4323161543642248202L, MethodHandles.lookup().lookupClass()).a(273035876824151L) ^ 0x1395A6F26931L;
            Cipher var10000 = var2 = Cipher.getInstance("DES/CBC/NoPadding");
            SecretKeyFactory var10002 = SecretKeyFactory.getInstance("DES");
            byte[] var10003 = new byte[] { (byte) (var7 >>> 56), 0, 0, 0, 0, 0, 0, 0 };
            for (int var3 = 1; var3 < 8; ++var3) {
                var10003[var3] = (byte) (var7 << var3 * 8 >>> 56);
            }
            var10000.init(2, (Key) var10002.generateSecret(new DESKeySpec(var10003)), new IvParameterSpec(new byte[8]));
            long var4 = 6639742008748140829L;
            byte[] var6 = var2.doFinal(new byte[] { (byte) (var4 >>> 56), (byte) (var4 >>> 48), (byte) (var4 >>> 40), (byte) (var4 >>> 32), (byte) (var4 >>> 24), (byte) (var4 >>> 16), (byte) (var4 >>> 8), (byte) var4 });
            long var15 = ((long) var6[0] & 0xFFL) << 56 | ((long) var6[1] & 0xFFL) << 48 | ((long) var6[2] & 0xFFL) << 40 | ((long) var6[3] & 0xFFL) << 32 | ((long) var6[4] & 0xFFL) << 24 | ((long) var6[5] & 0xFFL) << 16 | ((long) var6[6] & 0xFFL) << 8 | (long) var6[7] & 0xFFL;
            int var10001 = -1;
            long var0 = var15;
            g = new int[EnumFacing.values().length];
            try {
                g[EnumFacing.NORTH.ordinal()] = 1;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                g[EnumFacing.EAST.ordinal()] = 2;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                g[EnumFacing.SOUTH.ordinal()] = 3;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                g[EnumFacing.WEST.ordinal()] = 4;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                g[EnumFacing.DOWN.ordinal()] = 5;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
            try {
                g[EnumFacing.UP.ordinal()] = (int) var0;
            } catch (NoSuchFieldError noSuchFieldError) {
            }
        } catch (Throwable checkedFailure) {
            throw SourceExceptions.propagate(checkedFailure);
        }
    }
}
