package myau.modules.swordswap;

import java.util.Objects;
import myau.modules.SwordSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.item.*;

public final class SwordSwapMinecraft implements SwordSwap.Host {
    public interface Bindings {
        boolean inPit();
        boolean auraEnabledWithTarget();
        boolean attackKeyDown();
        boolean autoHealing();
    }
    private final Minecraft mc;
    private final Bindings bindings;
    public SwordSwapMinecraft(Minecraft mc,Bindings bindings){this.mc=Objects.requireNonNull(mc,"minecraft");this.bindings=Objects.requireNonNull(bindings,"bindings");}
    public boolean ready(){return mc.field_71439_g!=null&&mc.field_71441_e!=null&&mc.field_71442_b!=null;}
    public Object playerIdentity(){return mc.field_71439_g;}
    public Object worldIdentity(){return mc.field_71441_e;}
    public boolean screenOpen(){return mc.field_71462_r!=null;}
    public boolean inPit(){return bindings.inPit();}
    public boolean auraEnabledWithTarget(){return bindings.auraEnabledWithTarget();}
    public boolean attackKeyDown(){return bindings.attackKeyDown();}
    public boolean swinging(){return mc.field_71439_g.field_82175_bq;}
    public boolean autoHealing(){return bindings.autoHealing();}
    public boolean consuming(){
        ItemStack stack=mc.field_71439_g.func_71011_bu();
        return stack!=null&&(stack.func_77975_n()==EnumAction.EAT||stack.func_77975_n()==EnumAction.DRINK);
    }
    public int currentSlot(){return mc.field_71439_g.field_71071_by.field_70461_c;}
    public Object stack(int slot){return mc.field_71439_g.field_71071_by.func_70301_a(slot);}
    public boolean allowedDestination(Object value){
        Item item=((ItemStack)value).func_77973_b();
        return !(item instanceof ItemFood||item instanceof ItemBlock||item instanceof ItemBow||item instanceof ItemSkull||item instanceof ItemArmor);
    }
    public void selectSlot(int slot){mc.field_71439_g.field_71071_by.field_70461_c=slot;}
    public void syncHeldItem(){if(mc.field_71442_b!=null)SwordSwapMinecraftAccess.sync(mc.field_71442_b);}
    public void showItem(Object stack,int selectedSlot){SwordSwapMinecraftAccess.show(mc.func_175597_ag(),stack,selectedSlot);}
}
