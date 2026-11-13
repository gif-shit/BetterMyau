package myau;

import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.potion.Potion;

public class AutoHealModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ElapsedTimer healDelayTimer = new ElapsedTimer();
    private boolean wasBelowHealthThreshold;
    private int previousHotbarSlot = -1;
    public final PercentSetting health = new PercentSetting("health", 35);
    public final IntegerSetting delay = new IntegerSetting("delay", 4000, 0, 5000);
    public final BooleanSetting regenCheck = new BooleanSetting("regen-check", false);

    public AutoHealModule() {
        super("AutoHeal", false, 0);
    }

    private int findHealingItemSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack == null || !stack.func_82837_s())
                continue;
            String displayName = stack.func_82833_r();
            if (stack.func_77973_b() instanceof ItemSkull) {
                if (displayName.contains("\u00a76") && displayName.contains("Golden Head")) {
                    return slot;
                }
                if (!displayName.matches("\\S+\u00a7c's Head"))
                    continue;
                return slot;
            }
            if (!(stack.func_77973_b() instanceof ItemSoup)
                    || !displayName.contains("\u00a7a")
                    || !displayName.contains("Tasty Soup")
                    && !displayName.contains("Assist Soup"))
                continue;
            return slot;
        }
        return -1;
    }

    private boolean isRegenerationBlockingHeal() {
        return this.regenCheck.getValue() && minecraft.field_71439_g.func_70644_a(Potion.field_76428_l);
    }

    public boolean hasPendingSlotRestore() {
        return this.previousHotbarSlot != -1;
    }

    @EventHandler(priority = 50)
    public void onPreTick(PreTickEvent event) {
        int healingSlot;
        boolean belowHealthThreshold;
        if (!this.isEnabled()) {
            return;
        }
        float currentHealth = (float) Math.ceil(minecraft.field_71439_g.func_110143_aJ() + minecraft.field_71439_g.func_110139_bj());
        boolean bl = belowHealthThreshold = currentHealth / minecraft.field_71439_g.func_110138_aP() <= (float) this.health.getValue() / 100.0f;
        if (this.wasBelowHealthThreshold
                && belowHealthThreshold
                && !this.isRegenerationBlockingHeal()
                && this.healDelayTimer.hasElapsed(this.delay.getValue().intValue())
                && (healingSlot = this.findHealingItemSlot()) != -1) {
            this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
            minecraft.field_71439_g.field_71071_by.field_70461_c = healingSlot;
            ((AccessorPlayerControllerMP) minecraft.field_71442_b).callSyncCurrentPlayItem();
            PacketUtils.send(new C08PacketPlayerBlockPlacement(minecraft.field_71439_g.func_70694_bm()));
            this.healDelayTimer.reset();
        }
        this.wasBelowHealthThreshold = belowHealthThreshold;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (this.previousHotbarSlot == -1) {
            return;
        }
        minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
        this.previousHotbarSlot = -1;
    }

    @EventHandler
    public void onLeftClick(LeftClickEvent event) {
        if (this.isEnabled() && this.hasPendingSlotRestore()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClick(RightClickEvent event) {
        if (this.isEnabled() && this.hasPendingSlotRestore()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockClick(BlockClickEvent event) {
        if (this.isEnabled() && this.hasPendingSlotRestore()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarChange(HotbarChangeEvent event) {
        if (this.isEnabled() && this.hasPendingSlotRestore()) {
            event.setCancelled(true);
        }
    }
}
