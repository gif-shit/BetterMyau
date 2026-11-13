package myau;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public class VClipCommand extends Command {

    private static final Minecraft minecraft = Minecraft.func_71410_x();
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public VClipCommand() {
        super(new ArrayList<String>(Collections.singletonList("vclip")));
    }

    @Override
    public void execute(ArrayList<String> arguments) {
        if (arguments.size() < 2) {
            ChatUtils.addFormattedMessage(String.format("%sUsage: .%s <&odistance&r>&r", "&7[&bMyau&7] &r", arguments.get(0).toLowerCase(Locale.ROOT)));
            return;
        }
        double distance = 0.0;
        try {
            distance = Double.parseDouble(arguments.get(1));
        } catch (NumberFormatException numberFormatException) {
        }
        minecraft.field_71439_g.func_70634_a(minecraft.field_71439_g.field_70165_t, minecraft.field_71439_g.field_70163_u + distance, minecraft.field_71439_g.field_70161_v);
        ChatUtils.addFormattedMessage(String.format("%sClipped (%s blocks)", "&7[&bMyau&7] &r", decimalFormat.format(distance)));
    }
}
