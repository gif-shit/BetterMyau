package myau;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.BooleanSetting;
import myau.EventHandler;
import myau.IntegerSetting;
import myau.MathUtils;
import myau.ModeSetting;
import myau.Module;
import myau.PacketReceiveEvent;
import myau.PercentSetting;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.WorldLoadEvent;
import myau.mixin.AccessorMinecraft;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraftforge.common.ForgeModContainer;

public class XrayModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final LinkedHashSet<Integer> supportedBlocks = new LinkedHashSet<Integer>(Arrays.asList(56, 14, 15, 16, 73, 74, 21, 129, 52, 83, 115));
    private static final LinkedHashSet<Vec3i> adjacentOffsets = new LinkedHashSet<Vec3i>(Arrays.asList(new Vec3i(0, -1, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0)));
    private static final LinkedHashSet<Vec3i> radiusTwoOffsets = new LinkedHashSet<Vec3i>(Arrays.asList(new Vec3i(0, -2, 0), new Vec3i(1, -1, 0), new Vec3i(0, -1, -1), new Vec3i(0, -1, 0), new Vec3i(0, -1, 1), new Vec3i(-1, -1, 0), new Vec3i(2, 0, 0), new Vec3i(0, 0, 2), new Vec3i(0, 0, -2), new Vec3i(-2, 0, 0), new Vec3i(1, 0, -1), new Vec3i(1, 0, 0), new Vec3i(1, 0, 1), new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, -1), new Vec3i(-1, 0, 0), new Vec3i(-1, 0, 1), new Vec3i(1, 1, 0), new Vec3i(0, 1, -1), new Vec3i(0, 1, 0), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 0), new Vec3i(0, 2, 0)));
    public final CopyOnWriteArraySet<BlockPos> discoveredBlocks = new CopyOnWriteArraySet();
    public final CopyOnWriteArraySet<BlockPos> updatedBlocks = new CopyOnWriteArraySet();
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[]{"SOFT", "FULL"});
    public final PercentSetting opacity = new PercentSetting("opacity", 50);
    public final IntegerSetting range = new IntegerSetting("range", 64, 16, 512);
    public final BooleanSetting cavesOnly = new BooleanSetting("caves-only", true);
    public final IntegerSetting cavesRadius = new IntegerSetting("caves-radius", 2, 1, 2);
    public final BooleanSetting diamonds = new BooleanSetting("diamonds", true);
    public final BooleanSetting diamondsTracers = new BooleanSetting("diamonds-tracers", true, this.diamonds::getValue);
    public final BooleanSetting gold = new BooleanSetting("gold", true);
    public final BooleanSetting goldTracers = new BooleanSetting("gold-tracers", true, this.gold::getValue);
    public final BooleanSetting iron = new BooleanSetting("iron", false);
    public final BooleanSetting ironTracers = new BooleanSetting("iron-tracers", false, this.iron::getValue);
    public final BooleanSetting coal = new BooleanSetting("coal", false);
    public final BooleanSetting coalTracers = new BooleanSetting("coal-tracers", false, this.coal::getValue);
    public final BooleanSetting redstone = new BooleanSetting("redstone", false);
    public final BooleanSetting redstoneTracers = new BooleanSetting("redstone-tracers", false, this.redstone::getValue);
    public final BooleanSetting lapis = new BooleanSetting("lapis", false);
    public final BooleanSetting lapisTracers = new BooleanSetting("lapis-tracers", false, this.lapis::getValue);
    public final BooleanSetting emeralds = new BooleanSetting("emeralds", false);
    public final BooleanSetting emeraldsTracers = new BooleanSetting("emeralds-tracers", false, this.emeralds::getValue);
    public final BooleanSetting spawners = new BooleanSetting("spawners", false);
    public final BooleanSetting spawnersTracers = new BooleanSetting("spawners-tracers", false, this.spawners::getValue);
    public final BooleanSetting canes = new BooleanSetting("canes", false);
    public final BooleanSetting canesTracers = new BooleanSetting("canes-tracers", false, this.canes::getValue);
    public final BooleanSetting warts = new BooleanSetting("warts", false);
    public final BooleanSetting wartsTracers = new BooleanSetting("warts-tracers", false, this.warts::getValue);

    public XrayModule() {
        super("Xray", false, 0);
    }

    public boolean isSupportedBlock(int blockId) {
        return supportedBlocks.contains(blockId);
    }

    public boolean isBlockEnabled(int blockId) {
        switch (blockId) {
            case 14: {
                return (Boolean)this.gold.getValue();
            }
            case 15: {
                return (Boolean)this.iron.getValue();
            }
            case 16: {
                return (Boolean)this.coal.getValue();
            }
            case 21: {
                return (Boolean)this.lapis.getValue();
            }
            case 52: {
                return (Boolean)this.spawners.getValue();
            }
            case 56: {
                return (Boolean)this.diamonds.getValue();
            }
            case 73:
            case 74: {
                return (Boolean)this.redstone.getValue();
            }
            case 83: {
                return (Boolean)this.canes.getValue();
            }
            case 115: {
                return (Boolean)this.warts.getValue();
            }
            case 129: {
                return (Boolean)this.emeralds.getValue();
            }
        }
        return false;
    }

    private boolean shouldDrawTracer(int blockId) {
        if (!this.isBlockEnabled(blockId)) {
            return false;
        }
        switch (blockId) {
            case 14: {
                return (Boolean)this.goldTracers.getValue();
            }
            case 15: {
                return (Boolean)this.ironTracers.getValue();
            }
            case 16: {
                return (Boolean)this.coalTracers.getValue();
            }
            case 21: {
                return (Boolean)this.lapisTracers.getValue();
            }
            case 52: {
                return (Boolean)this.spawnersTracers.getValue();
            }
            case 56: {
                return (Boolean)this.diamondsTracers.getValue();
            }
            case 73:
            case 74: {
                return (Boolean)this.redstoneTracers.getValue();
            }
            case 83: {
                return (Boolean)this.canesTracers.getValue();
            }
            case 115: {
                return (Boolean)this.wartsTracers.getValue();
            }
            case 129: {
                return (Boolean)this.emeraldsTracers.getValue();
            }
        }
        return false;
    }

    private Color colorFor(int blockId) {
        switch (blockId) {
            case 14: {
                return new Color(0xFFFF55);
            }
            case 15: {
                return new Color(0xFFFFFF);
            }
            case 16: {
                return new Color(0);
            }
            case 21: {
                return new Color(0x5555FF);
            }
            case 52: {
                return new Color(0xFF55FF);
            }
            case 56: {
                return new Color(0x55FFFF);
            }
            case 73:
            case 74: {
                return new Color(0xFF5555);
            }
            case 83: {
                return new Color(0xAAFFAA);
            }
            case 115: {
                return new Color(0xAA0000);
            }
            case 129: {
                return new Color(0x55FF55);
            }
        }
        return new Color(-1);
    }

    private void drawBlock(BlockPos position, int blockId, Vec3 tracerStart) {
        if (XrayModule.minecraft.field_71439_g.func_70011_f((double)position.func_177958_n(), (double)position.func_177956_o(), (double)position.func_177952_p()) <= ((Integer)this.range.getValue()).doubleValue()) {
            Color tint = this.colorFor(blockId);
            RenderUtils.outlineBlockBounds(position, 1.0, tint.getRed(), tint.getGreen(), tint.getBlue(), tint.getAlpha(), 1.5f);
            if (this.shouldDrawTracer(blockId)) {
                RenderUtils.drawTracer(tracerStart, (double)position.func_177958_n() + 0.5, (double)position.func_177956_o() + 0.5, (double)position.func_177952_p() + 0.5, (float)tint.getRed() / 255.0f, (float)tint.getGreen() / 255.0f, (float)tint.getBlue() / 255.0f, 1.0f, 1.5f);
            }
        }
    }

    private boolean isCaveOpening(BlockPos position) {
        if (!XrayModule.minecraft.field_71441_e.func_175668_a(position, false)) {
            return false;
        }
        Block block = XrayModule.minecraft.field_71441_e.func_180495_p(position).func_177230_c();
        return block instanceof BlockMobSpawner || !block.func_149730_j() || !block.func_149688_o().func_76218_k() || block.func_149744_f();
    }

    public boolean passesCaveCheck(BlockPos position) {
        if (!((Boolean)this.cavesOnly.getValue()).booleanValue()) {
            return true;
        }
        LinkedHashSet<Vec3i> offsets = (Integer)this.cavesRadius.getValue() >= 2 ? radiusTwoOffsets : adjacentOffsets;
        for (Vec3i offset : offsets) {
            if (!this.isCaveOpening(position.func_177971_a(offset))) continue;
            return true;
        }
        return false;
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        float partialTicks = ((AccessorMinecraft)XrayModule.minecraft).getTimer().field_74281_c;
        Vec3 direction = XrayModule.minecraft.field_71474_y.field_74320_O == 0 ? new Vec3(0.0, 0.0, 1.0).func_178789_a((float)(-Math.toRadians(MathUtils.lerpFloat(XrayModule.minecraft.func_175606_aa().field_70127_C, XrayModule.minecraft.func_175606_aa().field_70125_A, partialTicks)))).func_178785_b((float)(-Math.toRadians(MathUtils.lerpFloat(XrayModule.minecraft.func_175606_aa().field_70126_B, XrayModule.minecraft.func_175606_aa().field_70177_z, partialTicks)))) : new Vec3(0.0, 0.0, 0.0).func_178789_a((float)(-Math.toRadians(MathUtils.lerpFloat(XrayModule.minecraft.field_71439_g.field_70727_aS, XrayModule.minecraft.field_71439_g.field_70726_aT, partialTicks)))).func_178785_b((float)(-Math.toRadians(MathUtils.lerpFloat(XrayModule.minecraft.field_71439_g.field_71107_bF, XrayModule.minecraft.field_71439_g.field_71109_bG, partialTicks))));
        Vec3 tracerStart = new Vec3(direction.field_72450_a, direction.field_72448_b + (double)minecraft.func_175606_aa().func_70047_e(), direction.field_72449_c);
        RenderUtils.beginOverlay();
        for (BlockPos position : this.discoveredBlocks) {
            if (this.updatedBlocks.contains(position)) {
                this.discoveredBlocks.remove(position);
                continue;
            }
            this.drawTrackedBlock(position, tracerStart, this.discoveredBlocks);
        }
        for (BlockPos position : this.updatedBlocks) {
            this.drawTrackedBlock(position, tracerStart, this.updatedBlocks);
        }
        RenderUtils.endOverlay();
    }

    private void drawTrackedBlock(BlockPos position, Vec3 tracerStart, CopyOnWriteArraySet<BlockPos> source) {
        int blockId = Block.func_149682_b((Block)XrayModule.minecraft.field_71441_e.func_180495_p(position).func_177230_c());
        if (this.isBlockEnabled(blockId)) {
            this.drawBlock(position, blockId, tracerStart);
        } else {
            source.remove(position);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        S23PacketBlockChange packet;
        int blockId;
        if (event.getPacket() instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange packet2 = (S22PacketMultiBlockChange)event.getPacket();
            for (S22PacketMultiBlockChange.BlockUpdateData update : packet2.func_179844_a()) {
                int blockId2 = Block.func_149682_b((Block)update.func_180088_c().func_177230_c());
                if (!this.isBlockEnabled(blockId2)) continue;
                this.updatedBlocks.add(new BlockPos((Vec3i)update.func_180090_a()));
            }
            return;
        }
        if (event.getPacket() instanceof S23PacketBlockChange && this.isBlockEnabled(blockId = Block.func_149682_b((Block)(packet = (S23PacketBlockChange)event.getPacket()).func_180728_a().func_177230_c()))) {
            this.updatedBlocks.add(new BlockPos((Vec3i)packet.func_179827_b()));
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        this.discoveredBlocks.clear();
        this.updatedBlocks.clear();
    }

    @Override
    public void onEnable() {
        ForgeModContainer.forgeLightPipelineEnabled = false;
        RenderGlobal renderer = XrayModule.minecraft.field_71438_f;
        if (renderer != null) {
            renderer.func_72712_a();
        }
    }

    @Override
    public void onDisable() {
        ForgeModContainer.forgeLightPipelineEnabled = true;
        RenderGlobal renderer = XrayModule.minecraft.field_71438_f;
        if (renderer != null) {
            renderer.func_72712_a();
        }
    }

    @Override
    public void onSettingChanged(String name) {
        this.discoveredBlocks.clear();
        this.updatedBlocks.clear();
        if (this.isEnabled()) {
            RenderGlobal renderer = XrayModule.minecraft.field_71438_f;
            if (XrayModule.minecraft.field_71438_f == null) {
                return;
            }
            renderer.func_72712_a();
        }
    }
}
