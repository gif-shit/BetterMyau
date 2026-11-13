package myau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class HideCommand extends Command {

    public HideCommand() {
        super(new ArrayList<String>(Arrays.asList("hide", "h")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (arguments.size() < 2) {
            ChatUtils.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r>&r", "&7[&bMyau&7] &r", arguments.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        String moduleName = arguments.get(1);
        if (moduleName.equals("*")) {
            for (Module module : MyauClient.moduleManager.modules.values()) {
                module.setHidden(true);
            }
            ChatUtils.addFormattedMessage(String.format("%sAll modules have been hidden in HUD&r", "&7[&bMyau&7] &r"));
            return;
        }
        Module module = MyauClient.moduleManager.getModule(moduleName);
        if (module == null) {
            ChatUtils.addFormattedMessage(String.format("%sModule &o%s&r not found&r", "&7[&bMyau&7] &r", moduleName));
            return;
        }
        if (module.isHidden()) {
            ChatUtils.addFormattedMessage(String.format("%s&o%s&r is already hidden in HUD&r", "&7[&bMyau&7] &r", module.getName()));
            return;
        }
        module.setHidden(true);
        ChatUtils.addFormattedMessage(String.format("%s&o%s&r has been hidden in HUD&r", "&7[&bMyau&7] &r", module.getName()));
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        if (arguments.size() != 2) {
            return new ArrayList<String>();
        }
        return MyauClient.moduleManager.modules.values().stream().filter(module -> !module.isHidden()).map(module -> module.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
    }
}
