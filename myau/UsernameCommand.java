package myau;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;
import myau.ChatUtils;
import myau.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.StringUtils;

public class UsernameCommand
extends Command {
    private static final Minecraft minecraft = Minecraft.func_71410_x();

    public UsernameCommand() {
        super(new ArrayList<String>(Arrays.asList("username", "name", "ign")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        Session session = minecraft.func_110432_I();
        if (session == null || StringUtils.func_151246_b((String)session.func_111285_a())) {
            return;
        }
        String username = session.func_111285_a();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null);
            ChatUtils.addFormattedMessage(String.format("%sYour username has been copied to the clipboard (&o%s&r)&r", "&7[&bMyau&7] &r", username));
        }
        catch (Exception exception) {
            ChatUtils.addFormattedMessage(String.format("%sFailed to copy&r", "&7[&bMyau&7] &r"));
        }
    }
}
