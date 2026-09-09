package dev.onelsey.incarnate.sense;

import java.util.UUID;

public record WardenSenseSnapshot(
    UUID worldId,
    double x,
    double y,
    double z,
    String kind,
    String eventKey,
    long expiresAfterTick
) {
    public boolean isActive(long controlTick) {
        return controlTick <= expiresAfterTick;
    }
}
