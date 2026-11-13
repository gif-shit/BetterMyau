package myau.openb4;

import java.util.ArrayList;
import java.util.List;
import myau.EventHandler;
import myau.MyauClient;
import myau.PacketReceiveEvent;
import myau.PacketSendEvent;
import myau.PostTickEvent;
import myau.PreTickEvent;
import myau.PreWalkingUpdateEvent;
import myau.RenderOverlayEvent;
import myau.RotationUpdateEvent;
import myau.WorldLoadEvent;
import myau.openb4.client.Chat;
import myau.openb4.client.Hypixel;
import myau.openb4.client.InfoHud;
import myau.openb4.event.MotionEvent;
import myau.openb4.module.Module;
import myau.openb4.module.dev.DebugAcModule;
import myau.openb4.module.dev.TestModule;
import myau.openb4.module.dev.ViewPacketsModule;
import myau.openb4.module.minigames.BedWarsModule;
import myau.openb4.module.minigames.SkyWarsModule;
import myau.openb4.module.world.WeatherModule;
import myau.openb4.util.Executor;
import myau.openb4.util.PacketUtils;
import myau.openb4.util.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ModuleEvents {

    private static final Minecraft mc = Minecraft.func_71410_x();

    private static List<Module> modules() {
        List<Module> modules = new ArrayList<>();
        for (myau.Module module : MyauClient.moduleManager.modules.values()) {
            if (module instanceof Module)
                modules.add((Module) module);
        }
        return modules;
    }

    private static boolean enabled(Class<? extends Module> type) {
        myau.Module module = MyauClient.moduleManager.modules.get(type);
        return module != null && module.isEnabled();
    }

    @EventHandler(priority = 1000)
    public void onPreTick(PreTickEvent event) {
        PacketUtils.sentSwing = false;
        PacketUtils.sentUseEntity = false;
        PacketUtils.sentAnyDigging = false;
        Executor.tick();
        for (Module module : modules()) {
            if (module instanceof WeatherModule && !module.isEnabled())
                module.afterDisable();
        }
        if (!Chat.inWorld())
            return;
        Hypixel.tick();
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        if (!Chat.inWorld())
            return;
        for (Module module : modules()) {
            if (module.isEnabled())
                module.onUpdate();
        }
    }

    @EventHandler
    public void onRotation(RotationUpdateEvent event) {
        RotationManager.serverYaw = event.getPreviousYaw();
        RotationManager.serverPitch = event.getPreviousPitch();
        RotationManager.wasRotating = RotationManager.rotating;
        RotationManager.rotating = false;
        if (!Chat.inWorld() || event.isModified())
            return;
        for (Module module : modules()) {
            if (!module.isEnabled())
                continue;
            module.onPreUpdate();
            float[] rotation = module.getRotations(event.getPreviousYaw(), event.getPreviousPitch());
            if (rotation == null)
                continue;
            event.setRotation(rotation[0], rotation[1], 3);
            event.setMovementYaw(rotation[0], 3);
            RotationManager.serverYaw = rotation[0];
            RotationManager.serverPitch = rotation[1];
            RotationManager.rotating = true;
            break;
        }
    }

    @EventHandler
    public void onPreMotion(PreWalkingUpdateEvent event) {
        if (!Chat.inWorld())
            return;
        MotionEvent motion = new MotionEvent(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, mc.field_71439_g.field_70122_E, mc.field_71439_g.func_70051_ag(), mc.field_71439_g.func_70093_af());
        for (Module module : modules()) if (module.isEnabled())
            module.onPreMotion(motion);
    }

    @EventHandler(priority = Integer.MIN_VALUE)
    public void onPacketSend(PacketSendEvent event) {
        Packet<?> packet = event.getPacket();
        if (enabled(ViewPacketsModule.class)
                && ViewPacketsModule.sent.getValue()
                && (!event.isCancelled()
                || ViewPacketsModule.includeCancelled.getValue()))
            ViewPacketsModule.onSent(packet);
        if (event.isCancelled() || !Chat.inWorld())
            return;
        if (enabled(DebugAcModule.class))
            DebugAcModule.onSent(packet);
        if (packet instanceof C0APacketAnimation)
            PacketUtils.sentSwing = true;
        if (packet instanceof C02PacketUseEntity)
            PacketUtils.sentUseEntity = true;
        if (packet instanceof C07PacketPlayerDigging) {
            PacketUtils.sentAnyDigging = true;
            if (enabled(TestModule.class))
                TestModule.onDig((C07PacketPlayerDigging) packet);
        }
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            ItemStack stack = ((C08PacketPlayerBlockPlacement) packet).func_149574_g();
            if (stack != null && stack.func_77973_b() instanceof ItemEnderPearl) {
                for (Module module : modules()) if (module.isEnabled())
                    module.onPearlThrown(stack);
            }
        }
    }

    @EventHandler(priority = Integer.MIN_VALUE)
    public void onPacketReceive(PacketReceiveEvent event) {
        Packet<?> packet = event.getPacket();
        WeatherModule weather = (WeatherModule) MyauClient.moduleManager.modules.get(WeatherModule.class);
        if (!event.isCancelled() && weather != null && weather.isEnabled() && !weather.onReceivePacket(packet, null))
            event.setCancelled(true);
        if (enabled(ViewPacketsModule.class)
                && ViewPacketsModule.received.getValue()
                && (!event.isCancelled()
                || ViewPacketsModule.includeCancelled.getValue()))
            mc.func_152344_a(() -> {
                if (enabled(ViewPacketsModule.class))
                    ViewPacketsModule.onReceived(packet);
            });
        if (event.isCancelled())
            return;
        if (packet instanceof S08PacketPlayerPosLook) PacketUtils.lastPosLook = System.currentTimeMillis();
        if (!enabled(BedWarsModule.class) && !(packet instanceof S08PacketPlayerPosLook && enabled(SkyWarsModule.class))) return;
        Object world = mc.field_71441_e;
        mc.func_152344_a(() -> {
            if (!Chat.inWorld() || mc.field_71441_e != world)
                return;
            if (packet instanceof S08PacketPlayerPosLook) {
                if (enabled(SkyWarsModule.class))
                    SkyWarsModule.onSetback();
            }
            for (Module module : modules()) {
                if (module.isEnabled() && !(module instanceof WeatherModule))
                    module.onReceivePacket(packet, mc.func_147114_u());
            }
        });
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type == 2 || !Chat.inWorld())
            return;
        String text = event.message.func_150260_c();
        if (Hypixel.onChat(text)) {
            event.setCanceled(true);
            return;
        }
        if (enabled(BedWarsModule.class))
            BedWarsModule.onGameStartChat(event, text, true, false, false);
        for (Module module : modules()) if (module.isEnabled())
            module.onChat(event, text);
    }

    @EventHandler(priority = -100)
    public void onRenderOverlay(RenderOverlayEvent event) {
        ScaledResolution resolution = new ScaledResolution(mc);
        myau.openb4.util.RenderUtils.scaledWidth = resolution.func_78326_a();
        myau.openb4.util.RenderUtils.scaledHeight = resolution.func_78328_b();
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public void onRenderTick(net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)
            InfoHud.render();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        Executor.clear();
        InfoHud.clear();
        Hypixel.state = Hypixel.State.UNKNOWN;
        Hypixel.lastMode = null;
        Hypixel.awaitingLocraw = false;
        Hypixel.pendingLocraw = false;
        for (Module module : modules()) {
            if (!module.isEnabled())
                continue;
            module.onDisable();
            if (module instanceof BedWarsModule || module instanceof SkyWarsModule)
                module.onEnable();
        }
        WeatherModule.savedTime = -9;
        WeatherModule.savedRain = -1;
        RotationManager.rotating = RotationManager.wasRotating = false;
    }
}
