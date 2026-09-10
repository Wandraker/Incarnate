package dev.onelsey.incarnate.input;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorPrimaryInputBridgeTest {
    @Test
    void detectsNoTargetSpectatorAction() {
        assertTrue(SpectatorPrimaryInputBridge.isSpectatorPrimaryAction(
            new ServerboundSpectatorActionPacket(OptionalInt.empty())
        ));
    }

    @Test
    void detectsEntityTargetedSpectatorAction() {
        assertTrue(SpectatorPrimaryInputBridge.isSpectatorPrimaryAction(
            new ServerboundSpectatorActionPacket(OptionalInt.of(42))
        ));
    }

    @Test
    void ignoresUnrelatedPackets() {
        assertFalse(SpectatorPrimaryInputBridge.isSpectatorPrimaryAction(new Object()));
    }

    private static final class ServerboundSpectatorActionPacket {
        private final OptionalInt entityId;

        private ServerboundSpectatorActionPacket(OptionalInt entityId) {
            this.entityId = entityId;
        }
    }
}
