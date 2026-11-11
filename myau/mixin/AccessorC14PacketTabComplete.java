package myau.mixin;

import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(value=Side.CLIENT)
@Mixin(value={C14PacketTabComplete.class})
public interface AccessorC14PacketTabComplete {
    @Accessor(value="message")
    public void setMessage(String var1);
}
