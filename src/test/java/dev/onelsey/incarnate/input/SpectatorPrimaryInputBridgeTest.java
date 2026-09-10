package dev.onelsey.incarnate.input;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorPrimaryInputBridgeTest {
    @Test
    void detectsNoTargetSpectatorAction() {
        assertTrue(SpectatorPrimaryInputBridge.isNoTargetSpectatorAction(
            new ServerboundSpectatorActionPacket(OptionalInt.empty())
        ));
    }

    @Test
    void ignoresSpectatorActionWithEntityTarget() {
        assertFalse(SpectatorPrimaryInputBridge.isNoTargetSpectatorAction(
            new ServerboundSpectatorActionPacket(OptionalInt.of(42))
        ));
    }

    @Test
    void ignoresUnrelatedPackets() {
        assertFalse(SpectatorPrimaryInputBridge.isNoTargetSpectatorAction(new Object()));
    }

    private static final class ServerboundSpectatorActionPacket {
        private final OptionalInt entityId;

        private ServerboundSpectatorActionPacket(OptionalInt entityId) {
            this.entityId = entityId;
        }
    }
}
