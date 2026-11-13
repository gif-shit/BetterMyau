package myau.openb4.module;

import myau.BooleanSetting;
import myau.openb4.event.MotionEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class Module extends myau.Module {

    protected static final Minecraft mc = Minecraft.func_71410_x();

    public Module(String name, int key) {
        super(name, false, key);
    }

    public void link() {
    }

    public void guiUpdate() {
    }

    public void guiButtonToggled(BooleanSetting setting) {
    }

    public void onPreUpdate() {
    }

    public void onUpdate() {
    }

    public void onPreMotion(MotionEvent event) {
    }

    public void afterDisable() {
    }

    public float[] getRotations(float yaw, float pitch) {
        return null;
    }

    public void onChat(ClientChatReceivedEvent event, String text) {
    }

    public void onPearlThrown(ItemStack stack) {
    }

    public boolean onReceivePacket(Packet packet, INetHandler handler) {
        return true;
    }

    public void enable() {
        setEnabled(true);
    }

    public void disable() {
        setEnabled(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() == enabled)
            return;
        super.setEnabled(enabled);
        if (isEnabled()) {
            MinecraftForge.EVENT_BUS.register(this);
            FMLCommonHandler.instance().bus().register(this);
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            FMLCommonHandler.instance().bus().unregister(this);
        }
    }

    @Override
    public void onSettingChanged(String name) {
        guiUpdate();
        for (java.lang.reflect.Field field : getClass().getFields()) {
            try {
                Object value = field.get(this);
                if (value instanceof BooleanSetting && ((BooleanSetting) value).getName().equals(name))
                    guiButtonToggled((BooleanSetting) value);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
