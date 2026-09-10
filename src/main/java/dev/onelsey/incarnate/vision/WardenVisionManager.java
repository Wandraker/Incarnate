package dev.onelsey.incarnate.vision;

import dev.onelsey.incarnate.IncarnatePlugin;
import dev.onelsey.incarnate.possession.PossessionSession;
import dev.onelsey.incarnate.sense.VesselPositionSnapshot;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WardenVisionManager {
    private final IncarnatePlugin plugin;
    private final boolean enabled;
    private final double entityHardLimit;
    private final double pruneDistance;
    private final int evaluationIntervalTicks;
    private final boolean visualDarkness;
    private final int visualRefreshTicks;
    private final int visualEffectDurationTicks;
    private final Map<UUID, VisionState> states = new ConcurrentHashMap<>();

    public WardenVisionManager(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("vision.warden.enabled", true);
        this.entityHardLimit = Math.max(2.0, plugin.getConfig().getDouble("vision.warden.entity-hard-limit", 12.0));
        this.pruneDistance = Math.max(entityHardLimit + 32.0, plugin.getConfig().getDouble("vision.warden.watch-prune-distance", 256.0));
        this.evaluationIntervalTicks = Math.max(1, plugin.getConfig().getInt("vision.warden.evaluation-interval-ticks", 2));
        this.visualDarkness = plugin.getConfig().getBoolean("vision.warden.visual-darkness", true);
        this.visualRefreshTicks = Math.max(20, plugin.getConfig().getInt("vision.warden.visual-refresh-ticks", 100));
        this.visualEffectDurationTicks = Math.max(40, visualRefreshTicks + 40);
    }

    public void ensureActive(PossessionSession session) {
        if (!applies(session)) {
            if (session != null) {
                deactivate(session.player(), true);
            }
            return;
        }

        UUID playerId = session.playerId();
        VisionState current = states.get(playerId);
        if (current != null && current.session == session) {
            return;
        }
        if (current != null) {
            removeState(playerId, current, true);
        }

        VisionState state = new VisionState(session);
        states.put(playerId, state);
        scheduleVisualRefresh(state, 0L);

        ScheduledTask guard = session.player().getScheduler().runAtFixedRate(plugin, task -> {
            if (states.get(playerId) != state) {
                task.cancel();
                return;
            }
            if (!session.isActive() || !session.player().isOnline() || session.vesselType() != EntityType.WARDEN) {
                task.cancel();
                removeState(playerId, state, session.player().isOnline());
                return;
            }
            state.guardTicks += 5;
            if (visualDarkness && state.guardTicks >= visualRefreshTicks) {
                state.guardTicks = 0;
                applyVisualDarkness(session.player());
            }
        }, () -> states.remove(playerId, state), 1L, 5L);
        state.guardTask = guard;
    }

    public boolean filterTrack(PossessionSession session, Player viewer, Entity entity) {
        if (!applies(session) || !viewer.getUniqueId().equals(session.playerId())) {
            return false;
        }
        ensureActive(session);
        VisionState state = states.get(session.playerId());
        if (state == null || state.session != session || exempt(session, viewer, entity)) {
            return false;
        }

        TargetState target = state.targets.computeIfAbsent(entity.getUniqueId(), ignored -> new TargetState(entity));
        if (target.retired.get()) {
            return true;
        }
        startWatcher(state, target);

        boolean hidden = initialHidden(session, entity);
        target.desiredHidden.set(hidden);
        if (hidden) {
            try {
                viewer.hideEntity(plugin, entity);
                target.appliedHidden.set(true);
            } catch (RuntimeException ignored) {
            }
            return true;
        }

        if (target.appliedHidden.get()) {
            queueVisibilityUpdate(state, target);
        }
        return false;
    }

    public void onUntrack(Player viewer, Entity entity) {
        VisionState state = states.get(viewer.getUniqueId());
        if (state == null) {
            return;
        }
        TargetState target = state.targets.get(entity.getUniqueId());
        if (target == null) {
            return;
        }
        if (!target.appliedHidden.get() && !target.desiredHidden.get()) {
            removeTarget(state, target, false);
        }
    }

    public void deactivate(Player player, boolean restoreClient) {
        VisionState state = states.remove(player.getUniqueId());
        if (state != null) {
            cleanupState(state, restoreClient);
        }
    }

    public void stop() {
        for (Map.Entry<UUID, VisionState> entry : Map.copyOf(states).entrySet()) {
            VisionState state = entry.getValue();
            if (states.remove(entry.getKey(), state)) {
                cleanupState(state, state.session.player().isOnline());
            }
        }
    }

    private boolean applies(PossessionSession session) {
        return enabled && session != null && session.isActive() && session.vesselType() == EntityType.WARDEN;
    }

    private static boolean exempt(PossessionSession session, Player viewer, Entity entity) {
        UUID entityId = entity.getUniqueId();
        return entityId.equals(viewer.getUniqueId()) || entityId.equals(session.vesselId());
    }

    private boolean initialHidden(PossessionSession session, Entity entity) {
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            return true;
        }
        VesselPositionSnapshot origin = session.vesselPosition();
        if (origin == null) {
            return true;
        }
        Location target = entity.getLocation();
        UUID targetWorld = target.getWorld() == null ? null : target.getWorld().getUID();
        boolean sameWorld = targetWorld != null && origin.worldId().equals(targetWorld);
        return WardenVisionMath.shouldHide(
            sameWorld,
            target.getX() - origin.x(),
            target.getY() - origin.y(),
            target.getZ() - origin.z(),
            entityHardLimit
        );
    }

    private void startWatcher(VisionState state, TargetState target) {
        if (!target.watcherStarted.compareAndSet(false, true)) {
            return;
        }
        PossessionSession session = state.session;
        Entity entity = target.entity;
        ScheduledTask watcher = entity.getScheduler().runAtFixedRate(plugin, task -> {
            if (states.get(session.playerId()) != state || !session.isActive()) {
                task.cancel();
                return;
            }
            if (!entity.isValid()) {
                task.cancel();
                retireTarget(state, target);
                return;
            }

            VesselPositionSnapshot origin = session.vesselPosition();
            if (origin == null) {
                updateDesired(state, target, true);
                return;
            }

            Location location = entity.getLocation();
            UUID targetWorld = location.getWorld() == null ? null : location.getWorld().getUID();
            boolean sameWorld = targetWorld != null && origin.worldId().equals(targetWorld);
            double dx = location.getX() - origin.x();
            double dy = location.getY() - origin.y();
            double dz = location.getZ() - origin.z();
            boolean hidden = WardenVisionMath.shouldHide(sameWorld, dx, dy, dz, entityHardLimit);
            updateDesired(state, target, hidden);

            if (hidden && (!sameWorld || !WardenVisionMath.withinRadius(dx, dy, dz, pruneDistance))) {
                retireTarget(state, target);
            }
        }, () -> retireTarget(state, target), 1L, evaluationIntervalTicks);
        target.watcherTask = watcher;
        if (watcher == null) {
            target.watcherStarted.set(false);
        }
    }

    private void updateDesired(VisionState state, TargetState target, boolean hidden) {
        if (target.retired.get()) {
            return;
        }
        boolean previous = target.desiredHidden.getAndSet(hidden);
        if (previous != hidden || target.appliedHidden.get() != hidden) {
            queueVisibilityUpdate(state, target);
        }
    }

    private void retireTarget(VisionState state, TargetState target) {
        if (!target.retired.compareAndSet(false, true)) {
            return;
        }
        target.desiredHidden.set(false);
        ScheduledTask watcher = target.watcherTask;
        if (watcher != null) {
            watcher.cancel();
        }
        if (target.appliedHidden.get()) {
            queueVisibilityUpdate(state, target);
        } else {
            removeTarget(state, target, false);
        }
    }

    private void queueVisibilityUpdate(VisionState state, TargetState target) {
        if (!target.updateQueued.compareAndSet(false, true)) {
            return;
        }
        Player player = state.session.player();
        ScheduledTask task = player.getScheduler().run(plugin, scheduled -> {
            try {
                if (states.get(state.session.playerId()) != state || !state.session.isActive() || !player.isOnline()) {
                    return;
                }
                boolean hidden = target.desiredHidden.get();
                try {
                    if (hidden) {
                        player.hideEntity(plugin, target.entity);
                    } else {
                        player.showEntity(plugin, target.entity);
                    }
                    target.appliedHidden.set(hidden);
                } catch (RuntimeException ignored) {
                }
            } finally {
                target.updateQueued.set(false);
                if (target.retired.get() && !target.appliedHidden.get()) {
                    removeTarget(state, target, false);
                    return;
                }
                if (states.get(state.session.playerId()) == state
                    && target.desiredHidden.get() != target.appliedHidden.get()) {
                    queueVisibilityUpdate(state, target);
                }
            }
        }, () -> target.updateQueued.set(false));
        if (task == null) {
            target.updateQueued.set(false);
        }
    }

    private void removeState(UUID playerId, VisionState state, boolean restoreClient) {
        if (states.remove(playerId, state)) {
            cleanupState(state, restoreClient);
        }
    }

    private void cleanupState(VisionState state, boolean restoreClient) {
        ScheduledTask guard = state.guardTask;
        if (guard != null) {
            guard.cancel();
        }
        for (TargetState target : state.targets.values()) {
            ScheduledTask watcher = target.watcherTask;
            if (watcher != null) {
                watcher.cancel();
            }
        }

        Player player = state.session.player();
        if (!restoreClient || !player.isOnline()) {
            state.targets.clear();
            return;
        }

        player.getScheduler().run(plugin, task -> {
            for (TargetState target : state.targets.values()) {
                if (target.appliedHidden.get() || target.desiredHidden.get()) {
                    try {
                        player.showEntity(plugin, target.entity);
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            state.targets.clear();
            restoreRealDarkness(player);
        }, () -> state.targets.clear());
    }

    private void removeTarget(VisionState state, TargetState target, boolean keepVisibilityUpdate) {
        if (!state.targets.remove(target.entity.getUniqueId(), target)) {
            return;
        }
        ScheduledTask watcher = target.watcherTask;
        if (watcher != null) {
            watcher.cancel();
        }
        if (!keepVisibilityUpdate) {
            target.updateQueued.set(false);
        }
    }

    private void scheduleVisualRefresh(VisionState state, long delayTicks) {
        if (!visualDarkness) {
            return;
        }
        Player player = state.session.player();
        player.getScheduler().runDelayed(plugin, task -> {
            if (states.get(state.session.playerId()) == state && state.session.isActive() && player.isOnline()) {
                applyVisualDarkness(player);
            }
        }, null, Math.max(1L, delayTicks + 1L));
    }

    private void applyVisualDarkness(Player player) {
        PotionEffect effect = new PotionEffect(
            PotionEffectType.DARKNESS,
            visualEffectDurationTicks,
            0,
            false,
            false,
            false
        );
        player.sendPotionEffectChange(player, effect);
    }

    private void restoreRealDarkness(Player player) {
        PotionEffect actual = player.getPotionEffect(PotionEffectType.DARKNESS);
        if (actual == null) {
            player.sendPotionEffectChangeRemove(player, PotionEffectType.DARKNESS);
        } else {
            player.sendPotionEffectChange(player, actual);
        }
    }

    private static final class VisionState {
        private final PossessionSession session;
        private final Map<UUID, TargetState> targets = new ConcurrentHashMap<>();
        private volatile ScheduledTask guardTask;
        private int guardTicks;

        private VisionState(PossessionSession session) {
            this.session = session;
        }
    }

    private static final class TargetState {
        private final Entity entity;
        private final AtomicBoolean watcherStarted = new AtomicBoolean(false);
        private final AtomicBoolean desiredHidden = new AtomicBoolean(false);
        private final AtomicBoolean appliedHidden = new AtomicBoolean(false);
        private final AtomicBoolean updateQueued = new AtomicBoolean(false);
        private final AtomicBoolean retired = new AtomicBoolean(false);
        private volatile ScheduledTask watcherTask;

        private TargetState(Entity entity) {
            this.entity = entity;
        }
    }
}
