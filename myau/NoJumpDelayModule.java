package myau;

import myau.EventHandler;
import myau.IntegerSetting;
import myau.Module;
import myau.PreTickEvent;
import myau.mixin.AccessorEntityLivingBase;
import net.minecraft.client.Minecraft;

public class NoJumpDelayModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final IntegerSetting delay = new IntegerSetting("delay", 3, 0, 8);

    public NoJumpDelayModule() {
        super("NoJumpDelay", false, 0);
    }

    @EventHandler(priority=100)
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        AccessorEntityLivingBase player = (AccessorEntityLivingBase)NoJumpDelayModule.minecraft.field_71439_g;
        player.setJumpTicks(Math.min(player.getJumpTicks(), (Integer)this.delay.getValue() + 1));
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{((Integer)this.delay.getValue()).toString()};
    }
}
