package myau.mixin;

import myau.NoSlowBlink;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemRenderer.class, remap = false)
public abstract class MixinNoSlowBlinkItemRenderer {

    @Shadow
    private ItemStack field_78453_b;

    @Redirect(method = "func_78440_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;func_71052_bv()I"))
    private int blinkFirstPersonUseCount(AbstractClientPlayer player) {
        int actual = player.func_71052_bv();
        if (actual == 0
                && this.field_78453_b != null
                && this.field_78453_b.func_77973_b() instanceof ItemSword
                && NoSlowBlink.shouldRenderSwordBlock(player))
            return 1;
        return actual;
    }
}
