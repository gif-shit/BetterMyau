package myau;

import myau.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class ChatUtils {
    private static final Minecraft minecraft = Minecraft.func_71410_x();

    private ChatUtils() {
    }

    public static void addComponent(IChatComponent component) {
        if (ChatUtils.minecraft.field_71439_g != null) {
            ChatUtils.minecraft.field_71439_g.func_145747_a(component);
        }
    }

    public static void addFormattedMessage(String message) {
        ChatUtils.addComponent((IChatComponent)new ChatComponentText(ChatColor.formatText(message)));
    }

    public static void addRawMessage(String message) {
        ChatUtils.addComponent((IChatComponent)new ChatComponentText(message));
    }

    public static void sendMessage(String message) {
        if (ChatUtils.minecraft.field_71439_g != null) {
            ChatUtils.minecraft.field_71439_g.func_71165_d(message);
        }
    }
}
