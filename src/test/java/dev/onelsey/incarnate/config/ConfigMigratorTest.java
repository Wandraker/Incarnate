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
        defaults.set("config-version", 4);
        defaults.set("movement.ground.maximum-walk-speed", 0.55);
        defaults.set("abilities.skeleton.cooldown-ticks", 12);
        defaults.set("control.release-at-vessel", true);
        defaults.set("camera.mode", "MOUNTED");
        defaults.set("camera.mount-retries", 8);

        assertTrue(ConfigMigrator.mergeMissing(user, defaults));
        assertEquals(0.41, user.getDouble("movement.ground.maximum-walk-speed"));
        assertEquals(27, user.getInt("abilities.skeleton.cooldown-ticks"));
        assertFalse(user.getBoolean("control.release-at-vessel"));
        assertEquals("MOUNTED", user.getString("camera.mode"));
        assertEquals(8, user.getInt("camera.mount-retries"));
        assertEquals(4, user.getInt("config-version"));
    }

    @Test
    void mergeIsNoOpOnceAllKeysExist() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 4);
        user.set("camera.mode", "SPECTATOR_TARGET");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("config-version", 4);
        defaults.set("camera.mode", "MOUNTED");

        assertFalse(ConfigMigrator.mergeMissing(user, defaults));
        assertEquals("SPECTATOR_TARGET", user.getString("camera.mode"));
    }

    @Test
    void schemaMigrationAdvancesVersionAndUnlocksLegacyDefaultExclusions() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 1);
        user.set("excluded-types", java.util.List.of("ENDER_DRAGON", "SHULKER"));

        assertTrue(ConfigMigrator.migrateSchema(user, 1));
        assertEquals(4, user.getInt("config-version"));
        assertTrue(user.getStringList("excluded-types").isEmpty());
    }

    @Test
    void schemaTwoMigrationAdvancesWithoutTouchingExistingValues() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 2);
        user.set("input.gestures.sneak-primary", false);

        assertTrue(ConfigMigrator.migrateSchema(user, 2));
        assertEquals(4, user.getInt("config-version"));
        assertFalse(user.getBoolean("input.gestures.sneak-primary"));
    }

    @Test
    void schemaMigrationPreservesCustomizedExclusions() {
        YamlConfiguration user = new YamlConfiguration();
        user.set("config-version", 1);
        user.set("excluded-types", java.util.List.of("ENDER_DRAGON", "SHULKER", "WARDEN"));

        assertTrue(ConfigMigrator.migrateSchema(user, 1));
        assertEquals(4, user.getInt("config-version"));
        assertEquals(java.util.List.of("ENDER_DRAGON", "SHULKER", "WARDEN"), user.getStringList("excluded-types"));
    }
}
