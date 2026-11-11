package myau;

import java.text.DecimalFormat;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.FloatSetting;
import myau.Module;
import myau.PreTickEvent;
import myau.RotationUtils;
import myau.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class FastPlaceModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat DELAY_FORMAT = new DecimalFormat("0.0#");
    private long remainingDelayMillis = 0L;
    public final FloatSetting delay = new FloatSetting("delay", Float.valueOf(1.0f), Float.valueOf(1.0f), Float.valueOf(3.0f));
    public final BooleanSetting blocksOnly = new BooleanSetting("blocks-only", true);
    public final BooleanSetting placeFix = new BooleanSetting("place-fix", true);

    public FastPlaceModule() {
        super("FastPlace", false, 0);
    }

    private boolean canPlaceImmediately() {
        ItemStack heldStack = FastPlaceModule.minecraft.field_71439_g.func_70694_bm();
        if (heldStack == null) {
            return (Boolean)this.blocksOnly.getValue() == false;
        }
        Item heldItem = heldStack.func_77973_b();
        if (heldItem instanceof ItemFishingRod) {
            return false;
        }
        if (!(heldItem instanceof ItemBlock)) {
            return (Boolean)this.blocksOnly.getValue() == false;
        }
        if (!((Boolean)this.placeFix.getValue()).booleanValue()) {
            return true;
        }
        MovingObjectPosition hit = RotationUtils.traceBlocks(FastPlaceModule.minecraft.field_71439_g.field_70177_z, FastPlaceModule.minecraft.field_71439_g.field_70125_A, FastPlaceModule.minecraft.field_71442_b.func_78757_d(), 1.0f);
        if (hit == null) {
            return false;
        }
        return hit.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK && ((ItemBlock)heldItem).func_179222_a((World)FastPlaceModule.minecraft.field_71441_e, hit.func_178782_a(), hit.field_178784_b, (EntityPlayer)FastPlaceModule.minecraft.field_71439_g, heldStack);
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        int rightClickDelay = ((AccessorMinecraft)minecraft).getRightClickDelayTimer();
        if (rightClickDelay == 4) {
            this.remainingDelayMillis += (long)(50.0f * ((Float)this.delay.getValue()).floatValue());
        }
        if (this.remainingDelayMillis > 0L) {
            this.remainingDelayMillis -= 50L;
        }
        if (this.remainingDelayMillis <= 0L && rightClickDelay > 1 && this.canPlaceImmediately()) {
            ((AccessorMinecraft)minecraft).setRightClickDelayTimer(0);
        }
    }

    @Override
    public void onDisable() {
        this.remainingDelayMillis = 0L;
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{DELAY_FORMAT.format(this.delay.getValue())};
    }
}
