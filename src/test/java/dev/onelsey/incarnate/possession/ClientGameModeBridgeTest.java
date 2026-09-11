package dev.onelsey.incarnate.possession;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientGameModeBridgeTest {
    @Test
    void mapsVanillaGameModeProtocolValues() {
        assertEquals(0.0F, ClientGameModeBridge.protocolValue(GameMode.SURVIVAL));
        assertEquals(1.0F, ClientGameModeBridge.protocolValue(GameMode.CREATIVE));
        assertEquals(2.0F, ClientGameModeBridge.protocolValue(GameMode.ADVENTURE));
        assertEquals(3.0F, ClientGameModeBridge.protocolValue(GameMode.SPECTATOR));
    }
}
