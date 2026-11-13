package myau.modules.scaffold;

import java.util.Objects;
import myau.modules.Scaffold;
import myau.InputUtils;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFalling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class ScaffoldMinecraft implements Scaffold.Host {
    public interface Bindings {
        boolean speedEnabled();
        boolean canPlace(Scaffold.Placement placement);
        boolean requestRotation(float yaw, float pitch, Scaffold.MoveFix moveFix);
        void releaseRotation();
        void requestSafeWalk(boolean value);
        void requestSneak(boolean value);
        void drawBlockCount(int count);
    }

    private final Minecraft mc;
    private final Bindings bindings;
    private int savedSlot=-1, selectedSlot=-1;
    private WorldClient slotWorld;

    public ScaffoldMinecraft(Minecraft mc, Bindings bindings) {
        this.mc=Objects.requireNonNull(mc,"minecraft");
        this.bindings=Objects.requireNonNull(bindings,"bindings");
    }

    @Override public boolean ready() {
        return mc.field_71439_g!=null && mc.field_71441_e!=null && mc.field_71442_b!=null && !mc.field_71439_g.field_70128_L;
    }

    @Override public Scaffold.Player player() {
        Scaffold.Player p=new Scaffold.Player();
        p.x=mc.field_71439_g.field_70165_t; p.feetY=mc.field_71439_g.func_174813_aQ().field_72338_b; p.z=mc.field_71439_g.field_70161_v;
        p.eyeY=mc.field_71439_g.field_70163_u+mc.field_71439_g.func_70047_e();
        p.motionX=mc.field_71439_g.field_70159_w; p.motionY=mc.field_71439_g.field_70181_x; p.motionZ=mc.field_71439_g.field_70179_y;
        p.yaw=mc.field_71439_g.field_70177_z; p.forward=mc.field_71439_g.field_71158_b.field_78900_b;
        p.strafe=mc.field_71439_g.field_71158_b.field_78902_a; p.onGround=mc.field_71439_g.field_70122_E;
        p.jumpDown=InputUtils.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i());
        p.sneakDown=InputUtils.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i());
        p.speedEnabled=bindings.speedEnabled();
        return p;
    }

    private static BlockPos pos(Scaffold.Pos p) { return new BlockPos(p.x,p.y,p.z); }
    private static EnumFacing face(Scaffold.Face f) { return EnumFacing.valueOf(f.name()); }

    @Override public double reach() { return mc.field_71442_b.func_78757_d(); }
    @Override public boolean replaceable(Scaffold.Pos p) {
        BlockPos at=pos(p);
        return mc.field_71441_e.func_180495_p(at).func_177230_c().func_176200_f(mc.field_71441_e,at);
    }
    @Override public boolean clickable(Scaffold.Pos p) {
        Block block=mc.field_71441_e.func_180495_p(pos(p)).func_177230_c();
        return block.func_149730_j() && !(block instanceof BlockContainer) && !block.func_149688_o().func_76224_d();
    }
    @Override public boolean visible(Scaffold.Placement p) {
        Scaffold.Player player=player();
        Vec3 eyes=new Vec3(player.x,player.eyeY,player.z);
        Vec3 hit=new Vec3(p.hitX-p.face.x*0.001,p.hitY-p.face.y*0.001,p.hitZ-p.face.z*0.001);
        MovingObjectPosition result=mc.field_71441_e.func_147447_a(eyes,hit,false,true,false);
        return result!=null && result.field_72313_a==MovingObjectPosition.MovingObjectType.BLOCK
                && result.func_178782_a().equals(pos(p.support)) && result.field_178784_b==face(p.face);
    }

    @Override public int blockCount(int slot) {
        ItemStack stack=mc.field_71439_g.field_71071_by.func_70301_a(slot);
        if (stack==null || stack.field_77994_a<=0 || !(stack.func_77973_b() instanceof ItemBlock)) return 0;
        Block block=((ItemBlock)stack.func_77973_b()).func_179223_d();
        // shortcut: conservative full-cube inventory filter; expand after recovering the original whitelist.
        return block.func_149730_j() && !(block instanceof BlockContainer) && !(block instanceof BlockFalling)
                && !block.func_149688_o().func_76224_d() ? stack.field_77994_a : 0;
    }
    @Override public int currentSlot() { return mc.field_71439_g.field_71071_by.field_70461_c; }
    public int getSavedSlot() { return savedSlot; }
    @Override public void selectSlot(int slot, boolean spoof) {
        if (slot<0 || slot>8) throw new IllegalArgumentException("hotbar slot: 0..8");
        if (savedSlot<0 || slotWorld!=mc.field_71441_e || currentSlot()!=selectedSlot) {
            savedSlot=currentSlot(); slotWorld=mc.field_71441_e;
        }
        mc.field_71439_g.field_71071_by.field_70461_c=slot;
        selectedSlot=slot;
        ((AccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
    }
    @Override public void restoreSlot() {
        if (savedSlot>=0 && ready() && slotWorld==mc.field_71441_e && currentSlot()==selectedSlot) {
            mc.field_71439_g.field_71071_by.field_70461_c=savedSlot;
            ((AccessorPlayerControllerMP)mc.field_71442_b).callSyncCurrentPlayItem();
        }
        savedSlot=-1; selectedSlot=-1; slotWorld=null;
    }
    @Override public boolean place(int slot, Scaffold.Placement p) {
        ItemStack stack=mc.field_71439_g.field_71071_by.func_70301_a(slot);
        if (stack==null || stack.field_77994_a<=0 || !bindings.canPlace(p)) return false;
        return mc.field_71442_b.func_178890_a(mc.field_71439_g,mc.field_71441_e,stack,pos(p.support),face(p.face),
                new Vec3(p.hitX,p.hitY,p.hitZ));
    }
    @Override public void swing() { mc.field_71439_g.func_71038_i(); }
    @Override public void setMotion(double x, double y, double z) {
        mc.field_71439_g.field_70159_w=x; mc.field_71439_g.field_70181_x=y; mc.field_71439_g.field_70179_y=z;
    }
    @Override public void setSprinting(boolean value) { mc.field_71439_g.func_70031_b(value); }
    @Override public void requestSafeWalk(boolean value) { bindings.requestSafeWalk(value); }
    @Override public void requestSneak(boolean value) { bindings.requestSneak(value); }
    @Override public boolean requestRotation(float yaw, float pitch, Scaffold.MoveFix moveFix) {
        return bindings.requestRotation(yaw,pitch,moveFix);
    }
    @Override public void releaseRotation() { bindings.releaseRotation(); }
    @Override public void drawBlockCount(int count) { bindings.drawBlockCount(count); }
}
