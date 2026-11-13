package myau.openb4.module.world;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.ModeSetting;
import myau.openb4.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WeatherModule extends Module {

    public static WeatherModule instance;
    public static final FloatSetting time = new FloatSetting("time", 0.0f, 0.0f, 24.0f, 0.3f);
    public static final ModeSetting lightning = new ModeSetting("lightning", 0, new String[] { "DISABLED", "VANILLA", "UNCOMMON", "COMMON", "FREQUENT", "INTENSE", "CHAOTIC" });
    public static final BooleanSetting rain = new BooleanSetting("rain", false);
    public static Lightning mode = Lightning.Disabled;
    public static long savedTime = -9L;
    public static long savedRain = -1L;
    public static long lastThunder;
    private static final Random RANDOM = new Random();
    private static Field activeChunkSet;
    private static Field updateLCG;

    public enum Lightning {

        Disabled,
        Vanilla,
        Uncommon,
        Common,
        Frequent,
        Intense,
        Chaotic;

        public boolean shouldStrike() {
            if (this == Vanilla) {
                return RANDOM.nextInt(100000) == 0;
            }
            double d = Math.random();
            switch(this) {
                case Uncommon:
                    return d > 0.9999;
                case Common:
                    return d > 0.999;
                case Frequent:
                    return d > 0.99;
                case Intense:
                    return d > 0.9;
                case Chaotic:
                    return d > 0.3;
                default:
                    return false;
            }
        }
    }

    public WeatherModule(int key) {
        super("Weather", key);
        instance = this;
    }

    public void link() {
    }

    public void guiUpdate() {
        mode = Lightning.values()[lightning.getValue().intValue()];
    }

    public void onUpdate() {
        this.updateTime();
        this.updateRain();
        this.strike();
    }

    public void afterDisable() {
        if (!this.isEnabled()) {
            this.updateTime();
            this.updateRain();
        }
    }

    private void updateTime() {
        Minecraft mc = Minecraft.func_71410_x();
        if (this.isEnabled() && (time.getValue() != 0)) {
            if (savedTime == -9L) {
                savedTime = mc.field_71441_e.func_72820_D();
            }
            mc.field_71441_e.func_72877_b((long) (-1000.0 * (16.0 + time.getValue())));
        } else if (savedTime != -9L) {
            mc.field_71441_e.func_72877_b(savedTime);
            savedTime = -9L;
        }
    }

    private void updateRain() {
        Minecraft mc = Minecraft.func_71410_x();
        if (this.isEnabled() && rain.getValue()) {
            if (savedRain == -1L) {
                savedRain = mc.field_71441_e.func_72896_J() ? 1L : 0L;
            }
            this.worldInfo().func_76084_b(true);
        } else if (savedRain != -1L) {
            this.worldInfo().func_76084_b(savedRain == 1L);
            savedRain = -1L;
        }
    }

    private WorldInfo worldInfo() {
        MinecraftServer server = MinecraftServer.func_71276_C();
        if (server != null && server.field_71305_c != null && server.field_71305_c.length > 0) {
            return server.field_71305_c[0].func_72912_H();
        }
        return Minecraft.func_71410_x().field_71441_e.func_72912_H();
    }

    @SuppressWarnings("unchecked")
    private void strike() {
        Minecraft mc = Minecraft.func_71410_x();
        if (!(lightning.getValue() != 0) || mc.func_147113_T()) {
            return;
        }
        try {
            if (activeChunkSet == null) {
                activeChunkSet = ReflectionHelper.findField(World.class, "activeChunkSet", "field_72993_I");
                updateLCG = ReflectionHelper.findField(World.class, "updateLCG", "field_73005_l");
            }
            long l = System.currentTimeMillis();
            Set<ChunkCoordIntPair> set = (Set<ChunkCoordIntPair>) activeChunkSet.get(mc.field_71441_e);
            for (ChunkCoordIntPair c : set) {
                if (!mode.shouldStrike())
                    continue;
                int x = c.field_77276_a * 16;
                int z = c.field_77275_b * 16;
                int n3 = (Integer) updateLCG.get(mc.field_71441_e);
                n3 = n3 * 3 + 1013904223;
                updateLCG.set(mc.field_71441_e, n3);
                int n4 = n3 >> 2;
                BlockPos pos = this.target(new BlockPos(x + (n4 & 0xF), 0, z + (n4 >> 8 & 0xF)));
                if (!mc.field_71441_e.func_175678_i(pos))
                    continue;
                mc.field_71441_e.func_72942_c(new EntityLightningBolt(mc.field_71441_e, pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()));
                if (Math.abs(l - lastThunder) <= 250L)
                    continue;
                mc.field_71441_e.func_72980_b(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p(), "ambient.weather.thunder", 10.0f, 0.8f + RANDOM.nextFloat() * 0.2f, false);
                lastThunder = l;
            }
        } catch (Exception exception) {
        }
    }

    private BlockPos target(BlockPos blockPos) {
        Minecraft mc = Minecraft.func_71410_x();
        BlockPos top = mc.field_71441_e.func_175725_q(blockPos);
        BlockPos sky = new BlockPos(top.func_177958_n(), mc.field_71441_e.func_72800_K(), top.func_177952_p());
        AxisAlignedBB box = new AxisAlignedBB(top, sky).func_72314_b(3.0, 3.0, 3.0);
        List<EntityLivingBase> list = mc.field_71441_e.func_175647_a(EntityLivingBase.class, box, WeatherModule::canStrike);
        return !list.isEmpty() ? list.get(RANDOM.nextInt(list.size())).func_180425_c() : top;
    }

    private static boolean canStrike(EntityLivingBase e) {
        return e != null && e.func_70089_S() && Minecraft.func_71410_x().field_71441_e.func_175678_i(e.func_180425_c());
    }

    @Override
    public boolean onReceivePacket(net.minecraft.network.Packet packet, net.minecraft.network.INetHandler handler) {
        if (packet instanceof S03PacketTimeUpdate && time.getValue() != 0) {
            savedTime = ((S03PacketTimeUpdate) packet).func_149365_d();
            return false;
        }
        return true;
    }
}
