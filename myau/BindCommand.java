package myau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.ChatUtils;
import myau.Command;
import myau.InputUtils;
import myau.Module;
import myau.MyauClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class BindCommand
extends Command {
    public BindCommand() {
        super(new ArrayList<String>(Arrays.asList("bind", "b")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        String moduleName;
        int mouseButton;
        if (arguments.size() < 3) {
            if (arguments.size() == 2 && (arguments.get(1).equalsIgnoreCase("list") || arguments.get(1).equalsIgnoreCase("l"))) {
                List boundModules = MyauClient.moduleManager.modules.values().stream().filter(module -> module.getKeyCode() != 0).collect(Collectors.toList());
                if (boundModules.isEmpty()) {
                    ChatUtils.addFormattedMessage(String.format("%sNo binds&r", "&7[&bMyau&7] &r"));
                    return;
                }
                ChatUtils.addFormattedMessage(String.format("%sBinds:&r", "&7[&bMyau&7] &r"));
                for (Module module2 : boundModules) {
                    ChatUtils.addFormattedMessage(String.format("%s\u00bb&r %s&r", module2.isHidden() ? "&8" : "&7", module2.getDisplayText()));
                }
                return;
            }
            String command = arguments.get(0).toLowerCase(Locale.ROOT);
            ChatUtils.addFormattedMessage(String.format("%sUsage: .%s <&omodule&r> <&okey&r>&r | .%s &olist&r", "&7[&bMyau&7] &r", command, command));
            return;
        }
        int keyCode = Keyboard.getKeyIndex((String)arguments.get(2).toUpperCase());
        if (keyCode == 0 && (mouseButton = Mouse.getButtonIndex((String)arguments.get(2).toUpperCase())) != -1) {
            keyCode = mouseButton - 100;
        }
        if ((moduleName = arguments.get(1)).equals("*")) {
            for (Module module3 : MyauClient.moduleManager.modules.values()) {
                module3.setKeyCode(keyCode);
            }
            ChatUtils.addFormattedMessage(String.format("%sBound all modules to &l[%s]&r", "&7[&bMyau&7] &r", InputUtils.getKeyName(keyCode)));
            return;
        }
        Module module4 = MyauClient.moduleManager.getModule(moduleName);
        if (module4 == null) {
            ChatUtils.addFormattedMessage(String.format("%sModule not found (&o%s&r)&r", "&7[&bMyau&7] &r", moduleName));
            return;
        }
        module4.setKeyCode(keyCode);
        ChatUtils.addFormattedMessage(String.format("%sBound &o%s&r to &l[%s]&r", "&7[&bMyau&7] &r", module4.getName(), InputUtils.getKeyName(keyCode)));
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        if (arguments.size() == 2) {
            ArrayList completions = MyauClient.moduleManager.modules.values().stream().map(module -> module.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toCollection(ArrayList::new));
            completions.add("list");
            completions.add("l");
            return completions;
        }
        if (arguments.size() != 3) {
            return new ArrayList<String>();
        }
        String moduleName = arguments.get(1);
        if (!moduleName.equals("*") && MyauClient.moduleManager.getModule(moduleName) == null) {
            return new ArrayList<String>();
        }
        ArrayList<String> completions = new ArrayList<String>();
        for (int keyCode = 0; keyCode < 256; ++keyCode) {
            String keyName = Keyboard.getKeyName((int)keyCode);
            if (keyName == null) continue;
            completions.add(keyName.toLowerCase(Locale.ROOT));
        }
        for (int button = 0; button < Mouse.getButtonCount(); ++button) {
            String buttonName = Mouse.getButtonName((int)button);
            if (buttonName == null) continue;
            completions.add(buttonName.toLowerCase(Locale.ROOT));
        }
        return completions;
    }
}
