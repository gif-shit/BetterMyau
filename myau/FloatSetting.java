package myau;

import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;

public class FloatSetting extends Setting<Float> {

    private final Float min;
    private final Float max;
    private float step;

    public FloatSetting(String name, Float defaultValue, Float min, Float max) {
        this(name, defaultValue, min, max, null);
    }

    public FloatSetting(String name, Float defaultValue, Float min, Float max, BooleanSupplier visibilityCondition) {
        super(name, defaultValue, value -> value.floatValue() >= min.floatValue() && value.floatValue() <= max.floatValue(), visibilityCondition);
        this.min = min;
        this.max = max;
    }

    public FloatSetting(String name, Float defaultValue, Float min, Float max, float step) {
        this(name, defaultValue, min, max);
        this.step = step;
    }

    @Override
    public boolean setValue(Float value) {
        if (value == null || !Float.isFinite(value) || value < min || value > max) return false;
        if (step > 0) value = Math.max(min, Math.min(max, min + Math.round((value - min) / step) * step));
        return super.setValue(value);
    }

    @Override
    public String getUsage() {
        return String.format("%s-%s", this.min, this.max);
    }

    @Override
    public String getDisplayValue() {
        return String.format("&6%s", this.getValue());
    }

    @Override
    public boolean parseValue(String input) {
        return this.setValue(Float.valueOf(Float.parseFloat(input)));
    }

    @Override
    public boolean readJson(JsonObject json) {
        return this.setValue(Float.valueOf(json.get(this.getName()).getAsNumber().floatValue()));
    }

    @Override
    public void writeJson(JsonObject json) {
        json.addProperty(this.getName(), (Number) this.getValue());
    }
}
