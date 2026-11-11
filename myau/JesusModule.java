package myau;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import myau.BooleanSetting;
import myau.FloatSetting;
import myau.Module;

public class JesusModule
extends Module {
    private static final DecimalFormat speedFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    public final FloatSetting speed = new FloatSetting("speed", Float.valueOf(2.5f), Float.valueOf(0.0f), Float.valueOf(3.0f));
    public final BooleanSetting noPush = new BooleanSetting("no-push", true);
    public final BooleanSetting groundOnly = new BooleanSetting("ground-only", true);

    public JesusModule() {
        super("Jesus", false, 0);
    }

    @Override
    public String[] getHudInfo() {
        return new String[]{speedFormat.format(this.speed.getValue())};
    }
}
