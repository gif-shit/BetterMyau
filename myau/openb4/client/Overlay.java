package myau.openb4.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

public final class Overlay {

    public static boolean ownScreen() {
        return Minecraft.func_71410_x().field_71462_r instanceof myau.ClickGuiScreen;
    }

    public static boolean canRender() {
        Minecraft mc = Minecraft.func_71410_x();
        return Chat.inWorld() && !mc.field_71474_y.field_74330_P && (mc.field_71462_r == null || mc.field_71462_r instanceof GuiChat);
    }
}
