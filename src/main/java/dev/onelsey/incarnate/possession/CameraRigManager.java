package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import dev.onelsey.incarnate.presentation.PresentationProxyIsolationListener;
import dev.onelsey.incarnate.presentation.PresentationProxyRegistry;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Pose;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CameraRigManager {
    private final IncarnatePlugin plugin;
    private final Consumer<PossessionSession> failureHandler;
    private final NamespacedKey cameraDistanceKey;
    private final NamespacedKey shadowCameraDistanceKey;
    private final NamespacedKey legacyCameraDistanceKey;
    private final Pose controllerPose;
    private final double verticalOffset;
    private final double hardSnapDistance;
    private final double correctionGain;
    private final double maxCorrectionSpeed;
    private final double bodyOffsetBase;
    private final double bodyOffsetScale;
    private final double bodyOffsetMinimum;
    private final double bodyOffsetMaximum;
    private final boolean adaptiveThirdPersonDistance;
    private final double thirdPersonBase;
    private final double thirdPersonBodyScale;
    private final double thirdPersonMinimum;
    private final double thirdPersonMaximum;

    private final Map<UUID, VesselCameraSnapshot> vesselSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, CameraSnapshot> cameraSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastControllerTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastProxyTargets = new ConcurrentHashMap<>();
    private final Map<UUID, Entity> proxies = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> vesselSnapshotTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> proxyTasks = new ConcurrentHashMap<>();
    private final Set<UUID> proxyTeleporting = ConcurrentHashMap.newKeySet();
    private final Set<UUID> proxyShown = ConcurrentHashMap.newKeySet();

    public CameraRigManager(IncarnatePlugin plugin, Consumer<PossessionSession> failureHandler) {
        this.plugin = plugin;
        this.failureHandler = failureHandler;
        this.cameraDistanceKey = new NamespacedKey(plugin, "presentation_proxy_camera_distance");
        this.shadowCameraDistanceKey = new NamespacedKey(plugin, "camera_shadow_distance");
        this.legacyCameraDistanceKey = new NamespacedKey(plugin, "camera_rig_distance");
        this.controllerPose = parsePose(plugin.getConfig().getString("camera.presentation.controller-pose", "SWIMMING"));
        this.verticalOffset = plugin.getConfig().getDouble("camera.presentation.vertical-offset", 0.0);
        this.hardSnapDistance = Math.max(0.25, plugin.getConfig().getDouble("camera.presentation.hard-snap-distance", 1.25));
        this.correctionGain = clamp(plugin.getConfig().getDouble("camera.presentation.correction-gain", 0.75), 0.0, 1.0);
        this.maxCorrectionSpeed = Math.max(0.01, plugin.getConfig().getDouble("camera.presentation.max-correction-speed", 0.45));
        this.bodyOffsetBase = Math.max(0.0, plugin.getConfig().getDouble("camera.presentation.body-offset-base", 0.15));
        this.bodyOffsetScale = Math.max(0.0, plugin.getConfig().getDouble("camera.presentation.body-offset-scale", 0.85));
        this.bodyOffsetMinimum = Math.max(0.0, plugin.getConfig().getDouble("camera.presentation.body-offset-minimum", 0.50));
        this.bodyOffsetMaximum = Math.max(this.bodyOffsetMinimum, plugin.getConfig().getDouble("camera.presentation.body-offset-maximum", 1.50));
        this.adaptiveThirdPersonDistance = plugin.getConfig().getBoolean("camera.presentation.adaptive-third-person-distance", true);
        this.thirdPersonBase = plugin.getConfig().getDouble("camera.presentation.third-person-base", 2.5);
        this.thirdPersonBodyScale = plugin.getConfig().getDouble("camera.presentation.third-person-body-scale", 1.35);
        this.thirdPersonMinimum = Math.max(1.0, plugin.getConfig().getDouble("camera.presentation.third-person-minimum", 4.0));
        this.thirdPersonMaximum = Math.max(this.thirdPersonMinimum, plugin.getConfig().getDouble("camera.presentation.third-person-maximum", 32.0));
        plugin.getServer().getPluginManager().registerEvents(new PresentationProxyIsolationListener(), plugin);
    }

    public boolean attach(PossessionSession session) {
        Player player = session.player();
        Mob vessel = session.vessel();
        if (!session.isActive() || !player.isOnline() || player.isInsideVehicle()) {
            return false;
        }
        if (vessel.getType() == EntityType.ENDER_DRAGON || vessel.getType() == EntityType.WITHER) {
            plugin.getLogger().warning("Presentation-proxy camera is not enabled for boss vessel " + vessel.getType() + " yet.");
            return false;
        }

        session.cameraRigId(null);
        session.cameraTransport(CameraTransport.CAMERA_RIG);
        session.endCameraRigTeleport();
        prepareController(player);
        applyThirdPersonDistance(player, session);
        player.hideEntity(plugin, vessel);

        VesselCameraSnapshot initial = new VesselCameraSnapshot(
            vessel.getWorld().getUID(),
            session.lastKnownVesselLocation().getX(),
            session.lastKnownVesselLocation().getY(),
            session.lastKnownVesselLocation().getZ(),
            session.vesselEyeHeight(),
            session.vesselWidth(),
            session.vesselHeight(),
            vessel.getPose()
        );
        vesselSnapshots.put(session.playerId(), initial);

        Location target = controllerDestination(initial, player);
        if (target == null) {
            clearThirdPersonDistance(player);
            player.showEntity(plugin, vessel);
            return false;
        }
        lastControllerTargets.put(session.playerId(), target.clone());
        snapControllerToTarget(session, player, target);
        publishCameraSnapshot(session, player);

        ScheduledTask playerTask = player.getScheduler().runAtFixedRate(
            plugin,
            ignored -> tickController(session),
            () -> fail(session),
            1L,
            1L
        );
        session.cameraRigTask(playerTask);
        if (playerTask == null) {
            detach(session, player);
            return false;
        }

        ScheduledTask vesselTask = vessel.getScheduler().runAtFixedRate(
            plugin,
            task -> tickVesselSnapshot(session, vessel, task),
            () -> {
                if (session.isActive()) {
                    fail(session);
                }
            },
            1L,
            1L
        );
        if (vesselTask == null) {
            detach(session, player);
            return false;
        }
        vesselSnapshotTasks.put(session.playerId(), vesselTask);
        return true;
    }

    private void prepareController(Player player) {
        player.setNoPhysics(true);
        player.setPose(controllerPose, true);
        player.setAllowFlight(true);
        player.setFlySpeed(0.0f);
        if (!player.isFlying()) {
            player.setFlying(true);
        }
        player.setFallDistance(0.0f);
    }

    private void tickController(PossessionSession session) {
        if (!session.isActive()) {
            return;
        }
        Player player = session.player();
        if (!player.isOnline()) {
            fail(session);
            return;
        }
        if (!Bukkit.isOwnedByCurrentRegion(player)) {
            return;
        }

        prepareController(player);
        publishCameraSnapshot(session, player);
        if (session.cameraRigTeleportInProgress()) {
            return;
        }

        VesselCameraSnapshot snapshot = vesselSnapshots.get(session.playerId());
        Location target = controllerDestination(snapshot, player);
        if (target == null) {
            fail(session);
            return;
        }

        Location current = player.getLocation();
        Location previousTarget = lastControllerTargets.put(session.playerId(), target.clone());
        if (current.getWorld() != target.getWorld()) {
            snapControllerToTarget(session, player, target);
            return;
        }

        Vector error = target.toVector().subtract(current.toVector());
        if (error.lengthSquared() >= hardSnapDistance * hardSnapDistance) {
            snapControllerToTarget(session, player, target);
            return;
        }

        Vector targetMotion = new Vector();
        if (previousTarget != null && previousTarget.getWorld() == target.getWorld()) {
            targetMotion = target.toVector().subtract(previousTarget.toVector());
        }
        player.setVelocity(targetMotion.add(limit(error.multiply(correctionGain), maxCorrectionSpeed)));
    }

    private void publishCameraSnapshot(PossessionSession session, Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) {
            return;
        }
        cameraSnapshots.put(session.playerId(), new CameraSnapshot(
            world.getUID(),
            eye.getX(),
            eye.getY(),
            eye.getZ(),
            player.getYaw(),
            player.getPitch()
        ));
    }

    private void tickVesselSnapshot(PossessionSession session, Mob vessel, ScheduledTask task) {
        if (!session.isActive() || !vessel.isValid()) {
            task.cancel();
            return;
        }

        VesselCameraSnapshot snapshot = new VesselCameraSnapshot(
            vessel.getWorld().getUID(),
            vessel.getX(),
            vessel.getY(),
            vessel.getZ(),
            vessel.getEyeHeight(),
            vessel.getWidth(),
            vessel.getHeight(),
            vessel.getPose()
        );
        vesselSnapshots.put(session.playerId(), snapshot);

        if (!proxies.containsKey(session.playerId())) {
            createPresentationProxy(session, vessel, snapshot);
        }
    }

    private void createPresentationProxy(PossessionSession session, Mob vessel, VesselCameraSnapshot snapshot) {
        if (!session.isActive() || proxies.containsKey(session.playerId())) {
            return;
        }

        Entity proxy;
        try {
            proxy = vessel.copy();
            proxy.setVisibleByDefault(false);
            proxy.setPersistent(false);
            proxy.setInvulnerable(true);
            proxy.setSilent(true);
            proxy.setGravity(false);
            proxy.setNoPhysics(true);
            proxy.setPose(snapshot.pose(), true);
            proxy.eject();

            PersistentDataContainer data = proxy.getPersistentDataContainer();
            for (NamespacedKey key : new HashSet<>(data.getKeys())) {
                data.remove(key);
            }
            for (String tag : new HashSet<>(proxy.getScoreboardTags())) {
                proxy.removeScoreboardTag(tag);
            }

            if (proxy instanceof LivingEntity living) {
                living.setCollidable(false);
                living.setAI(false);
            }
            if (proxy instanceof Mob proxyMob) {
                proxyMob.setAware(false);
                proxyMob.setTarget(null);
                proxyMob.setRemoveWhenFarAway(false);
            }

            Location spawn = vessel.getLocation().clone();
            if (!proxy.spawnAt(spawn, CreatureSpawnEvent.SpawnReason.CUSTOM)) {
                fail(session);
                return;
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not create Incarnate presentation proxy", ex);
            fail(session);
            return;
        }

        proxies.put(session.playerId(), proxy);
        session.cameraRigId(proxy.getUniqueId());
        PresentationProxyRegistry.register(proxy, session.playerId());

        ScheduledTask proxyTask = proxy.getScheduler().runAtFixedRate(
            plugin,
            task -> tickPresentationProxy(session, proxy, task),
            () -> {
                PresentationProxyRegistry.unregister(proxy);
                proxies.remove(session.playerId(), proxy);
                proxyTasks.remove(session.playerId());
                if (session.isActive()) {
                    fail(session);
                }
            },
            1L,
            1L
        );
        if (proxyTask == null) {
            PresentationProxyRegistry.unregister(proxy);
            proxies.remove(session.playerId(), proxy);
            proxy.remove();
            fail(session);
            return;
        }
        proxyTasks.put(session.playerId(), proxyTask);
    }

    private void tickPresentationProxy(PossessionSession session, Entity proxy, ScheduledTask task) {
        if (!session.isActive() || !proxy.isValid()) {
            task.cancel();
            return;
        }

        CameraSnapshot camera = cameraSnapshots.get(session.playerId());
        VesselCameraSnapshot vessel = vesselSnapshots.get(session.playerId());
        Location target = presentationDestination(camera, vessel);
        if (target == null) {
            return;
        }

        proxy.setNoPhysics(true);
        proxy.setGravity(false);
        proxy.setPose(vessel.pose(), true);
        proxy.setRotation(target.getYaw(), 0.0f);

        Location current = proxy.getLocation();
        Location previousTarget = lastProxyTargets.put(session.playerId(), target.clone());
        boolean firstPlacement = !proxyShown.contains(session.playerId());
        if (firstPlacement || current.getWorld() != target.getWorld()) {
            snapProxyToTarget(session, proxy, target, true);
            return;
        }

        Vector error = target.toVector().subtract(current.toVector());
        if (error.lengthSquared() >= hardSnapDistance * hardSnapDistance) {
            snapProxyToTarget(session, proxy, target, false);
            return;
        }

        Vector targetMotion = new Vector();
        if (previousTarget != null && previousTarget.getWorld() == target.getWorld()) {
            targetMotion = target.toVector().subtract(previousTarget.toVector());
        }
        proxy.setVelocity(targetMotion.add(limit(error.multiply(correctionGain), maxCorrectionSpeed)));
    }

    private Location controllerDestination(VesselCameraSnapshot snapshot, Player player) {
        if (snapshot == null) {
            return null;
        }
        World world = Bukkit.getWorld(snapshot.worldId());
        if (world == null) {
            return null;
        }
        Location target = new Location(
            world,
            snapshot.x(),
            snapshot.y() + snapshot.eyeHeight() - player.getEyeHeight() + verticalOffset,
            snapshot.z(),
            player.getYaw(),
            player.getPitch()
        );
        return target;
    }

    private Location presentationDestination(CameraSnapshot camera, VesselCameraSnapshot vessel) {
        if (camera == null || vessel == null) {
            return null;
        }
        World world = Bukkit.getWorld(camera.worldId());
        if (world == null) {
            return null;
        }
        double offset = clamp(
            bodyOffsetBase + vessel.width() * bodyOffsetScale,
            bodyOffsetMinimum,
            bodyOffsetMaximum
        );
        double yawRadians = Math.toRadians(camera.yaw());
        double x = camera.x() + Math.sin(yawRadians) * offset;
        double z = camera.z() - Math.cos(yawRadians) * offset;
        double y = camera.y() - vessel.eyeHeight();
        return new Location(world, x, y, z, camera.yaw(), 0.0f);
    }

    private void snapControllerToTarget(PossessionSession session, Player player, Location target) {
        if (!session.isActive() || !player.isOnline() || !session.beginCameraRigTeleport()) {
            return;
        }

        session.cameraTeleportInProgress(true);
        if (target.getWorld() == player.getWorld() && Bukkit.isOwnedByCurrentRegion(target)) {
            boolean success;
            try {
                success = player.teleport(target);
            } catch (RuntimeException ex) {
                success = false;
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Presentation controller snap failed", ex);
            } finally {
                session.cameraTeleportInProgress(false);
                session.endCameraRigTeleport();
            }
            if (!success) {
                fail(session);
            } else {
                publishCameraSnapshot(session, player);
            }
            return;
        }

        player.teleportAsync(target).whenComplete((success, error) -> {
            session.cameraTeleportInProgress(false);
            session.endCameraRigTeleport();
            if (session.isActive() && (error != null || !Boolean.TRUE.equals(success))) {
                if (error != null) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Presentation controller async snap failed", error);
                }
                fail(session);
            }
        });
    }

    private void snapProxyToTarget(PossessionSession session, Entity proxy, Location target, boolean showAfter) {
        UUID playerId = session.playerId();
        if (!proxyTeleporting.add(playerId)) {
            return;
        }
        proxy.setVelocity(new Vector());
        proxy.teleportAsync(target).whenComplete((success, error) -> {
            proxyTeleporting.remove(playerId);
            if (!session.isActive()) {
                return;
            }
            if (error != null || !Boolean.TRUE.equals(success)) {
                if (error != null) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Presentation proxy teleport failed", error);
                }
                fail(session);
                return;
            }
            if (showAfter || !proxyShown.contains(playerId)) {
                showProxyToOwner(session, proxy);
            }
        });
    }

    private void showProxyToOwner(PossessionSession session, Entity proxy) {
        Player player = session.player();
        ScheduledTask task = player.getScheduler().run(plugin, ignored -> {
            if (!session.isActive() || !player.isOnline()) {
                return;
            }
            player.hideEntity(plugin, session.vessel());
            player.showEntity(plugin, proxy);
            proxyShown.add(session.playerId());
        }, null);
        if (task == null) {
            fail(session);
        }
    }

    private void applyThirdPersonDistance(Player player, PossessionSession session) {
        clearThirdPersonDistance(player);
        if (!adaptiveThirdPersonDistance) {
            return;
        }
        AttributeInstance instance = player.getAttribute(Attribute.CAMERA_DISTANCE);
        if (instance == null) {
            return;
        }
        double body = Math.max(session.vesselWidth(), session.vesselHeight());
        double desired = Math.max(
            thirdPersonMinimum,
            Math.min(thirdPersonMaximum, thirdPersonBase + body * thirdPersonBodyScale)
        );
        double amount = desired - instance.getValue();
        if (Math.abs(amount) < 1.0e-4) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(
            cameraDistanceKey,
            amount,
            AttributeModifier.Operation.ADD_NUMBER
        ));
    }

    private void clearThirdPersonDistance(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.CAMERA_DISTANCE);
        if (instance == null) {
            return;
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.getKey().equals(cameraDistanceKey)
                || modifier.getKey().equals(shadowCameraDistanceKey)
                || modifier.getKey().equals(legacyCameraDistanceKey)) {
                instance.removeModifier(modifier);
            }
        }
    }

    public void detach(PossessionSession session, Player player) {
        ScheduledTask playerTask = session.cameraRigTask();
        if (playerTask != null) {
            playerTask.cancel();
            session.cameraRigTask(null);
        }
        ScheduledTask vesselTask = vesselSnapshotTasks.remove(session.playerId());
        if (vesselTask != null) {
            vesselTask.cancel();
        }
        ScheduledTask proxyTask = proxyTasks.remove(session.playerId());
        if (proxyTask != null) {
            proxyTask.cancel();
        }

        Entity proxy = proxies.remove(session.playerId());
        UUID proxyId = session.cameraRigId();
        PresentationProxyRegistry.unregister(proxyId);
        if (proxy != null) {
            PresentationProxyRegistry.unregister(proxy);
            proxy.getScheduler().run(plugin, ignored -> {
                if (proxy.isValid()) {
                    proxy.remove();
                }
            }, () -> PresentationProxyRegistry.unregister(proxyId));
        }

        session.endCameraRigTeleport();
        session.cameraTeleportInProgress(false);
        session.cameraRigId(null);
        proxyTeleporting.remove(session.playerId());
        proxyShown.remove(session.playerId());
        vesselSnapshots.remove(session.playerId());
        cameraSnapshots.remove(session.playerId());
        lastControllerTargets.remove(session.playerId());
        lastProxyTargets.remove(session.playerId());
        clearThirdPersonDistance(player);

        if (player.isOnline()) {
            player.showEntity(plugin, session.vessel());
            player.setVelocity(new Vector());
            player.setFallDistance(0.0f);
            player.setNoPhysics(session.playerState().noPhysics());
            player.setPose(session.playerState().pose(), session.playerState().fixedPose());
        }
    }

    private static Pose parsePose(String raw) {
        if (raw != null) {
            try {
                return Pose.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Pose.SWIMMING;
    }

    private static Vector limit(Vector vector, double maximum) {
        double lengthSquared = vector.lengthSquared();
        double maximumSquared = maximum * maximum;
        if (lengthSquared <= maximumSquared || lengthSquared <= 1.0e-12) {
            return vector;
        }
        return vector.multiply(maximum / Math.sqrt(lengthSquared));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void fail(PossessionSession session) {
        if (session.isActive()) {
            failureHandler.accept(session);
        }
    }

    private record VesselCameraSnapshot(
        UUID worldId,
        double x,
        double y,
        double z,
        double eyeHeight,
        double width,
        double height,
        Pose pose
    ) {
    }

    private record CameraSnapshot(
        UUID worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
    }
}
