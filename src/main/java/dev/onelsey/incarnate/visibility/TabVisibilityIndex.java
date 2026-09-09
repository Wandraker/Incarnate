package dev.onelsey.incarnate.visibility;

import dev.onelsey.incarnate.IncarnatePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

final class TabVisibilityIndex {
    record Entry(UUID controllerId, UUID viewerId) {
    }

    private final IncarnatePlugin plugin;
    private final Path indexPath;
    private final Set<Entry> entries = new HashSet<>();

    TabVisibilityIndex(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.indexPath = plugin.getDataFolder().toPath().resolve("tab-visibility-index.txt");
        load();
    }

    synchronized Set<Entry> snapshot() {
        return Set.copyOf(entries);
    }

    synchronized void mark(UUID controllerId, UUID viewerId) {
        Entry entry = new Entry(controllerId, viewerId);
        if (!entries.add(entry)) {
            return;
        }
        if (!persist()) {
            entries.remove(entry);
        }
    }

    synchronized void forget(UUID controllerId, UUID viewerId) {
        Entry entry = new Entry(controllerId, viewerId);
        if (!entries.remove(entry)) {
            return;
        }
        if (!persist()) {
            entries.add(entry);
        }
    }

    synchronized void forgetController(UUID controllerId) {
        Set<Entry> removed = new HashSet<>();
        entries.removeIf(entry -> {
            if (entry.controllerId().equals(controllerId)) {
                removed.add(entry);
                return true;
            }
            return false;
        });
        if (removed.isEmpty()) {
            return;
        }
        if (!persist()) {
            entries.addAll(removed);
        }
    }

    private void load() {
        synchronized (this) {
            entries.clear();
            if (!Files.isRegularFile(indexPath)) {
                return;
            }

            try {
                for (String raw : Files.readAllLines(indexPath, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length != 2) {
                        plugin.getLogger().warning("Ignoring invalid tab visibility entry: " + line);
                        continue;
                    }
                    try {
                        entries.add(new Entry(UUID.fromString(parts[0]), UUID.fromString(parts[1])));
                    } catch (IllegalArgumentException ex) {
                        plugin.getLogger().warning("Ignoring invalid tab visibility UUID entry: " + line);
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not read Incarnate tab visibility index", ex);
            }
        }
    }

    private boolean persist() {
        try {
            Files.createDirectories(indexPath.getParent());
            Path temp = tempPath();
            if (entries.isEmpty()) {
                Files.deleteIfExists(indexPath);
                Files.deleteIfExists(temp);
                return true;
            }

            List<String> lines = entries.stream()
                .map(entry -> entry.controllerId() + " " + entry.viewerId())
                .sorted()
                .toList();
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
            plugin.getLogger().log(Level.SEVERE, "Could not persist Incarnate tab visibility index", ex);
            return false;
        }
    }

    private Path tempPath() {
        return indexPath.resolveSibling(indexPath.getFileName() + ".tmp");
    }
}
