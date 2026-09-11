package dev.onelsey.incarnate.presentation;

import org.bukkit.entity.Entity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PresentationProxyRegistry {
    private static final ConcurrentMap<UUID, UUID> OWNERS = new ConcurrentHashMap<>();

    private PresentationProxyRegistry() {
    }

    public static void register(Entity entity, UUID ownerId) {
        if (entity != null && ownerId != null) {
            OWNERS.put(entity.getUniqueId(), ownerId);
        }
    }

    public static UUID owner(Entity entity) {
        return entity == null ? null : OWNERS.get(entity.getUniqueId());
    }

    public static boolean isProxy(Entity entity) {
        return owner(entity) != null;
    }

    public static void unregister(Entity entity) {
        if (entity != null) {
            OWNERS.remove(entity.getUniqueId());
        }
    }

    public static void unregister(UUID entityId) {
        if (entityId != null) {
            OWNERS.remove(entityId);
        }
    }
}
