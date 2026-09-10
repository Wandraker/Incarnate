package dev.onelsey.incarnate.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WardenSonicMathTest {
    @Test
    void resistanceScalesVanillaKnockbackRange() {
        assertEquals(1.0, WardenSonicMath.resistanceScale(0.0));
        assertEquals(0.5, WardenSonicMath.resistanceScale(0.5));
        assertEquals(0.0, WardenSonicMath.resistanceScale(1.0));
        assertEquals(0.0, WardenSonicMath.resistanceScale(2.0));
        assertEquals(1.0, WardenSonicMath.resistanceScale(-1.0));
        assertEquals(1.0, WardenSonicMath.resistanceScale(Double.NaN));
    }
}
