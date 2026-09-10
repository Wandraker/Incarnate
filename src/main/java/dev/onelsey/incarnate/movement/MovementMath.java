package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import org.bukkit.util.Vector;

final class MovementMath {
    private MovementMath() {
    }

    static Vector horizontal(InputSnapshot input, float yaw) {
        double forward = input.forwardAxis();
        double strafe = input.strafeAxis();
        double length = Math.hypot(forward, strafe);
        if (length < 0.0001) {
            return new Vector();
        }

        if (length > 1.0) {
            forward /= length;
            strafe /= length;
        }

        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Vector(
            (-sin * forward) - (cos * strafe),
            0.0,
            (cos * forward) - (sin * strafe)
        );
    }

    static Vector threeDimensional(InputSnapshot input, ViewSnapshot view, double verticalInput) {
        Vector forward = direction(view.yaw(), view.pitch());
        Vector right = direction(view.yaw() + 90.0f, 0.0f);
        Vector result = forward.multiply(input.forwardAxis()).add(right.multiply(input.strafeAxis()));
        result.setY(result.getY() + verticalInput);
        if (result.lengthSquared() > 1.0) {
            result.normalize();
        }
        return result;
    }

    static Vector direction(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(clampPitch(pitch));
        double cosPitch = Math.cos(pitchRadians);
        return new Vector(
            -Math.sin(yawRadians) * cosPitch,
            -Math.sin(pitchRadians),
            Math.cos(yawRadians) * cosPitch
        );
    }

    static float dragonBodyYaw(float controllerYaw) {
        return wrapDegrees(controllerYaw + 180.0f);
    }

    static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    static float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    static double lerp(double from, double to, double factor) {
        return from + ((to - from) * clamp01(factor));
    }

    static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
