package myau;

import java.util.ArrayList;
import java.util.Comparator;
import myau.BedNukerModule;
import myau.BlockClickEvent;
import myau.BlockPlacementTarget;
import myau.BlockUtils;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.HUDModule;
import myau.HotbarChangeEvent;
import myau.ItemUtils;
import myau.LeftClickEvent;
import myau.LivingUpdateEvent;
import myau.LongJumpModule;
import myau.ModeSetting;
import myau.Module;
import myau.MoveFlyingEvent;
import myau.MovementInputEvent;
import myau.MovementUtils;
import myau.MyauClient;
import myau.PacketUtils;
import myau.PercentSetting;
import myau.PlayerUtils;
import myau.RandomUtils;
import myau.RenderOverlayEvent;
import myau.RightClickEvent;
import myau.RotationState;
import myau.RotationUpdateEvent;
import myau.RotationUtils;
import myau.SafeWalkEvent;
import myau.mixin.AccessorEntityLivingBase;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.WorldSettings;

public class ScaffoldModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final double[] FACE_SAMPLE_OFFSETS = new double[]{0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375, 0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875, 0.78125, 0.84375, 0.90625, 0.96875};
    private int placementDelayTicks = 0;
    private int previousHotbarSlot = -1;
    private int remainingStackBlocks = -1;
    private float placementYaw = -180.0f;
    private float placementPitch = 0.0f;
    private boolean hasPlacementRotation = false;
    private float tickPlacementYaw;
    private float tickPlacementPitch;
    private int towerPhase = 0;
    private int towerCycleCount = 0;
    private int keepYState = 0;
    private int keepYLevel = 256;
    private boolean keepYRecoveryPlacement = false;
    private boolean tellyRotationActive = false;
    private EnumFacing pendingPlacementFace = null;
    private static final int HYPIXEL_ROTATION = 4;
    private boolean wasOnGround;
    private int jumpBaseY;
    private int hypixelJumpDelay;

    public final ModeSetting rotations = new ModeSetting("rotations", 2, new String[]{"NONE", "DEFAULT", "BACKWARDS", "SIDEWAYS", "HYPIXEL"});
    public final PercentSetting rotationSmoothing = new PercentSetting("rotation-smoothing", 65, () -> rotations.getValue() == 1 || isHypixelRotation());
    public final ModeSetting moveFix = new ModeSetting("move-fix", 1, new String[]{"NONE", "SILENT"});
    public final ModeSetting sprint = new ModeSetting("sprint", 0, new String[]{"NONE", "VANILLA"});
    public final PercentSetting groundMotion = new PercentSetting("ground-motion", 100);
    public final PercentSetting airMotion = new PercentSetting("air-motion", 100);
    public final PercentSetting speedMotion = new PercentSetting("speed-motion", 100);
    public final ModeSetting tower = new ModeSetting("tower", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final ModeSetting keepY = new ModeSetting("keep-y", 0, new String[]{"NONE", "VANILLA", "EXTRA", "TELLY"});
    public final BooleanSetting keepYOnPress = new BooleanSetting("keep-y-on-press", false, this::isKeepYOnPressVisible);
    public final BooleanSetting multiPlace = new BooleanSetting("multi-place", true);
    public final BooleanSetting safeWalk = new BooleanSetting("safe-walk", true);
    public final BooleanSetting swing = new BooleanSetting("swing", true);
    public final BooleanSetting itemSpoof = new BooleanSetting("item-spoof", false);
    public final BooleanSetting blockCounter = new BooleanSetting("block-counter", true);

    private boolean shouldDisableSprint() {
        if (this.isTellyTakeoff()) {
            return false;
        }
        boolean bl = (Integer)this.keepY.getValue() == 1 || (Integer)this.keepY.getValue() == 2;
        boolean bl2 = bl;
        if (bl && this.keepYState > 0) {
            return false;
        }
        return (Integer)this.sprint.getValue() == 0;
    }

    private boolean canPlaceWithActiveModules() {
        BedNukerModule bedNukerModule = (BedNukerModule)MyauClient.moduleManager.modules.get(BedNukerModule.class);
        if (bedNukerModule.isEnabled() && bedNukerModule.isControllingInteractions()) {
            return false;
        }
        LongJumpModule longJumpModule = (LongJumpModule)MyauClient.moduleManager.modules.get(LongJumpModule.class);
        return !longJumpModule.isEnabled() || !longJumpModule.isAutomaticFireballMode() || longJumpModule.hasJumpStarted();
    }

    private EnumFacing findConnectingFace(BlockPos blockPos, BlockPos blockPos2) {
        EnumFacing enumFacing = null;
        double d = 0.0;
        for (EnumFacing enumFacing2 : EnumFacing.field_82609_l) {
            BlockPos blockPos3;
            if (enumFacing2 == EnumFacing.DOWN || (blockPos3 = blockPos.func_177972_a(enumFacing2)).func_177956_o() > blockPos2.func_177956_o() || !BlockUtils.isReplaceableAt(blockPos3)) continue;
            double d2 = blockPos3.func_177957_d((double)blockPos2.func_177958_n() + 0.5, (double)blockPos2.func_177956_o() + 0.5, (double)blockPos2.func_177952_p() + 0.5);
            if (!(enumFacing == null || d2 < d || d2 == d && enumFacing2 == EnumFacing.UP)) continue;
            d = d2;
            enumFacing = enumFacing2;
        }
        return enumFacing;
    }

    private BlockPlacementTarget findPlacementTarget() {
        int n = MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70163_u);
        int n2 = this.keepYState != 0 && !this.keepYRecoveryPlacement ? Math.min(n, this.keepYLevel) : n;
        BlockPos blockPos = new BlockPos(MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70165_t), n2 - 1, MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70161_v));
        if (!BlockUtils.isReplaceableAt(blockPos)) {
            return null;
        }
        ArrayList<BlockPos> arrayList = new ArrayList<BlockPos>();
        double d = ScaffoldModule.minecraft.field_71442_b.func_78757_d();
        for (int i = -4; i <= 4; ++i) {
            for (int j = -4; j <= 0; ++j) {
                for (int k = -4; k <= 4; ++k) {
                    double d2;
                    BlockPos blockPos3 = blockPos.func_177982_a(i, j, k);
                    if (BlockUtils.isReplaceableAt(blockPos3) || BlockUtils.isInteractableAt(blockPos3) || (d2 = ScaffoldModule.minecraft.field_71439_g.func_70011_f((double)blockPos3.func_177958_n() + 0.5, (double)blockPos3.func_177956_o() + 0.5, (double)blockPos3.func_177952_p() + 0.5)) > d || this.keepYState != 0 && !this.keepYRecoveryPlacement && blockPos3.func_177956_o() >= this.keepYLevel) continue;
                    for (EnumFacing enumFacing : EnumFacing.field_82609_l) {
                        if (enumFacing == EnumFacing.DOWN || !BlockUtils.isReplaceableAt(blockPos3.func_177972_a(enumFacing))) continue;
                        arrayList.add(blockPos3);
                    }
                }
            }
        }
        if (arrayList.isEmpty()) {
            return null;
        }
        arrayList.sort(Comparator.comparingDouble(blockPos2 -> ScaffoldModule.targetDistanceSquared(blockPos, blockPos2)));
        BlockPos blockPos4 = (BlockPos)arrayList.get(0);
        EnumFacing enumFacing = this.findConnectingFace(blockPos4, blockPos);
        return enumFacing == null ? null : new BlockPlacementTarget(blockPos4, enumFacing);
    }

    private MovingObjectPosition tracePlacement(BlockPos blockPos, EnumFacing enumFacing, float f, float f2) {
        MovingObjectPosition movingObjectPosition = RotationUtils.traceBlocks(f, f2, ScaffoldModule.minecraft.field_71442_b.func_78757_d(), 1.0f);
        if (movingObjectPosition == null || movingObjectPosition.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK || !movingObjectPosition.func_178782_a().equals((Object)blockPos) || movingObjectPosition.field_178784_b != enumFacing) {
            return null;
        }
        return movingObjectPosition;
    }

    private void placeBlock(BlockPos blockPos, EnumFacing enumFacing) {
        if (!ItemUtils.isHoldingPlaceableBlock() || this.remainingStackBlocks <= 0) {
            return;
        }
        MovingObjectPosition movingObjectPosition = this.tracePlacement(blockPos, enumFacing, this.tickPlacementYaw, this.tickPlacementPitch);
        if (movingObjectPosition == null) {
            return;
        }
        boolean bl = ScaffoldModule.minecraft.field_71442_b.func_178890_a(ScaffoldModule.minecraft.field_71439_g, ScaffoldModule.minecraft.field_71441_e, ScaffoldModule.minecraft.field_71439_g.field_71071_by.func_70448_g(), blockPos, enumFacing, movingObjectPosition.field_72307_f);
        if (!bl) {
            return;
        }
        if (ScaffoldModule.minecraft.field_71442_b.func_178889_l() != WorldSettings.GameType.CREATIVE) {
            --this.remainingStackBlocks;
        }
        if (((Boolean)this.swing.getValue()).booleanValue()) {
            ScaffoldModule.minecraft.field_71439_g.func_71038_i();
        } else {
            PacketUtils.send(new C0APacketAnimation());
        }
    }

    private EnumFacing facingFromYaw(float f) {
        if (f < -135.0f || f > 135.0f) {
            return EnumFacing.NORTH;
        }
        if (f < -45.0f) {
            return EnumFacing.EAST;
        }
        if (f < 45.0f) {
            return EnumFacing.SOUTH;
        }
        return EnumFacing.WEST;
    }

    private double distanceToBlockEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH: {
                return ScaffoldModule.minecraft.field_71439_g.field_70161_v - Math.floor(ScaffoldModule.minecraft.field_71439_g.field_70161_v);
            }
            case EAST: {
                return Math.ceil(ScaffoldModule.minecraft.field_71439_g.field_70165_t) - ScaffoldModule.minecraft.field_71439_g.field_70165_t;
            }
            case SOUTH: {
                return Math.ceil(ScaffoldModule.minecraft.field_71439_g.field_70161_v) - ScaffoldModule.minecraft.field_71439_g.field_70161_v;
            }
        }
        return ScaffoldModule.minecraft.field_71439_g.field_70165_t - Math.floor(ScaffoldModule.minecraft.field_71439_g.field_70165_t);
    }

    private float getMotionMultiplier() {
        int n = !ScaffoldModule.minecraft.field_71439_g.field_70122_E ? (Integer)this.airMotion.getValue() : (MovementUtils.speedPotionLevel() > 0 ? ((Integer)this.speedMotion.getValue()).intValue() : ((Integer)this.groundMotion.getValue()).intValue());
        return (float)n / 100.0f;
    }

    private double randomEdgeApproachDistance() {
        return 0.2155 - RandomUtils.randomDouble(1.0E-4, 9.0E-4);
    }

    private float getMovementYaw() {
        return MovementUtils.movementYawForInput(ScaffoldModule.minecraft.field_71439_g.field_70177_z, MovementUtils.forwardInput(), MovementUtils.strafeInput());
    }

    private boolean isDiagonalYaw(float f) {
        float f2 = Math.abs(f % 90.0f);
        return f2 > 20.0f && f2 < 70.0f;
    }

    private boolean isHypixelRotation() {
        return rotations.getValue() == HYPIXEL_ROTATION;
    }

    private boolean isJumpCycleActive() {
        return keepY.getValue() != 0 && (!keepYOnPress.getValue() || PlayerUtils.isUseItemKeyDown())
                || tower.getValue() == 3 && minecraft.field_71474_y.field_74314_A.func_151470_d();
    }

    private boolean isTowering() {
        return tower.getValue() != 0 && minecraft.field_71474_y.field_74314_A.func_151470_d();
    }

    private int hypixelTicksToLevel() {
        double y = minecraft.field_71439_g.field_70163_u;
        int level = keepYState != 0 ? keepYLevel : isTowering() ? jumpBaseY + 1 : MathHelper.func_76128_c(y);
        return HypixelScaffoldPacing.ticksToLevel(y, minecraft.field_71439_g.field_70181_x, level);
    }

    private boolean isTellyTakeoff() {
        if (!ScaffoldModule.minecraft.field_71439_g.field_70122_E || !MovementUtils.hasMovementInput()) {
            return false;
        }
        if (hypixelJumpDelay > 0 || PlayerUtils.hasCollisionAbove()) {
            return false;
        }
        if (((AccessorEntityLivingBase)ScaffoldModule.minecraft.field_71439_g).getJumpTicks() > 1) {
            return false;
        }
        if ((Integer)this.keepY.getValue() == 3 && this.keepYState > 0) {
            return true;
        }
        return (Integer)this.tower.getValue() == 3 && ScaffoldModule.minecraft.field_71474_y.field_74314_A.func_151470_d();
    }


    public ScaffoldModule() {
        super("Scaffold", false, 0);
    }

    public int getPreviousHotbarSlot() {
        return this.previousHotbarSlot;
    }

    private void selectPlacementHotbarSlot() {
        ItemStack itemStack = ScaffoldModule.minecraft.field_71439_g.func_70694_bm();
        int n = ItemUtils.isPlaceableBlockStack(itemStack) ? itemStack.field_77994_a : 0;
        this.remainingStackBlocks = Math.min(this.remainingStackBlocks, n);
        if (this.remainingStackBlocks > 0) {
            return;
        }
        int n2 = ScaffoldModule.minecraft.field_71439_g.field_71071_by.field_70461_c;
        if (this.remainingStackBlocks == 0) {
            --n2;
        }
        for (int i = n2; i > n2 - 9; --i) {
            int n3 = (i % 9 + 9) % 9;
            ItemStack itemStack2 = ScaffoldModule.minecraft.field_71439_g.field_71071_by.func_70301_a(n3);
            if (!ItemUtils.isPlaceableBlockStack(itemStack2)) continue;
            ScaffoldModule.minecraft.field_71439_g.field_71071_by.field_70461_c = n3;
            this.remainingStackBlocks = itemStack2.field_77994_a;
            return;
        }
    }

    private Vec3 findBestPlacementHit(BlockPlacementTarget blockPlacementTarget, float f, float f2) {
        if (blockPlacementTarget == null) {
            return null;
        }
        if (isHypixelRotation()) {
            float yaw = RotationUtils.snapToMouseSensitivity(this.placementYaw, f);
            float pitch = MathHelper.func_76131_a(RotationUtils.snapToMouseSensitivity(this.placementPitch, f2), -90.0f, 90.0f);
            MovingObjectPosition hit = tracePlacement(blockPlacementTarget.position(), blockPlacementTarget.face(), yaw, pitch);
            if (hit != null && minecraft.field_71439_g.func_174824_e(1.0f).func_72438_d(hit.field_72307_f)
                    <= minecraft.field_71442_b.func_78757_d()) {
                this.placementYaw = yaw;
                this.placementPitch = pitch;
                this.hasPlacementRotation = true;
                return hit.field_72307_f;
            }
        }
        double[] dArray = FACE_SAMPLE_OFFSETS;
        double[] dArray2 = FACE_SAMPLE_OFFSETS;
        double[] dArray3 = FACE_SAMPLE_OFFSETS;
        switch (blockPlacementTarget.face()) {
            case NORTH: {
                dArray3 = new double[]{0.0};
                break;
            }
            case EAST: {
                dArray = new double[]{1.0};
                break;
            }
            case SOUTH: {
                dArray3 = new double[]{1.0};
                break;
            }
            case WEST: {
                dArray = new double[]{0.0};
                break;
            }
            case DOWN: {
                dArray2 = new double[]{0.0};
                break;
            }
            case UP: {
                dArray2 = new double[]{1.0};
            }
        }
        float f3 = -180.0f;
        float f4 = 0.0f;
        float f5 = 0.0f;
        Vec3 vec3 = null;
        for (double d : dArray) {
            for (double d2 : dArray2) {
                for (double d3 : dArray3) {
                    double d4 = (double)blockPlacementTarget.position().func_177958_n() + d - ScaffoldModule.minecraft.field_71439_g.field_70165_t;
                    double d5 = (double)blockPlacementTarget.position().func_177956_o() + d2 - ScaffoldModule.minecraft.field_71439_g.field_70163_u - (double)ScaffoldModule.minecraft.field_71439_g.func_70047_e();
                    double d6 = (double)blockPlacementTarget.position().func_177952_p() + d3 - ScaffoldModule.minecraft.field_71439_g.field_70161_v;
                    float[] fArray = RotationUtils.rotationsFromDelta(d4, d5, d6);
                    fArray[0] = RotationUtils.snapToMouseSensitivity(fArray[0], f);
                    fArray[1] = RotationUtils.snapToMouseSensitivity(fArray[1], f2);
                    fArray[1] = MathHelper.func_76131_a((float)fArray[1], (float)-90.0f, (float)90.0f);
                    MovingObjectPosition movingObjectPosition = RotationUtils.traceBlocks(fArray[0], fArray[1], ScaffoldModule.minecraft.field_71442_b.func_78757_d(), 1.0f);
                    if (movingObjectPosition == null || movingObjectPosition.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK || !movingObjectPosition.func_178782_a().equals((Object)blockPlacementTarget.position()) || movingObjectPosition.field_178784_b != blockPlacementTarget.face()) continue;
                    float f6 = Math.abs(MathHelper.func_76142_g((float)(fArray[0] - this.placementYaw))) + Math.abs(fArray[1] - this.placementPitch);
                    if (!(f3 == -180.0f && f4 == 0.0f || f6 < f5)) continue;
                    f3 = fArray[0];
                    f4 = fArray[1];
                    f5 = f6;
                    vec3 = movingObjectPosition.field_72307_f;
                }
            }
        }
        if (f3 != -180.0f || f4 != 0.0f) {
            this.placementYaw = f3;
            this.placementPitch = f4;
            this.hasPlacementRotation = true;
        }
        return vec3;
    }

    private void applyPlacementRotation(RotationUpdateEvent event, BlockPlacementTarget target,
                                        float previousYaw, float previousPitch, float backwardYaw, float sidewaysYaw) {
        int mode = rotations.getValue();
        if (mode == 0) return;
        if (hasPlacementRotation && MovementUtils.hasMovementInput()
                && Math.abs(MathHelper.func_76142_g(backwardYaw - placementYaw)) < 90.0f
                && (mode == 2 || mode == 3) && target != null) {
            float yaw = RotationUtils.snapToMouseSensitivity(mode == 2 ? backwardYaw : sidewaysYaw, previousYaw);
            if (tracePlacement(target.position(), target.face(), yaw, placementPitch) != null) placementYaw = yaw;
        }

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
            if (isHypixelRotation() && !minecraft.field_71439_g.field_70122_E) {
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
            } else if (mode == 1 || isHypixelRotation()) {
                float[] turn = smoothPlacement(previousYaw, previousPitch, difference, rotationSmoothing.getValue(), maxChange);
                yaw = turn[0];
                pitch = turn[1];
            } else if (Math.abs(difference) > maxChange) {
                yaw = RotationUtils.snapToMouseSensitivity(previousYaw + RotationUtils.clampAngleChange(difference, maxChange), previousYaw);
                placementDelayTicks = Math.max(placementDelayTicks, 1);
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

    private void placeCurrentAndAdditionalBlocks(BlockPlacementTarget blockPlacementTarget, Vec3 vec3) {
        if (blockPlacementTarget == null || vec3 == null || this.placementDelayTicks > 0) {
            return;
        }
        this.placeBlock(blockPlacementTarget.position(), blockPlacementTarget.face());
        if (!((Boolean)this.multiPlace.getValue()).booleanValue()) {
            return;
        }
        for (int i = 0; i < 3 && (blockPlacementTarget = this.findPlacementTarget()) != null; ++i) {
            double d;
            MovingObjectPosition movingObjectPosition = RotationUtils.traceBlocks(this.placementYaw, this.placementPitch, ScaffoldModule.minecraft.field_71442_b.func_78757_d(), 1.0f);
            if (movingObjectPosition != null && movingObjectPosition.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK && movingObjectPosition.func_178782_a().equals((Object)blockPlacementTarget.position()) && movingObjectPosition.field_178784_b == blockPlacementTarget.face()) {
                this.placeBlock(blockPlacementTarget.position(), blockPlacementTarget.face());
                continue;
            }
            Vec3 vec32 = BlockUtils.randomFacePoint(blockPlacementTarget.position(), blockPlacementTarget.face());
            double d2 = vec32.field_72450_a - ScaffoldModule.minecraft.field_71439_g.field_70165_t;
            double d3 = vec32.field_72448_b - ScaffoldModule.minecraft.field_71439_g.field_70163_u - (double)ScaffoldModule.minecraft.field_71439_g.func_70047_e();
            float[] fArray = RotationUtils.rotationsFromDelta(d2, d3, d = vec32.field_72449_c - ScaffoldModule.minecraft.field_71439_g.field_70161_v);
            if (Math.abs(MathHelper.func_76142_g((float)(fArray[0] - this.placementYaw))) >= 120.0f || Math.abs(fArray[1] - this.placementPitch) >= 60.0f || (movingObjectPosition = RotationUtils.traceBlocks(fArray[0], fArray[1], ScaffoldModule.minecraft.field_71442_b.func_78757_d(), 1.0f)) == null || movingObjectPosition.field_72313_a != MovingObjectPosition.MovingObjectType.BLOCK || !movingObjectPosition.func_178782_a().equals((Object)blockPlacementTarget.position()) || movingObjectPosition.field_178784_b != blockPlacementTarget.face()) break;
            this.placeBlock(blockPlacementTarget.position(), blockPlacementTarget.face());
        }
    }

    private boolean handlePendingTowerPlacement() {
        if (this.pendingPlacementFace == null) {
            return false;
        }
        if (this.placementDelayTicks <= 0) {
            BlockPos blockPos = new BlockPos(MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70165_t), MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70163_u) - 1, MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70161_v));
            this.placeBlock(blockPos, this.pendingPlacementFace);
        }
        this.pendingPlacementFace = null;
        return true;
    }

    private void handleExtraKeepYRecovery() {
        if ((Integer)this.keepY.getValue() != 2 || this.keepYState <= 0 || ScaffoldModule.minecraft.field_71439_g.field_70122_E) {
            return;
        }
        int n = MathHelper.func_76128_c((double)(ScaffoldModule.minecraft.field_71439_g.field_70163_u + ScaffoldModule.minecraft.field_71439_g.field_70181_x));
        if (n > this.keepYLevel || ScaffoldModule.minecraft.field_71439_g.field_70163_u <= (double)(this.keepYLevel + 1)) {
            return;
        }
        this.keepYRecoveryPlacement = true;
        BlockPlacementTarget blockPlacementTarget = this.findPlacementTarget();
        if (blockPlacementTarget == null || this.placementDelayTicks > 0) {
            return;
        }
        this.placeBlock(blockPlacementTarget.position(), blockPlacementTarget.face());
    }

    @EventHandler(priority=50)
    public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
        float f;
        if (!this.isEnabled()) {
            return;
        }
        if (this.placementDelayTicks > 0) {
            --this.placementDelayTicks;
        }
        boolean onGround = minecraft.field_71439_g.field_70122_E;
        if (hypixelJumpDelay > 0) --hypixelJumpDelay;
        if (onGround && !wasOnGround && isHypixelRotation() && isJumpCycleActive()) {
            hypixelJumpDelay = HypixelScaffoldPacing.JUMP_DELAY_TICKS;
        }
        wasOnGround = onGround;
        if (onGround) jumpBaseY = MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
        if (ScaffoldModule.minecraft.field_71439_g.field_70122_E) {
            if (this.keepYState > 0) {
                --this.keepYState;
            }
            if (this.keepYState < 0) {
                ++this.keepYState;
            }
            if (!(this.keepYState != 0 || (Integer)this.keepY.getValue() == 0 || ((Boolean)this.keepYOnPress.getValue()).booleanValue() && !PlayerUtils.isUseItemKeyDown() || ScaffoldModule.minecraft.field_71474_y.field_74314_A.func_151470_d() || ScaffoldModule.minecraft.field_71439_g.func_70644_a(Potion.field_76430_j))) {
                this.keepYState = 1;
            }
            if (!this.keepYRecoveryPlacement) {
                this.keepYLevel = MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70163_u);
            }
            this.keepYRecoveryPlacement = false;
            this.tellyRotationActive = false;
        }
        if (!this.canPlaceWithActiveModules()) {
            return;
        }
        this.selectPlacementHotbarSlot();
        float f2 = rotationUpdateEvent.getPreviousYaw();
        float f3 = rotationUpdateEvent.getPreviousPitch();
        float f4 = this.getMovementYaw();
        float f5 = f4 - 180.0f;
        if (this.isDiagonalYaw(f4)) {
            f = f5;
        } else {
            float f6 = (f4 + 180.0f) % 90.0f < 45.0f ? 1.0f : -1.0f;
            f = f4 - 135.0f * f6;
        }
        if (!this.hasPlacementRotation) {
            if (this.placementYaw == -180.0f && this.placementPitch == 0.0f) {
                this.placementPitch = RotationUtils.snapToMouseSensitivity(RandomUtils.randomFloat(65.0f, 85.0f), f3);
            }
            switch ((Integer)this.rotations.getValue()) {
                case 2: {
                    this.placementYaw = RotationUtils.snapToMouseSensitivity(f5, f2);
                    break;
                }
                default: {
                    this.placementYaw = RotationUtils.snapToMouseSensitivity(f, f2);
                }
            }
        }
        BlockPlacementTarget blockPlacementTarget = this.findPlacementTarget();
        Vec3 vec3 = this.findBestPlacementHit(blockPlacementTarget, f2, f3);
        this.applyPlacementRotation(rotationUpdateEvent, blockPlacementTarget, f2, f3, f5, f);
        this.tickPlacementYaw = rotationUpdateEvent.getYaw();
        this.tickPlacementPitch = rotationUpdateEvent.getPitch();
        this.placeCurrentAndAdditionalBlocks(blockPlacementTarget, vec3);
        if (this.handlePendingTowerPlacement()) {
            return;
        }
        this.handleExtraKeepYRecovery();
    }

    @EventHandler
    public void onMoveFlying(MoveFlyingEvent moveFlyingEvent) {
        if (!this.isEnabled() || ScaffoldModule.minecraft.field_71439_g.field_70123_F || ScaffoldModule.minecraft.field_71439_g.field_70737_aN > 5 || ScaffoldModule.minecraft.field_71439_g.func_70644_a(Potion.field_76430_j) || !ScaffoldModule.minecraft.field_71474_y.field_74314_A.func_151470_d() || !ItemUtils.isHoldingPlaceableBlock()) {
            this.towerPhase = 0;
            this.towerCycleCount = 0;
            return;
        }
        int n = (int)(ScaffoldModule.minecraft.field_71439_g.field_70163_u % 1.0 * 100.0);
        switch ((Integer)this.tower.getValue()) {
            case 1: {
                this.applyVanillaTowerMotion(n, moveFlyingEvent);
                return;
            }
            case 2: {
                this.applyExtraTowerMotion(n, moveFlyingEvent);
                return;
            }
        }
        this.towerPhase = 0;
        this.towerCycleCount = 0;
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent movementInputEvent) {
        if (!this.isEnabled()) {
            return;
        }
        if ((Integer)this.moveFix.getValue() == 1 && RotationState.isActive() && RotationState.movementPriority() == 3.0f && MovementUtils.hasMovementInput()) {
            MovementUtils.correctMovementForYaw(RotationState.movementYaw());
        }
        if (hypixelJumpDelay > 0) {
            minecraft.field_71439_g.field_71158_b.field_78901_c = false;
            return;
        }
        if (!ScaffoldModule.minecraft.field_71439_g.field_70122_E || this.keepYState <= 0 || !MovementUtils.hasMovementInput()) {
            return;
        }
        ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78901_c = true;
    }

    @EventHandler
    public void onLivingUpdate(LivingUpdateEvent livingUpdateEvent) {
        if (!this.isEnabled()) {
            return;
        }
        float f = this.getMotionMultiplier();
        if (f != 1.0f) {
            if (ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78900_b != 0.0f && ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78902_a != 0.0f) {
                float f2 = 1.0f / (float)Math.sqrt(2.0);
                ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78900_b *= f2;
                ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78902_a *= f2;
            }
            ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78900_b *= f;
            ScaffoldModule.minecraft.field_71439_g.field_71158_b.field_78902_a *= f;
        }
        if (this.shouldDisableSprint()) {
            ScaffoldModule.minecraft.field_71439_g.func_70031_b(false);
        }
    }

    @EventHandler
    public void onSafeWalk(SafeWalkEvent safeWalkEvent) {
        if (!this.isEnabled() || !((Boolean)this.safeWalk.getValue()).booleanValue() || !ScaffoldModule.minecraft.field_71439_g.field_70122_E || ScaffoldModule.minecraft.field_71439_g.field_70181_x > 0.0) {
            return;
        }
        if (PlayerUtils.isOffsetCollisionFree(ScaffoldModule.minecraft.field_71439_g.field_70159_w, -1.0, ScaffoldModule.minecraft.field_71439_g.field_70179_y)) {
            safeWalkEvent.setSafeWalk(true);
        }
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent renderOverlayEvent) {
        if (!this.isEnabled() || !((Boolean)this.blockCounter.getValue()).booleanValue()) {
            return;
        }
        int n = 0;
        for (int i = 0; i < 9; ++i) {
            Block block;
            ItemStack itemStack = ScaffoldModule.minecraft.field_71439_g.field_71071_by.func_70301_a(i);
            if (itemStack == null || itemStack.field_77994_a <= 0 || !(itemStack.func_77973_b() instanceof ItemBlock) || BlockUtils.isInteractable(block = ((ItemBlock)itemStack.func_77973_b()).func_179223_d()) || !BlockUtils.isSuitableFullBlock(block)) continue;
            n += itemStack.field_77994_a;
        }
        this.drawBlockCounter(n);
    }

    @EventHandler
    public void onLeftClick(LeftClickEvent leftClickEvent) {
        if (this.isEnabled()) {
            leftClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClick(RightClickEvent rightClickEvent) {
        if (this.isEnabled()) {
            rightClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockClick(BlockClickEvent blockClickEvent) {
        if (this.isEnabled()) {
            blockClickEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarChange(HotbarChangeEvent hotbarChangeEvent) {
        if (!this.isEnabled()) {
            return;
        }
        this.previousHotbarSlot = hotbarChangeEvent.getTargetSlot(this.previousHotbarSlot);
        hotbarChangeEvent.setCancelled(true);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        resetHypixelRotation();
    }

    @Override
    public void onSettingChanged(String name) {
        if ("rotations".equals(name)) resetHypixelRotation();
    }

    private void resetHypixelRotation() {
        hypixelJumpDelay = 0;
        wasOnGround = minecraft.field_71439_g != null && minecraft.field_71439_g.field_70122_E;
        jumpBaseY = minecraft.field_71439_g == null ? 0 : MathHelper.func_76128_c(minecraft.field_71439_g.field_70163_u);
    }

    @Override
    public void onEnable() {
        resetHypixelRotation();
        this.previousHotbarSlot = ScaffoldModule.minecraft.field_71439_g != null ? ScaffoldModule.minecraft.field_71439_g.field_71071_by.field_70461_c : -1;
        this.remainingStackBlocks = -1;
        this.placementDelayTicks = 0;
        this.placementYaw = -180.0f;
        this.placementPitch = 0.0f;
        this.hasPlacementRotation = false;
        this.towerPhase = 0;
        this.towerCycleCount = 0;
        this.tellyRotationActive = false;
    }

    @Override
    public void onDisable() {
        resetHypixelRotation();
        if (ScaffoldModule.minecraft.field_71439_g != null && this.previousHotbarSlot != -1) {
            ScaffoldModule.minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
        }
    }

    private boolean isKeepYOnPressVisible() {
        return (Integer)this.keepY.getValue() != 0;
    }

    private static double targetDistanceSquared(BlockPos blockPos, BlockPos blockPos2) {
        return blockPos2.func_177957_d((double)blockPos.func_177958_n() + 0.5, (double)blockPos.func_177956_o() + 0.5, (double)blockPos.func_177952_p() + 0.5);
    }

    private void applyVanillaTowerMotion(int n, MoveFlyingEvent moveFlyingEvent) {
        switch (this.towerPhase) {
            case 0: {
                if (ScaffoldModule.minecraft.field_71439_g.field_70122_E) {
                    this.towerPhase = 1;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
                }
                return;
            }
            case 1: {
                if (n == 0 && PlayerUtils.hasCollisionBelow()) {
                    this.keepYLevel = MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70163_u);
                    this.towerPhase = 2;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = 0.42f;
                    if (MovementUtils.hasMovementInput()) {
                        MovementUtils.setHorizontalMotion(MovementUtils.horizontalSpeed(), MovementUtils.movementYaw());
                    } else {
                        MovementUtils.setSpeedAlongMotion(0.0);
                        moveFlyingEvent.setForward(0.0f);
                        moveFlyingEvent.setStrafe(0.0f);
                    }
                    return;
                }
                this.towerPhase = 0;
                return;
            }
            case 2: {
                this.towerPhase = 3;
                ScaffoldModule.minecraft.field_71439_g.field_70181_x = 0.75 - ScaffoldModule.minecraft.field_71439_g.field_70163_u % 1.0;
                return;
            }
            case 3: {
                this.towerPhase = 1;
                ScaffoldModule.minecraft.field_71439_g.field_70181_x = 1.0 - ScaffoldModule.minecraft.field_71439_g.field_70163_u % 1.0;
                return;
            }
        }
        this.towerPhase = 0;
    }

    private void applyExtraTowerMotion(int n, MoveFlyingEvent moveFlyingEvent) {
        switch (this.towerPhase) {
            case 0: {
                if (ScaffoldModule.minecraft.field_71439_g.field_70122_E) {
                    this.towerPhase = 1;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
                }
                return;
            }
            case 1: {
                if (n == 0 && PlayerUtils.hasCollisionBelow()) {
                    this.keepYLevel = MathHelper.func_76128_c((double)ScaffoldModule.minecraft.field_71439_g.field_70163_u);
                    if (!MovementUtils.hasMovementInput()) {
                        this.towerCycleCount = 2;
                        MovementUtils.setSpeedAlongMotion(0.0);
                        moveFlyingEvent.setForward(0.0f);
                        moveFlyingEvent.setStrafe(0.0f);
                        EnumFacing enumFacing = this.facingFromYaw(MathHelper.func_76142_g((float)(this.placementYaw - 180.0f)));
                        double d = this.distanceToBlockEdge(enumFacing);
                        if (d > 0.1) {
                            if (ScaffoldModule.minecraft.field_71439_g.field_70122_E) {
                                Vec3i vec3i = enumFacing.func_176730_m();
                                double d2 = Math.min(this.randomEdgeApproachDistance(), d - 0.05);
                                double d3 = RandomUtils.randomDouble(0.02, 0.03);
                                AxisAlignedBB axisAlignedBB = ScaffoldModule.minecraft.field_71439_g.func_174813_aQ().func_72317_d((double)vec3i.func_177958_n() * (d2 - d3), 0.0, (double)vec3i.func_177952_p() * (d2 - d3));
                                if (ScaffoldModule.minecraft.field_71441_e.func_72945_a((Entity)ScaffoldModule.minecraft.field_71439_g, axisAlignedBB).isEmpty()) {
                                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = -0.0784000015258789;
                                    ScaffoldModule.minecraft.field_71439_g.func_70107_b(axisAlignedBB.field_72340_a + (axisAlignedBB.field_72336_d - axisAlignedBB.field_72340_a) / 2.0, axisAlignedBB.field_72338_b, axisAlignedBB.field_72339_c + (axisAlignedBB.field_72334_f - axisAlignedBB.field_72339_c) / 2.0);
                                }
                                return;
                            }
                        } else {
                            this.towerPhase = 2;
                            this.pendingPlacementFace = enumFacing;
                            ScaffoldModule.minecraft.field_71439_g.field_70181_x = 0.42f;
                        }
                        return;
                    }
                    this.towerPhase = 2;
                    ++this.towerCycleCount;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = 0.42f;
                    MovementUtils.setHorizontalMotion(MovementUtils.horizontalSpeed(), MovementUtils.movementYaw());
                    return;
                }
                this.towerPhase = 0;
                this.towerCycleCount = 0;
                return;
            }
            case 2: {
                this.towerPhase = 3;
                ScaffoldModule.minecraft.field_71439_g.field_70181_x -= RandomUtils.randomDouble(0.00101, 0.00109);
                return;
            }
            case 3: {
                if (this.towerCycleCount >= 4) {
                    this.towerPhase = 4;
                    this.towerCycleCount = 0;
                } else {
                    this.towerPhase = 1;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x = 1.0 - ScaffoldModule.minecraft.field_71439_g.field_70163_u % 1.0;
                }
                return;
            }
            case 4: {
                this.towerPhase = 5;
                return;
            }
            case 5: {
                if (!PlayerUtils.hasCollisionBelow()) {
                    this.towerPhase = 0;
                } else {
                    this.towerPhase = 1;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x -= 0.08;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x *= (double)0.98f;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x -= 0.08;
                    ScaffoldModule.minecraft.field_71439_g.field_70181_x *= (double)0.98f;
                }
                return;
            }
        }
        this.towerPhase = 0;
        this.towerCycleCount = 0;
    }

    private void drawBlockCounter(int n) {
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
