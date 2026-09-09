package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

final class VesselRecoveryIndex {
    private final IncarnatePlugin plugin;
    private final Path indexPath;
    private final Set<UUID> vesselIds = new HashSet<>();

    VesselRecoveryIndex(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.indexPath = plugin.getDataFolder().toPath().resolve("vessel-recovery-index.txt");
        load();
    }

    synchronized Set<UUID> snapshot() {
        return Set.copyOf(vesselIds);
    }

    synchronized boolean mark(UUID vesselId) {
        if (!vesselIds.add(vesselId)) {
            return true;
        }
        if (persist()) {
            return true;
        }
        vesselIds.remove(vesselId);
        return false;
    }

    synchronized void forget(UUID vesselId) {
        if (!vesselIds.remove(vesselId)) {
            return;
        }
        if (!persist()) {
            vesselIds.add(vesselId);
        }
    }

    private void load() {
        synchronized (this) {
            vesselIds.clear();
            if (!Files.isRegularFile(indexPath)) {
                return;
            }

            try {
                for (String raw : Files.readAllLines(indexPath, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    try {
                        vesselIds.add(UUID.fromString(line));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Ignoring invalid vessel recovery UUID in " + indexPath.getFileName() + ": " + line);
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not read Incarnate vessel recovery index", ex);
            }
        }
    }

    private boolean persist() {
        try {
            Files.createDirectories(indexPath.getParent());
            if (vesselIds.isEmpty()) {
                Files.deleteIfExists(indexPath);
                Files.deleteIfExists(tempPath());
                return true;
            }

            List<String> lines = new ArrayList<>(vesselIds.size());
            vesselIds.stream().map(UUID::toString).sorted().forEach(lines::add);

            Path temp = tempPath();
            Files.write(
                temp,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );

            try {
                Files.move(temp, indexPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, indexPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not persist Incarnate vessel recovery index", ex);
            return false;
        }
    }

    private Path tempPath() {
        return indexPath.resolveSibling(indexPath.getFileName() + ".tmp");
    }
}
