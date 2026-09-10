package dev.onelsey.incarnate.possession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BodyStateCodecTest {
    @Test
    void foxMaskPreservesIndependentFlags() {
        int mask = BodyStateCodec.packFox(true, false, true, false, true, true);
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.FOX_CROUCHING));
        assertFalse(BodyStateCodec.has(mask, BodyStateCodec.FOX_SLEEPING));
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.FOX_INTERESTED));
        assertFalse(BodyStateCodec.has(mask, BodyStateCodec.FOX_LEAPING));
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.FOX_DEFENDING));
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.FOX_FACEPLANTED));
    }

    @Test
    void pandaMaskPreservesIndependentFlags() {
        int mask = BodyStateCodec.packPanda(true, false, true);
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.PANDA_ROLLING));
        assertFalse(BodyStateCodec.has(mask, BodyStateCodec.PANDA_SNEEZING));
        assertTrue(BodyStateCodec.has(mask, BodyStateCodec.PANDA_ON_BACK));
    }
}
