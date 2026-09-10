package dev.onelsey.incarnate.ability;

public final class WardenSonicMath {
    private WardenSonicMath() {
    }

    public static double resistanceScale(double resistance) {
        if (!Double.isFinite(resistance)) {
            return 1.0;
        }
        return 1.0 - Math.max(0.0, Math.min(1.0, resistance));
    }
}
