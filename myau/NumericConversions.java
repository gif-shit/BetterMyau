package myau;

final class NumericConversions {

    private NumericConversions() {
    }

    static long truncateFloatToLong(float input) {
        return Float.isNaN(input) || input >= 9.223372E18f ? Long.MIN_VALUE : (long) input;
    }
}
