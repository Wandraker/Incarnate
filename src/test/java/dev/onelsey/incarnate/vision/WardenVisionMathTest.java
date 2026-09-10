package dev.onelsey.incarnate.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardenVisionMathTest {
    @Test
    void keepsEntitiesInsideHardRadiusVisible() {
        assertFalse(WardenVisionMath.shouldHide(true, 3.0, 4.0, 0.0, 5.0));
    }

    @Test
    void treatsBoundaryAsVisible() {
        assertTrue(WardenVisionMath.withinRadius(0.0, 0.0, 12.0, 12.0));
    }

    @Test
    void hidesEntitiesOutsideHardRadius() {
        assertTrue(WardenVisionMath.shouldHide(true, 0.0, 0.0, 12.01, 12.0));
    }

    @Test
    void hidesEntitiesFromOtherWorlds() {
        assertTrue(WardenVisionMath.shouldHide(false, 0.0, 0.0, 0.0, 12.0));
    }
}
