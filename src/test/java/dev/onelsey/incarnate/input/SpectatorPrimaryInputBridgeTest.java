package dev.onelsey.incarnate.input;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void detectsSwapOffhandPlayerAction() {
        assertTrue(SpectatorPrimaryInputBridge.isSwapOffhandAction(
            new ServerboundPlayerActionPacket(PlayerAction.SWAP_ITEM_WITH_OFFHAND)
        ));
    }

    @Test
    void readsPublicPlayerActionAccessorWithoutPrivateFieldAccess() {
        assertEquals("SWAP_ITEM_WITH_OFFHAND", SpectatorPrimaryInputBridge.playerActionName(
            new AccessorOnlyPacket(PlayerAction.SWAP_ITEM_WITH_OFFHAND)
        ));
    }

    @Test
    void ignoresOtherPlayerActions() {
        assertFalse(SpectatorPrimaryInputBridge.isSwapOffhandAction(
            new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM)
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

    private enum PlayerAction {
        SWAP_ITEM_WITH_OFFHAND,
        DROP_ITEM
    }

    public static final class AccessorOnlyPacket {
        private final PlayerAction action;

        private AccessorOnlyPacket(PlayerAction action) {
            this.action = action;
        }

        public PlayerAction getAction() {
            return action;
        }
    }

    private static final class ServerboundPlayerActionPacket {
        private final PlayerAction action;

        private ServerboundPlayerActionPacket(PlayerAction action) {
            this.action = action;
        }
    }
}
