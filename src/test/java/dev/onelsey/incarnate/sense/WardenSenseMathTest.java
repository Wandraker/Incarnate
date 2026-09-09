package dev.onelsey.incarnate.sense;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WardenSenseMathTest {
    @Test
    void directionIsRelativeToMinecraftYaw() {
        assertEquals("front", WardenSenseMath.directionKey(0.0f, 0.0, 10.0));
        assertEquals("right", WardenSenseMath.directionKey(0.0f, -10.0, 0.0));
        assertEquals("left", WardenSenseMath.directionKey(0.0f, 10.0, 0.0));
        assertEquals("back", WardenSenseMath.directionKey(0.0f, 0.0, -10.0));
        assertEquals("front", WardenSenseMath.directionKey(90.0f, -10.0, 0.0));
        assertEquals("front-right", WardenSenseMath.directionKey(0.0f, -10.0, 10.0));
    }

    @Test
    void gameEventsAreGroupedIntoStableHudKinds() {
        assertEquals("movement", WardenSenseMath.kindKey("step"));
        assertEquals("movement", WardenSenseMath.kindKey("minecraft:swim"));
        assertEquals("projectile", WardenSenseMath.kindKey("projectile_shoot"));
        assertEquals("combat", WardenSenseMath.kindKey("entity_damage"));
        assertEquals("block", WardenSenseMath.kindKey("block_destroy"));
        assertEquals("action", WardenSenseMath.kindKey("container_open"));
        assertEquals("vibration", WardenSenseMath.kindKey("shriek"));
    }

    @Test
    void senseSnapshotExpiresOnControlTicks() {
        WardenSenseSnapshot snapshot = new WardenSenseSnapshot(
            UUID.randomUUID(), 1.0, 2.0, 3.0, "movement", "step", 42L
        );
        assertTrue(snapshot.isActive(42L));
        assertFalse(snapshot.isActive(43L));
    }
}
