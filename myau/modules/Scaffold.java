package myau.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class Scaffold {
    public enum Rotations { NONE, DEFAULT, BACKWARDS, SIDEWAYS, STABLE }
    public enum MoveFix { NONE, SILENT }
    public enum Sprint { NONE, VANILLA }
    public enum Tower { NONE, VANILLA, EXTRA, TELLY }
    public enum VerticalTower { NONE, PREDICTION }
    public enum KeepY { NONE, VANILLA, EXTRA, TELLY, OLDNCP }
    public enum Face {
        DOWN(0,-1,0), UP(0,1,0), NORTH(0,0,-1), SOUTH(0,0,1), WEST(-1,0,0), EAST(1,0,0);
        public final int x, y, z;
        Face(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
        public Face opposite() { return values()[ordinal() ^ 1]; }
    }

    public static final class Pos {
        public final int x, y, z;
        public Pos(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
        public Pos offset(Face face) { return new Pos(x+face.x,y+face.y,z+face.z); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Pos)) return false;
            Pos p=(Pos)o; return x==p.x && y==p.y && z==p.z;
        }
        @Override public int hashCode() { return (x*31+y)*31+z; }
    }

    public static final class Player {
        public double x, feetY, z, eyeY, motionX, motionY, motionZ;
        public float yaw, forward, strafe;
        public boolean onGround, jumpDown, sneakDown, speedEnabled;
        public boolean moving() { return forward != 0 || strafe != 0; }
    }

    public static final class Placement {
        public final Pos target, support;
        public final Face face;
        public final double hitX, hitY, hitZ;
        public final float yaw, pitch;
        Placement(Pos target, Pos support, Face face, double x, double y, double z, Player p) {
            this.target=target; this.support=support; this.face=face;
            hitX=x; hitY=y; hitZ=z;
            double dx=x-p.x, dy=y-p.eyeY, dz=z-p.z;
            yaw=(float)Math.toDegrees(Math.atan2(dz,dx))-90.0F;
            pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        }
    }

    public static final class Settings {
        public Rotations rotations=Rotations.BACKWARDS;
        public MoveFix moveFix=MoveFix.SILENT;
        public Sprint sprint=Sprint.NONE;
        public int groundMotion=100, airMotion=100, speedMotion=100;
        public Tower tower=Tower.NONE;
        public VerticalTower verticalTower=VerticalTower.NONE;
        public boolean towerOnPress;
        public KeepY keepY=KeepY.NONE;
        public boolean keepYOnPress;
        public int sneak, sneakTicks=1;
        public boolean multiPlace=true, safeWalk=true, swing=true, itemSpoof, blockCounter=true;

        public void validate() {
            Objects.requireNonNull(rotations,"rotations"); Objects.requireNonNull(moveFix,"moveFix");
            Objects.requireNonNull(sprint,"sprint"); Objects.requireNonNull(tower,"tower");
            Objects.requireNonNull(verticalTower,"verticalTower"); Objects.requireNonNull(keepY,"keepY");
            for (int value : new int[]{groundMotion,airMotion,speedMotion})
                if (value<0 || value>100) throw new IllegalArgumentException("motion: 0..100");
            if (sneak<0 || sneak>7 || sneakTicks<1 || sneakTicks>10)
                throw new IllegalArgumentException("sneak: 0..7; sneakTicks: 1..10");
        }
    }

    public interface Host {
        boolean ready();
        Player player();
        double reach();
        boolean replaceable(Pos pos);
        boolean clickable(Pos pos);
        boolean visible(Placement placement);
        int blockCount(int slot);
        int currentSlot();
        void selectSlot(int slot, boolean spoof);
        void restoreSlot();
        boolean place(int slot, Placement placement);
        void swing();
        void setMotion(double x, double y, double z);
        void setSprinting(boolean value);
        void requestSafeWalk(boolean value);
        void requestSneak(boolean value);
        boolean requestRotation(float yaw, float pitch, MoveFix moveFix);
        void releaseRotation();
        void drawBlockCount(int count);
    }

    public final Settings settings=new Settings();
    private final Host host;
    private boolean enabled, anchorSet, rotationOwned;
    private int anchorY, placedSinceSneak, sneakRemaining, placedSinceJump;
    private int movementTick=Integer.MIN_VALUE, placementTick=Integer.MIN_VALUE;
    private float stableYaw=Float.NaN;
    private Placement lastPlacement;

    public Scaffold(Host host) { this.host=Objects.requireNonNull(host,"host"); }
    public boolean isEnabled() { return enabled; }
    public Placement lastPlacement() { return lastPlacement; }
    public int placementLevel() { return anchorY+1; }

    public void setEnabled(boolean value) {
        settings.validate();
        if (enabled==value) return;
        enabled=value; reset();
    }

    public void onWorldChange() { reset(); }

    public void onAfterMovementInput(int tick) {
        settings.validate();
        if (!enabled || !host.ready()) { reset(); return; }
        if (movementTick==tick) return;
        movementTick=tick;
        Player p=host.player();
        if (!anchorSet) { anchorY=floor(p.feetY)-1; anchorSet=true; }
        boolean keepY=settings.keepY!=KeepY.NONE && (!settings.keepYOnPress || p.jumpDown);
        if (!keepY || p.feetY<anchorY+1) anchorY=floor(p.feetY)-1;
        host.requestSafeWalk(settings.safeWalk && p.onGround);
        host.requestSneak(p.sneakDown || sneakRemaining>0);
        if (sneakRemaining>0) sneakRemaining--;
        host.setSprinting(settings.sprint==Sprint.VANILLA && p.moving() && !p.sneakDown);
        double factor=(p.onGround?settings.groundMotion:settings.airMotion)/100.0;
        if (p.speedEnabled) factor*=settings.speedMotion/100.0;
        double y=p.motionY;
        // shortcut: variant movement policies are inferred; exact native tower/keep-y timing needs traces.
        boolean tower=settings.tower!=Tower.NONE && (!settings.towerOnPress || p.jumpDown)
                && (!p.moving() || settings.tower==Tower.TELLY);
        if (tower && p.onGround) y=0.41999998688697815;
        if (tower && settings.tower==Tower.EXTRA && !p.onGround && y<0 && fraction(p.feetY)<0.1)
            y=-fraction(p.feetY);
        if (keepY && p.moving() && p.onGround) {
            if (settings.keepY==KeepY.EXTRA
                    || settings.keepY==KeepY.TELLY && placedSinceJump>=2
                    || settings.keepY==KeepY.OLDNCP) { y=0.41999998688697815; placedSinceJump=0; }
        }
        host.setMotion(p.motionX*factor,y,p.motionZ*factor);
    }

    public int onMotion(int tick) {
        settings.validate();
        if (!enabled || !host.ready()) { reset(); return 0; }
        if (placementTick==tick) return 0;
        placementTick=tick;
        Player p=host.player();
        int slot=bestSlot();
        if (slot<0) { releaseRotation(); lastPlacement=null; host.restoreSlot(); return 0; }
        if (!anchorSet) { anchorY=floor(p.feetY)-1; anchorSet=true; }
        int y=settings.keepY!=KeepY.NONE && (!settings.keepYOnPress || p.jumpDown)
                ? anchorY : floor(p.feetY)-1;
        double x=p.x, z=p.z;
        if (settings.verticalTower==VerticalTower.PREDICTION && !p.onGround) {
            if (settings.keepY==KeepY.NONE || settings.keepYOnPress && !p.jumpDown)
                y=Math.min(y,floor(p.feetY+p.motionY)-1);
        }
        List<Pos> targets=new ArrayList<>();
        targets.add(new Pos(floor(x),y,floor(z)));
        if (settings.multiPlace) {
            for (double dx:new double[]{-0.29,0.29}) for (double dz:new double[]{-0.29,0.29}) {
                Pos pos=new Pos(floor(x+dx),y,floor(z+dz));
                if (!targets.contains(pos)) targets.add(pos);
            }
        }
        int placed=0;
        lastPlacement=null;
        try {
            for (Pos target:targets) {
                if (host.blockCount(slot)<=0) break;
                Placement plan=findPlacement(target,p);
                if (plan==null) continue;
                if (settings.rotations!=Rotations.NONE) {
                    float yaw=plan.yaw;
                    if (!host.requestRotation(yaw,plan.pitch,settings.moveFix)) continue;
                    rotationOwned=true;
                    stableYaw=yaw;
                } else releaseRotation();
                host.selectSlot(slot,settings.itemSpoof);
                if (!host.place(slot,plan)) continue;
                lastPlacement=plan;
                placed++; placedSinceJump++; placedSinceSneak++;
                if (settings.swing) host.swing();
                if (settings.sneak>0 && placedSinceSneak>=settings.sneak) {
                    placedSinceSneak=0; sneakRemaining=settings.sneakTicks;
                }
                if (!settings.multiPlace) break;
            }
        } catch (RuntimeException | Error failure) {
            releaseRotation();
            lastPlacement=null;
            throw failure;
        } finally {
            if (settings.itemSpoof) host.restoreSlot();
        }
        if (lastPlacement==null) releaseRotation();
        return placed;
    }

    private int bestSlot() {
        int current=host.currentSlot(), best=-1, count=0;
        for (int slot=0;slot<9;slot++) {
            int n=host.blockCount(slot);
            if (n>count || n==count && n>0 && slot==current) { best=slot; count=n; }
        }
        return best;
    }

    private Placement findPlacement(Pos target, Player p) {
        if (target.y<0 || target.y>=256 || !host.replaceable(target)) return null;
        List<Placement> candidates=new ArrayList<>();
        for (Face direction:Face.values()) {
            Pos support=target.offset(direction);
            if (support.y<0 || support.y>=256 || !host.clickable(support)) continue;
            Face face=direction.opposite();
            // shortcut: nine samples replace the original fine face grid until that helper is ported.
            for (double a:new double[]{0.25,0.5,0.75}) for (double b:new double[]{0.25,0.5,0.75}) {
                double hx=support.x+(face.x==0?a:face.x>0?1:0);
                double hy=support.y+(face.y==0?(face.x==0?b:a):face.y>0?1:0);
                double hz=support.z+(face.z==0?b:face.z>0?1:0);
                Placement plan=new Placement(target,support,face,hx,hy,hz,p);
                if (distanceSquared(plan,p)<=host.reach()*host.reach() && host.visible(plan)) candidates.add(plan);
            }
        }
        candidates.sort(Comparator.comparingDouble(plan->aimScore(plan,p)));
        return candidates.isEmpty()?null:candidates.get(0);
    }

    private double aimScore(Placement plan, Player p) {
        float preferred=plan.yaw;
        switch (settings.rotations) {
            case BACKWARDS: preferred=p.yaw+180; break;
            case SIDEWAYS: preferred=p.yaw+90; break;
            case STABLE:
                preferred=Float.isNaN(stableYaw)?p.yaw:stableYaw; break;
            default: break;
        }
        double angle=(plan.yaw-preferred)%360;
        if (angle>180) angle-=360;
        if (angle<-180) angle+=360;
        return Math.abs(angle)+distanceSquared(plan,p)*0.01;
    }

    private static double distanceSquared(Placement plan, Player p) {
        double x=plan.hitX-p.x,y=plan.hitY-p.eyeY,z=plan.hitZ-p.z;
        return x*x+y*y+z*z;
    }

    private static int floor(double v) { return (int)Math.floor(v); }
    private static double fraction(double v) { return v-Math.floor(v); }

    public void onRender() {
        if (!enabled || !host.ready() || !settings.blockCounter) return;
        int count=0;
        for (int slot=0;slot<9;slot++) count+=host.blockCount(slot);
        host.drawBlockCount(count);
    }

    private void releaseRotation() {
        if (rotationOwned) host.releaseRotation();
        rotationOwned=false;
    }

    private void reset() {
        releaseRotation(); host.restoreSlot(); host.requestSafeWalk(false); host.requestSneak(false);
        anchorSet=false; stableYaw=Float.NaN; lastPlacement=null;
        placedSinceSneak=0; sneakRemaining=0; placedSinceJump=0;
        movementTick=Integer.MIN_VALUE; placementTick=Integer.MIN_VALUE;
    }
}
