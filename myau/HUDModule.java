package myau;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.mixin.AccessorGuiChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

public class HUDModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    public final ModeSetting color = new ModeSetting("color", 3, new String[] { "RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123" });
    public final FloatSetting colorSpeed = new FloatSetting("color-speed", 1.0f, 0.5f, 1.5f);
    public final PercentSetting colorSaturation = new PercentSetting("color-saturation", 50);
    public final PercentSetting colorBrightness = new PercentSetting("color-brightness", 100);
    public final ColorSetting customColor1 = new ColorSetting("custom-color-1", 0xFFFFFF, this::usesFirstCustomColor);
    public final ColorSetting customColor2 = new ColorSetting("custom-color-2", 0xFFFFFF, this::usesSecondCustomColor);
    public final ColorSetting customColor3 = new ColorSetting("custom-color-3", 0xFFFFFF, this::usesThirdCustomColor);
    public final ModeSetting positionX = new ModeSetting("position-x", 0, new String[] { "LEFT", "RIGHT" });
    public final ModeSetting positionY = new ModeSetting("position-y", 0, new String[] { "TOP", "BOTTOM" });
    public final IntegerSetting offsetX = new IntegerSetting("offset-x", 2, 0, 255);
    public final IntegerSetting offsetY = new IntegerSetting("offset-y", 2, 0, 255);
    public final FloatSetting scale = new FloatSetting("scale", 1.0f, 0.5f, 1.5f);
    public final PercentSetting background = new PercentSetting("background", 25);
    public final BooleanSetting bar = new BooleanSetting("bar", true);
    public final BooleanSetting wrapBar = new BooleanSetting("wrap-bar", false, this.bar::getValue);
    public final BooleanSetting shadow = new BooleanSetting("shadow", true);
    public final BooleanSetting suffixes = new BooleanSetting("suffixes", true);
    public final BooleanSetting lowerCase = new BooleanSetting("lower-case", false);
    public final BooleanSetting chatOutline = new BooleanSetting("chat-outline", true);
    public final BooleanSetting blinkTimer = new BooleanSetting("blink-timer", true);
    public final BooleanSetting toggleSounds = new BooleanSetting("toggle-sounds", true);
    public final BooleanSetting toggleAlerts = new BooleanSetting("toggle-alerts", true);
    public final BooleanSetting watermark = new BooleanSetting("watermark", true);
    public final BooleanSetting tabGui = new BooleanSetting("tab-gui", true);
    public final ColorSetting tabColor = new ColorSetting("tab-color", TabGui.DEFAULT_ACCENT, () -> this.watermark.getValue() || this.tabGui.getValue());
    private final TabGui tabs = new TabGui();

    @EventHandler
    public void onPreTick(PreTickEvent event) {
        this.tabs.tick(minecraft, this.tabGui.getValue());
    }

    @EventHandler(priority = -100)
    public void onRenderWidgets(RenderOverlayEvent event) {
        if (minecraft.field_71474_y.field_74330_P || minecraft.field_71462_r instanceof ClickGuiScreen)
            return;
        boolean showWatermark = this.watermark.getValue();
        boolean showTabs = this.tabGui.getValue();
        if (!showWatermark && !showTabs)
            return;
        GlStateManager.func_179097_i();
        try {
            this.tabs.draw(minecraft, showWatermark, showTabs, this.tabColor.getValue());
        } finally {
            GlStateManager.func_179126_j();
        }
    }

    public HUDModule() {
        super("HUD", true, 0, true);
    }

    private String displayName(Module module) {
        String name = module.getName();
        return this.lowerCase.getValue() ? name.toLowerCase(Locale.ROOT) : name;
    }

    private String[] displaySuffixes(Module module) {
        String[] values = module.getHudInfo();
        boolean lowercase = this.lowerCase.getValue();
        if (!lowercase) {
            return values;
        }
        for (int index = 0; index < values.length; ++index) {
            values[index] = values[index].toLowerCase();
        }
        return values;
    }

    private int displayWidth(String name, String[] values) {
        int width = minecraft.field_71466_p.func_78256_a(name);
        boolean includeSuffixes = this.suffixes.getValue();
        if (includeSuffixes) {
            for (String suffix : values) {
                width += 3 + minecraft.field_71466_p.func_78256_a(suffix);
            }
        }
        return width;
    }

    private int moduleWidth(Module module) {
        return this.displayWidth(this.displayName(module), this.displaySuffixes(module));
    }

    private float colorPhase(long time, long row) {
        long period = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5f, this.colorSpeed.getValue().floatValue()), 1.5f), 3.0));
        return 1.0f - (float) (Math.abs(time - row * 300L) % period) / (float) period;
    }

    public Color getColor(long time) {
        return this.getColorForRow(time, 0L);
    }

    public Color getColorForRow(long time, long row) {
        Color base = Color.white;
        int mode = this.color.getValue();
        switch(mode) {
            case 0:
                {
                    base = ColorUtils.fromHSB(this.colorPhase(time, row), 1.0f, 1.0f);
                    break;
                }
            case 1:
                {
                    base = ColorUtils.fromHSB(this.colorPhase(time / 3L, 0L), 1.0f, 1.0f);
                    break;
                }
            case 2:
                {
                    float hue = this.colorPhase(time, row);
                    if (hue % 1.0f < 0.5f) {
                        hue = 1.0f - hue % 1.0f;
                    }
                    base = ColorUtils.fromHSB(hue, 1.0f, 1.0f);
                    break;
                }
            case 3:
                {
                    base = new Color(this.customColor1.getValue());
                    break;
                }
            case 4:
                {
                    double phase = this.colorPhase(time, row);
                    float blend = (float) (2.0 * Math.abs(phase - Math.floor(phase + 0.5)));
                    base = ColorUtils.interpolate(blend, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue()));
                    break;
                }
            case 5:
                {
                    double threeColorPhase = this.colorPhase(time, row);
                    float threeColorBlend = (float) (2.0 * Math.abs(threeColorPhase - Math.floor(threeColorPhase + 0.5)));
                    base = threeColorBlend <= 0.5f ? ColorUtils.interpolate(threeColorBlend * 2.0f, new Color(this.customColor1.getValue()), new Color(this.customColor2.getValue())) : ColorUtils.interpolate((threeColorBlend - 0.5f) * 2.0f, new Color(this.customColor2.getValue()), new Color(this.customColor3.getValue()));
                }
        }
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0f), hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0f));
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        long elapsed;
        OutgoingBufferOwner mode;
        int horizontalPosition;
        boolean outline = this.chatOutline.getValue();
        if (outline && minecraft.field_71462_r instanceof GuiChat) {
            String input = ((AccessorGuiChat) minecraft.field_71462_r).getInputField().func_146179_b().trim();
            if (MyauClient.commandManager != null && MyauClient.commandManager.isClientCommand(input)) {
                RenderUtils.beginOverlay();
                RenderUtils.drawBorderedRect(2.0f, minecraft.field_71462_r.field_146295_m - 14, minecraft.field_71462_r.field_146294_l - 2, minecraft.field_71462_r.field_146295_m - 2, 1.5f, 0, this.getColor(System.currentTimeMillis()).getRGB());
                RenderUtils.endOverlay();
            }
        }
        if (!this.isEnabled()) {
            return;
        }
        if (minecraft.field_71474_y.field_74330_P) {
            return;
        }
        List<Module> modules = MyauClient.moduleManager.modules.values().stream().filter(HUDModule::shouldDisplay).sorted(Comparator.comparingInt(this::moduleWidth).reversed()).collect(Collectors.toList());
        float rowHeight = (float) minecraft.field_71466_p.field_78288_b - 1.0f;
        boolean showBar = this.bar.getValue();
        boolean wrapped = showBar && this.wrapBar.getValue();
        boolean shadowed = this.shadow.getValue();
        float hudScale = this.scale.getValue();
        float barSpace = showBar ? (shadowed ? 2.0f : 1.0f) : 0.0f;
        float x = this.offsetX.getValue() + (1.0f + barSpace) * hudScale;
        float y = this.offsetY.getValue() + (wrapped ? 2.0f : 1.0f) * hudScale;
        horizontalPosition = this.positionX.getValue();
        int verticalPosition = this.positionY.getValue();
        boolean rightAligned = horizontalPosition == 1;
        boolean fromTop = verticalPosition == 0;
        boolean showWatermark = this.watermark.getValue();
        boolean showTabs = this.tabGui.getValue();
        int topLeftWidth = showTabs ? TabGui.WIDTH : minecraft.field_71466_p.func_78256_a("myau") * 2;
        if ((showWatermark || showTabs) && horizontalPosition == 0 && verticalPosition == 0 && x < TabGui.LEFT + topLeftWidth) {
            y = Math.max(y, this.tabs.bottom(showWatermark, showTabs) + 4.0f);
        }
        if (horizontalPosition == 1) {
            x = (float) new ScaledResolution(minecraft).func_78326_a() - x;
        }
        if (verticalPosition == 1) {
            y = (float) new ScaledResolution(minecraft).func_78328_b() - y - rowHeight * this.scale.getValue();
        }
        GlStateManager.func_179094_E();
        GlStateManager.func_179152_a((float) this.scale.getValue(), (float) this.scale.getValue(), (float) 0.0f);
        long now = System.currentTimeMillis();
        long row = 0L;
        float previousWidth = 0.0f;
        int backgroundColor = Math.round(this.background.getValue() / 100.0f * 255.0f) << 24;
        for (Module module : modules) {
            String name = this.displayName(module);
            String[] values = this.displaySuffixes(module);
            int width = this.displayWidth(name, values);
            float rowWidth = width - (shadowed ? 0.0f : 1.0f);
            int rgb = this.getColorForRow(now, row).getRGB();
            boolean first = row == 0L;
            boolean last = row == modules.size() - 1L;
            float left = x / hudScale - 1.0f - (rightAligned ? rowWidth : 0.0f);
            float right = x / hudScale + 1.0f + (rightAligned ? 0.0f : rowWidth);
            float top = y / hudScale - ((fromTop ? first : shadowed) ? 1.0f : 0.0f);
            float bottom = y / hudScale + rowHeight + ((fromTop ? shadowed : first) ? 1.0f : 0.0f);
            RenderUtils.beginOverlay();
            RenderUtils.fillRect(left, top, right, bottom, backgroundColor);
            if (wrapped) {
                this.drawWrappedBar(left, top, right, bottom, previousWidth, first, last, rgb);
            } else if (showBar) {
                if (this.shadow.getValue()) {
                    RenderUtils.fillRect(x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -3.0f : 1.0f), y / this.scale.getValue() - (this.positionY.getValue() == 0 ? (row == 0L ? 1.0f : 0.0f) : 1.0f), x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0f : 2.0f), y / this.scale.getValue() + rowHeight + (this.positionY.getValue() == 0 || row == 0L ? 1.0f : 0.0f), rgb);
                    RenderUtils.fillRect(x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0f : 2.0f), y / this.scale.getValue() - (this.positionY.getValue() == 0 ? (row == 0L ? 1.0f : 0.0f) : 1.0f), x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -1.0f : 3.0f), y / this.scale.getValue() + rowHeight + (this.positionY.getValue() == 0 || row == 0L ? 1.0f : 0.0f), (rgb & 0xFCFCFC) >> 2 | rgb & 0xFF000000);
                } else {
                    RenderUtils.fillRect(x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -2.0f : 1.0f), y / this.scale.getValue() - (this.positionY.getValue() == 0 && row == 0L ? 1.0f : 0.0f), x / this.scale.getValue() + (this.positionX.getValue() == 0 ? -1.0f : 2.0f), y / this.scale.getValue() + rowHeight + (this.positionY.getValue() != 0 && row == 0L ? 1.0f : 0.0f), rgb);
                }
            }
            RenderUtils.endOverlay();
            GlStateManager.func_179097_i();
            this.drawLabel(name, x, y, rowWidth, false, 0.0f, rgb);
            if (this.suffixes.getValue() && values.length > 0) {
                float suffixOffset = (float) minecraft.field_71466_p.func_78256_a(name) + 3.0f;
                for (String suffix : values) {
                    this.drawLabel(suffix, x, y, rowWidth, true, suffixOffset, ChatColor.GRAY.getRgb());
                    suffixOffset += (float) minecraft.field_71466_p.func_78256_a(suffix) + (this.shadow.getValue() ? 3.0f : 2.0f);
                }
            }
            y += (rowHeight + (shadowed ? 1.0f : 0.0f)) * hudScale * (fromTop ? 1.0f : -1.0f);
            previousWidth = rowWidth;
            ++row;
        }
        if (this.blinkTimer.getValue()
                && (mode = MyauClient.outgoingPacketBuffer.owner()) != OutgoingBufferOwner.NONE
                && mode != OutgoingBufferOwner.AUTO_BLOCK
                && (elapsed = MyauClient.outgoingPacketBuffer.movementPacketCount()) > 0L) {
            GlStateManager.func_179147_l();
            GlStateManager.func_179112_b((int) 770, (int) 771);
            minecraft.field_71466_p.func_175065_a(String.valueOf(elapsed), (float) new ScaledResolution(minecraft).func_78326_a() / 2.0f / this.scale.getValue() - (float) minecraft.field_71466_p.func_78256_a(String.valueOf(elapsed)) / 2.0f, (float) new ScaledResolution(minecraft).func_78328_b() / 5.0f * 3.0f / this.scale.getValue(), this.getColorForRow(now, row).getRGB() & 0xFFFFFF | 0xBF000000, this.shadow.getValue().booleanValue());
            GlStateManager.func_179084_k();
        }
        GlStateManager.func_179126_j();
        GlStateManager.func_179121_F();
    }

    private void drawWrappedBar(float left, float top, float right, float bottom, float previousWidth, boolean first, boolean last, int color) {
        boolean fromTop = this.positionY.getValue() == 0;
        RenderUtils.fillRect(left - 1.0f, top, left, bottom, color);
        RenderUtils.fillRect(right, top, right + 1.0f, bottom, color);
        if (fromTop ? first : last) {
            RenderUtils.fillRect(left - 1.0f, top - 1.0f, right + 1.0f, top, color);
        }
        if (fromTop ? last : first) {
            RenderUtils.fillRect(left - 1.0f, bottom, right + 1.0f, bottom + 1.0f, color);
        }
        if (first)
            return;
        float widthChange = previousWidth - (right - left - 2.0f);
        if (Math.abs(widthChange) < 0.01f)
            return;
        // Paint only the exposed ledge at the join between two different row widths.
        float seam = fromTop ? top : bottom;
        float stepTop = fromTop == (widthChange > 0.0f) ? seam : seam - 1.0f;
        if (this.positionX.getValue() == 1) {
            float previousLeft = left - widthChange;
            RenderUtils.fillRect(Math.min(left, previousLeft) - 1.0f, stepTop, Math.max(left, previousLeft), stepTop + 1.0f, color);
        } else {
            float previousRight = right + widthChange;
            RenderUtils.fillRect(Math.min(right, previousRight), stepTop, Math.max(right, previousRight) + 1.0f, stepTop + 1.0f, color);
        }
    }

    private void drawLabel(String text, float x, float y, float rowWidth, boolean isSuffix, float suffixOffset, int rgb) {
        if (this.shadow.getValue()) {
            minecraft.field_71466_p.func_175063_a(text, this.labelX(x, rowWidth, isSuffix, suffixOffset), y / this.scale.getValue(), rgb);
        } else {
            minecraft.field_71466_p.func_175065_a(text, this.labelX(x, rowWidth, isSuffix, suffixOffset), y / this.scale.getValue() + (this.positionY.getValue() == 1 ? 1.0f : 0.0f), rgb, false);
        }
    }

    private float labelX(float x, float rowWidth, boolean isSuffix, float suffixOffset) {
        float position = x / this.scale.getValue() - (this.positionX.getValue() == 1 ? rowWidth : 0.0f);
        return isSuffix ? position + suffixOffset : position;
    }

    private static boolean shouldDisplay(Module module) {
        boolean enabled = module.isEnabled();
        return enabled && !module.isHidden();
    }

    private boolean usesFirstCustomColor() {
        int mode = this.color.getValue();
        return mode == 3 || this.color.getValue() == 4 || this.color.getValue() == 5;
    }

    private boolean usesSecondCustomColor() {
        int mode = this.color.getValue();
        return mode == 4 || this.color.getValue() == 5;
    }

    private boolean usesThirdCustomColor() {
        int mode = this.color.getValue();
        return mode == 5;
    }
}
