package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ClickGuiModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    public final BooleanSetting showCombat = new BooleanSetting("show-combat", true);
    public final BooleanSetting showMovement = new BooleanSetting("show-movement", true);
    public final BooleanSetting showPlayer = new BooleanSetting("show-player", true);
    public final BooleanSetting showRender = new BooleanSetting("show-render", true);
    public final BooleanSetting showExploits = new BooleanSetting("show-exploits", true);
    public final BooleanSetting showMisc = new BooleanSetting("show-misc", false);
    public final BooleanSetting showConfigs = new BooleanSetting("show-configs", true);

    BooleanSetting categorySetting(String title) {
        switch (title) {
            case "Combat": return showCombat;
            case "Movement": return showMovement;
            case "Player": return showPlayer;
            case "Render": return showRender;
            case "Exploits": return showExploits;
            case "Misc": return showMisc;
            case "Configs": return showConfigs;
            default: return null;
        }
    }

    static boolean categoryVisible(String title) {
        ClickGuiModule gui = (ClickGuiModule) MyauClient.moduleManager.modules.get(ClickGuiModule.class);
        BooleanSetting setting = gui == null ? null : gui.categorySetting(title);
        return setting == null ? !"Misc".equals(title) : setting.getValue();
    }

    public ClickGuiModule() {
        super("ClickGui", false, 210, true);
        ConfigBinds.load();
    }

    @Override
    public void onEnable() {
        minecraft.func_147108_a((GuiScreen) new ClickGuiScreen());
        this.setEnabled(false);
    }

    @EventHandler
    public void onPreTick(PreTickEvent preTickEvent) {
        ConfigBinds.tick();
    }
}
