package myau;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PlayerListCommand extends Command {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    public PlayerListCommand() {
        super(new ArrayList<String>(Arrays.asList("playerlist", "players")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        ArrayList<String> players = new ArrayList<String>();
        for (NetworkPlayerInfo playerInfo : minecraft.func_147114_u().func_175106_d()) {
            players.add(playerInfo.func_178845_a().getName().replace("\u00a7", "&"));
        }
        if (players.isEmpty()) {
            ChatUtils.addFormattedMessage(String.format("%sNo players&r", "&7[&bMyau&7] &r"));
            return;
        }
        ChatUtils.addRawMessage(String.format(ChatColor.formatText("%sPlayers:&r %s"), ChatColor.formatText("&7[&bMyau&7] &r"), String.join((CharSequence) ", ", players)));
    }
}
