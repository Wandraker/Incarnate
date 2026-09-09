package dev.onelsey.incarnate.sense;

import org.bukkit.Location;

import java.util.UUID;

public record VesselPositionSnapshot(UUID worldId, double x, double y, double z) {
    public static VesselPositionSnapshot from(Location location) {
        return new VesselPositionSnapshot(
            location.getWorld().getUID(),
            location.getX(),
            location.getY(),
            location.getZ()
        );
    }

    public double distanceSquared(double otherX, double otherY, double otherZ) {
        double dx = otherX - x;
        double dy = otherY - y;
        double dz = otherZ - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
