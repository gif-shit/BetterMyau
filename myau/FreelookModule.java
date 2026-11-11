package myau;

import myau.BooleanSetting;
import myau.EventHandler;
import myau.InputUtils;
import myau.IntegerSetting;
import myau.Module;
import myau.PreTickEvent;
import myau.WorldLoadEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;

public class FreelookModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static FreelookModule instance;
    private static boolean active;
    private static float cameraYaw;
    private static float cameraPitch;
    private static boolean cameraSwapped;
    private static Entity swappedEntity;
    private static final float[] savedRotation;
    public final BooleanSetting hold = new BooleanSetting("hold", true);
    public final BooleanSetting invertPitch = new BooleanSetting("invert-pitch", false);
    public final BooleanSetting lockPitch = new BooleanSetting("lock-pitch", true);
    public final BooleanSetting customFov = new BooleanSetting("custom-fov", false);
    public final IntegerSetting fov = new IntegerSetting("fov", 90, 10, 150, this.customFov::getValue);
    private int previousPerspective;
    private float lastFov;

    public FreelookModule() {
        super("FreeLook", false, 56);
        instance = this;
    }

    @Override
    public void onEnable() {
        if (FreelookModule.minecraft.field_71439_g == null) {
            this.setEnabled(false);
            return;
        }
        GameSettings gameSettings = FreelookModule.minecraft.field_71474_y;
        cameraYaw = FreelookModule.minecraft.field_71439_g.field_70177_z;
        cameraPitch = FreelookModule.minecraft.field_71439_g.field_70125_A;
        this.previousPerspective = gameSettings.field_74320_O;
        this.lastFov = gameSettings.field_74334_X;
        active = true;
        FreelookModule.applyView(1);
    }

    @Override
    public void onDisable() {
        if (!active) {
            return;
        }
        active = false;
        FreelookModule.applyView(this.previousPerspective);
        if (((Boolean)this.customFov.getValue()).booleanValue()) {
            FreelookModule.minecraft.field_71474_y.field_74334_X = this.lastFov;
        }
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        int n;
        if (!this.isEnabled() || !active) {
            return;
        }
        if (((Boolean)this.customFov.getValue()).booleanValue()) {
            FreelookModule.minecraft.field_71474_y.field_74334_X = ((Integer)this.fov.getValue()).intValue();
        }
        if (((Boolean)this.hold.getValue()).booleanValue() && (n = this.getKeyCode()) != 0 && (FreelookModule.minecraft.field_71462_r != null || !InputUtils.isKeyDown(n))) {
            this.setEnabled(false);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent worldLoadEvent) {
        if (active) {
            this.setEnabled(false);
        }
    }

    private static void applyView(int n) {
        FreelookModule.minecraft.field_71474_y.field_74320_O = Math.max(0, Math.min(2, n));
        if (FreelookModule.minecraft.field_71460_t != null) {
            if (n == 0) {
                FreelookModule.minecraft.field_71460_t.func_175066_a(minecraft.func_175606_aa());
            } else if (n == 1) {
                FreelookModule.minecraft.field_71460_t.func_175066_a(null);
            }
        }
        if (FreelookModule.minecraft.field_71438_f != null) {
            FreelookModule.minecraft.field_71438_f.func_174979_m();
        }
    }

    public static boolean handleAngles(float f, float f2) {
        FreelookModule freelookModule = instance;
        if (!active || freelookModule == null || !freelookModule.isEnabled()) {
            return false;
        }
        cameraYaw += f * 0.15f;
        float f3 = f2 * 0.15f;
        if (((Boolean)freelookModule.invertPitch.getValue()).booleanValue()) {
            f3 = -f3;
        }
        cameraPitch -= f3;
        if (((Boolean)freelookModule.lockPitch.getValue()).booleanValue()) {
            cameraPitch = Math.max(-90.0f, Math.min(90.0f, cameraPitch));
        }
        return true;
    }

    public static void beginCamera() {
        Entity entity = minecraft.func_175606_aa();
        FreelookModule freelookModule = instance;
        if (!active || entity == null || freelookModule == null || !freelookModule.isEnabled()) {
            cameraSwapped = false;
            return;
        }
        swappedEntity = entity;
        FreelookModule.savedRotation[0] = entity.field_70177_z;
        FreelookModule.savedRotation[1] = entity.field_70126_B;
        FreelookModule.savedRotation[2] = entity.field_70125_A;
        FreelookModule.savedRotation[3] = entity.field_70127_C;
        entity.field_70177_z = cameraYaw;
        entity.field_70126_B = cameraYaw;
        entity.field_70125_A = cameraPitch;
        entity.field_70127_C = cameraPitch;
        cameraSwapped = true;
    }

    public static void endCamera() {
        if (!cameraSwapped || swappedEntity == null) {
            return;
        }
        FreelookModule.swappedEntity.field_70177_z = savedRotation[0];
        FreelookModule.swappedEntity.field_70126_B = savedRotation[1];
        FreelookModule.swappedEntity.field_70125_A = savedRotation[2];
        FreelookModule.swappedEntity.field_70127_C = savedRotation[3];
        cameraSwapped = false;
        swappedEntity = null;
    }

    static {
        savedRotation = new float[4];
    }
}
