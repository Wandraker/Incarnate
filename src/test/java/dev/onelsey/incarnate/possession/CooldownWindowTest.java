package dev.onelsey.incarnate.possession;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownWindowTest {
    @Test
    void firstUseIsImmediatelyAvailableAndRemainingIsObservable() {
        CooldownWindow cooldown = new CooldownWindow();

        assertTrue(cooldown.tryAcquire(0, 10));
        assertEquals(10, cooldown.remaining(0));
        assertEquals(5, cooldown.remaining(5));
        assertFalse(cooldown.tryAcquire(9, 10));
        assertEquals(1, cooldown.remaining(9));
        assertTrue(cooldown.tryAcquire(10, 10));
        assertEquals(10, cooldown.remaining(10));
    }

    @Test
    void cooldownHasAtLeastOneTick() {
        CooldownWindow cooldown = new CooldownWindow();

        assertTrue(cooldown.tryAcquire(25, 0));
        assertEquals(1, cooldown.remaining(25));
        assertFalse(cooldown.tryAcquire(25, 0));
        assertTrue(cooldown.tryAcquire(26, 0));
    }
}
