package myau;

import net.minecraft.client.Minecraft;

public class ViewClipModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    public ViewClipModule() {
        super("ViewClip", false, 0);
    }

    @Override
    public void onEnable() {
        this.reloadRenderers();
    }

    @Override
    public void onDisable() {
        this.reloadRenderers();
    }

    private void reloadRenderers() {
        if (minecraft.field_71441_e == null) {
            return;
        }
        minecraft.field_71438_f.func_72712_a();
    }
}
