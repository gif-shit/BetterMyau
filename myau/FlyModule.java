package myau;

import myau.mixin.AccessorC03PacketPlayer;
import myau.mixin.AccessorEntity;
import myau.mixin.AccessorMinecraft;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class FlyModule extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final ModeSetting flightMode = new ModeSetting("flight-mode", 0, new String[]{"Rewound A", "Rewound B"});
    public final ModeSetting mode = new ModeSetting("mode", 1, new String[]{"VANILLA", "OLDNCP"}, () -> flightMode.getValue() == 0);
    public final BooleanSetting viewBobbing = new BooleanSetting("view-bobbing", true, () -> flightMode.getValue() == 0);
    public final ModeSetting oldNcpMode = new ModeSetting("oldncp-mode", 1, new String[]{"SLOW", "DAMAGE"}, this::isOldNcp);
    public final BooleanSetting dropFly = new BooleanSetting("drop-fly", false, this::isOldNcp);
    public final FloatSetting speed = new FloatSetting("speed", 4.25f, 1.0f, 5.0f, this::isDamage);
    public final PercentSetting decay = new PercentSetting("decay", 40, 20, 100, this::isDamage);
    public final ModeSetting damageMode = new ModeSetting("damage-mode", 0, new String[]{"INSTANT", "BOW"}, this::isDamage);
    public final BooleanSetting timerBoost = new BooleanSetting("timer-boost", false, this::isOldNcp);
    public final FloatSetting timer = new FloatSetting("timer", 2.0f, 1.0f, 30.0f, () -> isOldNcp() && timerBoost.getValue());
    public final IntegerSetting timerDuration = new IntegerSetting("timer-duration", 400, 50, 1000, () -> isOldNcp() && timerBoost.getValue());
    public final FloatSetting horizontalSpeed = new FloatSetting("horizontal-speed", 1.0f, 0.0f, 100.0f, () -> flightMode.getValue() == 0 && (mode.getValue() == 0 || this.dropActive));
    public final BooleanSetting vertical = new BooleanSetting("vertical", false, () -> isOldNcp() && !this.dropActive);
    public final FloatSetting verticalSpeed = new FloatSetting("vertical-speed", 1.0f, 0.0f, 100.0f, () -> flightMode.getValue() == 0 && (mode.getValue() == 0 || this.dropActive || (isOldNcp() && vertical.getValue())));
    public final IntegerSetting activationTimeV = new IntegerSetting("activation-time-v", 45, 1, 60, () -> flightMode.getValue() == 1);
    public final IntegerSetting activationTimeH = new IntegerSetting("activation-time-h", 2, 1, 50, () -> flightMode.getValue() == 1);

    private boolean receivedVelocity, wasOnGround, dropActive, waitingForGround;
    private double referenceHeight = Double.NaN;
    private double moveSpeed, lastDistance, verticalMotion, decayAccumulator, baselineSpeed, decayGap;
    private int decayTicks, airTicks, bowStage, bowTicks, velocityWaitTicks;
    private int bowSlot = -1, previousSlot = -1;
    private long boostStartedAt;
    private boolean timerActive;
    private float savedTimerSpeed = 1.0f;
    private int verticalDelay, horizontalDelay;
    private double lastMotionY;
    private boolean hasPreviousMotion, rewoundBActive;

    public FlyModule() {
        super("Fly", false, 0);
    }

    private boolean isOldNcp() {
        return flightMode.getValue() == 0 && mode.getValue() == 1;
    }

    private boolean isDamage() {
        return isOldNcp() && oldNcpMode.getValue() == 1;
    }

    private boolean timerModuleActive() {
        Module module = MyauClient.moduleManager == null ? null : MyauClient.moduleManager.modules.get(TimerModule.class);
        return module != null && module.isEnabled();
    }

    private void updateHeight() {
        EntityPlayerSP player = minecraft.field_71439_g;
        if (player == null) referenceHeight = Double.NaN;
        else if (player.field_70122_E || Double.isNaN(referenceHeight) || player.field_70163_u > referenceHeight) referenceHeight = player.field_70163_u;
    }

    private void startBoost() {
        if (!timerBoost.getValue() || timerModuleActive()) return;
        if (timerActive) restoreTimer();
        savedTimerSpeed = ((AccessorMinecraft)minecraft).getTimer().field_74278_d;
        ((AccessorMinecraft)minecraft).getTimer().field_74278_d = timer.getValue();
        timerActive = true;
    }

    private void restoreTimer() {
        if (!timerActive) return;
        timerActive = false;
        if (!timerModuleActive()) ((AccessorMinecraft)minecraft).getTimer().field_74278_d = savedTimerSpeed;
    }

    private void selectSlot(int slot) {
        if (minecraft.field_71439_g == null || slot < 0 || slot > 8) return;
        PacketUtils.send(new C09PacketHeldItemChange(slot));
        ((AccessorPlayerControllerMP)minecraft.field_71442_b).setCurrentPlayerItem(slot);
        minecraft.field_71439_g.field_71071_by.field_70461_c = slot;
    }

    private void restoreSlot() {
        if (previousSlot >= 0 && previousSlot <= 8 && previousSlot != bowSlot) selectSlot(previousSlot);
        bowSlot = previousSlot = -1;
    }

    private void sendPosition(double y, boolean ground) {
        EntityPlayerSP player = minecraft.field_71439_g;
        PacketUtils.send(new C03PacketPlayer.C04PacketPlayerPosition(player.field_70165_t, y, player.field_70161_v, ground));
    }

    private void sendLook() {
        EntityPlayerSP player = minecraft.field_71439_g;
        PacketUtils.send(new C03PacketPlayer.C05PacketPlayerLook(player.field_70177_z, -90.0f, player.field_70122_E));
    }

    private int potionLevel(Potion potion) {
        EntityPlayerSP player = minecraft.field_71439_g;
        return player.func_70644_a(potion) ? player.func_70660_b(potion).func_76458_c() + 1 : 0;
    }

    private void prepareDamage() {
        EntityPlayerSP player = minecraft.field_71439_g;
        if (player == null || minecraft.field_71441_e == null || player.field_70128_L) return;
        if (damageMode.getValue() == 1) {
            bowStage = bowTicks = velocityWaitTicks = 0;
            bowSlot = -1;
            for (int slot = 0; slot < 9; ++slot) {
                ItemStack stack = player.field_71071_by.func_70301_a(slot);
                if (stack != null && stack.func_77973_b() instanceof ItemBow) { bowSlot = slot; break; }
            }
            if (bowSlot < 0) {
                ChatUtils.addFormattedMessage("&7[&bMyau&7] &rFly: &cNo bow found in your hotbar!&r");
                setEnabled(false);
                return;
            }
            boolean arrows = player.field_71075_bZ.field_75098_d;
            for (int slot = 0; slot < 36 && !arrows; ++slot) {
                ItemStack stack = player.field_71071_by.func_70301_a(slot);
                arrows = stack != null && stack.func_77973_b() == Items.field_151032_g;
            }
            if (!arrows) {
                ChatUtils.addFormattedMessage("&7[&bMyau&7] &rFly: &cNo arrows found in your inventory!&r");
                setEnabled(false);
                return;
            }
            previousSlot = player.field_71071_by.field_70461_c;
            selectSlot(bowSlot);
            bowStage = 1;
            receivedVelocity = false;
            return;
        }
        double distance = (double)(player.field_70143_R + (float)potionLevel(Potion.field_76430_j)) + 1.0e-5;
        for (double offset = 0.0; offset < distance; offset += 0.06249) {
            sendPosition(player.field_70163_u + 0.06249, false);
            sendPosition(player.field_70163_u, false);
        }
        double y = player.func_174813_aQ().field_72338_b;
        for (int tick = 0; tick < 60; ++tick) {
            sendPosition(y + 0.13029834580989086 + 7.045809890852092e-4 * RandomUtils.randomDouble(0.0, 1.0), false);
            sendPosition(y + 0.07029834580989085 + 7.045809890852092e-4 * RandomUtils.randomDouble(0.0, 1.0), false);
        }
        sendPosition(y, true);
    }

    private void advanceBow() {
        if (bowStage == 0) return;
        if (bowStage == 3) {
            if (!receivedVelocity && ++velocityWaitTicks >= 30) {
                ChatUtils.addFormattedMessage("&7[&bMyau&7] &rFly: &cBow self damage timeout&r");
                setEnabled(false);
            }
        } else if (bowStage == 1) {
            sendLook();
            PacketUtils.send(new C08PacketPlayerBlockPlacement(minecraft.field_71439_g.func_70694_bm()));
            bowStage = 2;
            bowTicks = 0;
        } else if (bowStage == 2 && ++bowTicks >= 4) {
            sendLook();
            PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN));
            bowStage = 3;
            velocityWaitTicks = 0;
            restoreSlot();
        }
    }

    @Override
    public void onEnable() {
        resetRewoundB();
        rewoundBActive = flightMode.getValue() == 1;
        if (rewoundBActive) return;
        receivedVelocity = wasOnGround = false;
        moveSpeed = lastDistance = verticalMotion = decayAccumulator = baselineSpeed = 0.0;
        decayTicks = airTicks = bowStage = bowTicks = velocityWaitTicks = 0;
        bowSlot = previousSlot = -1;
        updateHeight();
        EntityPlayerSP player = minecraft.field_71439_g;
        dropActive = isOldNcp() && dropFly.getValue() && player != null && !player.field_70122_E && !Double.isNaN(referenceHeight) && referenceHeight - player.field_70163_u > 3.0;
        waitingForGround = isDamage() && player != null && !player.field_70122_E && !dropActive;
        boostStartedAt = System.currentTimeMillis();
        if (dropActive) return;
        if (isDamage() && !waitingForGround) prepareDamage();
        else if (isOldNcp() && !isDamage()) startBoost();
    }

    @Override
    public void onDisable() {
        resetRewoundB();
        if (rewoundBActive) { rewoundBActive = false; return; }
        bowStage = bowTicks = 0;
        restoreSlot();
        dropActive = waitingForGround = false;
        decayAccumulator = baselineSpeed = 0.0;
        decayTicks = 0;
        restoreTimer();
        if (minecraft.field_71439_g != null) {
            minecraft.field_71439_g.field_70181_x = 0.0;
            MovementUtils.setSpeedAlongMotion(0.0);
        }
        InputUtils.updateKeyState(minecraft.field_71474_y.field_74311_E.func_151463_i());
    }

    @EventHandler(priority=-1000)
    public void onPreTick(PreTickEvent event) {
        if (flightMode.getValue() != 0) return;
        updateHeight();
        if (minecraft.field_71439_g == null) return;
        if (isEnabled() && isDamage() && damageMode.getValue() == 1) advanceBow();
        if (isEnabled() && isDamage() && waitingForGround && !dropActive && minecraft.field_71439_g.field_70122_E) {
            waitingForGround = false;
            prepareDamage();
        }
        if (!isEnabled() || !isOldNcp() || !timerBoost.getValue() || timerModuleActive()) { restoreTimer(); return; }
        if (!timerActive) return;
        if (System.currentTimeMillis() - boostStartedAt >= timerDuration.getValue()) restoreTimer();
        else ((AccessorMinecraft)minecraft).getTimer().field_74278_d = timer.getValue();
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        EntityPlayerSP player = minecraft.field_71439_g;
        if (!isEnabled() || flightMode.getValue() != 0 || player == null) return;
        if (isDamage() && !waitingForGround) lastDistance = Math.hypot(player.field_70165_t - player.field_70169_q, player.field_70161_v - player.field_70166_s);
        if (!viewBobbing.getValue() || minecraft.field_71441_e == null || player.field_70122_E || player.func_110143_aJ() <= 0.0f || player.func_70115_ae() || player.func_175149_v()) return;
        float amount = (float)Math.min(0.1, MovementUtils.horizontalSpeed());
        player.field_71109_bG = player.field_71107_bF + (amount - player.field_71107_bF) * 0.4f;
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!isEnabled() || flightMode.getValue() != 0 || (mode.getValue() != 0 && !dropActive) || minecraft.field_71439_g == null) return;
        verticalMotion = 0.0;
        if (minecraft.field_71462_r != null) return;
        if (InputUtils.isKeyDown(minecraft.field_71474_y.field_74314_A.func_151463_i())) verticalMotion += (double)verticalSpeed.getValue() * (double)0.42f;
        if (InputUtils.isKeyDown(minecraft.field_71474_y.field_74311_E.func_151463_i())) verticalMotion -= (double)verticalSpeed.getValue() * (double)0.42f;
        InputUtils.setKeyState(minecraft.field_71474_y.field_74311_E.func_151463_i(), false);
        minecraft.field_71439_g.field_71158_b.field_78899_d = false;
    }

    private void applyVanilla(MoveFlyingEvent event) {
        if (!minecraft.field_71439_g.field_70122_E) minecraft.field_71439_g.field_70181_x = verticalMotion;
        MovementUtils.setSpeedAlongMotion(0.0);
        event.setForward(MovementUtils.forwardInput());
        event.setStrafe(MovementUtils.strafeInput());
        event.setFriction((float)MovementUtils.baseMovementSpeed() * horizontalSpeed.getValue());
    }

    private double slowBaseSpeed() {
        EntityPlayerSP player = minecraft.field_71439_g;
        double value = 0.27;
        if (((AccessorEntity)player).getIsInWeb()) value *= 0.4072398190045249;
        if (player.func_70617_f_()) value *= 0.7692307692307693;
        if (minecraft.field_71441_e.func_72953_d(player.func_174813_aQ())) value *= 0.5203619909502263;
        value *= 1.0 + 0.2 * potionLevel(Potion.field_76424_c);
        value /= 1.0 + 0.2 * potionLevel(Potion.field_76421_d);
        return value;
    }

    private double damageBaseSpeed() {
        return (double)(minecraft.field_71439_g.func_70051_ag() ? 0.287f : 0.223f) * (1.0 + 0.2 * potionLevel(Potion.field_76424_c));
    }

    private static double decayStep(double value, double base, double amount) {
        double next = Math.min(Math.min(value - value / 160.0, value - (value - base) / 33.3), value - (value - base) * 0.020000000000000018) - 1.0e-5;
        return Math.max(base, value - (value - next) * amount);
    }

    private double updateDecay(double value, double base) {
        double amount = (double)decay.getValue() / 100.0;
        if (baselineSpeed <= 0.0) baselineSpeed = value;
        float timerSpeed = ((AccessorMinecraft)minecraft).getTimer().field_74278_d;
        decayAccumulator += Float.isFinite(timerSpeed) && timerSpeed > 0.0f ? 1.0 / timerSpeed : 1.0;
        double steps = Math.floor(decayAccumulator + 1.0e-9);
        decayAccumulator = Math.max(0.0, decayAccumulator - steps);
        for (double step = 0.0; step < steps; step += 1.0) {
            if (value > base) value = decayStep(value, base, amount);
            if (decay.getValue() == 100) continue;
            baselineSpeed = decayStep(baselineSpeed, base, 1.0);
            if (++decayTicks == 60) decayGap = Math.max(0.0, value - baselineSpeed);
            else if (decayTicks > 60) value = Math.min(value, baselineSpeed + decayGap * (1.0 - Math.min(1.0, (decayTicks - 60) / 30.0)));
        }
        return value;
    }

    private void applyDamage(MoveFlyingEvent event) {
        EntityPlayerSP player = minecraft.field_71439_g;
        event.setFriction(0.0f);
        MovementUtils.setSpeedAlongMotion(0.0);
        if (player.field_70172_ad >= 19) receivedVelocity = true;
        boolean ground = player.field_70122_E;
        if (!ground) {
            player.field_70181_x = 0.0;
            if ((airTicks & 1) == 0) player.func_70107_b(player.field_70165_t, player.field_70163_u + 1.0e-14 + RandomUtils.randomDouble(0.0, 1.0) * 9.9999e-10, player.field_70161_v);
            else if (airTicks == 7) player.func_70107_b(player.field_70165_t, player.field_70163_u - (double)0.07f, player.field_70161_v);
            if (minecraft.field_71462_r == null && minecraft.field_71474_y.field_74311_E.func_151470_d() && !receivedVelocity) player.field_70181_x = -1.25;
            ++airTicks;
        }
        double base = damageBaseSpeed();
        if (!receivedVelocity) { decayAccumulator = baselineSpeed = 0.0; decayTicks = 0; return; }
        if (event.getForward() == 0.0f && event.getStrafe() == 0.0f) {
            moveSpeed = base;
            lastDistance = decayAccumulator = baselineSpeed = 0.0;
            decayTicks = 0;
            wasOnGround = false;
            return;
        }
        if (ground) {
            decayAccumulator = baselineSpeed = 0.0;
            decayTicks = 0;
            if (wasOnGround) {
                player.field_70181_x = (double)0.42f + (double)(0.1f * potionLevel(Potion.field_76430_j));
                moveSpeed *= (double)2.145f;
                if (System.currentTimeMillis() - boostStartedAt < timerDuration.getValue()) startBoost();
            } else moveSpeed = (double)(player.func_70051_ag() ? 0.287f : 0.223f) * speed.getValue();
        } else if (wasOnGround) {
            decayAccumulator = baselineSpeed = 0.0;
            decayTicks = 0;
            moveSpeed = lastDistance - 0.66 * (lastDistance - base);
        } else moveSpeed = updateDecay(lastDistance, base);
        moveSpeed = Math.max(moveSpeed, base);
        MovementUtils.setHorizontalMotion(moveSpeed, MovementUtils.movementYaw());
        wasOnGround = ground;
    }

    @EventHandler
    public void onMoveFlying(MoveFlyingEvent event) {
        if (!isEnabled() || flightMode.getValue() != 0 || minecraft.field_71439_g == null) return;
        if (dropActive) applyVanilla(event);
        else if (isDamage() && !waitingForGround) applyDamage(event);
        else if (isOldNcp()) {
            minecraft.field_71439_g.field_70143_R = 0.0f;
            minecraft.field_71439_g.field_70181_x = 0.0;
            MovementUtils.setSpeedAlongMotion(0.0);
            event.setFriction(0.0f);
            if (event.getForward() * event.getForward() + event.getStrafe() * event.getStrafe() >= 1.0e-4f) MovementUtils.setHorizontalMotion(slowBaseSpeed(), MovementUtils.movementYaw());
        } else applyVanilla(event);
        EntityPlayerSP player = minecraft.field_71439_g;
        if (vertical.getValue() && isOldNcp() && !dropActive && !waitingForGround && minecraft.field_71462_r == null
                && (!isDamage() || (receivedVelocity && !player.field_70122_E))
                && InputUtils.isKeyDown(minecraft.field_71474_y.field_74314_A.func_151463_i())) {
            player.field_70181_x = (double)verticalSpeed.getValue() * (double)0.42f;
        }
    }

    public boolean getPacketGround(boolean ground) {
        if (!isEnabled() || !isDamage() || waitingForGround) return ground;
        return airTicks <= 6 ? airTicks == 6 : ground;
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        EntityPlayerSP player = minecraft.field_71439_g;
        if (!isEnabled() || !isOldNcp() || (oldNcpMode.getValue() != 0 && !waitingForGround) || dropActive || event.isCancelled() || player == null || !(event.getPacket() instanceof C03PacketPlayer)) return;
        C03PacketPlayer packet = (C03PacketPlayer)event.getPacket();
        boolean even = player.field_70173_aa % 2 == 0;
        double offset = 1.0e-5 + (even ? 1.0 : -1.0) * (1.0e-10 + 9.9999e-6 * RandomUtils.randomDouble(0.0, 1.0));
        if (packet instanceof C03PacketPlayer.C04PacketPlayerPosition || packet instanceof C03PacketPlayer.C06PacketPlayerPosLook) ((AccessorC03PacketPlayer)packet).setY(packet.func_149467_d() + offset);
        ((AccessorC03PacketPlayer)packet).setOnGround(even);
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled()) return;
        EntityPlayerSP player = minecraft.field_71439_g;
        if (isDamage() && damageMode.getValue() == 1 && player != null && event.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity)event.getPacket()).func_149412_c() == player.func_145782_y()) receivedVelocity = true;
        }
        if (isOldNcp() && !event.isCancelled() && event.getPacket() instanceof S08PacketPlayerPosLook) setEnabled(false);
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (timerActive) ((AccessorMinecraft)minecraft).getTimer().field_74278_d = timer.getValue();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        resetRewoundB();
        dropActive = false;
        referenceHeight = Double.NaN;
        decayAccumulator = baselineSpeed = 0.0;
        decayTicks = 0;
        restoreTimer();
    }

    @Override
    public void onSettingChanged(String name) {
        if (!isEnabled()) return;
        if (name.equals("timer-boost")) {
            restoreTimer();
            if (isOldNcp() && !isDamage() && timerBoost.getValue()) { boostStartedAt = System.currentTimeMillis(); startBoost(); }
        }
        if (name.equals("flight-mode") || name.equals("mode") || name.equals("oldncp-mode") || name.equals("damage-mode")) {
            onDisable();
            verticalMotion = 0.0;
            onEnable();
        }
        if (name.equals("oldncp-mode")) waitingForGround = isDamage() && minecraft.field_71439_g != null && !minecraft.field_71439_g.field_70122_E;
        if (name.equals("drop-fly")) { updateHeight(); if (!dropFly.getValue()) dropActive = false; }
    }

    @Override
    public String[] getHudInfo() {
        if (flightMode.getValue() == 1) return new String[]{"Rewound B"};
        if (!isOldNcp()) return new String[]{mode.getModeName()};
        if (dropActive) return new String[]{"OldNCP Drop"};
        return new String[]{isDamage() && !waitingForGround ? "OldNCP Damage" : "OldNCP Slow"};
    }

    private void resetRewoundB() {
        verticalDelay = horizontalDelay = 0;
        lastMotionY = 0.0;
        hasPreviousMotion = false;
    }

    @EventHandler
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled() || flightMode.getValue() != 1) return;
        EntityPlayerSP player = minecraft.field_71439_g;
        if (player == null) { resetRewoundB(); return; }
        double y = player.field_70181_x;
        if (hasPreviousMotion && y > 0.7 && y > lastMotionY) {
            verticalDelay = activationTimeV.getValue();
            horizontalDelay = activationTimeH.getValue();
        }
        if (verticalDelay > 0) { y = 2.54; --verticalDelay; }
        if (horizontalDelay > 0) {
            double yaw = player.field_70177_z * Math.PI / 180.0;
            player.field_70159_w = -Math.sin(yaw) * 1.63;
            player.field_70179_y = Math.cos(yaw) * 1.63;
            --horizontalDelay;
        }
        player.field_70181_x = y;
        lastMotionY = y;
        hasPreviousMotion = true;
    }
}
