package myau;

import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class LongJumpModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ElapsedTimer activationTimer = new ElapsedTimer();
    private final ElapsedTimer damageWaitTimer = new ElapsedTimer();
    private boolean jumpStarted;
    private int jumpTicks;
    private int jumpPhase;
    private boolean launchRotationReady;
    private boolean fireballLaunchAttempted;
    private int previousHotbarSlot = -1;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[] { "FIREBALL", "FIREBALL_MANUAL", "FIREBALL_HIGH", "FIREBALL_FLAT" });
    public final FloatSetting motion = new FloatSetting("motion", 1.0f, 1.0f, 20.0f);
    public final FloatSetting speedMotion = new FloatSetting("speed-motion", 1.0f, 1.0f, 20.0f);
    public final PercentSetting strafe = new PercentSetting("strafe", 0);

    public LongJumpModule() {
        super("LongJump", false, 0);
    }

    private int findFireballSlot() {
        if (minecraft.field_71439_g == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack == null || !(stack.func_77973_b() instanceof ItemFireball))
                continue;
            return slot;
        }
        return -1;
    }

    private double getConfiguredMotion() {
        return MovementUtils.speedPotionLevel() > 0 ? this.speedMotion.getValue().doubleValue() : this.motion.getValue().doubleValue();
    }

    public boolean isAutomaticFireballMode() {
        int currentMode = this.mode.getValue();
        return currentMode == 0 || currentMode == 2 || currentMode == 3;
    }

    public boolean isManualFireballMode() {
        return this.mode.getValue() == 1;
    }

    public boolean isFireballMode() {
        return this.isAutomaticFireballMode() || this.isManualFireballMode();
    }

    public boolean isAwaitingLaunchVelocity() {
        return !this.activationTimer.hasElapsed(1000L) && !this.jumpStarted;
    }

    public boolean hasJumpStarted() {
        return this.jumpStarted;
    }

    @EventHandler(priority = 100)
    public void onVelocity(VelocityEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            return;
        }
        if (!this.isManualFireballMode() && !this.isAutomaticFireballMode()) {
            return;
        }
        if (this.isAwaitingLaunchVelocity()) {
            event.setCancelled(true);
            this.jumpStarted = true;
            this.jumpTicks = 0;
        }
    }

    @EventHandler(priority = 100)
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled() || !this.isAutomaticFireballMode() || this.fireballLaunchAttempted || !this.launchRotationReady) {
            return;
        }
        int fireballSlot = this.findFireballSlot();
        if (fireballSlot == -1) {
            return;
        }
        this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
        minecraft.field_71439_g.field_71071_by.field_70461_c = fireballSlot;
        ((AccessorPlayerControllerMP) minecraft.field_71442_b).callSyncCurrentPlayItem();
        PacketUtils.send(new C08PacketPlayerBlockPlacement(minecraft.field_71439_g.func_70694_bm()));
        this.activationTimer.reset();
        this.fireballLaunchAttempted = true;
    }

    @EventHandler(priority = 100)
    public void onPostTick(PostTickEvent event) {
        if (this.previousHotbarSlot == -1) {
            return;
        }
        minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
        this.previousHotbarSlot = -1;
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled() || !this.isFireballMode()) {
            return;
        }
        if (this.jumpStarted) {
            ++this.jumpTicks;
            if (this.jumpTicks == 1) {
                switch(this.mode.getValue()) {
                    case 0:
                    case 1:
                        {
                            this.jumpPhase = 0;
                            break;
                        }
                    case 2:
                        {
                            this.jumpPhase = 1;
                            break;
                        }
                    case 3:
                        {
                            this.jumpPhase = MovementUtils.hasMovementInput() ? 2 : 1;
                            break;
                        }
                }
            }
            if (this.jumpTicks == 2 && MovementUtils.hasMovementInput()) {
                MovementUtils.setSpeedAlongMotion(MovementUtils.horizontalSpeed() * this.getConfiguredMotion());
            }
            if (this.jumpTicks >= 1 && this.jumpTicks <= 30) {
                switch(this.jumpPhase) {
                    case 1:
                        {
                            if (this.jumpTicks == 1) {
                                minecraft.field_71439_g.field_70181_x *= 0.75;
                                break;
                            }
                            double adjustedMotionY = minecraft.field_71439_g.field_70181_x / (double) 0.98f + 0.055;
                            if (!(adjustedMotionY > 0.0))
                                break;
                            minecraft.field_71439_g.field_70181_x = adjustedMotionY;
                            break;
                        }
                    case 2:
                        {
                            if (this.jumpTicks == 1) {
                                minecraft.field_71439_g.field_70181_x *= 0.75;
                                break;
                            }
                            minecraft.field_71439_g.field_70181_x = 0.01 + (double) this.jumpTicks * 0.003;
                            break;
                        }
                }
            }
            if (this.jumpTicks >= 30) {
                this.jumpStarted = false;
                this.jumpTicks = 0;
                this.jumpPhase = 0;
                if (this.isAutomaticFireballMode()) {
                    this.setEnabled(false);
                }
                return;
            }
        }
        if (!this.isAutomaticFireballMode() || this.jumpStarted) {
            return;
        }
        if (this.damageWaitTimer.hasElapsed(1500L)) {
            this.setEnabled(false);
            return;
        }
        this.launchRotationReady = true;
        float yaw = RotationUtils.snapToMouseSensitivity(minecraft.field_71439_g.field_70177_z - 180.0f - RandomUtils.randomFloat(0.0f, 1.0f), event.getPreviousYaw());
        float pitch = RotationUtils.snapToMouseSensitivity(89.0f + RandomUtils.randomFloat(-0.25f, 0.25f), event.getPreviousPitch());
        event.setRotation(yaw, pitch, 4);
        event.setMovementYaw(yaw, 4);
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (!(this.isEnabled()
                && RotationState.isActive()
                && RotationState.movementPriority() == 4.0f
                && MovementUtils.hasMovementInput())) {
            return;
        }
        MovementUtils.correctMovementForYaw(RotationState.movementYaw());
    }

    @EventHandler
    public void onMoveFlying(MoveFlyingEvent event) {
        if (!(this.isEnabled()
                && this.isFireballMode()
                && this.jumpStarted
                && this.jumpTicks >= 5
                && this.jumpTicks <= 30
                && this.strafe.getValue() > 0)) {
            return;
        }
        double horizontalSpeed = MovementUtils.horizontalSpeed();
        MovementUtils.setHorizontalMotion(horizontalSpeed * (double) ((float) (100 - this.strafe.getValue()) / 100.0f), MovementUtils.motionYaw());
        MovementUtils.addHorizontalMotion(horizontalSpeed * (double) ((float) this.strafe.getValue() / 100.0f), MovementUtils.movementYaw());
        MovementUtils.setSpeedAlongMotion(horizontalSpeed);
    }

    @EventHandler
    public void onKeyPress(KeyPressEvent event) {
        if (event.getKeyCode() != minecraft.field_71474_y.field_74313_G.func_151463_i()) {
            return;
        }
        ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70448_g();
        if (stack != null && stack.func_77973_b() instanceof ItemFireball) {
            this.activationTimer.reset();
        }
    }

    @EventHandler(priority = 50)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!this.isEnabled() || event.isCancelled() || !(event.getPacket() instanceof S08PacketPlayerPosLook)) {
            return;
        }
        this.jumpStarted = false;
        this.jumpTicks = 0;
        this.jumpPhase = 0;
        if (this.isAutomaticFireballMode()) {
            this.setEnabled(false);
        }
    }

    @Override
    public void onEnable() {
        this.damageWaitTimer.reset();
        if (this.isAutomaticFireballMode() && this.findFireballSlot() == -1) {
            this.setEnabled(false);
            ChatUtils.addFormattedMessage(String.format("%s%s: &cNo fireball found in your hotbar!&r", "&7[&bMyau&7] &r", this.getName()));
        }
    }

    @Override
    public void onDisable() {
        this.jumpStarted = false;
        this.jumpTicks = 0;
        this.jumpPhase = 0;
        this.launchRotationReady = false;
        this.fireballLaunchAttempted = false;
    }

    @Override
    public String[] getHudInfo() {
        return new String[] { "Fireball" };
    }
}
