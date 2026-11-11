package myau.mixin;

import myau.MyauClient;
import myau.ScaffoldModule;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(value=Side.CLIENT)
@Mixin(value={GuiIngame.class})
public abstract class MixinGuiIngame {
    @Redirect(method={"updateTick"}, at=@At(value="INVOKE", target="Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirectCurrentHotbarItem(InventoryPlayer inventoryPlayer) {
        int spoofedSlot;
        if (MyauClient.moduleManager == null) {
            return inventoryPlayer.func_70448_g();
        }
        ScaffoldModule scaffold = (ScaffoldModule)MyauClient.moduleManager.modules.get(ScaffoldModule.class);
        if (scaffold != null && scaffold.isEnabled() && ((Boolean)scaffold.itemSpoof.getValue()).booleanValue() && (spoofedSlot = scaffold.getPreviousHotbarSlot()) >= 0) {
            return inventoryPlayer.func_70301_a(spoofedSlot);
        }
        return inventoryPlayer.func_70448_g();
    }
}
