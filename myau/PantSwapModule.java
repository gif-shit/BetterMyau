package myau;

import myau.modules.PantSwap;
import myau.modules.pantswap.PantSwapMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

public final class PantSwapModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final PantSwap engine = new PantSwap(new PantSwapMinecraft(minecraft));
    private volatile Object connection;
    public final IntegerSetting delay = new IntegerSetting("delay", 100, 0, 1000);
    public final BooleanSetting inventory = new BooleanSetting("inventory", false);

    public PantSwapModule() {
        super("PantSwap", false, 0);
    }

    @Override
    public void onSettingChanged(String name) {
        engine.settings.delay = delay.getValue();
        engine.settings.inventory = inventory.getValue();
    }

    @Override
    public void onEnable() {
        onSettingChanged("");
        connection = minecraft.func_147114_u() == null ? null : minecraft.func_147114_u().func_147298_b();
        engine.setEnabled(true);
        if (!engine.isEnabled())
            setEnabled(false);
    }

    @Override
    public void onDisable() {
        engine.setEnabled(false);
        connection = null;
    }

    @EventHandler(priority = -50)
    public void onPreTick(PreTickEvent event) {
        engine.onTick();
        if (isEnabled() && !engine.isEnabled())
            setEnabled(false);
    }

    @EventHandler(priority = 100)
    public void onWindowClick(WindowClickEvent event) {
        if (engine.cancelInventoryAction())
            event.setCancelled(true);
    }

    @EventHandler(priority = 100)
    public void onPacketSend(PacketSendEvent event) {
        if (!event.isCancelled() && event.getPacket() instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow packet = (C0EPacketClickWindow) event.getPacket();
            engine.onPacketSend(packet.func_149548_c(), packet.func_149547_f());
        }
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveEvent event) {
        if (connection != null && event.getConnection() == connection && event.getPacket() instanceof S32PacketConfirmTransaction) {
            S32PacketConfirmTransaction packet = (S32PacketConfirmTransaction) event.getPacket();
            engine.onPacketReceive(packet.func_148889_c(), packet.func_148890_d(), packet.func_148888_e(), event.isCancelled());
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        engine.onWorldChange();
        setEnabled(false);
    }
}
