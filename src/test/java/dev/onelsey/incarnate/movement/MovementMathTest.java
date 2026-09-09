package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MovementMathTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void forwardAtZeroYawPointsPositiveZ() {
        Vector vector = MovementMath.horizontal(input(true, false, false, false), 0.0f);
        assertEquals(0.0, vector.getX(), EPSILON);
        assertEquals(1.0, vector.getZ(), EPSILON);
    }

    @Test
    void rightAtZeroYawPointsNegativeX() {
        Vector vector = MovementMath.horizontal(input(false, false, false, true), 0.0f);
        assertEquals(-1.0, vector.getX(), EPSILON);
        assertEquals(0.0, vector.getZ(), EPSILON);
    }

    @Test
    void leftAtZeroYawPointsPositiveX() {
        Vector vector = MovementMath.horizontal(input(false, false, true, false), 0.0f);
        assertEquals(1.0, vector.getX(), EPSILON);
        assertEquals(0.0, vector.getZ(), EPSILON);
    }

    @Test
    void diagonalGroundInputIsNormalized() {
        Vector vector = MovementMath.horizontal(input(true, false, false, true), 0.0f);
        assertEquals(1.0, vector.length(), EPSILON);
    }

    @Test
    void threeDimensionalInputNeverExceedsUnitLength() {
        InputSnapshot input = new InputSnapshot(true, false, false, true, true, false, false);
        Vector vector = MovementMath.threeDimensional(input, new ViewSnapshot(35.0f, -20.0f), 1.0);
        assertTrue(vector.length() <= 1.0 + EPSILON);
    }

    @Test
    void dragonBodyYawCompensatesForVanillaDragonFacing() {
        assertEquals(-180.0f, MovementMath.dragonBodyYaw(0.0f), 0.0001f);
        assertEquals(-90.0f, MovementMath.dragonBodyYaw(90.0f), 0.0001f);
        assertEquals(90.0f, MovementMath.dragonBodyYaw(-90.0f), 0.0001f);
        assertEquals(-1.0f, MovementMath.dragonBodyYaw(179.0f), 0.0001f);
        assertEquals(1.0f, MovementMath.dragonBodyYaw(-179.0f), 0.0001f);
    }

    @Test
    void pitchIsClamped() {
        assertEquals(90.0f, MovementMath.clampPitch(120.0f));
        assertEquals(-90.0f, MovementMath.clampPitch(-120.0f));
    }

    @Test
    void lerpClampsFactor() {
        assertEquals(10.0, MovementMath.lerp(0.0, 10.0, 2.0), EPSILON);
        assertEquals(0.0, MovementMath.lerp(0.0, 10.0, -1.0), EPSILON);
    }

    private static InputSnapshot input(boolean forward, boolean backward, boolean left, boolean right) {
        return new InputSnapshot(forward, backward, left, right, false, false, false);
    }
}
