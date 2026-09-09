package dev.onelsey.incarnate.sense;

import java.util.Locale;

public final class WardenSenseMath {
    private WardenSenseMath() {
    }

    public static String directionKey(float yaw, double dx, double dz) {
        if (dx * dx + dz * dz < 1.0E-8) {
            return "near";
        }

        double yawRadians = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = -Math.cos(yawRadians);
        double rightZ = -Math.sin(yawRadians);

        double forward = dx * forwardX + dz * forwardZ;
        double right = dx * rightX + dz * rightZ;
        double angle = Math.toDegrees(Math.atan2(right, forward));
        int sector = Math.floorMod((int) Math.floor((angle + 22.5) / 45.0), 8);

        return switch (sector) {
            case 0 -> "front";
            case 1 -> "front-right";
            case 2 -> "right";
            case 3 -> "back-right";
            case 4 -> "back";
            case 5 -> "back-left";
            case 6 -> "left";
            case 7 -> "front-left";
            default -> "near";
        };
    }

    public static String kindKey(String eventKey) {
        String key = eventKey == null ? "" : eventKey.toLowerCase(Locale.ROOT);
        if (containsAny(key, "step", "swim", "splash", "flap", "hit_ground", "elytra_glide")) {
            return "movement";
        }
        if (containsAny(key, "projectile_shoot", "projectile_land")) {
            return "projectile";
        }
        if (containsAny(key, "entity_damage", "entity_die", "explode")) {
            return "combat";
        }
        if (key.startsWith("block_") || containsAny(key, "fluid_pickup", "fluid_place")) {
            return "block";
        }
        if (containsAny(key, "container_", "item_interact", "equip", "unequip", "eat", "drink", "shear", "teleport")) {
            return "action";
        }
        return "vibration";
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
