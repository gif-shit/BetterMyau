package myau;

import myau.ChatUtils;
import myau.EventHandler;
import myau.KeyPressEvent;
import myau.Module;
import myau.MyauClient;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public class MCFModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final int MIDDLE_MOUSE_KEY = -98;

    public MCFModule() {
        super("MCF", false, 0, true);
    }

    @EventHandler
    public void onKeyPress(KeyPressEvent event) {
        if (!this.isEnabled() || event.getKeyCode() != -98) {
            return;
        }
        if (MCFModule.minecraft.field_71476_x == null || MCFModule.minecraft.field_71476_x.field_72313_a != MovingObjectPosition.MovingObjectType.ENTITY) {
            return;
        }
        if (!(MCFModule.minecraft.field_71476_x.field_72308_g instanceof EntityPlayer)) {
            return;
        }
        String playerName = MCFModule.minecraft.field_71476_x.field_72308_g.func_70005_c_();
        if (MyauClient.friendList.contains(playerName)) {
            MyauClient.friendList.remove(playerName);
            ChatUtils.addFormattedMessage(String.format("%sRemoved &o%s&r from your friend list&r", "&7[&bMyau&7] &r", playerName));
        } else {
            MyauClient.friendList.add(playerName);
            ChatUtils.addFormattedMessage(String.format("%sAdded &o%s&r to your friend list&r", "&7[&bMyau&7] &r", playerName));
        }
    }
}
