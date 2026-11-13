import java.util.*;
import myau.modules.Scaffold;

public final class ScaffoldCoreRegression {
    static int checks;
    static void check(boolean value,String why) { checks++; if (!value) throw new AssertionError(why); }
    static void near(double actual,double expected,String why) { check(Math.abs(actual-expected)<1e-6,why+": "+actual+" vs "+expected); }
    static void invalid(Runnable action,String why) {
        boolean failed=false;
        try { action.run(); } catch (IllegalArgumentException|NullPointerException expected) { failed=true; }
        check(failed,why);
    }

    static final class ScaffoldHost implements Scaffold.Host {
        final Scaffold.Player p=new Scaffold.Player();
        final Set<Scaffold.Pos> solid=new HashSet<>();
        final int[] blocks=new int[9];
        boolean ready=true, visible=true, acceptRotation=true, failPlace, throwPlace, safe, sneak, sprint;
        int slot, saved=-1, selected=-1, placed, swings, rotations, releases, restoreCalls, rendered=-1;
        double reach=4.5;
        Scaffold.Placement plan;
        ScaffoldHost() {
            p.x=.5;p.feetY=64;p.z=.5;p.eyeY=65.62;p.onGround=true;p.motionX=1;p.motionZ=2;p.forward=1;
            blocks[0]=16;solid.add(new Scaffold.Pos(0,62,0));
        }
        public boolean ready(){return ready;}
        public Scaffold.Player player(){return p;}
        public double reach(){return reach;}
        public boolean replaceable(Scaffold.Pos p){return !solid.contains(p);}
        public boolean clickable(Scaffold.Pos p){return solid.contains(p);}
        public boolean visible(Scaffold.Placement p){return visible;}
        public int blockCount(int slot){return blocks[slot];}
        public int currentSlot(){return slot;}
        public void selectSlot(int value,boolean spoof){if(saved<0)saved=slot;slot=value;selected=value;}
        public void restoreSlot(){restoreCalls++;if(saved>=0&&slot==selected)slot=saved;saved=-1;selected=-1;}
        public boolean place(int slot,Scaffold.Placement value){
            if(throwPlace)throw new IllegalStateException("fixture controller failure");
            plan=value;if(failPlace)return false;
            check(value.support.offset(value.face).equals(value.target),"support/face reaches intended target");
            check(solid.contains(value.support)&&!solid.contains(value.target),"support solid / target empty at placement");
            solid.add(value.target);blocks[slot]--;placed++;return true;
        }
        public void swing(){swings++;}
        public void setMotion(double x,double y,double z){p.motionX=x;p.motionY=y;p.motionZ=z;}
        public void setSprinting(boolean value){sprint=value;}
        public void requestSafeWalk(boolean value){safe=value;}
        public void requestSneak(boolean value){sneak=value;}
        public boolean requestRotation(float yaw,float pitch,Scaffold.MoveFix fix){rotations++;return acceptRotation;}
        public void releaseRotation(){releases++;}
        public void drawBlockCount(int count){rendered=count;}
    }

    static void scaffoldChecks() {
        int start=checks;
        for (Scaffold.Face face:Scaffold.Face.values()) check(face.opposite().opposite()==face,"opposite "+face);
        ScaffoldHost h=new ScaffoldHost();Scaffold m=new Scaffold(h);
        check(m.settings.rotations==Scaffold.Rotations.BACKWARDS && m.settings.moveFix==Scaffold.MoveFix.SILENT,"rotation defaults");
        check(m.settings.sprint==Scaffold.Sprint.NONE && m.settings.tower==Scaffold.Tower.NONE
                && m.settings.verticalTower==Scaffold.VerticalTower.NONE && m.settings.keepY==Scaffold.KeepY.NONE,"mode defaults");
        check(m.settings.groundMotion==100 && m.settings.airMotion==100 && m.settings.speedMotion==100,"motion defaults");
        check(m.settings.multiPlace && m.settings.safeWalk && m.settings.swing && m.settings.blockCounter
                && !m.settings.itemSpoof && !m.settings.towerOnPress && !m.settings.keepYOnPress,"flag defaults");
        check(m.onMotion(1)==0,"disabled no placement");m.setEnabled(true);m.onAfterMovementInput(1);
        check(h.safe && !h.sprint,"ground safe walk and disabled sprint");
        near(h.p.motionX,1,"default horizontal motion");
        check(m.onMotion(1)==1 && h.placed==1 && h.swings==1,"basic full placement");
        check(h.plan.target.equals(new Scaffold.Pos(0,63,0)) && h.plan.face==Scaffold.Face.UP,"below feet and correct face");
        check(h.plan.pitch>=-90 && h.plan.pitch<=90 && Float.isFinite(h.plan.yaw),"finite aim ray");
        check(m.onMotion(1)==0 && h.placed==1,"duplicate event protection");
        check(m.onMotion(2)==0,"don't place into occupied target");
        m.onRender();check(h.rendered==15,"live block count");m.settings.blockCounter=false;h.rendered=-1;m.onRender();check(h.rendered==-1,"counter option");
        m.settings.groundMotion=50;m.settings.speedMotion=50;h.p.speedEnabled=true;
        m.onAfterMovementInput(2);near(h.p.motionX,.25,"ground/speed factors");
        m.onAfterMovementInput(2);near(h.p.motionX,.25,"do not rescale twice per tick");
        h.p.onGround=false;m.settings.airMotion=40;h.p.motionX=1; m.onAfterMovementInput(3);near(h.p.motionX,.2,"air/speed factors");
        check(!h.safe,"safe walk not forced in air");m.settings.sprint=Scaffold.Sprint.VANILLA;
        h.p.sneakDown=true;m.onAfterMovementInput(4);check(!h.sprint&&h.sneak,"physical sneak preserved");
        h.p.sneakDown=false;m.onAfterMovementInput(5);check(h.sprint&&!h.sneak,"vanilla sprint restored");
        m.setEnabled(false);check(!h.safe&&!h.sneak&&h.saved==-1,"disable releases ownership");

        h=new ScaffoldHost();m=new Scaffold(h);m.setEnabled(true);h.visible=false;
        check(m.onMotion(1)==0 && h.rotations==0,"blocked ray not placed");h.visible=true;h.reach=1;
        check(m.onMotion(2)==0,"reach checked");h.reach=4.5;h.blocks[0]=0;
        check(m.onMotion(3)==0,"empty inventory");h.blocks[3]=32;
        h.acceptRotation=false;check(m.onMotion(4)==0 && h.placed==0,"lost rotation ownership stops placement");
        h.acceptRotation=true;m.settings.rotations=Scaffold.Rotations.NONE;m.settings.itemSpoof=true;
        int rotationCalls=h.rotations;check(m.onMotion(5)==1 && h.slot==0,"spoof restores original slot");
        check(h.rotations==rotationCalls,"none does not submit rotations");
        h.solid.remove(new Scaffold.Pos(0,63,0));h.throwPlace=true;
        m.settings.rotations=Scaffold.Rotations.DEFAULT;
        boolean threw=false;try{m.onMotion(6);}catch(IllegalStateException expected){threw=true;}
        check(threw && h.slot==0 && h.saved==-1 && h.releases>0 && m.lastPlacement()==null,
                "throwing controller restores spoof slot and releases aim ownership");

        h=new ScaffoldHost();h.p.x=.99;h.p.z=.99;
        for(int x=0;x<2;x++)for(int z=0;z<2;z++)h.solid.add(new Scaffold.Pos(x,62,z));
        m=new Scaffold(h);m.settings.sneak=2;m.settings.sneakTicks=2;m.setEnabled(true);
        check(m.onMotion(1)==4,"multi-place covers four footprint cells");
        m.onAfterMovementInput(1);check(h.sneak,"placement count starts sneak interval");
        m.onAfterMovementInput(2);check(h.sneak,"configured sneak duration second tick");
        m.onAfterMovementInput(3);check(!h.sneak,"sneak interval ends");

        h=new ScaffoldHost();h.p.x=-.2;h.p.z=-.2;h.solid.clear();h.solid.add(new Scaffold.Pos(-1,62,-1));
        m=new Scaffold(h);m.settings.multiPlace=false;m.setEnabled(true);
        check(m.onMotion(1)==1 && h.plan.target.equals(new Scaffold.Pos(-1,63,-1)),"negative coordinates floor correctly");
        m.onWorldChange();check(m.lastPlacement()==null,"world clears placement");h.ready=false;
        check(m.onMotion(2)==0,"world unavailable");

        for(Scaffold.Tower tower:Scaffold.Tower.values()) {
            h=new ScaffoldHost();h.p.forward=0;m=new Scaffold(h);m.settings.tower=tower;m.setEnabled(true);
            m.onAfterMovementInput(1);near(h.p.motionY,tower==Scaffold.Tower.NONE?0:.41999998688697815,"tower policy "+tower);
            m.settings.towerOnPress=true;h.p.motionY=0;m.onAfterMovementInput(2);near(h.p.motionY,0,"tower press gate");
        }
        for(Scaffold.KeepY mode:Scaffold.KeepY.values()) {
            h=new ScaffoldHost();m=new Scaffold(h);m.settings.keepY=mode;m.setEnabled(true);m.onAfterMovementInput(1);
            h.p.feetY=65;h.p.eyeY=66.62;h.solid.add(new Scaffold.Pos(0,63,0));h.solid.remove(new Scaffold.Pos(0,62,0));
            m.onMotion(1);
            if(mode==Scaffold.KeepY.NONE)check(h.plan!=null&&h.plan.target.y==64,"none follows feet");
            else check(h.plan==null,"keep-y doesn't place above anchor "+mode);
        }
        for(Scaffold.Rotations rotations:Scaffold.Rotations.values()) {
            h=new ScaffoldHost();m=new Scaffold(h);m.settings.rotations=rotations;m.setEnabled(true);
            check(m.onMotion(1)==1,"reachable rotation policy "+rotations);
            check((h.rotations>0)==(rotations!=Scaffold.Rotations.NONE),"rotation submission "+rotations);
        }
        final Scaffold invalidModule=new Scaffold(new ScaffoldHost());
        invalidModule.settings.sneak=8;invalid(()->invalidModule.settings.validate(),"invalid sneak");invalidModule.settings.sneak=0;
        invalidModule.settings.sneakTicks=0;invalid(()->invalidModule.settings.validate(),"invalid duration");invalidModule.settings.sneakTicks=1;
        invalidModule.settings.airMotion=101;invalid(()->invalidModule.settings.validate(),"invalid percent");invalidModule.settings.airMotion=100;
        invalidModule.settings.keepY=null;invalid(()->invalidModule.settings.validate(),"null mode");
        System.out.println("Scaffold="+(checks-start));
    }

    public static void main(String[] args) { scaffoldChecks(); System.out.println("Scaffold core checks passed: " + checks); }
}
