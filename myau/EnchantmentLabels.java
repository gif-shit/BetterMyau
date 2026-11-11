package myau;

import java.util.HashMap;
import myau.EnchantmentLabel;

final class EnchantmentLabels
extends HashMap<Integer, EnchantmentLabel> {
    EnchantmentLabels() {
        this.put(0, new EnchantmentLabel("Pr", 4));
        this.put(1, new EnchantmentLabel("Fp", 4));
        this.put(2, new EnchantmentLabel("Ff", 4));
        this.put(3, new EnchantmentLabel("Bp", 4));
        this.put(4, new EnchantmentLabel("Pp", 4));
        this.put(5, new EnchantmentLabel("Re", 3));
        this.put(6, new EnchantmentLabel("Aq", 1));
        this.put(7, new EnchantmentLabel("Th", 3));
        this.put(8, new EnchantmentLabel("Ds", 3));
        this.put(16, new EnchantmentLabel("Sh", 5));
        this.put(17, new EnchantmentLabel("Sm", 5));
        this.put(18, new EnchantmentLabel("BoA", 5));
        this.put(19, new EnchantmentLabel("Kb", 2));
        this.put(20, new EnchantmentLabel("Fa", 2));
        this.put(21, new EnchantmentLabel("Lo", 3));
        this.put(32, new EnchantmentLabel("Ef", 5));
        this.put(33, new EnchantmentLabel("St", 1));
        this.put(34, new EnchantmentLabel("Ub", 3));
        this.put(35, new EnchantmentLabel("Fo", 3));
        this.put(48, new EnchantmentLabel("Po", 5));
        this.put(49, new EnchantmentLabel("Pu", 2));
        this.put(50, new EnchantmentLabel("Fl", 1));
        this.put(51, new EnchantmentLabel("Inf", 1));
        this.put(61, new EnchantmentLabel("LoS", 3));
        this.put(62, new EnchantmentLabel("Lu", 3));
    }
}
