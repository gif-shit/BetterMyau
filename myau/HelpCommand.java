package myau;

import java.util.ArrayList;
import java.util.Arrays;
import myau.ChatUtils;
import myau.Command;
import myau.ModuleSettingsCommand;
import myau.MyauClient;

public class HelpCommand
extends Command {
    public HelpCommand() {
        super(new ArrayList<String>(Arrays.asList("help", "commands")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (MyauClient.moduleManager.modules.isEmpty()) {
            return;
        }
        ChatUtils.addFormattedMessage(String.format("%sCommands:&r", "&7[&bMyau&7] &r"));
        for (Command command : MyauClient.commandManager.commands) {
            if (command instanceof ModuleSettingsCommand) continue;
            ChatUtils.addFormattedMessage(String.format("&7\u00bb&r .%s&r", String.join((CharSequence)" &7/&r .", command.aliases)));
        }
    }
}
