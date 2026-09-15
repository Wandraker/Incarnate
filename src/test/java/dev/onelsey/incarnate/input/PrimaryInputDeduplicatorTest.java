package dev.onelsey.incarnate.input;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimaryInputDeduplicatorTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void packetPulseSuppressesNearbyBukkitFallbacks() {
        PrimaryInputDeduplicator deduplicator = new PrimaryInputDeduplicator(100L, 35L);
        deduplicator.markSpectatorPacket(PLAYER, 1_000L);

        assertFalse(deduplicator.accept(PLAYER, PrimaryInputTransport.ARM_SWING, 1_050L));
        assertFalse(deduplicator.accept(PLAYER, PrimaryInputTransport.INTERACT, 1_075L));
        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.SPECTATOR_PACKET, 1_080L));
    }

    @Test
    void repeatedPacketPulsesRemainDistinctClicks() {
        PrimaryInputDeduplicator deduplicator = new PrimaryInputDeduplicator(100L, 35L);

        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.SPECTATOR_PACKET, 1_000L));
        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.SPECTATOR_PACKET, 1_020L));
        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.SPECTATOR_PACKET, 1_040L));
    }

    @Test
    void fallbackOnlyModeMergesDifferentEventsForOneClick() {
        PrimaryInputDeduplicator deduplicator = new PrimaryInputDeduplicator(100L, 35L);

        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.INTERACT, 1_000L));
        assertFalse(deduplicator.accept(PLAYER, PrimaryInputTransport.ARM_SWING, 1_020L));
        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.INTERACT, 1_060L));
    }

    @Test
    void stalePacketDoesNotDisableFallbacks() {
        PrimaryInputDeduplicator deduplicator = new PrimaryInputDeduplicator(100L, 35L);
        deduplicator.markSpectatorPacket(PLAYER, 1_000L);

        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.ARM_SWING, 1_101L));
    }

    @Test
    void clearRemovesPreviousTransportHistory() {
        PrimaryInputDeduplicator deduplicator = new PrimaryInputDeduplicator(100L, 35L);
        deduplicator.markSpectatorPacket(PLAYER, 1_000L);
        deduplicator.clear(PLAYER);

        assertTrue(deduplicator.accept(PLAYER, PrimaryInputTransport.INTERACT, 1_010L));
    }
}
