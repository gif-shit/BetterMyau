package myau;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import myau.BooleanSetting;
import myau.ClickGuiModule;
import myau.ColorSetting;
import myau.ConfigBinds;
import myau.ConfigProfile;
import myau.FloatSetting;
import myau.HUDModule;
import myau.InputUtils;
import myau.ModeSetting;
import myau.Module;
import myau.MyauClient;
import myau.PercentSetting;
import myau.Setting;
import myau.StringSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ClickGuiScreen
extends GuiScreen {
    private static final int PANEL_WIDTH = 142;
    private static final int MIN_PANEL_WIDTH = 120;
    private static final int PANEL_GAP = 8;
    private static final int HEADER_HEIGHT = 19;
    private static final int MODULE_HEIGHT = 15;
    private static final int SETTING_HEIGHT = 14;
    private static final int BUTTON_WIDTH = 13;
    private static final int TOP = 29;
    private static final int FOOTER = 18;

    private static final int BACKDROP = 0x9B09090D;
    private static final int PANEL = 0xF515151A;
    private static final int PANEL_HEADER = 0xFF212127;
    private static final int BORDER = 0xFF36343C;
    private static final int LABEL = 0xFFE7E5EA;
    private static final int MUTED = 0xFFABA7B1;
    private static final int BINDING = 0xFFFFD885;

    private static final String CONFIGS_TITLE = "Configs";
    private static final List<Panel> PANELS = new ArrayList<>();
    private static final Set<Module> EXPANDED = new HashSet<>();
    private static final Map<Setting<?>, double[]> RANGES = new HashMap<>();
    private static int scroll;
    private int minScroll;
    private final List<Row> rows = new ArrayList<Row>();
    private Panel draggingPanel;
    private int dragOffsetX;
    private int dragOffsetY;
    private Row draggingSlider;
    private boolean closeArmed;
    private Module bindingModule;
    private String bindingConfig;
    private StringSetting editingString;
    private String editingOriginal;

    public ClickGuiScreen() {
        if (PANELS.isEmpty()) {
            PANELS.add(new Panel(ModuleCategory.COMBAT.label));
            PANELS.add(new Panel(ModuleCategory.MOVEMENT.label));
            PANELS.add(new Panel(ModuleCategory.PLAYER.label));
            PANELS.add(new Panel(ModuleCategory.RENDER.label));
            PANELS.add(new Panel(ModuleCategory.EXPLOITS.label));
            PANELS.add(new Panel(CONFIGS_TITLE));
        }
        for (Panel panel : PANELS) {
            panel.modules.clear();
            panel.configs.clear();
        }
        for (Module module : MyauClient.moduleManager.modules.values()) {
            for (Panel panel : PANELS) {
                if (panel.title.equals(ModuleCategory.of(module).label)) panel.modules.add(module);
            }
        }
        for (Panel panel : PANELS) {
            if (panel.title.equals(CONFIGS_TITLE)) panel.configs.addAll(listConfigs());
        }
    }

    private static List<String> listConfigs() {
        List<String> configs = new ArrayList<>();
        File[] files = new File("./config/Myau/").listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (file.isFile() && name.toLowerCase(Locale.ROOT).endsWith(".json")) {
                    configs.add(name.substring(0, name.length() - 5));
                }
            }
        }
        configs.sort(String.CASE_INSENSITIVE_ORDER);
        return configs;
    }

    private int columnCount() {
        int visible = 0;
        for (Panel panel : PANELS) {
            if (!panel.isEmpty()) visible++;
        }
        int available = this.field_146294_l - 16 + PANEL_GAP;
        return Math.max(1, Math.min(visible, available / (MIN_PANEL_WIDTH + PANEL_GAP)));
    }

    private int panelWidth() {
        int columns = columnCount();
        int width = (this.field_146294_l - 16 - (columns - 1) * PANEL_GAP) / columns;
        return Math.min(PANEL_WIDTH, Math.max(32, width));
    }

    private void placePanels() {
        int columns = columnCount();
        int width = panelWidth();
        int left = Math.max(8, (this.field_146294_l - columns * (width + PANEL_GAP) + PANEL_GAP) / 2);
        int column = 0;
        int y = TOP;
        int tallest = 0;
        int bottom = TOP;
        for (Panel panel : PANELS) {
            if (panel.isEmpty()) continue;
            int height = panel.height();
            if (!panel.manuallyPlaced) {
                panel.x = left + column * (width + PANEL_GAP);
                panel.y = y;
            }
            bottom = Math.max(bottom, panel.y + height);
            tallest = Math.max(tallest, height);
            if (++column == columns) {
                column = 0;
                y += tallest + PANEL_GAP;
                tallest = 0;
            }
        }
        this.minScroll = Math.min(0, this.field_146295_m - FOOTER - PANEL_GAP - bottom);
        scroll = Math.max(minScroll, Math.min(0, scroll));
    }

    private int toggleKey() {
        Module module = MyauClient.moduleManager.modules.get(ClickGuiModule.class);
        int n = module == null ? 0 : module.getKeyCode();
        return n > 0 ? n : 0;
    }

    private boolean isBinding() {
        return this.bindingModule != null || this.bindingConfig != null;
    }

    private void startBinding(Module module, String string) {
        boolean bl = module != null ? this.bindingModule == module : string != null && string.equals(this.bindingConfig);
        this.bindingModule = null;
        this.bindingConfig = null;
        if (bl) {
            return;
        }
        this.bindingModule = module;
        this.bindingConfig = string;
    }

    private void applyBinding(int n) {
        if (this.bindingModule != null) {
            this.bindingModule.setKeyCode(n);
        } else if (this.bindingConfig != null) {
            ConfigBinds.set(this.bindingConfig, n);
        }
        this.bindingModule = null;
        this.bindingConfig = null;
    }

    private static String keyLabel(int n) {
        String string = InputUtils.getKeyName(n);
        if (string == null) {
            return "?";
        }
        return (string = string.toUpperCase(Locale.ROOT)).length() > 5 ? string.substring(0, 5) : string;
    }

    public boolean func_73868_f() {
        return false;
    }

    @Override
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        FontRenderer font = this.field_146297_k.field_71466_p;
        int toggle = this.toggleKey();
        if (toggle == 0 || !Keyboard.isKeyDown(toggle)) this.closeArmed = true;
        if (this.draggingSlider != null) {
            if (Mouse.isButtonDown(0)) this.applySlider(this.draggingSlider, mouseX);
            else this.draggingSlider = null;
        }
        if (this.draggingPanel != null) {
            if (Mouse.isButtonDown(0)) {
                int x = mouseX - this.dragOffsetX;
                int y = mouseY - this.dragOffsetY - scroll;
                if (x != this.draggingPanel.x || y != this.draggingPanel.y) {
                    this.draggingPanel.manuallyPlaced = true;
                    this.draggingPanel.x = Math.max(2, Math.min(this.field_146294_l - panelWidth() - 2, x));
                    this.draggingPanel.y = Math.max(TOP, y);
                }
            } else {
                this.draggingPanel = null;
            }
        }

        this.layout();
        Gui.func_73734_a(0, 0, this.field_146294_l, this.field_146295_m, BACKDROP);
        HUDModule hud = (HUDModule) MyauClient.moduleManager.modules.get(HUDModule.class);
        long now = System.currentTimeMillis();
        int accent = 0xFF000000 | (hud == null ? TabGui.DEFAULT_ACCENT : hud.getColor(now).getRGB());
        if (accent == 0xFFFFFFFF && hud != null) accent = 0xFF000000 | hud.tabColor.getValue();
        // Paint each panel and its rows together so a dragged panel covers the one behind it.
        Panel previous = null;
        for (Row row : this.rows) {
            if (row.panel != previous) {
                previous = row.panel;
                Gui.func_73734_a(row.x - 1, row.y - 1, row.x + row.w + 1, row.panel.bottom + 1, BORDER);
                Gui.func_73734_a(row.x, row.y, row.x + row.w, row.panel.bottom, PANEL);
            }
            boolean hovered = mouseX >= row.x && mouseX < row.x + row.w && mouseY >= row.y && mouseY < row.y + row.h;
            switch (row.kind) {
                case 0:
                    drawHeader(font, row, accent);
                    break;
                case 1:
                    this.drawModule(font, row, hovered, mouseX, accent);
                    break;
                case 3:
                    this.drawConfig(font, row, hovered, mouseX, accent);
                    break;
                default:
                    this.drawSetting(font, row, hovered, accent);
                    break;
            }
        }
        drawChrome(font, accent);
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    private void drawChrome(FontRenderer font, int accent) {
        Gui.func_73734_a(0, 0, this.field_146294_l, TOP - 5, PANEL_HEADER);
        Gui.func_73734_a(0, TOP - 6, this.field_146294_l, TOP - 5, BORDER);
        font.func_175063_a("m", 10, 8, accent);
        font.func_175063_a("yau", 10 + font.func_78256_a("m"), 8, LABEL);
        int footerY = this.field_146295_m - FOOTER;
        Gui.func_73734_a(0, footerY, this.field_146294_l, this.field_146295_m, PANEL_HEADER);
        Gui.func_73734_a(0, footerY, this.field_146294_l, footerY + 1, BORDER);
        String help = this.editingString != null ? "Enter to save  /  Esc to revert"
                : this.isBinding() ? "Press a key or middle mouse  /  Del to clear  /  Esc to cancel"
                : "Left click toggle  /  Right click settings  /  B bind  /  H hide";
        font.func_175063_a(fit(font, help, this.field_146294_l - 20), 10, footerY + 6,
                this.isBinding() ? BINDING : MUTED);
    }

    private static String fit(FontRenderer font, String label, int width) {
        if (width <= 0) return "";
        if (font.func_78256_a(label) <= width) return label;
        int ellipsis = font.func_78256_a("...");
        return width > ellipsis ? font.func_78269_a(label, width - ellipsis) + "..." : "";
    }

    private static void drawHeader(FontRenderer font, Row row, int accent) {
        Gui.func_73734_a(row.x, row.y, row.x + row.w, row.y + row.h, PANEL_HEADER);
        Gui.func_73734_a(row.x, row.y, row.x + row.w, row.y + 1, accent);
        Gui.func_73734_a(row.x, row.y + row.h - 1, row.x + row.w, row.y + row.h, BORDER);
        font.func_175063_a(row.panel.title, row.x + 7, row.y + 6, LABEL);
        String mark = row.panel.open ? "-" : "+";
        font.func_175063_a(mark, row.x + row.w - 7 - font.func_78256_a(mark), row.y + 6, MUTED);
    }

    private static void drawButton(FontRenderer font, String label, int x, Row row,
                                   boolean hovered, boolean active, int accent) {
        if (hovered || active) {
            Gui.func_73734_a(x + 1, row.y + 2, x + BUTTON_WIDTH - 1, row.y + row.h - 2,
                    active ? (accent & 0xFFFFFF) | 0x50000000 : 0x30FFFFFF);
        }
        font.func_175063_a(label, x + (BUTTON_WIDTH - font.func_78256_a(label)) / 2.0f,
                row.y + 4, active ? accent : MUTED);
    }

    private static void drawRowState(Row row, boolean hovered, boolean active, int accent) {
        if (active) {
            Gui.func_73734_a(row.x, row.y, row.x + row.w, row.y + row.h, (accent & 0xFFFFFF) | 0x24000000);
            Gui.func_73734_a(row.x, row.y, row.x + 2, row.y + row.h, accent);
        } else if (hovered) {
            Gui.func_73734_a(row.x, row.y, row.x + row.w, row.y + row.h, 0x18FFFFFF);
        }
    }

    private void drawModule(FontRenderer font, Row row, boolean hovered, int mouseX, int accent) {
        Module module = row.module;
        boolean binding = this.bindingModule == module;
        int hideX = row.x + row.w - BUTTON_WIDTH;
        int bindX = hideX - BUTTON_WIDTH;
        int end = bindX - 4;
        drawRowState(row, hovered, module.isEnabled(), accent);
        if (!settingsOf(module).isEmpty()) {
            String marker = EXPANDED.contains(module) ? "v" : ">";
            end -= font.func_78256_a(marker);
            font.func_175063_a(marker, end, row.y + 4, MUTED);
            end -= 4;
        }
        if (binding || module.getKeyCode() != 0) {
            String key = binding ? "[...]" : keyLabel(module.getKeyCode());
            end -= font.func_78256_a(key);
            font.func_175063_a(key, end, row.y + 4, binding ? BINDING : MUTED);
            end -= 5;
        }
        font.func_175063_a(fit(font, module.getName(), end - row.x - 7),
                row.x + 7, row.y + 4, module.isEnabled() ? LABEL : MUTED);
        drawButton(font, "B", bindX, row, hovered && mouseX >= bindX && mouseX < hideX, binding, accent);
        drawButton(font, "H", hideX, row, hovered && mouseX >= hideX, module.isHidden(), accent);
    }

    private void drawConfig(FontRenderer font, Row row, boolean hovered, int mouseX, int accent) {
        boolean active = row.config.equalsIgnoreCase(ConfigProfile.activeProfileName);
        boolean binding = row.config.equals(this.bindingConfig);
        int bindX = row.x + row.w - BUTTON_WIDTH;
        int end = bindX - 4;
        int code = ConfigBinds.get(row.config);
        drawRowState(row, hovered, active, accent);
        if (binding || code != 0) {
            String key = binding ? "[...]" : keyLabel(code);
            end -= font.func_78256_a(key);
            font.func_175063_a(key, end, row.y + 4, binding ? BINDING : MUTED);
            end -= 5;
        }
        font.func_175063_a(fit(font, row.config, end - row.x - 7), row.x + 7, row.y + 4, active ? LABEL : MUTED);
        drawButton(font, "B", bindX, row, hovered && mouseX >= bindX, binding, accent);
    }

    private void drawSetting(FontRenderer font, Row row, boolean hovered, int accent) {
        Setting<?> setting = row.setting;
        int right = row.x + row.w - 6;
        int labelX = row.x + 9;
        int y = row.y + 2;
        Gui.func_73734_a(row.x, row.y, row.x + row.w, row.y + row.h, hovered ? 0xFF27262D : 0xFF1D1C22);
        Gui.func_73734_a(row.x + 3, row.y, row.x + 4, row.y + row.h, BORDER);
        String label = setting.getName().replace('-', ' ');
        if (setting instanceof BooleanSetting) {
            boolean enabled = (Boolean) setting.getValue();
            font.func_175063_a(fit(font, label, right - labelX - 15), labelX, y, LABEL);
            Gui.func_73734_a(right - 11, y + 1, right, y + 7, enabled ? accent : BORDER);
            int knob = enabled ? right - 4 : right - 10;
            Gui.func_73734_a(knob, y + 2, knob + 3, y + 6, LABEL);
            return;
        }

        String value;
        int valueColor = accent;
        if (setting instanceof ModeSetting) {
            value = ((ModeSetting) setting).getModeName();
        } else if (setting instanceof StringSetting) {
            boolean editing = setting == this.editingString;
            value = (String) setting.getValue();
            if (editing && System.currentTimeMillis() / 500L % 2L == 0L) value += "_";
            valueColor = editing ? BINDING : MUTED;
        } else {
            double number = numericValue(row);
            double[] range = row.channel < 0 ? rangeOf(setting) : null;
            double min = range == null ? 0.0 : range[0];
            double max = range == null ? 255.0 : range[1];
            double fraction = max <= min ? 0.0 : Math.max(0.0, Math.min(1.0, (number - min) / (max - min)));
            int trackEnd = row.x + row.w - 4;
            int trackStart = row.x + 4;
            int trackY = row.y + row.h - 2;
            Gui.func_73734_a(trackStart, trackY, trackEnd, trackY + 1, BORDER);
            if (row.channel >= 0) {
                label += " " + "RGB".charAt(row.channel);
                valueColor = row.channel == 0 ? 0xFFE48484 : row.channel == 1 ? 0xFF81D49A : 0xFF8EAAEF;
            }
            Gui.func_73734_a(trackStart, trackY, trackStart + (int) ((trackEnd - trackStart) * fraction), trackY + 1, valueColor);
            value = formatValue(row, number);
        }
        int room = right - labelX;
        value = fit(font, value, room / 2);
        int valueWidth = font.func_78256_a(value);
        font.func_175063_a(fit(font, label, room - valueWidth - 5), labelX, y, LABEL);
        font.func_175063_a(value, right - valueWidth, y, valueColor);
    }

    private void layout() {
        this.placePanels();
        this.rows.clear();
        int width = this.panelWidth();
        for (Panel panel : PANELS) {
            if (panel.isEmpty()) continue;
            int n = panel.y + scroll;
            this.rows.add(new Row(0, panel, null, null, -1, null, panel.x, n, width, HEADER_HEIGHT));
            n += HEADER_HEIGHT;
            if (panel.open) {
                for (String string : panel.configs) {
                    this.rows.add(new Row(3, panel, null, null, -1, string, panel.x, n, width, MODULE_HEIGHT));
                    n += MODULE_HEIGHT;
                }
                for (Module module : panel.modules) {
                    this.rows.add(new Row(1, panel, module, null, -1, null, panel.x, n, width, MODULE_HEIGHT));
                    n += MODULE_HEIGHT;
                    if (!EXPANDED.contains(module)) continue;
                    for (Setting<?> setting : ClickGuiScreen.settingsOf(module)) {
                        if (!setting.isVisible()) continue;
                        int n2 = setting instanceof ColorSetting ? 3 : 1;
                        for (int i = 0; i < n2; ++i) {
                            this.rows.add(new Row(2, panel, module, setting, setting instanceof ColorSetting ? i : -1, null, panel.x, n, width, SETTING_HEIGHT));
                            n += SETTING_HEIGHT;
                        }
                    }
                }
            }
            panel.bottom = n;
        }
    }

    private static List<Setting<?>> settingsOf(Module module) {
        List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
        return settings == null ? Collections.emptyList() : settings;
    }

    private static double[] rangeOf(Setting<?> setting) {
        double[] dArray = RANGES.get(setting);
        if (dArray != null) {
            return dArray;
        }
        double[] dArray2 = new double[]{0.0, 1.0};
        try {
            Field field = setting.getClass().getDeclaredField("min");
            Field field2 = setting.getClass().getDeclaredField("max");
            field.setAccessible(true);
            field2.setAccessible(true);
            dArray2[0] = ((Number)field.get(setting)).doubleValue();
            dArray2[1] = ((Number)field2.get(setting)).doubleValue();
        }
        catch (Exception exception) {
            // empty catch block
        }
        RANGES.put(setting, dArray2);
        return dArray2;
    }

    private static double numericValue(Row row) {
        Setting<?> setting = row.setting;
        if (row.channel >= 0) {
            int n = (Integer)((ColorSetting)setting).getValue();
            return n >> 16 - 8 * row.channel & 0xFF;
        }
        return ((Number)setting.getValue()).doubleValue();
    }

    private static String formatValue(Row row, double d) {
        if (row.channel >= 0) {
            return String.valueOf((int)d);
        }
        if (row.setting instanceof FloatSetting) {
            String string = String.format("%.2f", d);
            string = string.indexOf(46) >= 0 ? string.replaceAll("0+$", "").replaceAll("\\.$", "") : string;
            return string;
        }
        if (row.setting instanceof PercentSetting) {
            return (int)d + "%";
        }
        return String.valueOf((int)d);
    }

    private void applySlider(Row row, int n) {
        Setting<?> setting = row.setting;
        double d = Math.max(0.0, Math.min(1.0, (double)(n - (row.x + 4)) / (double)(row.w - 8)));
        if (row.channel >= 0) {
            int n2 = (int)Math.round(d * 255.0);
            int n3 = 16 - 8 * row.channel;
            int n4 = (Integer)((ColorSetting)setting).getValue();
            n4 = n4 & ~(255 << n3) | n2 << n3;
            ((ColorSetting) setting).setValue(n4 & 0xFFFFFF);
            return;
        }
        double[] dArray = ClickGuiScreen.rangeOf(setting);
        double d2 = dArray[0] + d * (dArray[1] - dArray[0]);
        if (setting instanceof FloatSetting) {
            float f = (float)Math.round(d2 * 100.0) / 100.0f;
            f = Math.max((float)dArray[0], Math.min((float)dArray[1], f));
            ((FloatSetting) setting).setValue(f);
        } else {
            int n5 = (int)Math.round(d2);
            n5 = Math.max((int)dArray[0], Math.min((int)dArray[1], n5));
            if (setting instanceof PercentSetting) ((PercentSetting) setting).setValue(n5);
            else ((IntegerSetting) setting).setValue(n5);
        }
    }

    protected void func_73864_a(int n, int n2, int n3) {
        if (this.isBinding()) {
            if (n3 >= 2) {
                this.applyBinding(n3 - 100);
            } else {
                this.bindingModule = null;
                this.bindingConfig = null;
            }
            return;
        }
        this.editingString = null;
        if (n2 < TOP - 5 || n2 >= this.field_146295_m - FOOTER) return;
        for (int i = this.rows.size() - 1; i >= 0; --i) {
            Row row = this.rows.get(i);
            if (n < row.x || n >= row.x + row.w || n2 < row.y || n2 >= row.y + row.h) continue;
            switch (row.kind) {
                case 0: {
                    if (n3 == 0) {
                        this.draggingPanel = row.panel;
                        this.dragOffsetX = n - row.panel.x;
                        this.dragOffsetY = n2 - (row.panel.y + scroll);
                    } else if (n3 == 1) {
                        row.panel.open = !row.panel.open;
                    }
                    return;
                }
                case 1: {
                    Module module = row.module;
                    int n4 = row.x + row.w - BUTTON_WIDTH;
                    int n5 = n4 - BUTTON_WIDTH;
                    if (n >= n4) {
                        if (n3 == 0) {
                            module.setHidden(!module.isHidden());
                        }
                        return;
                    }
                    if (n >= n5) {
                        if (n3 == 0) {
                            this.startBinding(module, null);
                        } else if (n3 == 1) {
                            module.setKeyCode(0);
                        }
                        return;
                    }
                    if (n3 == 0) {
                        if (!(module instanceof ClickGuiModule)) {
                            MyauClient.moduleManager.toggle(module);
                        }
                    } else if (n3 == 1 && !ClickGuiScreen.settingsOf(module).isEmpty()) {
                        if (!EXPANDED.remove(module)) {
                            EXPANDED.add(module);
                        }
                    } else if (n3 == 2) {
                        this.startBinding(module, null);
                    }
                    return;
                }
                case 3: {
                    int n6 = row.x + row.w - BUTTON_WIDTH;
                    if (n >= n6) {
                        if (n3 == 0) {
                            this.startBinding(null, row.config);
                        } else if (n3 == 1) {
                            ConfigBinds.set(row.config, 0);
                        }
                        return;
                    }
                    if (n3 == 0) {
                        ConfigBinds.loadConfig(row.config);
                    } else if (n3 == 2) {
                        this.startBinding(null, row.config);
                    }
                    return;
                }
            }
            Setting<?> setting = row.setting;
            if (setting instanceof BooleanSetting) {
                if (n3 == 0) {
                    ((BooleanSetting) setting).setValue(!(Boolean) setting.getValue());
                }
            } else if (setting instanceof ModeSetting) {
                int n7 = ((ModeSetting)setting).getModes().length;
                int n8 = (Integer)setting.getValue();
                if (n7 > 0 && (n3 == 0 || n3 == 1)) {
                    int n9 = n3 == 0 ? (n8 + 1) % n7 : (n8 - 1 + n7) % n7;
                    ((ModeSetting) setting).setValue(n9);
                }
            } else if (setting instanceof StringSetting) {
                if (n3 == 0) {
                    this.editingString = (StringSetting)setting;
                    this.editingOriginal = (String)setting.getValue();
                }
            } else if (n3 == 0) {
                this.draggingSlider = row;
                this.applySlider(row, n);
            }
            return;
        }
    }

    protected void func_146286_b(int n, int n2, int n3) {
        this.draggingSlider = null;
        this.draggingPanel = null;
    }

    public void func_146274_d() throws IOException {
        super.func_146274_d();
        int n = Mouse.getEventDWheel();
        if (n != 0) scroll = Math.max(this.minScroll, Math.min(0, scroll + (n > 0 ? 22 : -22)));
    }

    protected void func_73869_a(char c, int n) throws IOException {
        if (this.editingString != null) {
            StringSetting stringSetting = this.editingString;
            String string = (String)stringSetting.getValue();
            if (n == 28 || n == 156) {
                this.editingString = null;
            } else if (n == 1) {
                stringSetting.setValue(this.editingOriginal);
                this.editingString = null;
            } else if (n == 14) {
                if (!string.isEmpty()) {
                    stringSetting.setValue(string.substring(0, string.length() - 1));
                }
            } else if (c >= ' ' && c != '\u007f' && string.length() < 48) {
                stringSetting.setValue(string + c);
            }
            return;
        }
        if (this.isBinding()) {
            if (n == 1) {
                this.bindingModule = null;
                this.bindingConfig = null;
            } else if (n == 211 || n == 14) {
                this.applyBinding(0);
            } else if (n > 0) {
                this.applyBinding(n);
            }
            return;
        }
        if (n > 0 && n == this.toggleKey()) {
            if (this.closeArmed) {
                this.field_146297_k.func_147108_a(null);
            }
            return;
        }
        super.func_73869_a(c, n);
    }

    private static class Row {
        final int kind;
        final Panel panel;
        final Module module;
        final Setting<?> setting;
        final int channel;
        final String config;
        final int x;
        final int y;
        final int w;
        final int h;

        Row(int n, Panel panel, Module module, Setting<?> setting, int n2, String string, int n3, int n4, int n5, int n6) {
            this.kind = n;
            this.panel = panel;
            this.module = module;
            this.setting = setting;
            this.channel = n2;
            this.config = string;
            this.x = n3;
            this.y = n4;
            this.w = n5;
            this.h = n6;
        }
    }

    private static class Panel {
        final String title;
        final List<Module> modules = new ArrayList<Module>();
        final List<String> configs = new ArrayList<String>();
        int x;
        int y;
        int bottom;
        boolean open = true;
        boolean manuallyPlaced;

        int height() {
            int height = HEADER_HEIGHT;
            if (!open) return height;
            height += (configs.size() + modules.size()) * MODULE_HEIGHT;
            for (Module module : modules) {
                if (!EXPANDED.contains(module)) continue;
                for (Setting<?> setting : settingsOf(module)) {
                    if (setting.isVisible()) height += SETTING_HEIGHT * (setting instanceof ColorSetting ? 3 : 1);
                }
            }
            return height;
        }

        Panel(String string) {
            this.title = string;
        }

        boolean isEmpty() {
            return this.modules.isEmpty() && this.configs.isEmpty();
        }
    }
}
