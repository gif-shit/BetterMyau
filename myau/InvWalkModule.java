package myau;

import com.google.common.base.CaseFormat;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import myau.EventHandler;
import myau.InputUtils;
import myau.ModeSetting;
import myau.Module;
import myau.MyauClient;
import myau.PacketSendEvent;
import myau.PacketUtils;
import myau.PreTickEvent;
import myau.RotationUpdateEvent;
import myau.SprintModule;
import myau.mixin.AccessorC0DPacketCloseWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

public class InvWalkModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final Queue<C0EPacketClickWindow> queuedWindowClicks = new ConcurrentLinkedQueue<C0EPacketClickWindow>();
    private boolean movementKeysUpdated = false;
    private C16PacketClientStatus pendingInventoryOpenPacket = null;
    private int clickReleaseDelayTicks = 0;
    public final ModeSetting mode = new ModeSetting("mode", 1, new String[]{"VANILLA", "LEGIT", "HYPIXEL"});

    public InvWalkModule() {
        super("InvWalk", false, 0);
    }

    public void updateMovementKeys() {
        KeyBinding[] movementKeys;
        for (KeyBinding binding : movementKeys = new KeyBinding[]{InvWalkModule.minecraft.field_71474_y.field_74351_w, InvWalkModule.minecraft.field_71474_y.field_74370_x, InvWalkModule.minecraft.field_71474_y.field_74368_y, InvWalkModule.minecraft.field_71474_y.field_74366_z, InvWalkModule.minecraft.field_71474_y.field_74314_A, InvWalkModule.minecraft.field_71474_y.field_151444_V}) {
            InputUtils.updateKeyState(binding.func_151463_i());
        }
        if (MyauClient.moduleManager.modules.get(SprintModule.class).isEnabled()) {
            InputUtils.setKeyState(InvWalkModule.minecraft.field_71474_y.field_151444_V.func_151463_i(), true);
        }
        this.movementKeysUpdated = true;
    }

    public boolean canUpdateMovementKeys() {
        if (!(InvWalkModule.minecraft.field_71462_r instanceof GuiContainer) || InvWalkModule.minecraft.field_71462_r instanceof GuiContainerCreative) {
            return false;
        }
        switch ((Integer)this.mode.getValue()) {
            case 1: {
                return InvWalkModule.minecraft.field_71462_r instanceof GuiInventory && this.pendingInventoryOpenPacket != null && this.queuedWindowClicks.isEmpty();
            }
            case 2: {
                return InvWalkModule.minecraft.field_71462_r instanceof GuiInventory && this.queuedWindowClicks.isEmpty();
            }
        }
        return true;
    }

    @EventHandler(priority=-100)
    public void onPreTick(PreTickEvent event) {
        C0EPacketClickWindow queuedClick;
        while ((queuedClick = this.queuedWindowClicks.poll()) != null) {
            PacketUtils.sendWithoutSendEvent(queuedClick);
        }
    }

    @EventHandler(priority=-100)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.canUpdateMovementKeys() && this.clickReleaseDelayTicks == 0) {
            this.updateMovementKeys();
            return;
        }
        if (this.movementKeysUpdated) {
            if (InvWalkModule.minecraft.field_71462_r != null) {
                KeyBinding.func_74506_a();
            }
            this.movementKeysUpdated = false;
        }
        if (this.pendingInventoryOpenPacket != null) {
            PacketUtils.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
            this.pendingInventoryOpenPacket = null;
        }
        if (this.clickReleaseDelayTicks > 0) {
            --this.clickReleaseDelayTicks;
        }
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getPacket() instanceof C16PacketClientStatus) {
            if ((Integer)this.mode.getValue() != 1) {
                return;
            }
            C16PacketClientStatus statusPacket = (C16PacketClientStatus)event.getPacket();
            if (statusPacket.func_149435_c() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                event.setCancelled(true);
                this.pendingInventoryOpenPacket = statusPacket;
            }
            return;
        }
        if (event.getPacket() instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow clickPacket = (C0EPacketClickWindow)event.getPacket();
            switch ((Integer)this.mode.getValue()) {
                case 1: {
                    if (clickPacket.func_149548_c() != 0) break;
                    if ((clickPacket.func_149542_h() == 3 || clickPacket.func_149542_h() == 4) && clickPacket.func_149544_d() == -999) {
                        event.setCancelled(true);
                        return;
                    }
                    if (this.pendingInventoryOpenPacket == null) break;
                    KeyBinding.func_74506_a();
                    event.setCancelled(true);
                    this.queuedWindowClicks.offer(clickPacket);
                    break;
                }
                case 2: {
                    if (clickPacket.func_149548_c() != 0) break;
                    if ((clickPacket.func_149542_h() == 3 || clickPacket.func_149542_h() == 4) && clickPacket.func_149544_d() == -999) {
                        event.setCancelled(true);
                        break;
                    }
                    KeyBinding.func_74506_a();
                    event.setCancelled(true);
                    this.queuedWindowClicks.offer(clickPacket);
                    this.clickReleaseDelayTicks = 8;
                    break;
                }
            }
            if (this.pendingInventoryOpenPacket != null) {
                PacketUtils.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
                this.pendingInventoryOpenPacket = null;
            }
            return;
        }
        if (event.getPacket() instanceof C0DPacketCloseWindow) {
            C0DPacketCloseWindow closePacket = (C0DPacketCloseWindow)event.getPacket();
            if (this.pendingInventoryOpenPacket != null && ((AccessorC0DPacketCloseWindow)closePacket).getWindowId() == 0) {
                this.pendingInventoryOpenPacket = null;
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.movementKeysUpdated) {
            if (InvWalkModule.minecraft.field_71462_r != null) {
                KeyBinding.func_74506_a();
            }
            this.movementKeysUpdated = false;
        }
        if (this.pendingInventoryOpenPacket != null) {
            PacketUtils.sendWithoutSendEvent(this.pendingInventoryOpenPacket);
            this.pendingInventoryOpenPacket = null;
        }
        this.clickReleaseDelayTicks = 0;
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName())};
    }
}
