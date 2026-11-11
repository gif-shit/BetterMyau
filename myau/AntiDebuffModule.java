package myau;

import myau.BooleanSetting;
import myau.Module;

public class AntiDebuffModule
extends Module {
    public final BooleanSetting blindness = new BooleanSetting("blindness", true);
    public final BooleanSetting nausea = new BooleanSetting("nausea", true);

    public AntiDebuffModule() {
        super("AntiDebuff", false, 0);
    }
}
