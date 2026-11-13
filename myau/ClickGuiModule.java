package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ClickGuiModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

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
