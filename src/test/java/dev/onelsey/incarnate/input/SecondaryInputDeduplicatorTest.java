package dev.onelsey.incarnate.input;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondaryInputDeduplicatorTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void packetSuppressesFollowingBukkitSwapFromSamePhysicalPress() {
        SecondaryInputDeduplicator dedup = new SecondaryInputDeduplicator(100L);
        dedup.markPacket(PLAYER, 1_000L);
        assertTrue(dedup.accept(PLAYER, SecondaryInputTransport.PLAYER_ACTION_PACKET, 1_001L));
        assertFalse(dedup.accept(PLAYER, SecondaryInputTransport.BUKKIT_SWAP, 1_050L));
    }

    @Test
    void separatePacketPressesAreNeverMerged() {
        SecondaryInputDeduplicator dedup = new SecondaryInputDeduplicator(100L);
        dedup.markPacket(PLAYER, 1_000L);
        assertTrue(dedup.accept(PLAYER, SecondaryInputTransport.PLAYER_ACTION_PACKET, 1_001L));
        dedup.markPacket(PLAYER, 1_010L);
        assertTrue(dedup.accept(PLAYER, SecondaryInputTransport.PLAYER_ACTION_PACKET, 1_011L));
    }

    @Test
    void bukkitFallbackWorksWithoutRecentPacket() {
        SecondaryInputDeduplicator dedup = new SecondaryInputDeduplicator(100L);
        assertTrue(dedup.accept(PLAYER, SecondaryInputTransport.BUKKIT_SWAP, 5_000L));
    }
}
