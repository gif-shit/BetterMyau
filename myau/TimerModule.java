package myau;

import com.google.common.base.CaseFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.mixin.AccessorEntityPlayerSP;
import myau.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class TimerModule extends Module {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat speedFormat = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private boolean timerSpeedOwned;
    private boolean allowNextMovementPacket;
    private boolean movementInputCleared;
    private boolean motionStored;
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private int bufferedMovementPackets;
    public final ModeSetting mode = new ModeSetting("mode", 0, new String[] { "VANILLA", "HYPIXEL" });
    public final FloatSetting speed = new FloatSetting("speed", 1.0f, 0.0f, 10.0f, this::showSpeedSetting);

    public TimerModule() {
        super("Timer", false, 0);
    }

    private boolean readyToMove() {
        return this.bufferedMovementPackets >= 10 || ((AccessorEntityPlayerSP) minecraft.field_71439_g).getPositionUpdateTicks() >= 20;
    }

    private boolean showSpeedSetting() {
        int selection = this.mode.getValue();
        return selection == 0;
    }

    @EventHandler
    public void onPostTick(PostTickEvent event) {
        boolean enabled = this.isEnabled();
        if (!enabled || this.mode.getValue() != 0) {
            return;
        }
        if (minecraft.field_71462_r != null) {
            this.onDisable();
            return;
        }
        NoFallModule conflictingModule = (NoFallModule) MyauClient.moduleManager.modules.get(NoFallModule.class);
        if (conflictingModule.isPacketModeActive()) {
            this.timerSpeedOwned = false;
            return;
        }
        this.timerSpeedOwned = true;
        ((AccessorMinecraft) minecraft).getTimer().field_74278_d = this.speed.getValue().floatValue();
    }

    private void clearMovementInput() {
        minecraft.field_71439_g.field_71158_b.field_78902_a = 0.0f;
        minecraft.field_71439_g.field_71158_b.field_78900_b = 0.0f;
        minecraft.field_71439_g.field_71158_b.field_78901_c = false;
        minecraft.field_71439_g.field_71158_b.field_78899_d = false;
    }

    @EventHandler(priority = -1000)
    public void onMovementInput(MovementInputEvent event) {
        boolean enabled = this.isEnabled();
        if (enabled && this.mode.getValue() == 1 && !this.readyToMove()) {
            this.movementInputCleared = true;
            this.clearMovementInput();
            return;
        }
        if (this.movementInputCleared) {
            this.movementInputCleared = false;
            this.clearMovementInput();
        }
    }

    @EventHandler(priority = -1000)
    public void onMoveFlying(MoveFlyingEvent event) {
        boolean enabled = this.isEnabled();
        if (enabled && this.mode.getValue() == 1 && !this.readyToMove()) {
            if (!this.motionStored) {
                this.motionStored = true;
                this.savedMotionX = minecraft.field_71439_g.field_70159_w;
                this.savedMotionY = minecraft.field_71439_g.field_70181_x;
                this.savedMotionZ = minecraft.field_71439_g.field_70179_y;
            }
            minecraft.field_71439_g.field_70159_w = 0.0;
            if (!minecraft.field_71439_g.field_70122_E) {
                minecraft.field_71439_g.field_70181_x = 0.0;
            }
            minecraft.field_71439_g.field_70179_y = 0.0;
            return;
        }
        if (this.motionStored) {
            this.motionStored = false;
            minecraft.field_71439_g.field_70159_w = this.savedMotionX;
            minecraft.field_71439_g.field_70181_x = this.savedMotionY;
            minecraft.field_71439_g.field_70179_y = this.savedMotionZ;
        }
    }

    @EventHandler(priority = -1000)
    public void onPacketSend(PacketSendEvent event) {
        boolean enabled = this.isEnabled();
        if (!enabled || event.isCancelled() || !(event.getPacket() instanceof C03PacketPlayer)) {
            return;
        }
        if (this.allowNextMovementPacket) {
            this.allowNextMovementPacket = false;
            return;
        }
        if (this.mode.getValue() == 1 && !this.readyToMove()) {
            ++this.bufferedMovementPackets;
            if (!(event.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
                event.setCancelled(true);
            }
            return;
        }
        this.bufferedMovementPackets = 0;
    }

    @EventHandler(priority = -1000)
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getValue() != 1 || event.isCancelled() || !(event.getPacket() instanceof S08PacketPlayerPosLook)) {
            return;
        }
        this.allowNextMovementPacket = true;
        this.motionStored = false;
    }

    @Override
    public void onDisable() {
        if (this.timerSpeedOwned) {
            this.timerSpeedOwned = false;
            ((AccessorMinecraft) minecraft).getTimer().field_74278_d = 1.0f;
        }
        this.bufferedMovementPackets = 0;
    }

    @Override
    public void onSettingChanged(String name) {
        if (this.isEnabled()) {
            this.onDisable();
        }
    }

    @Override
    public String[] getHudInfo() {
        int selection = this.mode.getValue();
        switch(selection) {
            case 0:
                {
                    return new String[] { speedFormat.format(this.speed.getValue()) };
                }
            case 1:
                {
                    return new String[] { CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeName()) };
                }
        }
        return new String[0];
    }
}
