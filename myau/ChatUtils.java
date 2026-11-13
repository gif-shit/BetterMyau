package myau;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class ChatUtils {

    private static final Minecraft minecraft = Minecraft.func_71410_x();

    private ChatUtils() {
    }

    public static void addComponent(IChatComponent component) {
        if (minecraft.field_71439_g != null) {
            minecraft.field_71439_g.func_145747_a(component);
        }
    }

    public static void addFormattedMessage(String message) {
        addComponent((IChatComponent) new ChatComponentText(ChatColor.formatText(message)));
    }

    public static void addRawMessage(String message) {
        addComponent((IChatComponent) new ChatComponentText(message));
    }

    public static void sendMessage(String message) {
        if (minecraft.field_71439_g != null) {
            minecraft.field_71439_g.func_71165_d(message);
        }
    }
}
