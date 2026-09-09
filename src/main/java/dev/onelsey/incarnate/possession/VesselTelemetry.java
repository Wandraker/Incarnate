package dev.onelsey.incarnate.possession;

public record VesselTelemetry(double health, double maxHealth) {
    public VesselTelemetry {
        health = Math.max(0.0, health);
        maxHealth = Math.max(0.0, maxHealth);
    }
}
