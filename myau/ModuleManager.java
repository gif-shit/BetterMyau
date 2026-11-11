package myau;

import java.util.LinkedHashMap;
import myau.ChatUtils;
import myau.HUDModule;
import myau.InputUtils;
import myau.Module;
import myau.MutableValue;
import myau.SoundUtils;
import net.minecraft.client.Minecraft;

public class ModuleManager {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private boolean toggleSoundPending;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap();

    public Module getModule(String name) {
        return this.modules.values().stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public boolean toggle(Module module) {
        boolean requestedState = !module.isEnabled();
        module.setEnabled(requestedState);
        return module.isEnabled() == requestedState;
    }

    public void update() {
        if (ModuleManager.minecraft.field_71441_e == null || ModuleManager.minecraft.field_71439_g == null || ModuleManager.minecraft.field_71462_r != null) {
            this.resetKeyStates();
            return;
        }
        HUDModule hud = (HUDModule)this.modules.get(HUDModule.class);
        for (Module module : this.modules.values()) {
            int keyCode = module.getKeyCode();
            if (keyCode == 0) {
                module.setPreviousKeyState(null);
                continue;
            }
            boolean keyDown = InputUtils.isKeyDown(keyCode);
            MutableValue<Boolean> previousKeyState = module.getPreviousKeyState();
            if (previousKeyState != null && !((Boolean)previousKeyState.value).booleanValue() && keyDown && this.toggle(module)) {
                if (hud != null && ((Boolean)hud.toggleAlerts.getValue()).booleanValue()) {
                    ChatUtils.addFormattedMessage(String.format("%s%s: %s&r", "&7[&bMyau&7] &r", module.getName(), module.isEnabled() ? "&a&lON" : "&c&lOFF"));
                }
                if (hud != null && ((Boolean)hud.toggleSounds.getValue()).booleanValue()) {
                    this.toggleSoundPending = true;
                }
            }
            module.setPreviousKeyState(new MutableValue<Boolean>(keyDown));
        }
        if (this.toggleSoundPending) {
            this.toggleSoundPending = false;
            SoundUtils.play("random.click");
        }
    }

    private void resetKeyStates() {
        for (Module module : this.modules.values()) {
            module.setPreviousKeyState(null);
        }
        this.toggleSoundPending = false;
    }
}
