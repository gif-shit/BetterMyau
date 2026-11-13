package myau;

import com.google.common.base.CaseFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import myau.mixin.AccessorPlayerControllerMP;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;

public class BedNukerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ScheduledExecutorService bedScanExecutor = Executors.newScheduledThreadPool(1);
    private final ElapsedTimer bedBreakTimer = new ElapsedTimer();
    private final Color lowProgressColor = new Color(ChatColor.RED.getRgb());
    private final Color midProgressColor = new Color(ChatColor.YELLOW.getRgb());
    private final Color highProgressColor = new Color(ChatColor.GREEN.getRgb());
    private BlockPos targetBlock = null;
    private BlockPos whitelistedBed = null;
    private int breakPhase = 0;
    private int breakTicks = 0;
    private float accumulatedBreakDamage = 0.0f;
    private boolean targetIsBed = false;
    private int previousHotbarSlot = -1;
    private boolean interactionLocked = false;
    private boolean diggingInProgress = false;
    private boolean awaitingSpawnTeleport = false;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[] { "LEGIT", "SWAP" });
    public final FloatSetting range = new FloatSetting("range", 4.5f, 3.0f, 6.0f);
    public final PercentSetting speed = new PercentSetting("speed", 0);
    public final BooleanSetting groundSpoof = new BooleanSetting("ground-spoof", false);
    public final ModeSetting ignoreVelocity = new ModeSetting("ignore-velocity", 0, new String[] { "NONE", "CANCEL", "DELAY" });
    public final BooleanSetting surroundings = new BooleanSetting("surroundings", true);
    public final BooleanSetting toolCheck = new BooleanSetting("tool-check", true);
    public final BooleanSetting whitelist = new BooleanSetting("whitelist", true);
    public final BooleanSetting swing = new BooleanSetting("swing", true);
    public final ModeSetting moveFix = new ModeSetting("move-fix", 1, new String[] { "NONE", "SILENT", "STRICT" });
    public final ModeSetting showTarget = new ModeSetting("show-target", 1, new String[] { "NONE", "DEFAULT", "HUD" });
    public final ModeSetting showProgress = new ModeSetting("show-progress", 1, new String[] { "NONE", "DEFAULT", "HUD" });
    private static final long BED_SCAN_COOLDOWN_MILLIS = 500L;

    private void resetBreakState() {
        if (this.targetBlock != null) {
            minecraft.field_71441_e.func_175715_c(minecraft.field_71439_g.func_145782_y(), this.targetBlock, -1);
        }
        this.targetBlock = null;
        this.breakPhase = 0;
        this.breakTicks = 0;
        this.accumulatedBreakDamage = 0.0f;
        this.targetIsBed = false;
        this.interactionLocked = false;
        this.diggingInProgress = false;
    }

    private float getBreakProgress() {
        if (this.targetBlock == null) {
            return 0.0f;
        }
        float breakDamage = this.accumulatedBreakDamage;
        if (this.groundSpoof.getValue()) {
            IBlockState state = minecraft.field_71441_e.func_180495_p(this.targetBlock);
            int bestToolSlot = ItemUtils.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, state.func_177230_c());
            breakDamage = (float) this.breakTicks * this.getRelativeBlockHardness(state, this.targetBlock, bestToolSlot, true);
        }
        float completionThreshold = 1.0f - 0.3f * ((float) this.speed.getValue() / 100.0f);
        return Math.min(1.0f, breakDamage / completionThreshold);
    }

    private void restoreHotbarSlot() {
        if (this.previousHotbarSlot == -1) {
            return;
        }
        minecraft.field_71439_g.field_71071_by.field_70461_c = this.previousHotbarSlot;
        this.syncHeldItem();
        this.previousHotbarSlot = -1;
    }

    private void syncHeldItem() {
        AccessorPlayerControllerMP controller = (AccessorPlayerControllerMP) minecraft.field_71442_b;
        if (controller.getCurrentPlayerItem() != minecraft.field_71439_g.field_71071_by.field_70461_c) {
            minecraft.field_71439_g.func_71034_by();
        }
        controller.callSyncCurrentPlayItem();
    }

    private boolean hasRequiredTool(Block block) {
        Material material = block.func_149688_o();
        if (material != Material.field_151573_f && material != Material.field_151574_g && material != Material.field_151576_e) {
            return true;
        }
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
            if (stack == null || !(stack.func_77973_b() instanceof ItemPickaxe))
                continue;
            return true;
        }
        return false;
    }

    private EnumFacing findBreakFace(BlockPos position) {
        double deltaZ;
        double deltaY;
        double deltaX = (double) position.func_177958_n() + 0.5 - minecraft.field_71439_g.field_70165_t;
        float[] rotations = RotationUtils.rotationsFromDelta(deltaX, deltaY = (double) position.func_177956_o() + 0.25 - minecraft.field_71439_g.field_70163_u - (double) minecraft.field_71439_g.func_70047_e(), deltaZ = (double) position.func_177952_p() + 0.5 - minecraft.field_71439_g.field_70161_v);
        MovingObjectPosition hit = RotationUtils.traceBlocks(rotations[0], rotations[1], 8.0, 1.0f);
        return hit == null ? EnumFacing.UP : hit.field_178784_b;
    }

    private float getDigSpeedForSlot(IBlockState blockState, int slot, boolean onGround) {
        int efficiencyLevel;
        float digSpeed;
        ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
        float f = digSpeed = stack == null ? 1.0f : stack.func_77973_b().getDigSpeed(stack, blockState);
        if (digSpeed > 1.0f
                && stack != null
                && (efficiencyLevel = EnchantmentHelper.func_77506_a((int) Enchantment.field_77349_p.field_77352_x, (ItemStack) stack)) > 0) {
            digSpeed += (float) (efficiencyLevel * efficiencyLevel + 1);
        }
        if (minecraft.field_71439_g.func_70644_a(Potion.field_76422_e)) {
            int amplifier = minecraft.field_71439_g.func_70660_b(Potion.field_76422_e).func_76458_c();
            digSpeed *= 1.0f + (float) (amplifier + 1) * 0.2f;
        }
        if (minecraft.field_71439_g.func_70644_a(Potion.field_76419_f)) {
            switch(minecraft.field_71439_g.func_70660_b(Potion.field_76419_f).func_76458_c()) {
                case 0:
                    {
                        digSpeed *= 0.3f;
                        break;
                    }
                case 1:
                    {
                        digSpeed *= 0.09f;
                        break;
                    }
                case 2:
                    {
                        digSpeed *= 0.0027f;
                        break;
                    }
                default:
                    {
                        digSpeed *= 8.1E-4f;
                    }
            }
        }
        if (minecraft.field_71439_g.func_70055_a(Material.field_151586_h)
                && !EnchantmentHelper.func_77510_g((EntityLivingBase) minecraft.field_71439_g)) {
            digSpeed /= 5.0f;
        }
        if (!onGround) {
            digSpeed /= 5.0f;
        }
        return digSpeed;
    }

    private boolean canHarvestBlock(Block block, int slot) {
        if (block.func_149688_o().func_76229_l()) {
            return true;
        }
        ItemStack stack = minecraft.field_71439_g.field_71071_by.func_70301_a(slot);
        return stack != null && stack.func_150998_b(block);
    }

    private float getRelativeBlockHardness(IBlockState blockState, BlockPos position, int slot, boolean onGround) {
        Block block = blockState.func_177230_c();
        float hardness = block.func_176195_g((World) minecraft.field_71441_e, position);
        if (hardness < 0.0f) {
            return 0.0f;
        }
        float divisor = this.canHarvestBlock(block, slot) ? 30.0f : 100.0f;
        return this.getDigSpeedForSlot(blockState, slot, onGround) / hardness / divisor;
    }

    private float getBestBreakSpeed(BlockPos position) {
        IBlockState state = minecraft.field_71441_e.func_180495_p(position);
        int bestToolSlot = ItemUtils.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, state.func_177230_c());
        return this.getRelativeBlockHardness(state, position, bestToolSlot, minecraft.field_71439_g.field_70122_E);
    }

    private BlockPos findSurroundingBlock(BlockPos bedPosition) {
        IBlockState bedState = minecraft.field_71441_e.func_180495_p(bedPosition);
        if (!(bedState.func_177230_c() instanceof BlockBed)) {
            return null;
        }
        BlockBed.EnumPartType part = (BlockBed.EnumPartType) bedState.func_177229_b((IProperty) BlockBed.field_176472_a);
        EnumFacing facing = (EnumFacing) bedState.func_177229_b((IProperty) BlockBed.field_176387_N);
        BlockPos otherHalf = bedPosition.func_177972_a(part == BlockBed.EnumPartType.HEAD ? facing.func_176734_d() : facing);
        BlockPos[] bedHalves = new BlockPos[] { bedPosition, otherHalf };
        ArrayList<BlockPos> surroundingBlocks = new ArrayList<BlockPos>();
        EnumFacing[] surroundingFaces = new EnumFacing[] { EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST };
        for (BlockPos bedHalf : bedHalves) {
            for (EnumFacing surroundingFace : surroundingFaces) {
                BlockPos surroundingPosition = bedHalf.func_177972_a(surroundingFace);
                Block surroundingBlock = minecraft.field_71441_e.func_180495_p(surroundingPosition).func_177230_c();
                if (BlockUtils.isReplaceable(surroundingBlock)) {
                    return null;
                }
                if (surroundingBlock instanceof BlockBed)
                    continue;
                surroundingBlocks.add(surroundingPosition);
            }
        }
        if (surroundingBlocks.isEmpty()) {
            return null;
        }
        surroundingBlocks.sort(this::compareSurroundingBlocks);
        return (BlockPos) surroundingBlocks.get(0);
    }

    private BlockPos findTargetBlock() {
        return this.findTargetBlockNear(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e(), minecraft.field_71439_g.field_70161_v);
    }

    private BlockPos findTargetBlockNear(double originX, double originY, double originZ) {
        ArrayList<BlockPos> beds = new ArrayList<BlockPos>();
        int blockX = MathHelper.func_76128_c((double) originX);
        int blockY = MathHelper.func_76128_c((double) originY);
        int blockZ = MathHelper.func_76128_c((double) originZ);
        for (int x = blockX - 6; x <= blockX + 6; ++x) {
            for (int y = blockY - 6; y <= blockY + 6; ++y) {
                for (int z = blockZ - 6; z <= blockZ + 6; ++z) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (myau.openb4.module.minigames.BedWarsModule.isOwnBed(position)
                            || this.whitelist.getValue()
                            && this.whitelistedBed != null
                            && this.whitelistedBed.func_177951_i((Vec3i) position) < 10.0
                            || !(minecraft.field_71441_e.func_180495_p(position).func_177230_c() instanceof BlockBed)
                            || !PlayerUtils.isBlockWithinReachFrom(position, originX, originY, originZ, this.range.getValue().doubleValue()))
                        continue;
                    beds.add(position);
                }
            }
        }
        if (beds.isEmpty()) {
            return null;
        }
        beds.sort(Comparator.comparingDouble(BedNukerModule::distanceFromEyesSquared));
        for (BlockPos bed : beds) {
            if (!this.surroundings.getValue()) {
                return bed;
            }
            BlockPos surroundingBlock = this.findSurroundingBlock(bed);
            if (surroundingBlock == null) {
                return bed;
            }
            if (this.toolCheck.getValue()
                    && !this.hasRequiredTool(minecraft.field_71441_e.func_180495_p(surroundingBlock).func_177230_c()))
                continue;
            return surroundingBlock;
        }
        return null;
    }

    private void swingOrSendAnimation() {
        if (this.swing.getValue()) {
            minecraft.field_71439_g.func_71038_i();
            return;
        }
        PacketUtils.send(new C0APacketAnimation());
    }

    private Color getProgressColor(int displayMode) {
        switch(displayMode) {
            case 1:
                {
                    float progress = this.getBreakProgress();
                    if (progress <= 0.5f) {
                        return ColorUtils.interpolate(progress / 0.5f, this.lowProgressColor, this.midProgressColor);
                    }
                    return ColorUtils.interpolate((progress - 0.5f) / 0.5f, this.midProgressColor, this.highProgressColor);
                }
            case 2:
                {
                    return ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis());
                }
        }
        return new Color(-1);
    }

    public BedNukerModule() {
        super("BedNuker", false, 0);
    }

    public boolean isControllingInteractions() {
        return this.targetBlock != null && this.interactionLocked;
    }

    public boolean isDigging() {
        return this.targetBlock != null && this.diggingInProgress;
    }

    @EventHandler(priority = 50)
    public void onPreTick(PreTickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.targetBlock != null) {
            boolean invalidTarget;
            boolean bl = invalidTarget = minecraft.field_71441_e.func_175623_d(this.targetBlock)
                    || !PlayerUtils.isBlockWithinReach(this.targetBlock, this.range.getValue().doubleValue());
            if (!invalidTarget && !this.targetIsBed) {
                BlockPos newTarget = this.findTargetBlock();
                boolean bl2 = invalidTarget = newTarget != null && minecraft.field_71441_e.func_180495_p(newTarget).func_177230_c() instanceof BlockBed;
            }
            if (invalidTarget) {
                this.restoreHotbarSlot();
                this.resetBreakState();
            }
        }
        if (this.targetBlock != null) {
            IBlockState blockState = minecraft.field_71441_e.func_180495_p(this.targetBlock);
            int toolSlot = ItemUtils.findBestToolForBlock(minecraft.field_71439_g.field_71071_by.field_70461_c, blockState.func_177230_c());
            if (this.mode.getValue() == 0 && this.previousHotbarSlot == -1) {
                this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                minecraft.field_71439_g.field_71071_by.field_70461_c = toolSlot;
                this.syncHeldItem();
            }
            switch(this.breakPhase) {
                case 0:
                    {
                        if (minecraft.field_71439_g.func_71039_bw())
                            break;
                        this.swingOrSendAnimation();
                        PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, this.targetBlock, this.findBreakFace(this.targetBlock)));
                        this.swingOrSendAnimation();
                        minecraft.field_71452_i.func_180532_a(this.targetBlock, this.findBreakFace(this.targetBlock));
                        this.breakPhase = 1;
                        break;
                    }
                case 1:
                    {
                        if (this.mode.getValue() != 0) {
                            this.interactionLocked = false;
                        }
                        this.diggingInProgress = true;
                        ++this.breakTicks;
                        this.accumulatedBreakDamage += this.getRelativeBlockHardness(blockState, this.targetBlock, toolSlot, minecraft.field_71439_g.field_70122_E);
                        boolean effectiveGround = minecraft.field_71439_g.field_70122_E || this.groundSpoof.getValue();
                        float projectedDamage = (float) this.breakTicks * this.getRelativeBlockHardness(blockState, this.targetBlock, toolSlot, effectiveGround);
                        minecraft.field_71452_i.func_180532_a(this.targetBlock, this.findBreakFace(this.targetBlock));
                        float threshold = 1.0f - 0.3f * ((float) this.speed.getValue() / 100.0f);
                        if (!(this.accumulatedBreakDamage >= threshold) && !(projectedDamage >= threshold))
                            break;
                        if (this.mode.getValue() != 0) {
                            this.interactionLocked = true;
                            this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                            minecraft.field_71439_g.field_71071_by.field_70461_c = toolSlot;
                            this.syncHeldItem();
                            if (minecraft.field_71439_g.func_71039_bw()) {
                                this.previousHotbarSlot = minecraft.field_71439_g.field_71071_by.field_70461_c;
                                minecraft.field_71439_g.field_71071_by.field_70461_c = (minecraft.field_71439_g.field_71071_by.field_70461_c + 1) % 9;
                                this.syncHeldItem();
                            }
                        }
                        this.diggingInProgress = false;
                        PacketUtils.send(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.targetBlock, this.findBreakFace(this.targetBlock)));
                        this.swingOrSendAnimation();
                        IBlockState currentState = minecraft.field_71441_e.func_180495_p(this.targetBlock);
                        Block currentBlock = currentState.func_177230_c();
                        if (currentBlock.func_149688_o() != Material.field_151579_a) {
                            minecraft.field_71441_e.func_175718_b(2001, this.targetBlock, Block.func_176210_f((IBlockState) currentState));
                            minecraft.field_71441_e.func_175698_g(this.targetBlock);
                        }
                        if (currentBlock instanceof BlockBed) {
                            this.bedBreakTimer.reset();
                        }
                        this.breakPhase = 2;
                        break;
                    }
                case 2:
                    {
                        this.restoreHotbarSlot();
                        this.resetBreakState();
                        break;
                    }
                default:
                    {
                        this.resetBreakState();
                    }
            }
            if (this.targetBlock != null) {
                return;
            }
        }
        if (minecraft.field_71439_g.field_71075_bZ.field_75099_e && this.bedBreakTimer.hasElapsed(500L)) {
            this.targetBlock = this.findTargetBlock();
            this.breakPhase = 0;
            this.breakTicks = 0;
            this.accumulatedBreakDamage = 0.0f;
            this.targetIsBed = this.targetBlock != null && minecraft.field_71441_e.func_180495_p(this.targetBlock).func_177230_c() instanceof BlockBed;
            this.restoreHotbarSlot();
            if (this.targetBlock != null) {
                this.interactionLocked = true;
            }
        }
        if (this.targetBlock == null) {
            MyauClient.incomingPacketBuffer.setEnabled(false, IncomingBufferOwner.BED_NUKER);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled() || !this.isControllingInteractions()) {
            return;
        }
        double deltaX = (double) this.targetBlock.func_177958_n() + 0.5 - minecraft.field_71439_g.field_70165_t;
        double deltaY = (double) this.targetBlock.func_177956_o() + 0.5 - minecraft.field_71439_g.field_70163_u - (double) minecraft.field_71439_g.func_70047_e();
        double deltaZ = (double) this.targetBlock.func_177952_p() + 0.5 - minecraft.field_71439_g.field_70161_v;
        float previousYaw = event.getPreviousYaw();
        float previousPitch = event.getPreviousPitch();
        float[] rotation = RotationUtils.rotationsFromDelta(deltaX, deltaY, deltaZ);
        rotation[0] = RotationUtils.snapToMouseSensitivity(rotation[0], previousYaw);
        rotation[1] = RotationUtils.snapToMouseSensitivity(rotation[1], previousPitch);
        rotation[1] = MathHelper.func_76131_a((float) rotation[1], (float) -90.0f, (float) 90.0f);
        event.setRotation(rotation[0], rotation[1], 5);
        if (this.moveFix.getValue() != 0) {
            event.setMovementYaw(rotation[0], 5);
        }
    }

    @EventHandler
    public void onPreWalkingUpdate(PreWalkingUpdateEvent event) {
        if (!this.isEnabled() || !this.isDigging()) {
            return;
        }
        if (MyauClient.packetActionTracker.sentUseEntity
                || MyauClient.packetActionTracker.sentDigging
                || MyauClient.packetActionTracker.sentBlockPlacement
                || MyauClient.packetActionTracker.sentAnimation) {
            return;
        }
        this.swingOrSendAnimation();
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        if (!this.isEnabled() || this.moveFix.getValue() == 0) {
            return;
        }
        if (RotationState.isActive() && RotationState.movementPriority() == 5.0f && MovementUtils.hasMovementInput()) {
            MovementUtils.correctMovementForYaw(RotationState.movementYaw());
        }
    }

    @EventHandler(priority = 50)
    public void onVelocity(VelocityEvent event) {
        if (!this.isEnabled() || event.isCancelled() || event.getVelocityY() <= 0.0) {
            return;
        }
        if (this.ignoreVelocity.getValue() == 1 && this.targetBlock != null) {
            event.setCancelled(true);
            event.setVelocityX(minecraft.field_71439_g.field_70159_w);
            event.setVelocityY(minecraft.field_71439_g.field_70181_x);
            event.setVelocityZ(minecraft.field_71439_g.field_70179_y);
        }
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled() || this.targetBlock == null) {
            return;
        }
        if (this.targetIsBed && this.surroundings.getValue()) {
            return;
        }
        if (this.showProgress.getValue() == 0) {
            return;
        }
        HUDModule hud = (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
        float scale = hud.scale.getValue();
        String message = String.format("%d%%", (int) (this.getBreakProgress() * 100.0f));
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float) scale, (float) scale, (float) 0.0f);
        GlStateManager.func_179097_i();
        GlStateManager.func_179147_l();
        GlStateManager.func_179112_b((int) 770, (int) 771);
        int textWidth = minecraft.field_71466_p.func_78256_a(message);
        ScaledResolution resolution = new ScaledResolution(minecraft);
        minecraft.field_71466_p.func_175065_a(message, (float) resolution.func_78326_a() / 2.0f / scale - (float) textWidth / 2.0f, (float) resolution.func_78328_b() / 5.0f * 2.0f / scale, this.getProgressColor(this.showProgress.getValue()).getRGB() & 0xFFFFFF | 0xBF000000, hud.shadow.getValue().booleanValue());
        GlStateManager.func_179084_k();
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }

    @EventHandler(priority = -50)
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled() || this.targetBlock == null || minecraft.field_71441_e.func_175623_d(this.targetBlock)) {
            return;
        }
        minecraft.field_71441_e.func_175715_c(minecraft.field_71439_g.func_145782_y(), this.targetBlock, (int) (this.getBreakProgress() * 10.0f) - 1);
        if (this.showTarget.getValue() == 0) {
            return;
        }
        BedESPModule bedESP = (BedESPModule) MyauClient.moduleManager.modules.get(BedESPModule.class);
        Color color = this.getProgressColor(this.showTarget.getValue());
        double height = this.targetIsBed ? bedESP.getBedHeight() : 1.0;
        RenderUtils.beginOverlay();
        RenderUtils.fillBlockBounds(this.targetBlock, height, color.getRed(), color.getGreen(), color.getBlue(), 63);
        RenderUtils.endOverlay();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.awaitingSpawnTeleport = false;
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        S27PacketExplosion explosionPacket;
        String message;
        if (event.isCancelled()) {
            return;
        }
        if (event.getPacket() instanceof S02PacketChat
                && ((message = ((S02PacketChat) event.getPacket()).func_148915_c().func_150254_d()).contains("\u00a7e\u00a7lProtect your bed and destroy the enemy bed")
                || message.contains("\u00a7e\u00a7lDestroy the enemy bed and then eliminate them")
                || message.contains("\u00a7e\u00a7lEvery few seconds brings a new surprise"))) {
            this.awaitingSpawnTeleport = true;
        }
        if (event.getPacket() instanceof S08PacketPlayerPosLook && this.awaitingSpawnTeleport) {
            this.awaitingSpawnTeleport = false;
            this.whitelistedBed = null;
            this.bedScanExecutor.schedule(this::scanNearbyBedForWhitelist, 1L, TimeUnit.SECONDS);
        }
        if (!this.isEnabled()
                || this.targetBlock == null
                || this.ignoreVelocity.getValue() != 2
                || MyauClient.incomingPacketBuffer.owner() == IncomingBufferOwner.BED_NUKER) {
            return;
        }
        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) event.getPacket();
            if (velocityPacket.func_149412_c() == minecraft.field_71439_g.func_145782_y() && velocityPacket.func_149410_e() > 0) {
                MyauClient.incomingPacketBuffer.setOwner(IncomingBufferOwner.BED_NUKER);
                MyauClient.incomingPacketBuffer.queuedPackets.offer((Packet<INetHandlerPlayClient>) velocityPacket);
                event.setCancelled(true);
            }
            return;
        }
        if (event.getPacket() instanceof S27PacketExplosion
                && ((explosionPacket = (S27PacketExplosion) event.getPacket()).func_149149_c() != 0.0f
                || explosionPacket.func_149144_d() != 0.0f
                || explosionPacket.func_149147_e() != 0.0f)) {
            MyauClient.incomingPacketBuffer.setOwner(IncomingBufferOwner.BED_NUKER);
            MyauClient.incomingPacketBuffer.queuedPackets.offer((Packet<INetHandlerPlayClient>) explosionPacket);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeftClick(LeftClickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isControllingInteractions()) {
            event.setCancelled(true);
            return;
        }
        MovingObjectPosition hit = minecraft.field_71476_x;
        if (this.targetBlock != null && hit != null && hit.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClick(RightClickEvent event) {
        if (this.isEnabled() && this.isControllingInteractions()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockClick(BlockClickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.isControllingInteractions()) {
            event.setCancelled(true);
            return;
        }
        MovingObjectPosition hit = minecraft.field_71476_x;
        if (this.targetBlock != null && hit != null && hit.field_72313_a == MovingObjectPosition.MovingObjectType.BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHotbarChange(HotbarChangeEvent event) {
        if (!this.isEnabled() || this.previousHotbarSlot == -1) {
            return;
        }
        this.previousHotbarSlot = event.getTargetSlot(this.previousHotbarSlot);
        event.setCancelled(true);
    }

    @Override
    public void onDisable() {
        this.resetBreakState();
        this.previousHotbarSlot = -1;
        MyauClient.incomingPacketBuffer.setEnabled(false, IncomingBufferOwner.BED_NUKER);
    }

    @Override
    public String[] getHudInfo() {
        return new String[] { CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName()) };
    }

    private void scanNearbyBedForWhitelist() {
        if (minecraft.field_71439_g == null || minecraft.field_71441_e == null) {
            return;
        }
        int centerX = MathHelper.func_76128_c((double) minecraft.field_71439_g.field_70165_t);
        int centerY = MathHelper.func_76128_c((double) (minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e()));
        int centerZ = MathHelper.func_76128_c((double) minecraft.field_71439_g.field_70161_v);
        BlockPos closestBed = null;
        int closestDistance = Integer.MAX_VALUE;
        for (int x = centerX - 25; x <= centerX + 25; ++x) {
            for (int y = centerY - 25; y <= centerY + 25; ++y) {
                for (int z = centerZ - 25; z <= centerZ + 25; ++z) {
                    int distance;
                    BlockPos position = new BlockPos(x, y, z);
                    if (!(minecraft.field_71441_e.func_180495_p(position).func_177230_c() instanceof BlockBed)
                            || (distance = Math.abs(x - centerX) + Math.abs(y - centerY) + Math.abs(z - centerZ)) >= closestDistance)
                        continue;
                    closestBed = position;
                    closestDistance = distance;
                }
            }
        }
        if (closestBed != null) {
            this.whitelistedBed = closestBed;
        }
    }

    private static double distanceFromEyesSquared(BlockPos blockPos) {
        return blockPos.func_177957_d(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + (double) minecraft.field_71439_g.func_70047_e(), minecraft.field_71439_g.field_70161_v);
    }

    private int compareSurroundingBlocks(BlockPos first, BlockPos second) {
        int speedComparison = Float.compare(this.getBestBreakSpeed(second), this.getBestBreakSpeed(first));
        if (speedComparison != 0) {
            return speedComparison;
        }
        return Double.compare(distanceFromEyesSquared(first), distanceFromEyesSquared(second));
    }
}
