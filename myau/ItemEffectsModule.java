package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public final class ItemEffectsModule extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private EntityPlayerSP swingPlayer;
    private float previousAge = -1;
    private float swingAge = -1;
    private float swingDuration = 6;
    public final FloatSetting itemSize = new FloatSetting("item-size", 1f, 0.25f, 3f);
    public final FloatSetting attackSpeed = new FloatSetting("attack-speed", 1f, 0.1f, 5f);

    public ItemEffectsModule() {
        super("ItemEffects", false, 0);
    }

    public static ItemEffectsModule current() {
        return MyauClient.moduleManager == null ? null : (ItemEffectsModule) MyauClient.moduleManager.modules.get(ItemEffectsModule.class);
    }

    public float sizeMultiplier() {
        return isEnabled() ? itemSize.getValue() : 1;
    }

    public void onSwing() {
        EntityPlayerSP player = minecraft.field_71439_g;
        if (!isEnabled() || attackSpeed.getValue() == 1 || player == null) return;
        if (swingPlayer != player) resetAnimation();
        if (swingAge >= 0 && swingAge < swingDuration / 2) return;
        PotionEffect haste = player.func_70660_b(Potion.field_76422_e);
        PotionEffect fatigue = player.func_70660_b(Potion.field_76419_f);
        int ticks = haste != null ? 6 - (haste.func_76458_c() + 1) : fatigue != null ? 6 + (fatigue.func_76458_c() + 1) * 2 : 6;
        swingDuration = Math.max(1, ticks) / attackSpeed.getValue();
        swingPlayer = player;
        previousAge = swingAge = 0;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!isEnabled() || minecraft.field_71441_e == null || minecraft.field_71439_g != swingPlayer) {
            resetAnimation();
            return;
        }
        if (minecraft.func_147113_T()) return;
        previousAge = swingAge;
        if (swingAge >= 0) {
            ++swingAge;
            if (previousAge >= swingDuration) previousAge = swingAge = -1;
        }
    }

    public float renderSwingProgress(AbstractClientPlayer player, float vanilla, float partialTicks) {
        if (!isEnabled() || attackSpeed.getValue() == 1 || player != minecraft.field_71439_g) return vanilla;
        if (player != swingPlayer || swingAge < 0) return 0;
        float age = previousAge + (swingAge - previousAge) * Math.max(0, Math.min(1, partialTicks));
        return age >= swingDuration ? 0 : Math.max(0, age / swingDuration);
    }

    private void resetAnimation() {
        swingPlayer = null;
        previousAge = swingAge = -1;
    }

    @Override
    public void onEnable() {
        resetAnimation();
    }

    @Override
    public void onDisable() {
        resetAnimation();
    }

    @Override
    public void onSettingChanged(String name) {
        if ("attack-speed".equals(name)) resetAnimation();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        resetAnimation();
    }
}
