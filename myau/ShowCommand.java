package myau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.ChatUtils;
import myau.Command;
import myau.Module;
import myau.MyauClient;

public class ShowCommand
extends Command {
    public ShowCommand() {
        super(new ArrayList<String>(Arrays.asList("show", "s", "unhide")));
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
                module.setHidden(false);
            }
            ChatUtils.addFormattedMessage(String.format("%sAll modules are no longer hidden in HUD&r", "&7[&bMyau&7] &r"));
            return;
        }
        Module module = MyauClient.moduleManager.getModule(moduleName);
        if (module == null) {
            ChatUtils.addFormattedMessage(String.format("%sModule &o%s&r not found&r", "&7[&bMyau&7] &r", moduleName));
            return;
        }
        if (!module.isHidden()) {
            ChatUtils.addFormattedMessage(String.format("%s&o%s&r is not hidden in HUD&r", "&7[&bMyau&7] &r", module.getName()));
            return;
        }
        module.setHidden(false);
        ChatUtils.addFormattedMessage(String.format("%s&o%s&r is no longer hidden in HUD&r", "&7[&bMyau&7] &r", module.getName()));
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        if (arguments.size() != 2) {
            return new ArrayList<String>();
        }
        return MyauClient.moduleManager.modules.values().stream().filter(Module::isHidden).map(module -> module.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
    }
}
