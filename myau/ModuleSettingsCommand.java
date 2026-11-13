package myau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModuleSettingsCommand extends Command {

    public ModuleSettingsCommand() {
        super(new ArrayList<String>(MyauClient.moduleManager.modules.values().stream().map(Module::getName).collect(Collectors.toList())));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        Module module = MyauClient.moduleManager.getModule(arguments.get(0));
        if (arguments.size() >= 2) {
            this.handleSetting(module, arguments);
            return;
        }
        List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
        if (settings == null) {
            this.showNoProperties(module);
            return;
        }
        List<Setting<?>> visibleSettings = settings.stream().filter(Setting::isVisible).collect(Collectors.toList());
        if (visibleSettings.isEmpty()) {
            this.showNoProperties(module);
            return;
        }
        ChatUtils.addFormattedMessage(String.format("%s%s:&r", "&7[&bMyau&7] &r", module.getDisplayText()));
        for (Setting setting : visibleSettings) {
            ChatUtils.addFormattedMessage(String.format("&7\u00bb&r %s: %s&r", setting.getName(), setting.getDisplayValue()));
        }
    }

    private void handleSetting(Module module, ArrayList<String> arguments) {
        Setting<?> setting = MyauClient.settingManager.findSetting(module, arguments.get(1));
        if (setting == null) {
            ChatUtils.addFormattedMessage(String.format("%s%s has no property &o%s&r", "&7[&bMyau&7] &r", module.getName(), arguments.get(1)));
            return;
        }
        if (!setting.isVisible()) {
            ChatUtils.addFormattedMessage(String.format("%s%s: &o%s&r is not available&r", "&7[&bMyau&7] &r", module.getName(), setting.getName()));
            return;
        }
        if (arguments.size() < 3 && !(setting instanceof BooleanSetting)) {
            ChatUtils.addFormattedMessage(String.format("%s%s: &o%s&r is set to %s&r (%s)&r", "&7[&bMyau&7] &r", module.getName(), setting.getName(), setting.getDisplayValue(), setting.getUsage()));
            return;
        }
        String input = arguments.size() < 3 ? null : String.join((CharSequence) " ", arguments.subList(2, arguments.size()));
        try {
            if (setting.parseValue(input)) {
                ChatUtils.addFormattedMessage(String.format("%s%s: &o%s&r has been set to %s&r", "&7[&bMyau&7] &r", module.getName(), setting.getName(), setting.getDisplayValue()));
                return;
            }
        } catch (Exception exception) {
        }
        ChatUtils.addFormattedMessage(String.format("%sInvalid value for property &o%s&r (%s)&r", "&7[&bMyau&7] &r", setting.getName(), setting.getUsage()));
    }

    private void showNoProperties(Module module) {
        ChatUtils.addFormattedMessage(String.format("%s%s has no properties&r", "&7[&bMyau&7] &r", module.getDisplayText()));
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        Setting<?> setting2;
        Module module = MyauClient.moduleManager.getModule(arguments.get(0));
        if (module == null) {
            return new ArrayList<String>();
        }
        if (arguments.size() == 2) {
            List<Setting<?>> settings = MyauClient.settingManager.settingsByModule.get(module.getClass());
            if (settings == null) {
                return new ArrayList<String>();
            }
            return settings.stream().filter(Setting::isVisible).map(setting -> setting.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
        }
        if (arguments.size() == 3
                && (setting2 = MyauClient.settingManager.findSetting(module, arguments.get(1))) instanceof ModeSetting
                && setting2.isVisible()) {
            return Arrays.stream(((ModeSetting) setting2).getModes()).map(mode -> mode.toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<String>();
    }
}
