package dev.onelsey.incarnate.sense;

import java.util.UUID;

public record WardenSenseSnapshot(
    UUID worldId,
    double x,
    double y,
    double z,
    String kind,
    String eventKey,
    UUID sourceId,
    UUID sonicTargetId,
    long sonicTargetExpiresAfterTick,
    long expiresAfterTick
) {
    public boolean isActive(long controlTick) {
        return controlTick <= expiresAfterTick;
    }

    public UUID activeSonicTarget(long controlTick) {
        return sonicTargetId != null && controlTick <= sonicTargetExpiresAfterTick ? sonicTargetId : null;
    }
}
