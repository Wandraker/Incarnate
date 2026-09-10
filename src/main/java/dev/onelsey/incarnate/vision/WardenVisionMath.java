package dev.onelsey.incarnate.vision;

public final class WardenVisionMath {
    private WardenVisionMath() {
    }

    public static boolean withinRadius(double dx, double dy, double dz, double radius) {
        double clampedRadius = Math.max(0.0, radius);
        return dx * dx + dy * dy + dz * dz <= clampedRadius * clampedRadius;
    }

    public static boolean shouldHide(boolean sameWorld, double dx, double dy, double dz, double radius) {
        return !sameWorld || !withinRadius(dx, dy, dz, radius);
    }
}
