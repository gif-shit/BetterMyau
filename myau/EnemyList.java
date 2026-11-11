package myau;

import java.awt.Color;
import java.io.File;
import myau.ChatColor;
import myau.PlayerNameList;

public class EnemyList
extends PlayerNameList {
    public EnemyList() {
        super(new File("./config/Myau/", "enemies.txt"), new Color(ChatColor.DARK_RED.getRgb()));
    }
}
