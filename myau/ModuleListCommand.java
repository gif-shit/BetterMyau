package myau;

import java.util.ArrayList;
import java.util.Arrays;

public class ModuleListCommand extends Command {

    public ModuleListCommand() {
        super(new ArrayList<String>(Arrays.asList("list", "l", "modules", "myau")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (MyauClient.moduleManager.modules.isEmpty()) {
            return;
        }
        ChatUtils.addFormattedMessage(String.format("%sModules:&r", "&7[&bMyau&7] &r"));
        for (Module module : MyauClient.moduleManager.modules.values()) {
            ChatUtils.addFormattedMessage(String.format("%s\u00bb&r %s&r", module.isHidden() ? "&8" : "&7", module.getDisplayText()));
        }
    }
}
