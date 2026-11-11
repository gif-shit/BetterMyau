package myau;

import myau.EventHandler;
import myau.ModeSetting;
import myau.Module;
import myau.PostTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class FullbrightModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private float savedGamma = Float.NaN;
    private boolean removeNightVision;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"GAMMA", "EFFECT"});

    public FullbrightModule() {
        super("Fullbright", true, 0, true);
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int selectedMode = (Integer)this.mode.getValue();
        switch (selectedMode) {
            case 0: {
                FullbrightModule.minecraft.field_71474_y.field_74333_Y = 1000.0f;
                break;
            }
            case 1: {
                FullbrightModule.minecraft.field_71439_g.func_70690_d(new PotionEffect(Potion.field_76439_r.field_76415_H, 25940, 0));
            }
        }
    }

    @Override
    public void onEnable() {
        switch ((Integer)this.mode.getValue()) {
            case 0: {
                this.savedGamma = FullbrightModule.minecraft.field_71474_y.field_74333_Y;
                return;
            }
            case 1: {
                this.removeNightVision = true;
            }
        }
    }

    @Override
    public void onDisable() {
        boolean shouldRemoveEffect = Float.isNaN(this.savedGamma);
        if (!shouldRemoveEffect) {
            FullbrightModule.minecraft.field_71474_y.field_74333_Y = this.savedGamma;
            this.savedGamma = Float.NaN;
        }
        if (shouldRemoveEffect = this.removeNightVision) {
            if (FullbrightModule.minecraft.field_71439_g != null) {
                FullbrightModule.minecraft.field_71439_g.func_70618_n(Potion.field_76439_r.field_76415_H);
            }
            this.removeNightVision = false;
        }
    }

    @Override
    public void onSettingChanged(String settingName) {
        if (!this.isEnabled()) {
            return;
        }
        this.onDisable();
        this.onEnable();
    }
}
