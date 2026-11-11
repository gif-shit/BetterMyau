package myau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.ChatUtils;
import myau.Command;
import myau.Module;
import myau.MyauClient;

public class ToggleCommand
extends Command {
    public ToggleCommand() {
        super(new ArrayList<String>(Arrays.asList("toggle", "t")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (arguments.size() < 2) {
            ChatUtils.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r>&r", "&7[&bMyau&7] &r", arguments.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        Module module = MyauClient.moduleManager.getModule(arguments.get(1));
        if (module == null) {
            ChatUtils.addFormattedMessage(String.format("%sModule not found (&o%s&r)&r", "&7[&bMyau&7] &r", arguments.get(1)));
            return;
        }
        boolean shouldToggle = true;
        if (arguments.size() >= 3) {
            String requestedState = arguments.get(2);
            if (requestedState.equalsIgnoreCase("true") || requestedState.equalsIgnoreCase("on") || requestedState.equalsIgnoreCase("1")) {
                shouldToggle = !module.isEnabled();
            } else if (requestedState.equalsIgnoreCase("false") || requestedState.equalsIgnoreCase("off") || requestedState.equalsIgnoreCase("0")) {
                shouldToggle = module.isEnabled();
            }
        }
        if (shouldToggle && MyauClient.moduleManager.toggle(module)) {
            ChatUtils.addFormattedMessage(String.format("%s%s: %s&r", "&7[&bMyau&7] &r", module.getName(), module.isEnabled() ? "&a&lON" : "&c&lOFF"));
        }
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        if (arguments.size() != 2) {
            return new ArrayList<String>();
        }
        return MyauClient.moduleManager.modules.values().stream().map(module -> module.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
    }
}
