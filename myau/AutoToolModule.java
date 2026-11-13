package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

public class AutoToolModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private int selectedSlot = -1;
    private int previousSlot = -1;
    private int miningTicks;
    public final IntegerSetting delay = new IntegerSetting("delay", 0, 0, 5);
    public final BooleanSetting switchBack = new BooleanSetting("switch-back", true);
    public final BooleanSetting sneakOnly = new BooleanSetting("sneak-only", true);

    public AutoToolModule() {
        super("AutoTool", false, 0);
    }

    public int getPreviousSlot() {
        return this.previousSlot;
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        boolean enabled = this.isEnabled();
        if (!enabled) {
            return;
        }
        if (minecraft.field_71476_x != null
                && minecraft.field_71476_x.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK
                && minecraft.field_71474_y.field_74312_F.func_151470_d()
                && !minecraft.field_71439_g.func_71039_bw()) {
            int bestSlot;
            if (this.miningTicks >= this.delay.getValue()
                    && (!this.sneakOnly.getValue()
                    || InputUtils.isKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i()))
                    && minecraft.field_71439_g.field_71071_by.field_70461_c != (bestSlot = ItemUtils.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, minecraft.field_71441_e.func_180495_p(minecraft.field_71476_x.func_178782_a()).func_177230_c()))) {
                this.previousSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                minecraft.field_71439_g.field_71071_by.field_70461_c = this.selectedSlot = bestSlot;
            }
            ++this.miningTicks;
            return;
        }
        this.selectedSlot = -1;
        this.previousSlot = -1;
        this.miningTicks = 0;
    }

    @Override
    public void onDisable() {
        this.selectedSlot = -1;
        this.previousSlot = -1;
        this.miningTicks = 0;
    }
}
