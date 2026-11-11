package myau;

import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

/** OpenB4 Displace, adapted to BetterMyau's click and rotation events. */
public class DisplaceModule extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final int[] DROP_DISTANCES = {1, 2, 3, 4, 6, 8};
    private static final int ROTATION_PRIORITY = 2;

    private static final int LEFT = 0, RIGHT = 1, RANDOM = 2, VOID = 3;

    public final ModeSetting mode = new ModeSetting("mode", LEFT, new String[]{"Left", "Right", "Random", "Void"}) {
        @Override
        public boolean readJson(com.google.gson.JsonObject json) {
            String previous = json.get(getName()).getAsString();
            if ("OFFSET".equalsIgnoreCase(previous) && json.has("direction")) {
                return parseValue(json.get("direction").getAsString());
            }
            return super.readJson(json);
        }
    };
    public final IntegerSetting angle = new IntegerSetting("angle", 100, 10, 180);
    public final FloatSetting cooldown = new FloatSetting("cooldown", 0.5f, 0.2f, 8.0f);
    public final BooleanSetting hideRotations = new BooleanSetting("hide-rotations", false);
    public final BooleanSetting onlyKnockbackItems = new BooleanSetting("only-knockback-items", true);
    public final BooleanSetting overrideAttacks = new BooleanSetting("override-attacks", true);
    public final IntegerSetting hittableHurtTime = new IntegerSetting("hittable-hurt-time", 3, 0, 8);
    public final BooleanSetting playersOnly = new BooleanSetting("players-only", false);
    public final BooleanSetting renderArrow = new BooleanSetting("render-arrow", true);
    public final BooleanSetting manual = new BooleanSetting("manual", true);
    public final BooleanSetting autoClicker = new BooleanSetting("auto-clicker", true);
    public final BooleanSetting killAura = new BooleanSetting("kill-aura", true);

    private EntityLivingBase pending;
    private boolean pendingAura;
    private boolean rotationRequested;
    private float displacedYaw;
    private float displacedPitch;
    private long lastAt;
    private int holdTicks;
    private int blinkTicks;
    private int lastSide = -1;
    private int side = -1;
    private EntityLivingBase arrowTarget;
    private float arrowYaw;
    private float arrowFadeStartAlpha;
    private float arrowFadeEndAlpha;
    private long arrowFadeStartMs;
    private boolean arrowVisible;
    private long arrowNudgeStartMs = -1L;
    private int arrowNudgePlayerId = -1;

    public DisplaceModule() {
        super("Displace", false, 0);
    }

    public String[] getHudInfo() {
        return new String[]{mode.getModeName()};
    }

    static DisplaceModule current() {
        return (DisplaceModule) MyauClient.moduleManager.modules.get(DisplaceModule.class);
    }

    boolean isPendingAuraAttack() {
        return pending != null && pendingAura;
    }

    private boolean qualifies(Entity target, boolean checkWindow) {
        if (!(target instanceof EntityLivingBase) || target.field_70128_L || mc.field_71439_g.func_70115_ae()
                || (playersOnly.getValue() && !(target instanceof EntityPlayer))) return false;
        int knockback = EnchantmentHelper.func_77501_a(mc.field_71439_g);
        if (knockback == 0 && (onlyKnockbackItems.getValue() || !mc.field_71439_g.func_70051_ag())) return false;
        if (checkWindow && (((EntityLivingBase) target).field_70737_aN > hittableHurtTime.getValue()
                || System.currentTimeMillis() - lastAt < cooldown.getValue() * 1000.0 - 20.0)) return false;
        if (mode.getValue() != VOID) return true;
        int drop = dropSide(target, lastSide);
        if (checkWindow) side = lastSide = drop;
        return drop != -1;
    }

    @EventHandler(priority = -25)
    public void onLeftClick(LeftClickEvent event) {
        if (!isEnabled() || event.isCancelled() || !manual.getValue() || mc.field_71439_g == null
                || mc.field_71441_e == null || mc.field_71476_x == null) return;
        AutoClickerModule clicker = (AutoClickerModule) MyauClient.moduleManager.modules.get(AutoClickerModule.class);
        if (!autoClicker.getValue() && clicker.isSyntheticAttack()) return;
        Entity target = mc.field_71476_x.field_72308_g;
        if (pending == null && qualifies(target, true)) {
            queue((EntityLivingBase) target, false);
            event.setCancelled(true);
        } else if (overrideAttacks.getValue() && qualifies(target, false)) {
            event.setCancelled(true);
        }
    }

    boolean interceptAura(EntityLivingBase target, RotationUpdateEvent event) {
        if (!isEnabled() || !killAura.getValue()) return false;
        if (pending != null) return true;
        if (System.currentTimeMillis() - lastAt < 60L) {
            holdTicks = 3;
            return false;
        }
        if (qualifies(target, true)) {
            queue(target, true);
            requestRotation(event);
            return true;
        }
        return holdTicks > 0 || (overrideAttacks.getValue() && qualifies(target, false));
    }

    private void queue(EntityLivingBase target, boolean fromAura) {
        pending = target;
        pendingAura = fromAura;
        rotationRequested = false;
        lastAt = System.currentTimeMillis();
    }

    private void requestRotation(RotationUpdateEvent event) {
        int direction = side;
        side = -1;
        if (direction == -1) {
            direction = mode.getValue() == LEFT || (mode.getValue() == RANDOM && java.util.concurrent.ThreadLocalRandom.current().nextBoolean())
                    ? LEFT : RIGHT;
        }
        float offset = (direction == LEFT ? -angle.getValue() : angle.getValue()) + RandomUtils.randomFloat(-5f, 5f);
        displacedYaw = event.getYaw() + offset;
        displacedPitch = event.getPitch();
        event.setRotation(displacedYaw, displacedPitch, ROTATION_PRIORITY);
        event.setMovementYaw(displacedYaw, ROTATION_PRIORITY);
        rotationRequested = true;
        showArrow(pending, displacedYaw);
        if (hideRotations.getValue() && MyauClient.outgoingPacketBuffer.setEnabled(true, OutgoingBufferOwner.DISPLACE)) {
            blinkTicks = 2;
        }
    }

    @EventHandler(priority = -45)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!isEnabled() || pending == null) return;
        if (!EntityUtils.isLoaded(pending) || pending.field_70128_L || mc.field_71462_r != null
                || System.currentTimeMillis() - lastAt >= 250L) {
            cancelPending();
            return;
        }
        if (!rotationRequested) {
            requestRotation(event);
            return;
        }
        if (Math.abs(MathHelper.func_76142_g(event.getPreviousYaw() - displacedYaw)) > 0.01f
                || Math.abs(event.getPreviousPitch() - displacedPitch) > 0.01f) {
            cancelPending();
            return;
        }
        if (MyauClient.packetActionTracker.sentDigging || MyauClient.packetActionTracker.sentBlockPlacement) return;

        EntityLivingBase target = pending;
        KillAuraModule aura = (KillAuraModule) MyauClient.moduleManager.modules.get(KillAuraModule.class);
        if (pendingAura) {
            if (!aura.replayDisplacedAttack(target)) {
                cancelPending();
                return;
            }
        } else {
            ReachModule reach = (ReachModule) MyauClient.moduleManager.modules.get(ReachModule.class);
            if (RotationUtils.distanceToEntity(target) > (reach.isEnabled() ? reach.range.getValue() : 3.0f)) {
                cancelPending();
                return;
            }
            mc.field_71439_g.func_71038_i();
            mc.field_71442_b.func_78764_a(mc.field_71439_g, target);
        }
        if (renderArrow.getValue()) {
            showArrow(target, displacedYaw);
            triggerArrowNudge(target);
        }
        pending = null;
        pendingAura = false;
        rotationRequested = false;
        holdTicks = 3;
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (isEnabled() && RotationState.isActive() && RotationState.movementPriority() == ROTATION_PRIORITY
                && MovementUtils.hasMovementInput()) MovementUtils.correctMovementForYaw(RotationState.movementYaw());
    }

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        if (holdTicks > 0) --holdTicks;
        if (blinkTicks > 0 && --blinkTicks == 0) releaseBlink(false);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        releaseBlink(true);
        onDisable();
    }

    private void releaseBlink(boolean discard) {
        if (MyauClient.outgoingPacketBuffer.owner() == OutgoingBufferOwner.DISPLACE) {
            if (discard) MyauClient.outgoingPacketBuffer.queuedPackets.clear();
            MyauClient.outgoingPacketBuffer.setEnabled(false, OutgoingBufferOwner.DISPLACE);
        }
        blinkTicks = 0;
    }

    private void cancelPending() {
        pending = null;
        pendingAura = false;
        rotationRequested = false;
        lastAt = 0L;
        hideArrow();
        releaseBlink(false);
    }

    @Override
    public void onDisable() {
        cancelPending();
        clearArrow();
        holdTicks = 0;
        side = lastSide = -1;
    }

    private int dropSide(Entity target, int previous) {
        double dx = target.field_70165_t - mc.field_71439_g.field_70165_t;
        double dz = target.field_70161_v - mc.field_71439_g.field_70161_v;
        double length = Math.hypot(dx, dz);
        if (length < 0.001) return -1;
        dx /= length;
        dz /= length;
        double px = -dz;
        double pz = dx;
        int behind = dropScore(target, dx, dz, px, pz);
        int left = dropScore(target, -px, -pz, dx, dz);
        int right = dropScore(target, px, pz, dx, dz);
        if (behind >= left && behind >= right) return -1;
        if (left == right && previous != -1) return previous;
        return left > right ? LEFT : RIGHT;
    }

    private int dropScore(Entity target, double dx, double dz, double crossX, double crossZ) {
        int score = 0;
        double y = target.field_70163_u;
        for (int distance : DROP_DISTANCES) {
            for (int cross = -1; cross <= 1; ++cross) {
                double x = target.field_70165_t + dx * distance + crossX * cross;
                double z = target.field_70161_v + dz * distance + crossZ * cross;
                if (mc.field_71441_e.func_72901_a(new Vec3(x, y, z), new Vec3(x, y - 6.0, z), true) == null) {
                    score += distance < 6 ? 2 : 1;
                }
            }
        }
        return score;
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent renderWorldEvent) {
        if (!this.isEnabled() || !((Boolean)this.renderArrow.getValue()).booleanValue()) {
            this.clearArrow();
            return;
        }
        if (this.arrowTarget == null || this.arrowTarget.field_70128_L || this.arrowTarget.field_70725_aQ != 0) {
            this.clearArrow();
            return;
        }
        long l = System.currentTimeMillis();
        double d = this.getArrowNudgeProgress(this.arrowTarget, l);
        if (d >= 1.0) {
            this.clearArrow();
            return;
        }
        float f = this.getArrowAlpha(l) * this.getArrowNudgeAlpha(d);
        if (f <= 0.0f) {
            if (!this.arrowVisible) {
                this.clearArrow();
            }
            return;
        }
        this.drawArrow(this.arrowTarget, this.arrowYaw, renderWorldEvent.getPartialTicks(), f, this.getArrowNudgeOffset(d));
    }

    private void showArrow(EntityLivingBase entityPlayer, float f) {
        if (!this.renderArrow.getValue()) {
            return;
        }
        long l = System.currentTimeMillis();
        boolean bl = this.arrowTarget != entityPlayer;
        float previousAlpha = bl ? 0.0f : this.getArrowAlpha(l);
        if (bl) {
            this.clearArrowNudge();
        }
        this.arrowTarget = entityPlayer;
        this.arrowYaw = f;
        if (!this.arrowVisible || bl) {
            this.arrowFadeStartAlpha = previousAlpha;
            this.arrowFadeEndAlpha = 1.0f;
            this.arrowFadeStartMs = l;
        }
        this.arrowVisible = true;
    }

    private void hideArrow() {
        if (this.arrowTarget == null || !this.arrowVisible) {
            return;
        }
        long l = System.currentTimeMillis();
        this.arrowFadeStartAlpha = this.getArrowAlpha(l);
        this.arrowFadeEndAlpha = 0.0f;
        this.arrowFadeStartMs = l;
        this.arrowVisible = false;
    }

    private float getArrowAlpha(long l) {
        float f = Math.min(1.0f, Math.max(0.0f, (float)(l - this.arrowFadeStartMs) / 250.0f));
        return this.arrowFadeStartAlpha + (this.arrowFadeEndAlpha - this.arrowFadeStartAlpha) * f;
    }

    private void triggerArrowNudge(EntityLivingBase entityPlayer) {
        long l = System.currentTimeMillis();
        if (this.arrowNudgePlayerId == entityPlayer.func_145782_y() && l - this.arrowNudgeStartMs < 320L) {
            return;
        }
        this.arrowNudgePlayerId = entityPlayer.func_145782_y();
        this.arrowNudgeStartMs = l;
    }

    private double getArrowNudgeProgress(EntityLivingBase entityPlayer, long l) {
        if (entityPlayer == null || this.arrowNudgePlayerId != entityPlayer.func_145782_y() || this.arrowNudgeStartMs < 0L) {
            return -1.0;
        }
        double d = (double)(l - this.arrowNudgeStartMs) / 320.0;
        return Math.min(1.0, Math.max(0.0, d));
    }

    private double getArrowNudgeOffset(double d) {
        if (d < 0.0) {
            return 0.0;
        }
        double d2 = Math.exp(-6.0);
        double d3 = (1.0 - Math.exp(-6.0 * d)) / (1.0 - d2);
        return d3 * 0.3;
    }

    private float getArrowNudgeAlpha(double d) {
        if (d < 0.0 || d <= 0.58) {
            return 1.0f;
        }
        double d2 = (d - 0.58) / 0.42000000000000004;
        double d3 = Math.exp(-5.0);
        return (float)((Math.exp(-5.0 * d2) - d3) / (1.0 - d3));
    }

    private void clearArrowNudge() {
        this.arrowNudgeStartMs = -1L;
        this.arrowNudgePlayerId = -1;
    }

    private void clearArrow() {
        this.arrowTarget = null;
        this.arrowFadeStartAlpha = 0.0f;
        this.arrowFadeEndAlpha = 0.0f;
        this.arrowFadeStartMs = 0L;
        this.arrowVisible = false;
        this.clearArrowNudge();
    }

    private void drawArrow(EntityLivingBase entityPlayer, float f, float f2, float f3, double d) {
        double d2 = entityPlayer.field_70142_S + (entityPlayer.field_70165_t - entityPlayer.field_70142_S) * (double)f2;
        double d3 = entityPlayer.field_70137_T + (entityPlayer.field_70163_u - entityPlayer.field_70137_T) * (double)f2 + (double)entityPlayer.field_70131_O * 0.5;
        double d4 = entityPlayer.field_70136_U + (entityPlayer.field_70161_v - entityPlayer.field_70136_U) * (double)f2;
        double d5 = Math.toRadians(f);
        double d6 = -Math.sin(d5);
        double d7 = Math.cos(d5);
        double d8 = (double)entityPlayer.field_70130_N * 0.5 + 0.3 + d;
        double d9 = d2 + d6 * d8;
        double d10 = d4 + d7 * d8;
        double d11 = d9 + d6 * 0.74;
        double d12 = d10 + d7 * 0.74;
        double d13 = d9 + d6 * 0.56;
        double d14 = d10 + d7 * 0.56;
        double d15 = d11 + d6 * 0.52;
        double d16 = d12 + d7 * 0.52;
        AccessorRenderManager accessorRenderManager = (AccessorRenderManager)mc.func_175598_ae();
        double d17 = accessorRenderManager.getRenderPosX();
        double d18 = accessorRenderManager.getRenderPosY();
        double d19 = accessorRenderManager.getRenderPosZ();
        GL11.glPushMatrix();
        GL11.glPushAttrib((int)1048575);
        GL11.glEnable((int)3042);
        GL11.glBlendFunc((int)770, (int)771);
        GL11.glDisable((int)3553);
        GL11.glDisable((int)2896);
        GL11.glDisable((int)2929);
        GL11.glDisable((int)2884);
        GL11.glDepthMask((boolean)false);
        GL11.glEnable((int)2848);
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)(0.82f * f3));
        GL11.glBegin((int)4);
        this.vertex(d9, d3, d10, 0.0, d17, d18, d19);
        this.vertex(d11, d3, d12, -0.08, d17, d18, d19);
        this.vertex(d11, d3, d12, 0.08, d17, d18, d19);
        this.vertex(d11, d3, d12, -0.08, d17, d18, d19);
        this.vertex(d13, d3, d14, -0.3, d17, d18, d19);
        this.vertex(d15, d3, d16, 0.0, d17, d18, d19);
        this.vertex(d11, d3, d12, -0.08, d17, d18, d19);
        this.vertex(d15, d3, d16, 0.0, d17, d18, d19);
        this.vertex(d11, d3, d12, 0.08, d17, d18, d19);
        this.vertex(d11, d3, d12, 0.08, d17, d18, d19);
        this.vertex(d15, d3, d16, 0.0, d17, d18, d19);
        this.vertex(d13, d3, d14, 0.3, d17, d18, d19);
        GL11.glEnd();
        GL11.glLineWidth((float)2.0f);
        GL11.glColor4f((float)0.0f, (float)0.0f, (float)0.0f, (float)(0.95f * f3));
        GL11.glBegin((int)2);
        this.vertex(d9, d3, d10, 0.0, d17, d18, d19);
        this.vertex(d11, d3, d12, -0.08, d17, d18, d19);
        this.vertex(d13, d3, d14, -0.3, d17, d18, d19);
        this.vertex(d15, d3, d16, 0.0, d17, d18, d19);
        this.vertex(d13, d3, d14, 0.3, d17, d18, d19);
        this.vertex(d11, d3, d12, 0.08, d17, d18, d19);
        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void vertex(double d, double d2, double d3, double d4, double d5, double d6, double d7) {
        GL11.glVertex3d((double)(d - d5), (double)(d2 + d4 - d6), (double)(d3 - d7));
    }

}
