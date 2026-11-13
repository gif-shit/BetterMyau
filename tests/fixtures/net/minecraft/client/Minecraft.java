package net.minecraft.client;

import myau.mixin.AccessorMinecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.Timer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import org.apache.logging.log4j.Logger;

public class Minecraft implements AccessorMinecraft {
    private static final Minecraft instance = new Minecraft();
    public EntityPlayerSP field_71439_g;
    public WorldClient field_71441_e;
    public GameSettings field_71474_y;
    public PlayerControllerMP field_71442_b;
    public GuiScreen field_71462_r;
    public NetHandlerPlayClient handler;
    private Timer timer;

    public static Minecraft func_71410_x() { return instance; }
    public NetHandlerPlayClient func_147114_u() { return handler; }
    public Entity func_175606_aa() { return field_71439_g; }
    public Timer getTimer() { return timer; }
    public void setTimer(Timer value) { timer = value; }
    public Logger getLogger() { return null; }
    public int getRightClickDelayTimer() { return 0; }
    public void setRightClickDelayTimer(int value) { }
}
