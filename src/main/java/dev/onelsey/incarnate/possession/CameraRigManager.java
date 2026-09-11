package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public final class CameraRigManager {
    private final IncarnatePlugin plugin;
    private final Consumer<PossessionSession> failureHandler;
    private final NamespacedKey cameraDistanceKey;
    private final double verticalOffset;
    private final boolean adaptiveThirdPersonDistance;
    private final double thirdPersonBase;
    private final double thirdPersonBodyScale;
    private final double thirdPersonMinimum;
    private final double thirdPersonMaximum;

    public CameraRigManager(IncarnatePlugin plugin, Consumer<PossessionSession> failureHandler) {
        this.plugin = plugin;
        this.failureHandler = failureHandler;
        this.cameraDistanceKey = new NamespacedKey(plugin, "camera_rig_distance");
        this.verticalOffset = plugin.getConfig().getDouble("camera.rig.vertical-offset", 0.0);
        this.adaptiveThirdPersonDistance = plugin.getConfig().getBoolean("camera.rig.adaptive-third-person-distance", true);
        this.thirdPersonBase = plugin.getConfig().getDouble("camera.rig.third-person-base", 2.5);
        this.thirdPersonBodyScale = plugin.getConfig().getDouble("camera.rig.third-person-body-scale", 1.35);
        this.thirdPersonMinimum = Math.max(1.0, plugin.getConfig().getDouble("camera.rig.third-person-minimum", 4.0));
        this.thirdPersonMaximum = Math.max(this.thirdPersonMinimum, plugin.getConfig().getDouble("camera.rig.third-person-maximum", 32.0));
    }

    public boolean attach(PossessionSession session) {
        Player player = session.player();
        if (!session.isActive() || !player.isOnline() || player.isInsideVehicle()) {
            return false;
        }

        ArmorStand rig;
        try {
            Entity spawned = player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            if (!(spawned instanceof ArmorStand armorStand)) {
                spawned.remove();
                return false;
            }
            rig = armorStand;
            configureRig(rig);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not create Incarnate camera rig", ex);
            return false;
        }

        if (!rig.addPassenger(player)) {
            rig.remove();
            return false;
        }

        session.cameraRigId(rig.getUniqueId());
        session.cameraTransport(CameraTransport.CAMERA_RIG);
        applyThirdPersonDistance(player, session);

        ScheduledTask task = rig.getScheduler().runAtFixedRate(plugin, ignored -> tick(session, rig), () -> fail(session), 1L, 1L);
        session.cameraRigTask(task);
        if (task == null) {
            detach(session, player);
            return false;
        }
        return true;
    }

    private static void configureRig(ArmorStand rig) {
        rig.setVisible(false);
        rig.setMarker(true);
        rig.setSmall(true);
        rig.setBasePlate(false);
        rig.setArms(false);
        rig.setGravity(false);
        rig.setInvulnerable(true);
        rig.setSilent(true);
        rig.setPersistent(false);
        rig.setCollidable(false);
        rig.setCustomNameVisible(false);
    }

    private void tick(PossessionSession session, ArmorStand rig) {
        if (!session.isActive() || !rig.isValid()) {
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

        if (player.getVehicle() != rig) {
            if (!rig.addPassenger(player)) {
                fail(session);
                return;
            }
        }

        Location destination = destination(session, player);
        if (destination == null || destination.getWorld() == null || destination.getWorld() != rig.getWorld()) {
            fail(session);
            return;
        }

        if (Bukkit.isOwnedByCurrentRegion(destination)) {
            if (!rig.teleport(destination)) {
                fail(session);
            }
            return;
        }

        if (!session.beginCameraRigTeleport()) {
            return;
        }

        rig.teleportAsync(destination).whenComplete((success, error) -> {
            session.endCameraRigTeleport();
            if (error != null || !Boolean.TRUE.equals(success)) {
                fail(session);
            }
        });
    }

    private Location destination(PossessionSession session, Player player) {
        Location vessel = session.lastKnownVesselLocation();
        if (vessel == null) {
            return null;
        }
        double anchorY = session.vesselEyeHeight() - player.getEyeHeight() + verticalOffset;
        vessel.add(0.0, anchorY, 0.0);
        vessel.setYaw(0.0f);
        vessel.setPitch(0.0f);
        return vessel;
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
        double desired = Math.max(thirdPersonMinimum, Math.min(thirdPersonMaximum, thirdPersonBase + body * thirdPersonBodyScale));
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
            if (modifier.getKey().equals(cameraDistanceKey)) {
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
        clearThirdPersonDistance(player);

        UUID rigId = session.cameraRigId();
        session.cameraRigId(null);
        if (rigId == null) {
            return;
        }

        Entity entity = Bukkit.getEntity(rigId);
        if (entity == null) {
            return;
        }

        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            removeRig(entity, player);
            return;
        }

        entity.getScheduler().run(plugin, ignored -> removeRig(entity, player), null);
    }

    private static void removeRig(Entity rig, Player player) {
        if (player.getVehicle() == rig) {
            rig.removePassenger(player);
        }
        if (rig.isValid()) {
            rig.remove();
        }
    }

    private void fail(PossessionSession session) {
        if (session.isActive()) {
            failureHandler.accept(session);
        }
    }
}
