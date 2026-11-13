package myau;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;
import myau.mixin.AccessorRenderManager;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class BedESPModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final CopyOnWriteArraySet<BlockPos> bedHeads = new CopyOnWriteArraySet();
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[] { "DEFAULT", "FULL" });
    public final ModeSetting color = new ModeSetting("color", 0, new String[] { "CUSTOM", "HUD" });
    public final ColorSetting customColor = new ColorSetting("custom-color", 0xFF5555, this::showCustomColor);
    public final PercentSetting opacity = new PercentSetting("opacity", 25);
    public final BooleanSetting outline = new BooleanSetting("outline", false);
    public final BooleanSetting obsidian = new BooleanSetting("obsidian", true);

    public BedESPModule() {
        super("BedESP", false, 0);
    }

    public double getBedHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    private Color bedColor() {
        switch(this.color.getValue()) {
            case 0:
                {
                    return new Color(this.customColor.getValue());
                }
            case 1:
                {
                    return ((HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class)).getColor(System.currentTimeMillis());
                }
        }
        return new Color(-1);
    }

    private boolean showCustomColor() {
        int selection = this.color.getValue();
        return selection == 0;
    }

    private void drawObsidianBox(AxisAlignedBB bounds) {
        if (this.outline.getValue()) {
            RenderUtils.outlineBounds(bounds, 170, 0, 170, 255, 1.5f);
        }
        RenderUtils.fillBounds(bounds, 170, 0, 170, (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
    }

    private void drawObsidianBlock(BlockPos position) {
        if (this.outline.getValue()) {
            RenderUtils.outlineBlockBounds(position, 1.0, 170, 0, 170, 255, 1.5f);
        }
        RenderUtils.fillBlockBounds(position, 1.0, 170, 0, 170, (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
    }

    private AxisAlignedBB pairedBounds(BlockPos first, BlockPos second, double height) {
        return new AxisAlignedBB((double) Math.min(first.func_177958_n(), second.func_177958_n()), (double) first.func_177956_o(), (double) Math.min(first.func_177952_p(), second.func_177952_p()), Math.max((double) first.func_177958_n() + 1.0, (double) second.func_177958_n() + 1.0), (double) first.func_177956_o() + height, Math.max((double) first.func_177952_p() + 1.0, (double) second.func_177952_p() + 1.0)).func_72317_d(-((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosX(), -((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosY(), -((AccessorRenderManager) minecraft.func_175598_ae()).getRenderPosZ());
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        RenderUtils.beginOverlay();
        for (BlockPos head : this.bedHeads) {
            IBlockState headState = minecraft.field_71441_e.func_180495_p(head);
            if (!(headState.func_177230_c() instanceof BlockBed)
                    || headState.func_177229_b((IProperty) BlockBed.field_176472_a) != BlockBed.EnumPartType.HEAD) {
                this.bedHeads.remove(head);
                continue;
            }
            BlockPos foot = head.func_177972_a(((EnumFacing) headState.func_177229_b((IProperty) BlockBed.field_176387_N)).func_176734_d());
            IBlockState footState = minecraft.field_71441_e.func_180495_p(foot);
            if (!(footState.func_177230_c() instanceof BlockBed)
                    || footState.func_177229_b((IProperty) BlockBed.field_176472_a) != BlockBed.EnumPartType.FOOT)
                continue;
            if (this.obsidian.getValue()) {
                for (EnumFacing side : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                    BlockPos nearHead = head.func_177972_a(side);
                    BlockPos nearFoot = foot.func_177972_a(side);
                    boolean headCovered = minecraft.field_71441_e.func_180495_p(nearHead).func_177230_c() instanceof BlockObsidian;
                    boolean footCovered = minecraft.field_71441_e.func_180495_p(nearFoot).func_177230_c() instanceof BlockObsidian;
                    if (headCovered && footCovered) {
                        this.drawObsidianBox(this.pairedBounds(nearHead, nearFoot, 1.0));
                        continue;
                    }
                    if (headCovered) {
                        this.drawObsidianBlock(nearHead);
                        continue;
                    }
                    if (!footCovered)
                        continue;
                    this.drawObsidianBlock(nearFoot);
                }
            }
            AxisAlignedBB bounds = this.pairedBounds(head, foot, this.getBedHeight());
            Color selected = this.bedColor();
            if (this.outline.getValue()) {
                RenderUtils.outlineBounds(bounds, selected.getRed(), selected.getGreen(), selected.getBlue(), 255, 1.5f);
            }
            RenderUtils.fillBounds(bounds, selected.getRed(), selected.getGreen(), selected.getBlue(), (int) ((float) this.opacity.getValue() / 100.0f * 255.0f));
        }
        RenderUtils.endOverlay();
    }

    @Override
    public void onEnable() {
        if (minecraft.field_71438_f == null) {
            return;
        }
        minecraft.field_71438_f.func_72712_a();
    }
}
