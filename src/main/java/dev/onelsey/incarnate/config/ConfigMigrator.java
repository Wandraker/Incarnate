package dev.onelsey.incarnate.config;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigMigrator {
    private ConfigMigrator() {
    }

    public static void prepare(IncarnatePlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.isFile()) {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            return;
        }

        YamlConfiguration user = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration defaults = loadResource(plugin, "config.yml");
        boolean changed = mergeMissing(user, defaults);

        if (changed) {
            backup(configFile);
            atomicSave(user, configFile);
            plugin.getLogger().info("Updated config.yml with new Incarnate defaults without replacing existing values. A backup was created.");
        }
        plugin.reloadConfig();
    }

    private static boolean mergeMissing(YamlConfiguration target, YamlConfiguration defaults) {
        boolean changed = false;
        for (String path : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(path) || target.contains(path)) {
                continue;
            }
            target.set(path, defaults.get(path));
            changed = true;
        }
        return changed;
    }

    private static YamlConfiguration loadResource(IncarnatePlugin plugin, String path) {
        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled resource: " + path);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read bundled resource: " + path, ex);
        }
    }

    private static void backup(File configFile) {
        try {
            File backupDir = new File(configFile.getParentFile(), "backups");
            Files.createDirectories(backupDir.toPath());
            File backup = new File(backupDir, "config-pre-migration-" + System.currentTimeMillis() + ".yml");
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create config.yml migration backup", ex);
        }
    }

    private static void atomicSave(YamlConfiguration yaml, File destination) {
        File temp = new File(destination.getParentFile(), destination.getName() + ".tmp");
        try {
            yaml.save(temp);
            try {
                Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save migrated config.yml", ex);
        } finally {
            try {
                Files.deleteIfExists(temp.toPath());
            } catch (IOException ignored) {
            }
        }
    }
}
