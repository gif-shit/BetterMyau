package myau;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import myau.ChatColor;
import myau.ChatUtils;
import myau.Command;
import myau.ConfigProfile;
import myau.Module;
import myau.MyauClient;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class ConfigCommand
extends Command {
    private static final File CONFIG_DIRECTORY = new File("./config/Myau/");
    private static final FileFilter CONFIG_FILE_FILTER = new WildcardFileFilter("*.json", IOCase.INSENSITIVE);

    public ConfigCommand() {
        super(new ArrayList<String>(Arrays.asList("config", "cfg", "c")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (arguments.size() < 2) {
            String alias = arguments.get(0).toLowerCase(Locale.ROOT);
            ChatUtils.addFormattedMessage(String.format("%sUsage: .%s &oload&r/&osave&r <&oname&r> | .%s &oreset&r | .%s &olist&r | .%s &ofolder&r", "&7[&bMyau&7] &r", alias, alias, alias, alias));
            return;
        }
        String subcommand = arguments.get(1);
        if (subcommand.equalsIgnoreCase("l")) {
            subcommand = arguments.size() < 3 ? "list" : "load";
        }
        switch (subcommand = subcommand.toLowerCase(Locale.ROOT)) {
            case "load":
            case "reload": {
                ConfigCommand.loadConfig(arguments);
                return;
            }
            case "save":
            case "s": {
                ConfigCommand.saveConfig(arguments);
                return;
            }
            case "reset":
            case "clear": {
                ConfigCommand.resetConfig();
                return;
            }
            case "list": {
                ConfigCommand.listConfigs();
                return;
            }
            case "folder":
            case "f":
            case "directory":
            case "dir": {
                ConfigCommand.openConfigDirectory();
                return;
            }
        }
        ChatUtils.addFormattedMessage(String.format("%sInvalid argument (&o%s&r)&r", "&7[&bMyau&7] &r", arguments.get(1)));
    }

    private static void loadConfig(List<String> arguments) {
        ConfigProfile profile = new ConfigProfile(arguments.size() < 3 ? ConfigProfile.activeProfileName : arguments.get(2));
        if (profile.load()) {
            ChatUtils.addFormattedMessage(String.format("%sConfig has been loaded (&a&o%s&r)&r", "&7[&bMyau&7] &r", profile.fileName()));
        } else {
            ChatUtils.addFormattedMessage(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", "&7[&bMyau&7] &r", profile.fileName()));
        }
    }

    private static void saveConfig(List<String> arguments) {
        ConfigProfile profile = new ConfigProfile(arguments.size() < 3 ? ConfigProfile.activeProfileName : arguments.get(2));
        if (profile.save()) {
            ChatUtils.addFormattedMessage(String.format("%sConfig has been saved (&a&o%s&r)&r", "&7[&bMyau&7] &r", profile.fileName()));
        } else {
            ChatUtils.addFormattedMessage(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", "&7[&bMyau&7] &r", profile.fileName()));
        }
    }

    private static void resetConfig() {
        if (new ConfigProfile("predac1").load()) {
            for (Module module : MyauClient.moduleManager.modules.values()) {
                module.setEnabled(false);
            }
        } else {
            ChatUtils.addFormattedMessage(String.format("%sConfig couldn't be loaded (&c&opredac1.json&r)&r", "&7[&bMyau&7] &r"));
        }
    }

    private static void listConfigs() {
        try {
            File[] files = CONFIG_DIRECTORY.listFiles(CONFIG_FILE_FILTER);
            if (files == null) {
                throw new IllegalStateException("Unable to list config directory");
            }
            if (files.length == 0) {
                ChatUtils.addFormattedMessage(String.format("%sNo configs found (&o%s&r)&r", "&7[&bMyau&7] &r", CONFIG_DIRECTORY));
                return;
            }
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            ChatUtils.addFormattedMessage(String.format("%sConfigs:&r", "&7[&bMyau&7] &r"));
            for (File file : files) {
                String displayText = ChatColor.formatText(String.format("&7\u00bb&r &o%s&r", file.getName()));
                String command = String.format(".config load %s", FilenameUtils.removeExtension(file.getName()));
                ChatUtils.addComponent(new ChatComponentText(displayText).func_150255_a(new ChatStyle().func_150241_a(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).func_150209_a(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (IChatComponent)new ChatComponentText(command)))));
            }
        }
        catch (Exception exception) {
            ChatUtils.addFormattedMessage(String.format("%sFailed to read (&o%s&r)&r", "&7[&bMyau&7] &r", CONFIG_DIRECTORY));
        }
    }

    private static void openConfigDirectory() {
        try {
            Desktop.getDesktop().open(CONFIG_DIRECTORY);
        }
        catch (Exception exception) {
            ChatUtils.addFormattedMessage(String.format("%sFailed to open (&o%s&r)&r", "&7[&bMyau&7] &r", CONFIG_DIRECTORY));
        }
    }

    @Override
    public ArrayList<String> completeArguments(ArrayList<String> arguments) {
        if (arguments.size() == 2) {
            return new ArrayList<String>(Arrays.asList("load", "reload", "l", "save", "s", "reset", "clear", "list", "folder", "f", "directory", "dir"));
        }
        if (arguments.size() == 3) {
            String action = arguments.get(1).toLowerCase(Locale.ROOT);
            if (!(action.equals("load") || action.equals("reload") || action.equals("l") || action.equals("save") || action.equals("s"))) {
                return new ArrayList<String>();
            }
            File[] files = CONFIG_DIRECTORY.listFiles(CONFIG_FILE_FILTER);
            if (files == null) {
                return new ArrayList<String>();
            }
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            return Arrays.stream(files).map(File::getName).map(FilenameUtils::removeExtension).collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<String>();
    }
}
