package myau.mixin;

import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(value = Side.CLIENT)
@Mixin(value = { C0DPacketCloseWindow.class })
public interface AccessorC0DPacketCloseWindow {

    @Accessor
    public int getWindowId();
}
