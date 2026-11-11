package myau;

import java.awt.Color;
import java.io.File;
import myau.ChatColor;
import myau.PlayerNameList;

public class FriendList
extends PlayerNameList {
    public FriendList() {
        super(FriendList.configurationFile(), new Color(ChatColor.DARK_GREEN.getRgb()));
    }

    private static File configurationFile() {
        return new File("./config/Myau/", "friends.txt");
    }
}
