package dev.onelsey.incarnate.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {
    @Test
    void mergeAddsMissingDefaultsWithoutOverwritingUserValues() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("movement.ground.maximum-walk-speed", 0.41);
        user.set("abilities.skeleton.cooldown-ticks", 27);
        user.set("control.release-at-vessel", false);

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 13);
        defaults.set("movement.ground.maximum-walk-speed", 0.55);
        defaults.set("abilities.skeleton.cooldown-ticks", 12);
        defaults.set("control.release-at-vessel", true);
        defaults.set("camera.mode", "CONTROLLER_SHADOW");
        defaults.set("camera.attach-retries", 8);
        defaults.set("camera.shadow.hard-snap-distance", 1.25);
        defaults.set("vision.warden.enabled", true);
        defaults.set("vision.warden.entity-hard-limit", 12.0);
        defaults.set("abilities.melee.aim-assist-radius", 0.35);

        assertTrue(ConfigMigrator.mergeMissing(user, defaults));
        assertEquals(0.41, user.getDouble("movement.ground.maximum-walk-speed"));
        assertEquals(27, user.getInt("abilities.skeleton.cooldown-ticks"));
        assertFalse(user.getBoolean("control.release-at-vessel"));
        assertEquals("CONTROLLER_SHADOW", user.getString("camera.mode"));
        assertEquals(8, user.getInt("camera.attach-retries"));
        assertEquals(1.25, user.getDouble("camera.shadow.hard-snap-distance"));
        assertTrue(user.getBoolean("vision.warden.enabled"));
        assertEquals(12.0, user.getDouble("vision.warden.entity-hard-limit"));
        assertEquals(0.35, user.getDouble("abilities.melee.aim-assist-radius"));
        assertEquals(13, user.getInt("config-version"));
    }

    @Test
    void mergeIsNoOpOnceAllKeysExist() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 12);
        user.set("camera.mode", "SPECTATOR_TARGET");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 13);
        defaults.set("camera.mode", "CONTROLLER_SHADOW");

        assertFalse(ConfigMigrator.mergeMissing(user, defaults));
        assertEquals("SPECTATOR_TARGET", user.getString("camera.mode"));
    }

    @Test
    void schemaMigrationAdvancesVersionAndUnlocksLegacyDefaultExclusions() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 1);
        user.set("excluded-types", java.util.List.of("ENDER_DRAGON", "SHULKER"));

        assertTrue(ConfigMigrator.migrateSchema(user, 1));
        assertEquals(13, user.getInt("config-version"));
        assertTrue(user.getStringList("excluded-types").isEmpty());
    }

    @Test
    void schemaTwoMigrationAdvancesWithoutTouchingExistingValues() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 2);
        user.set("input.gestures.sneak-primary", false);

        assertTrue(ConfigMigrator.migrateSchema(user, 2));
        assertEquals(13, user.getInt("config-version"));
        assertFalse(user.getBoolean("input.gestures.sneak-primary"));
    }

    @Test
    void schemaTenMovesMountedDefaultToControllerShadow() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 10);
        user.set("camera.mode", "MOUNTED");
        user.set("camera.mount-retries", 13);

        assertTrue(ConfigMigrator.migrateSchema(user, 10));
        assertEquals(13, user.getInt("config-version"));
        assertEquals("CONTROLLER_SHADOW", user.getString("camera.mode"));
        assertEquals(13, user.getInt("camera.attach-retries"));
        assertFalse(user.contains("camera.mount-retries"));
    }

    @Test
    void schemaElevenMovesDirectEntityDefaultToControllerShadow() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 11);
        user.set("camera.mode", "DIRECT_ENTITY");

        assertTrue(ConfigMigrator.migrateSchema(user, 11));
        assertEquals(13, user.getInt("config-version"));
        assertEquals("CONTROLLER_SHADOW", user.getString("camera.mode"));
    }

    @Test
    void schemaTwelveMovesCameraRigToControllerShadowAndKeepsTuning() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 12);
        user.set("camera.mode", "CAMERA_RIG");
        user.set("camera.rig.vertical-offset", 0.15);
        user.set("camera.rig.adaptive-third-person-distance", false);
        user.set("camera.rig.third-person-base", 3.0);
        user.set("camera.rig.third-person-body-scale", 1.6);
        user.set("camera.rig.third-person-minimum", 5.0);
        user.set("camera.rig.third-person-maximum", 28.0);

        assertTrue(ConfigMigrator.migrateSchema(user, 12));
        assertEquals(13, user.getInt("config-version"));
        assertEquals("CONTROLLER_SHADOW", user.getString("camera.mode"));
        assertEquals(0.15, user.getDouble("camera.shadow.vertical-offset"));
        assertFalse(user.getBoolean("camera.shadow.adaptive-third-person-distance"));
        assertEquals(3.0, user.getDouble("camera.shadow.third-person-base"));
        assertEquals(1.6, user.getDouble("camera.shadow.third-person-body-scale"));
        assertEquals(5.0, user.getDouble("camera.shadow.third-person-minimum"));
        assertEquals(28.0, user.getDouble("camera.shadow.third-person-maximum"));
        assertFalse(user.contains("camera.rig"));
    }

    @Test
    void schemaMigrationPreservesCustomizedExclusions() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 1);
        user.set("excluded-types", java.util.List.of("ENDER_DRAGON", "SHULKER", "WARDEN"));

        assertTrue(ConfigMigrator.migrateSchema(user, 1));
        assertEquals(13, user.getInt("config-version"));
        assertEquals(java.util.List.of("ENDER_DRAGON", "SHULKER", "WARDEN"), user.getStringList("excluded-types"));
    }
}
