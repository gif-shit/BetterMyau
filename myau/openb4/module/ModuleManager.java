package myau.openb4.module;

import myau.MyauClient;
import myau.Setting;

public final class ModuleManager {

    public static myau.Module get(String name) {
        if ("BedAura".equals(name))
            name = "BedNuker";
        for (myau.Module module : MyauClient.moduleManager.modules.values()) {
            if (module.getName().equalsIgnoreCase(name.replace(" ", "")))
                return module;
        }
        return null;
    }

    public static boolean enabled(String name) {
        myau.Module module = get(name);
        return module != null && module.isEnabled();
    }

    public static Setting<?> setting(myau.Module module, String name) {
        if (module == null)
            return null;
        java.util.List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
        if (settings != null)
            for (Setting<?> setting : settings) {
                if (setting.getName().equalsIgnoreCase(name.replace(' ', '-')))
                    return setting;
            }
        return null;
    }
}
