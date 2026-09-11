package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CameraRigManager {
    private final IncarnatePlugin plugin;
    private final Consumer<PossessionSession> failureHandler;
    private final NamespacedKey cameraDistanceKey;
    private final NamespacedKey legacyCameraDistanceKey;
    private final double verticalOffset;
    private final double hardSnapDistance;
    private final double correctionGain;
    private final double maxCorrectionSpeed;
    private final boolean selfOcclusionClearance;
    private final double clearanceBase;
    private final double clearanceBodyScale;
    private final double clearanceMinimum;
    private final double clearanceMaximum;
    private final boolean adaptiveThirdPersonDistance;
    private final double thirdPersonBase;
    private final double thirdPersonBodyScale;
    private final double thirdPersonMinimum;
    private final double thirdPersonMaximum;
    private final Map<UUID, Location> lastTargets = new ConcurrentHashMap<>();

    public CameraRigManager(IncarnatePlugin plugin, Consumer<PossessionSession> failureHandler) {
        this.plugin = plugin;
        this.failureHandler = failureHandler;
        this.cameraDistanceKey = new NamespacedKey(plugin, "camera_shadow_distance");
        this.legacyCameraDistanceKey = new NamespacedKey(plugin, "camera_rig_distance");
        this.verticalOffset = plugin.getConfig().getDouble("camera.shadow.vertical-offset", 0.0);
        this.hardSnapDistance = Math.max(0.25, plugin.getConfig().getDouble("camera.shadow.hard-snap-distance", 1.25));
        this.correctionGain = clamp(plugin.getConfig().getDouble("camera.shadow.correction-gain", 0.65), 0.0, 1.0);
        this.maxCorrectionSpeed = Math.max(0.01, plugin.getConfig().getDouble("camera.shadow.max-correction-speed", 0.35));
        this.selfOcclusionClearance = plugin.getConfig().getBoolean("camera.shadow.self-occlusion-clearance", true);
        this.clearanceBase = Math.max(0.0, plugin.getConfig().getDouble("camera.shadow.clearance-base", 0.12));
        this.clearanceBodyScale = Math.max(0.0, plugin.getConfig().getDouble("camera.shadow.clearance-body-scale", 0.55));
        this.clearanceMinimum = Math.max(0.0, plugin.getConfig().getDouble("camera.shadow.clearance-minimum", 0.35));
        this.clearanceMaximum = Math.max(this.clearanceMinimum, plugin.getConfig().getDouble("camera.shadow.clearance-maximum", 1.25));
        this.adaptiveThirdPersonDistance = plugin.getConfig().getBoolean("camera.shadow.adaptive-third-person-distance", true);
        this.thirdPersonBase = plugin.getConfig().getDouble("camera.shadow.third-person-base", 2.5);
        this.thirdPersonBodyScale = plugin.getConfig().getDouble("camera.shadow.third-person-body-scale", 1.35);
        this.thirdPersonMinimum = Math.max(1.0, plugin.getConfig().getDouble("camera.shadow.third-person-minimum", 4.0));
        this.thirdPersonMaximum = Math.max(this.thirdPersonMinimum, plugin.getConfig().getDouble("camera.shadow.third-person-maximum", 32.0));
    }

    public boolean attach(PossessionSession session) {
        Player player = session.player();
        if (!session.isActive() || !player.isOnline() || player.isInsideVehicle()) {
            return false;
        }

        session.cameraRigId(null);
        session.cameraTransport(CameraTransport.CAMERA_RIG);
        session.endCameraRigTeleport();
        prepareController(player);
        applyThirdPersonDistance(player, session);

        Location target = destination(session, player);
        if (target == null) {
            clearThirdPersonDistance(player);
            return false;
        }
        lastTargets.put(session.playerId(), target.clone());
        snapToTarget(session, player, target);

        ScheduledTask task = player.getScheduler().runAtFixedRate(
            plugin,
            ignored -> tick(session),
            () -> fail(session),
            1L,
            1L
        );
        session.cameraRigTask(task);
        if (task == null) {
            detach(session, player);
            return false;
        }
        return true;
    }

    private void prepareController(Player player) {
        player.setAllowFlight(true);
        player.setFlySpeed(0.0f);
        if (!player.isFlying()) {
            player.setFlying(true);
        }
        player.setFallDistance(0.0f);
        player.setVelocity(new Vector());
    }

    private void tick(PossessionSession session) {
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
        if (session.cameraRigTeleportInProgress()) {
            return;
        }

        Location target = destination(session, player);
        if (target == null || target.getWorld() == null) {
            fail(session);
            return;
        }

        Location current = player.getLocation();
        Location previousTarget = lastTargets.put(session.playerId(), target.clone());
        if (current.getWorld() != target.getWorld()) {
            snapToTarget(session, player, target);
            return;
        }

        Vector error = target.toVector().subtract(current.toVector());
        if (error.lengthSquared() >= hardSnapDistance * hardSnapDistance) {
            snapToTarget(session, player, target);
            return;
        }

        Vector targetMotion = new Vector();
        if (previousTarget != null && previousTarget.getWorld() == target.getWorld()) {
            targetMotion = target.toVector().subtract(previousTarget.toVector());
        }

        Vector correction = limit(error.multiply(correctionGain), maxCorrectionSpeed);
        player.setVelocity(targetMotion.add(correction));
    }

    private Location destination(PossessionSession session, Player player) {
        Location vessel = session.lastKnownVesselLocation();
        if (vessel == null || vessel.getWorld() == null) {
            return null;
        }

        vessel.add(0.0, session.vesselEyeHeight() - player.getEyeHeight() + verticalOffset, 0.0);
        if (selfOcclusionClearance) {
            double clearance = clearanceFor(session);
            double yawRadians = Math.toRadians(player.getYaw());
            vessel.add(-Math.sin(yawRadians) * clearance, 0.0, Math.cos(yawRadians) * clearance);
        }
        vessel.setYaw(player.getYaw());
        vessel.setPitch(player.getPitch());
        return vessel;
    }

    private double clearanceFor(PossessionSession session) {
        return clamp(
            clearanceBase + session.vesselWidth() * clearanceBodyScale,
            clearanceMinimum,
            clearanceMaximum
        );
    }

    private void snapToTarget(PossessionSession session, Player player, Location target) {
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
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Controller shadow snap failed", ex);
            } finally {
                session.cameraTeleportInProgress(false);
                session.endCameraRigTeleport();
            }
            if (!success) {
                fail(session);
            }
            return;
        }

        player.teleportAsync(target).whenComplete((success, error) -> {
            session.cameraTeleportInProgress(false);
            session.endCameraRigTeleport();
            if (session.isActive() && (error != null || !Boolean.TRUE.equals(success))) {
                if (error != null) {
                    plugin.getLogger().log(java.util.logging.Level.WARNING, "Controller shadow async snap failed", error);
                }
                fail(session);
            }
        });
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
        double desired = Math.max(thirdPersonMinimum, thirdPersonBase + body * thirdPersonBodyScale);
        if (selfOcclusionClearance) {
            desired += clearanceFor(session);
        }
        desired = Math.min(thirdPersonMaximum, desired);
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
            if (modifier.getKey().equals(cameraDistanceKey) || modifier.getKey().equals(legacyCameraDistanceKey)) {
                instance.removeModifier(modifier);
            }
        }
    }

    public void detach(PossessionSession session, Player player) {
        ScheduledTask task = session.cameraRigTask();
        if (task != null) {
            task.cancel();
            session.cameraRigTask(null);
        }
        session.endCameraRigTeleport();
        session.cameraTeleportInProgress(false);
        session.cameraRigId(null);
        lastTargets.remove(session.playerId());
        clearThirdPersonDistance(player);
        if (player.isOnline()) {
            player.setVelocity(new Vector());
            player.setFallDistance(0.0f);
        }
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
}
