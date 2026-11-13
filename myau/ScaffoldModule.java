package myau;

import myau.modules.Scaffold;
import myau.modules.scaffold.ScaffoldMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class ScaffoldModule extends Module implements ScaffoldMinecraft.Bindings {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final ModeSetting rotations = new ModeSetting("rotations", 2, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS", "HYPIXEL", "STABLE"});
    public final PercentSetting rotationSmoothing = new PercentSetting("rotation-smoothing", 65, this::isHypixelRotation);
    public final ModeSetting moveFix = new ModeSetting("move-fix", 1, new String[]{"NONE", "SILENT"});
    public final ModeSetting sprint = new ModeSetting("sprint", 0, new String[]{"NONE", "VANILLA"});
    public final PercentSetting groundMotion = new PercentSetting("ground-motion", 100);
    public final PercentSetting airMotion = new PercentSetting("air-motion", 100);
    public final PercentSetting speedMotion = new PercentSetting("speed-motion", 100);
    public final ModeSetting tower = new ModeSetting("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final ModeSetting verticalTower = new ModeSetting("vertical-tower", 0, new String[]{"NONE", "PREDICTION"});
    public final BooleanSetting towerOnPress = new BooleanSetting("tower-on-press", false, () -> tower.getValue() != 0);
    public final ModeSetting keepY = new ModeSetting("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY", "OLDNCP"});
    public final BooleanSetting keepYOnPress = new BooleanSetting("keep-y-on-press", false, () -> keepY.getValue() != 0);
    public final IntegerSetting sneak = new IntegerSetting("sneak", 0, 0, 7);
    public final IntegerSetting sneakTicks = new IntegerSetting("sneak-ticks", 1, 1, 10, () -> sneak.getValue() > 0);
    public final BooleanSetting multiPlace = new BooleanSetting("multi-place", true);
    public final BooleanSetting safeWalk = new BooleanSetting("safe-walk", true);
    public final BooleanSetting swing = new BooleanSetting("swing", true);
    public final BooleanSetting itemSpoof = new BooleanSetting("item-spoof", false);
    public final BooleanSetting blockCounter = new BooleanSetting("block-counter", true);

    private final ScaffoldMinecraft adapter = new ScaffoldMinecraft(minecraft, this);
    private final Scaffold scaffold = new Scaffold(adapter);
    private RotationUpdateEvent originalRotation, workingRotation;
    private boolean rotationRequested, safeWalkRequested, tellyTakeoff, cycleRotation, awaitingRotation;
    private boolean tellyRotationActive;
    private float placementYaw = -180.0f, placementPitch;
    private int placementDelayTicks, jumpBaseY, keepYState, keepYLevel;
    private int movementTick = Integer.MIN_VALUE;

    public ScaffoldModule() {
        super("Scaffold", false, 0);
    }

    private void syncSettings() {
        Scaffold.Settings settings = scaffold.settings;
        int rotation = rotations.getValue();
        settings.rotations = rotation == 4 ? Scaffold.Rotations.DEFAULT : Scaffold.Rotations.values()[rotation == 5 ? 4 : rotation];
        settings.moveFix = Scaffold.MoveFix.values()[moveFix.getValue()];
        settings.sprint = Scaffold.Sprint.values()[sprint.getValue()];
        settings.groundMotion = groundMotion.getValue();
        settings.airMotion = airMotion.getValue();
        settings.speedMotion = speedMotion.getValue();
        settings.tower = Scaffold.Tower.values()[tower.getValue()];
        settings.verticalTower = Scaffold.VerticalTower.values()[verticalTower.getValue()];
        settings.towerOnPress = towerOnPress.getValue();
        settings.keepY = Scaffold.KeepY.values()[keepY.getValue()];
        settings.keepYOnPress = keepYOnPress.getValue();
        settings.sneak = sneak.getValue();
        settings.sneakTicks = sneakTicks.getValue();
        settings.multiPlace = multiPlace.getValue();
        settings.safeWalk = safeWalk.getValue();
        settings.swing = swing.getValue();
        settings.itemSpoof = itemSpoof.getValue();
        settings.blockCounter = blockCounter.getValue();
    }

    @Override
    public void onEnable() {
        syncSettings();
        resetRotation();
        scaffold.setEnabled(true);
    }

    @Override
    public void onDisable() {
        scaffold.setEnabled(false);
        resetRotation();
    }

    @Override
    public void onSettingChanged(String name) {
        syncSettings();
        if (isEnabled()) scaffold.onWorldChange();
        resetRotation();
    }

    private void resetRotation() {
        rotationRequested = safeWalkRequested = tellyTakeoff = cycleRotation = tellyRotationActive = awaitingRotation = false;
        originalRotation = workingRotation = null;
        placementYaw = -180.0f;
        placementPitch = 0.0f;
        placementDelayTicks = keepYState = 0;
        movementTick = Integer.MIN_VALUE;
        jumpBaseY = adapter.ready() ? MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u) : 0;
    }

    public int getPreviousHotbarSlot() {
        return adapter.getSavedSlot();
    }

    @EventHandler(priority=-2000)
    public void onMovementInput(MovementInputEvent event) {
        if (!isEnabled()) return;
        if (!adapter.ready()) { scaffold.onAfterMovementInput(Integer.MIN_VALUE); return; }
        if (movementTick == minecraft.field_71439_g.field_70173_aa) return;
        movementTick = minecraft.field_71439_g.field_70173_aa;
        if (placementDelayTicks > 0) --placementDelayTicks;
        boolean ground = minecraft.field_71439_g.field_70122_E;
        if (ground) jumpBaseY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
        scaffold.onAfterMovementInput(movementTick);
        Scaffold.Player player = adapter.player();
        keepYState = keepY.getValue() != 0 && (!keepYOnPress.getValue() || player.jumpDown) ? 1 : 0;
        keepYLevel = scaffold.placementLevel();
        tellyTakeoff = ground && player.moving() && player.motionY > 0.0
                && (keepY.getValue() == 3 && keepYState != 0 || tower.getValue() == 3 && player.jumpDown);
    }

    @EventHandler(priority=-2000)
    public void onPreWalkingUpdate(PreWalkingUpdateEvent event) {
        if (!isEnabled()) return;
        originalRotation = event.getRotation();
        workingRotation = originalRotation == null ? null : new RotationUpdateEvent(originalRotation);
        cycleRotation = rotationRequested = awaitingRotation = false;
        try {
            scaffold.onMotion(adapter.ready() ? minecraft.field_71439_g.field_70173_aa : Integer.MIN_VALUE);
        } finally {
            if (rotationRequested) event.setRotation(workingRotation);
            originalRotation = workingRotation = null;
        }
    }

    @EventHandler
    public void onSafeWalk(SafeWalkEvent event) {
        if (isEnabled() && adapter.ready() && safeWalkRequested) event.setSafeWalk(true);
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (isEnabled()) scaffold.onRender();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        scaffold.onWorldChange();
        resetRotation();
    }

    @Override
    public boolean speedEnabled() {
        Module speed = MyauClient.moduleManager == null ? null : MyauClient.moduleManager.modules.get(SpeedModule.class);
        return speed != null && speed.isEnabled();
    }

    @Override
    public void requestSafeWalk(boolean value) {
        safeWalkRequested = value;
    }

    @Override
    public void requestSneak(boolean value) {
        if (value && adapter.ready() && !minecraft.field_71439_g.field_71158_b.field_78899_d) {
            minecraft.field_71439_g.field_71158_b.field_78899_d = true;
            minecraft.field_71439_g.field_71158_b.field_78900_b *= 0.3f;
            minecraft.field_71439_g.field_71158_b.field_78902_a *= 0.3f;
        }
    }

    private void applyRotation(RotationUpdateEvent event) {
        if (event == null || !adapter.ready()) return;
        RotationState.replace(event.isModified() && !minecraft.field_71439_g.func_70115_ae(), event.getYaw(), event.getPitch(), event.getMovementYaw(), event.getMovementPriority());
        minecraft.field_71439_g.field_70177_z = event.getYaw();
        minecraft.field_71439_g.field_70125_A = event.getPitch();
    }

    @Override
    public boolean requestRotation(float yaw, float pitch, Scaffold.MoveFix fix) {
        if (workingRotation == null || originalRotation.getRotationPriority() > 3
                || fix == Scaffold.MoveFix.SILENT && originalRotation.getMovementPriority() >= 3) return false;
        if (!isHypixelRotation() || !cycleRotation) {
            workingRotation = new RotationUpdateEvent(originalRotation);
            if (isHypixelRotation()) {
                placementYaw = yaw;
                placementPitch = pitch;
                float backwardYaw = MathHelper.func_76142_g(MovementUtils.movementYawForInput(originalRotation.getYaw(), MovementUtils.forwardInput(), MovementUtils.strafeInput()) + 180.0f);
                applyHypixelRotation(workingRotation, originalRotation.getPreviousYaw(), originalRotation.getPreviousPitch(), backwardYaw);
            } else {
                workingRotation.setRotation(yaw, pitch, 3);
                if (fix == Scaffold.MoveFix.SILENT) workingRotation.setMovementYaw(yaw, 3);
            }
            cycleRotation = true;
        }
        rotationRequested = true;
        applyRotation(workingRotation);
        return true;
    }

    @Override
    public void releaseRotation() {
        if (awaitingRotation && isHypixelRotation() && workingRotation != null) return;
        rotationRequested = false;
        applyRotation(originalRotation);
    }

    @Override
    public boolean canPlace(Scaffold.Placement placement) {
        if (!adapter.ready()) return false;
        if (isHypixelRotation() && placementDelayTicks > 0) {
            awaitingRotation = true;
            return false;
        }
        float yaw = minecraft.field_71439_g.field_70177_z;
        float pitch = minecraft.field_71439_g.field_70125_A;
        MovingObjectPosition hit = RotationUtils.traceBlocks(yaw, pitch, minecraft.field_71442_b.func_78757_d(), 1.0f);
        boolean valid = hit != null && hit.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK
                && hit.func_178782_a().func_177958_n() == placement.support.x
                && hit.func_178782_a().func_177956_o() == placement.support.y
                && hit.func_178782_a().func_177952_p() == placement.support.z
                && hit.field_178784_b.name().equals(placement.face.name());
        awaitingRotation = !valid && isHypixelRotation();
        return valid;
    }

    private boolean isHypixelRotation() { return rotations.getValue() == 4; }
    private boolean isTellyTakeoff() { return tellyTakeoff; }
    private boolean isTowering() { return tower.getValue() != 0 && minecraft.field_71474_y.field_74314_A.func_151470_d(); }
    private boolean isDiagonalYaw(float yaw) {
        float angle = Math.abs(yaw % 90.0f);
        return angle > 20.0f && angle < 70.0f;
    }
    private int hypixelTicksToLevel() {
        double y = minecraft.field_71439_g.field_70163_u;
        int level = keepYState != 0 ? keepYLevel : isTowering() ? jumpBaseY + 1 : MathHelper.func_76128_c(y);
        return HypixelScaffoldPacing.ticksToLevel(y, minecraft.field_71439_g.field_70181_x, level);
    }

    private void applyHypixelRotation(RotationUpdateEvent event, float previousYaw, float previousPitch, float backwardYaw) {
        float yaw = placementYaw;
        float pitch = placementPitch;
        if (isTellyTakeoff()) {
            float difference = MathHelper.func_76142_g(minecraft.field_71439_g.field_70177_z - previousYaw);
            yaw = RotationUtils.snapToMouseSensitivity(previousYaw + difference * RandomUtils.randomFloat(0.98f, 0.99f), previousYaw);
            pitch = RotationUtils.snapToMouseSensitivity(RandomUtils.randomFloat(30.0f, 80.0f), previousPitch);
            placementDelayTicks = 3;
            tellyRotationActive = true;
        } else {
            float difference = MathHelper.func_76142_g(placementYaw - previousYaw);
            boolean diagonal = isDiagonalYaw(backwardYaw);
            float maxChange = RandomUtils.randomFloat(80.0f, 90.0f);
            if (tellyRotationActive && minecraft.field_71439_g.field_70181_x > 0.0 && placementDelayTicks <= 1 && !diagonal) {
                maxChange = RandomUtils.randomFloat(30.0f, 35.0f);
            }
            if (!minecraft.field_71439_g.field_70122_E) {
                int ticks = hypixelTicksToLevel();
                int turnTicks = isTowering() ? HypixelScaffoldPacing.towerTurnTicks(ticks, diagonal)
                        : HypixelScaffoldPacing.turnTicks(ticks, diagonal);
                float comfortable = diagonal ? RandomUtils.randomFloat(34.0f, 40.0f) : RandomUtils.randomFloat(22.0f, 28.0f);
                float step = HypixelScaffoldPacing.maxStep(difference, turnTicks, comfortable);
                if (turnTicks <= 1) {
                    yaw = RotationUtils.snapToMouseSensitivity(previousYaw + RotationUtils.clampAngleChange(difference, step), previousYaw);
                } else {
                    float[] turn = smoothPlacement(previousYaw, previousPitch, difference,
                            HypixelScaffoldPacing.smoothing(rotationSmoothing.getValue(), diagonal), step);
                    yaw = turn[0];
                    pitch = turn[1];
                }
            } else {
                float[] turn = smoothPlacement(previousYaw, previousPitch, difference, rotationSmoothing.getValue(), maxChange);
                yaw = turn[0];
                pitch = turn[1];
            }
        }
        event.setRotation(yaw, pitch, 3);
        if (moveFix.getValue() == 1) event.setMovementYaw(yaw, 3);
    }

    private float[] smoothPlacement(float previousYaw, float previousPitch, float yawDifference, int percent, float maxChange) {
        float yaw = placementYaw;
        float pitch = placementPitch;
        float smoothing = percent / 100.0f;
        float fraction = Math.max(0.45f, Math.min(1.0f, (1.0f - smoothing * 0.55f) * RandomUtils.randomFloat(0.92f, 1.08f)));
        if (Math.abs(yawDifference) > 4.0f + smoothing * 10.0f) {
            float step = Math.min(Math.abs(yawDifference), Math.abs(yawDifference) * fraction + 10.0f + (1.0f - smoothing) * 10.0f);
            yaw = RotationUtils.snapToMouseSensitivity(previousYaw + RotationUtils.clampAngleChange(Math.signum(yawDifference) * step, maxChange), previousYaw);
        }
        float pitchDifference = placementPitch - previousPitch;
        if (Math.abs(pitchDifference) > 3.0f + smoothing * 5.0f) {
            float step = Math.min(Math.abs(pitchDifference), Math.abs(pitchDifference) * fraction + 5.0f + (1.0f - smoothing) * 5.0f);
            pitch = RotationUtils.snapToMouseSensitivity(previousPitch + Math.signum(pitchDifference) * step, previousPitch);
        }
        return new float[]{yaw, pitch};
    }

    @Override
    public void drawBlockCount(int n) {
        HUDModule hUDModule = (HUDModule)MyauClient.moduleManager.modules.get(HUDModule.class);
        float f = ((Float)hUDModule.scale.getValue()).floatValue();
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float)f, (float)f, (float)0.0f);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int)770, (int)771);
        FontRenderer fontRenderer = ScaffoldModule.minecraft.field_71466_p;
        String string = String.format("%d block%s left", n, n != 1 ? "s" : "");
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        float f2 = ((float)scaledResolution.func_78326_a() / 2.0f + (float)fontRenderer.field_78288_b * 1.5f) / f;
        float f3 = (float)scaledResolution.func_78328_b() / 2.0f / f - (float)fontRenderer.field_78288_b / 2.0f + 1.0f;
        int n2 = n > 0 ? 0xFFFFFF : 0xFF5555;
        fontRenderer.func_175065_a(string, f2, f3, n2 | 0xBF000000, ((Boolean)hUDModule.shadow.getValue()).booleanValue());
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }
}
