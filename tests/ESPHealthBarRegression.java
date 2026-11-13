package myau;

import com.google.gson.JsonObject;

public final class ESPHealthBarRegression {
    private static int checks;

    private static void check(boolean condition, String message) {
        ++checks;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ESPModule esp = new ESPModule();
        check(esp.healthBarStyle.getValue() == 0 && esp.healthBarStyle.getModeName().equals("MYAU"), "Myau is the default health-bar style");
        check(!esp.healthBarStyle.isVisible() && !esp.hasOverlayHealthBar() && !esp.hasWorldHealthBar(), "disabled health bars stay disabled");
        JsonObject legacy = new JsonObject();
        legacy.addProperty("health-bar", "RAVEN");
        check(esp.healthBar.readJson(legacy) && esp.healthBar.getModeName().equals("3D"), "legacy RAVEN placement still loads as the existing 3D bar");
        check(esp.healthBarStyle.isVisible() && esp.hasWorldHealthBar() && !esp.hasOverlayHealthBar(), "legacy Myau 3D bars keep their render path");
        esp.healthBar.setValue(1);
        check(esp.hasOverlayHealthBar() && !esp.hasWorldHealthBar(), "Myau 2D bars keep their overlay render path");
        check(esp.healthBarStyle.parseValue("raven"), "Raven is available as a style");
        check(!esp.hasOverlayHealthBar() && esp.hasWorldHealthBar(), "Raven renders one camera-facing world bar without a duplicate overlay");
        JsonObject saved = new JsonObject();
        esp.healthBarStyle.writeJson(saved);
        esp.healthBarStyle.setValue(0);
        check(esp.healthBarStyle.readJson(saved) && esp.healthBarStyle.getModeName().equals("RAVEN"), "Raven style survives profile saving and loading");
        esp.healthBar.setValue(0);
        check(!esp.hasOverlayHealthBar() && !esp.hasWorldHealthBar(), "turning health bars off also turns Raven bars off");
        check(RavenHealthBar.fraction(20, 20) == 1 && RavenHealthBar.fraction(10, 20) == 0.5f, "Raven bar uses health divided by maximum health");
        check(RavenHealthBar.fraction(30, 20) == 1 && RavenHealthBar.fraction(-2, 20) == 0, "Raven bar clamps over-healing and negative health");
        check(RavenHealthBar.fraction(10, 0) == 0 && RavenHealthBar.fraction(Float.NaN, 20) == 0, "Raven bar handles unavailable or invalid health");
        check(RavenHealthBar.height(1.8f, false) == 70 && RavenHealthBar.height(1.8f, true) == 40 && RavenHealthBar.height(1f, false) == 40, "Raven keeps the supplied adult and child bar heights");
        check(RavenHealthBar.color(0.2f) == 0xffff2626 && RavenHealthBar.color(0.4f) == 0xffffd026
                && RavenHealthBar.color(0.6f) == 0xffffff26 && RavenHealthBar.color(1) == 0xff26ff26, "Raven keeps the supplied health colours");
        check(ModuleCategory.of(new DisplaceModule()) == ModuleCategory.COMBAT, "Displace appears in Combat");
        System.out.println("ESP health-bar checks passed: " + checks);
    }
}
