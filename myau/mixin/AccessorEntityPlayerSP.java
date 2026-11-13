package myau.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { EntityPlayerSP.class })
public interface AccessorEntityPlayerSP {

    @Accessor
    public int getPositionUpdateTicks();
}
