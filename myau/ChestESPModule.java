package myau;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import myau.BooleanSetting;
import myau.ColorSetting;
import myau.EventHandler;
import myau.MathUtils;
import myau.Module;
import myau.MyauClient;
import myau.PercentSetting;
import myau.RenderUtils;
import myau.RenderWorldEvent;
import myau.TracersModule;
import myau.mixin.AccessorMinecraft;
import myau.mixin.AccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

public class ChestESPModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final ColorSetting color = new ColorSetting("color", 0xFFAA00);
    public final PercentSetting opacity = new PercentSetting("opacity", 100);
    public final BooleanSetting tracers = new BooleanSetting("tracers", false);

    public ChestESPModule() {
        super("ChestESP", false, 0);
    }

    @EventHandler
    public void onRenderWorld(RenderWorldEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        RenderUtils.beginOverlay();
        List chests = ChestESPModule.minecraft.field_71441_e.field_147482_g.stream().filter(ChestESPModule::isChest).collect(Collectors.toList());
        for (TileEntity chest : chests) {
            AxisAlignedBB bounds = new AxisAlignedBB((double)chest.func_174877_v().func_177958_n() + 0.0625, (double)chest.func_174877_v().func_177956_o() + 0.0, (double)chest.func_174877_v().func_177952_p() + 0.0625, (double)chest.func_174877_v().func_177958_n() + 0.9375, (double)chest.func_174877_v().func_177956_o() + 0.875, (double)chest.func_174877_v().func_177952_p() + 0.9375).func_72317_d(-((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosX(), -((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosY(), -((AccessorRenderManager)minecraft.func_175598_ae()).getRenderPosZ());
            Color outline = new Color((Integer)this.color.getValue());
            RenderUtils.outlineBounds(bounds, outline.getRed(), outline.getGreen(), outline.getBlue(), (int)((float)((Integer)this.opacity.getValue()).intValue() / 100.0f * 255.0f), 1.5f);
            if (!((Boolean)this.tracers.getValue()).booleanValue()) continue;
            this.drawTracer(chest, outline);
        }
        RenderUtils.endOverlay();
    }

    private static boolean isChest(TileEntity tile) {
        return tile instanceof TileEntityChest || tile instanceof TileEntityEnderChest;
    }

    private void drawTracer(TileEntity chest, Color outline) {
        Vec3 start = ChestESPModule.minecraft.field_71474_y.field_74320_O == 0 ? new Vec3(0.0, 0.0, 1.0).func_178789_a((float)(-Math.toRadians(MathUtils.lerpFloat(ChestESPModule.minecraft.func_175606_aa().field_70127_C, ChestESPModule.minecraft.func_175606_aa().field_70125_A, ((AccessorMinecraft)ChestESPModule.minecraft).getTimer().field_74281_c)))).func_178785_b((float)(-Math.toRadians(MathUtils.lerpFloat(ChestESPModule.minecraft.func_175606_aa().field_70126_B, ChestESPModule.minecraft.func_175606_aa().field_70177_z, ((AccessorMinecraft)ChestESPModule.minecraft).getTimer().field_74281_c)))) : new Vec3(0.0, 0.0, 0.0).func_178789_a((float)(-Math.toRadians(MathUtils.lerpFloat(ChestESPModule.minecraft.field_71439_g.field_70727_aS, ChestESPModule.minecraft.field_71439_g.field_70726_aT, ((AccessorMinecraft)ChestESPModule.minecraft).getTimer().field_74281_c)))).func_178785_b((float)(-Math.toRadians(MathUtils.lerpFloat(ChestESPModule.minecraft.field_71439_g.field_71107_bF, ChestESPModule.minecraft.field_71439_g.field_71109_bG, ((AccessorMinecraft)ChestESPModule.minecraft).getTimer().field_74281_c))));
        start = new Vec3(start.field_72450_a, start.field_72448_b + (double)minecraft.func_175606_aa().func_70047_e(), start.field_72449_c);
        float tracerOpacity = (float)((Integer)((TracersModule)MyauClient.moduleManager.modules.get(TracersModule.class)).opacity.getValue()).intValue() / 100.0f;
        RenderUtils.drawTracer(start, (double)chest.func_174877_v().func_177958_n() + 0.5, (double)chest.func_174877_v().func_177956_o() + 0.5, (double)chest.func_174877_v().func_177952_p() + 0.5, (float)outline.getRed() / 255.0f, (float)outline.getGreen() / 255.0f, (float)outline.getBlue() / 255.0f, tracerOpacity, 1.5f);
    }
}
