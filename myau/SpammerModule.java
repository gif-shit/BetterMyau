package myau;

import myau.ElapsedTimer;
import myau.EventHandler;
import myau.FloatSetting;
import myau.IntegerSetting;
import myau.Module;
import myau.RenderOverlayEvent;
import myau.StringSetting;
import net.minecraft.client.Minecraft;

public class SpammerModule
extends Module {
    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private final ElapsedTimer timer = new ElapsedTimer();
    private int nextSuffixCharacter = 19968;
    public final StringSetting text = new StringSetting("text", "meow");
    public final FloatSetting delay = new FloatSetting("delay", Float.valueOf(3.5f), Float.valueOf(0.0f), Float.valueOf(3600.0f));
    public final IntegerSetting random = new IntegerSetting("random", 0, 0, 10);

    public SpammerModule() {
        super("Spammer", false, 0);
    }

    @EventHandler
    public void onRenderOverlay(RenderOverlayEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (!this.timer.hasElapsed((long)(((Float)this.delay.getValue()).floatValue() * 1000.0f))) {
            return;
        }
        this.timer.reset();
        String message = (String)this.text.getValue();
        int suffixLength = (Integer)this.random.getValue();
        if (suffixLength <= 0) {
            SpammerModule.minecraft.field_71439_g.func_71165_d(message);
            return;
        }
        message = String.format("%s ", message);
        for (int index = 0; index < suffixLength; ++index) {
            message = String.format("%s%s", message, Character.valueOf((char)this.nextSuffixCharacter));
            ++this.nextSuffixCharacter;
        }
        SpammerModule.minecraft.field_71439_g.func_71165_d(message);
    }
}
