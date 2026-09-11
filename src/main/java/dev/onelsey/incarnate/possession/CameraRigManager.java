package dev.onelsey.incarnate.possession;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import dev.onelsey.incarnate.IncarnatePlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class CameraRigManager {
    private static final AtomicInteger NEXT_VIRTUAL_ENTITY_ID = new AtomicInteger(Integer.MAX_VALUE - 100_000);

    private final IncarnatePlugin plugin;
    private final Consumer<PossessionSession> failureHandler;
    private final ProtocolManager protocol;
    private final PacketAdapter outgoingListener;
    private final PacketAdapter incomingListener;
    private final Set<PacketType> movementPackets;
    private final ConcurrentMap<UUID, PacketSession> sessions = new ConcurrentHashMap<>();

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
    private final int pairingRetryIntervalTicks;
    private final int pairingMaxAttempts;

    private final Method getPositionMoveRotationMethod;
    private final Method createPositionMoveRotationMethod;

    public CameraRigManager(IncarnatePlugin plugin, Consumer<PossessionSession> failureHandler) {
        this.plugin = plugin;
        this.failureHandler = failureHandler;
        this.protocol = ProtocolLibrary.getProtocolManager();
        this.cameraDistanceKey = new NamespacedKey(plugin, "protocol_presentation_camera_distance");
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
        this.pairingRetryIntervalTicks = Math.max(1, plugin.getConfig().getInt("camera.presentation.protocol.pairing-retry-interval-ticks", 5));
        this.pairingMaxAttempts = Math.max(1, plugin.getConfig().getInt("camera.presentation.protocol.pairing-max-attempts", 8));

        try {
            this.getPositionMoveRotationMethod = PacketContainer.class.getMethod("getPositionMoveRotation");
            Class<?> wrapper = Class.forName(
                "com.comphenix.protocol.wrappers.WrappedPositionMoveRotation",
                true,
                PacketContainer.class.getClassLoader()
            );
            this.createPositionMoveRotationMethod = wrapper.getMethod(
                "create",
                Vector.class,
                Vector.class,
                float.class,
                float.class
            );
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                "Incarnate requires the current ProtocolLib development build with Minecraft 26.2 position/rotation support.",
                ex
            );
        }

        Set<PacketType> movement = new HashSet<>();
        movement.add(PacketType.Play.Server.REL_ENTITY_MOVE);
        movement.add(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        movement.add(PacketType.Play.Server.ENTITY_LOOK);
        movement.add(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        movement.add(PacketType.Play.Server.ENTITY_TELEPORT);
        movement.add(PacketType.Play.Server.ENTITY_VELOCITY);
        optionalServerPacket("ENTITY_POSITION_SYNC").ifPresent(movement::add);
        this.movementPackets = Set.copyOf(movement);

        List<PacketType> outgoingTypes = new ArrayList<>();
        outgoingTypes.add(PacketType.Play.Server.SPAWN_ENTITY);
        outgoingTypes.add(PacketType.Play.Server.ENTITY_METADATA);
        outgoingTypes.add(PacketType.Play.Server.ENTITY_EQUIPMENT);
        outgoingTypes.add(PacketType.Play.Server.ENTITY_STATUS);
        outgoingTypes.add(PacketType.Play.Server.ANIMATION);
        outgoingTypes.add(PacketType.Play.Server.HURT_ANIMATION);
        outgoingTypes.add(PacketType.Play.Server.UPDATE_ATTRIBUTES);
        outgoingTypes.add(PacketType.Play.Server.ENTITY_EFFECT);
        outgoingTypes.add(PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
        outgoingTypes.add(PacketType.Play.Server.ENTITY_DESTROY);
        outgoingTypes.addAll(movementPackets);

        this.outgoingListener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, outgoingTypes) {
            @Override
            public void onPacketSending(PacketEvent event) {
                rewriteOutgoing(event);
            }
        };
        this.incomingListener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                suppressVirtualInteraction(event);
            }
        };
        protocol.addPacketListener(outgoingListener);
        protocol.addPacketListener(incomingListener);
    }

    public boolean attach(PossessionSession session) {
        Player player = session.player();
        Mob vessel = session.vessel();
        if (!session.isActive() || !player.isOnline() || player.isInsideVehicle()) {
            return false;
        }
        if (vessel.getType() == EntityType.ENDER_DRAGON || vessel.getType() == EntityType.WITHER) {
            plugin.getLogger().warning("ProtocolLib presentation is not enabled for boss vessel " + vessel.getType() + " yet.");
            return false;
        }
        if (sessions.containsKey(session.playerId())) {
            return false;
        }

        Location lastKnown = session.lastKnownVesselLocation();
        if (lastKnown == null || lastKnown.getWorld() == null) {
            return false;
        }

        prepareController(player);
        applyThirdPersonDistance(player, session);

        VesselSnapshot initial = new VesselSnapshot(
            lastKnown.getWorld().getUID(),
            lastKnown.getX(),
            lastKnown.getY(),
            lastKnown.getZ(),
            session.vesselEyeHeight(),
            session.vesselWidth(),
            session.vesselHeight()
        );
        int virtualId = allocateVirtualEntityId();
        PacketSession packetSession = new PacketSession(
            session,
            vessel.getEntityId(),
            virtualId,
            UUID.randomUUID(),
            initial
        );
        sessions.put(session.playerId(), packetSession);
        session.cameraRigId(null);
        session.cameraTransport(CameraTransport.CAMERA_RIG);
        session.endCameraRigTeleport();

        try {
            sendDestroy(player, packetSession.realEntityId);
            publishPresentationTarget(packetSession, player, initial);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not initialize ProtocolLib presentation for " + session.playerId(), ex);
            sessions.remove(session.playerId(), packetSession);
            clearThirdPersonDistance(player);
            restoreControllerState(session, player);
            return false;
        }

        Location controllerTarget = controllerDestination(initial, player);
        if (controllerTarget == null) {
            sessions.remove(session.playerId(), packetSession);
            clearThirdPersonDistance(player);
            restoreControllerState(session, player);
            return false;
        }
        packetSession.lastControllerTarget = controllerTarget.clone();
        snapControllerToTarget(packetSession, player, controllerTarget);

        ScheduledTask playerTask = player.getScheduler().runAtFixedRate(
            plugin,
            ignored -> tickController(packetSession),
            () -> markBroken(packetSession, "controller scheduler retired", null),
            1L,
            1L
        );
        session.cameraRigTask(playerTask);
        packetSession.playerTask = playerTask;
        if (playerTask == null) {
            detach(session, player);
            return false;
        }

        ScheduledTask vesselTask = vessel.getScheduler().runAtFixedRate(
            plugin,
            task -> tickVessel(packetSession, vessel, task),
            () -> markBroken(packetSession, "vessel scheduler retired", null),
            1L,
            1L
        );
        packetSession.vesselTask = vesselTask;
        if (vesselTask == null) {
            detach(session, player);
            return false;
        }

        requestPairing(packetSession, vessel, player);
        return true;
    }

    private void tickController(PacketSession state) {
        PossessionSession session = state.session;
        if (!session.isActive()) {
            return;
        }
        Player player = session.player();
        if (!player.isOnline()) {
            markBroken(state, "controller went offline", null);
            return;
        }
        if (!Bukkit.isOwnedByCurrentRegion(player)) {
            return;
        }
        if (state.broken.get()) {
            fail(session);
            return;
        }

        prepareController(player);
        VesselSnapshot snapshot = state.vesselSnapshot.get();
        if (snapshot == null) {
            markBroken(state, "missing vessel snapshot", null);
            return;
        }

        if (!session.cameraRigTeleportInProgress()) {
            Location target = controllerDestination(snapshot, player);
            if (target == null) {
                markBroken(state, "missing controller target world", null);
                return;
            }
            Location current = player.getLocation();
            Location previousTarget = state.lastControllerTarget;
            state.lastControllerTarget = target.clone();

            if (current.getWorld() != target.getWorld()) {
                snapControllerToTarget(state, player, target);
            } else {
                Vector error = target.toVector().subtract(current.toVector());
                if (error.lengthSquared() >= hardSnapDistance * hardSnapDistance) {
                    snapControllerToTarget(state, player, target);
                } else {
                    Vector targetMotion = new Vector();
                    if (previousTarget != null && previousTarget.getWorld() == target.getWorld()) {
                        targetMotion = target.toVector().subtract(previousTarget.toVector());
                    }
                    player.setVelocity(targetMotion.add(limit(error.multiply(correctionGain), maxCorrectionSpeed)));
                }
            }
        }

        publishPresentationTarget(state, player, snapshot);
        if (state.spawned.get()) {
            PresentationTarget target = state.presentationTarget.get();
            if (target != null) {
                try {
                    sendTransform(player, state.virtualEntityId, target);
                } catch (RuntimeException ex) {
                    markBroken(state, "virtual body transform failed", ex);
                }
            }
        }
    }

    private void tickVessel(PacketSession state, Mob vessel, ScheduledTask task) {
        PossessionSession session = state.session;
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            task.cancel();
            return;
        }

        state.vesselSnapshot.set(new VesselSnapshot(
            vessel.getWorld().getUID(),
            vessel.getX(),
            vessel.getY(),
            vessel.getZ(),
            vessel.getEyeHeight(),
            vessel.getWidth(),
            vessel.getHeight()
        ));

        if (!state.spawned.get()) {
            int ticks = state.unpairedTicks.incrementAndGet();
            if (ticks % pairingRetryIntervalTicks == 0) {
                int attempt = state.pairingAttempts.incrementAndGet();
                if (attempt > pairingMaxAttempts) {
                    markBroken(state, "ProtocolLib could not pair the virtual body after " + pairingMaxAttempts + " attempts", null);
                    return;
                }
                requestPairing(state, vessel, session.player());
            }
        } else {
            state.unpairedTicks.set(0);
            state.pairingAttempts.set(0);
        }
    }

    private void requestPairing(PacketSession state, Mob vessel, Player player) {
        if (!state.session.isActive() || !vessel.isValid() || !player.isOnline()) {
            return;
        }
        try {
            protocol.updateEntity(vessel, List.of(player));
        } catch (RuntimeException ex) {
            markBroken(state, "ProtocolLib entity pairing failed", ex);
        }
    }

    private void rewriteOutgoing(PacketEvent event) {
        PacketSession state = sessions.get(event.getPlayer().getUniqueId());
        if (state == null || !state.session.isActive()) {
            return;
        }

        try {
            PacketContainer packet = event.getPacket();
            PacketType type = packet.getType();

            if (type == PacketType.Play.Server.ENTITY_DESTROY) {
                rewriteDestroy(packet, state);
                return;
            }

            StructureModifier<Integer> integers = packet.getIntegers();
            if (integers.size() == 0 || integers.read(0) != state.realEntityId) {
                return;
            }

            if (movementPackets.contains(type)) {
                event.setCancelled(true);
                return;
            }

            integers.write(0, state.virtualEntityId);
            if (type == PacketType.Play.Server.SPAWN_ENTITY) {
                rewriteSpawn(packet, state);
                state.spawned.set(true);
                state.unpairedTicks.set(0);
                state.pairingAttempts.set(0);
            }
        } catch (RuntimeException ex) {
            markBroken(state, "ProtocolLib outgoing packet rewrite failed", ex);
            event.setCancelled(true);
        }
    }

    private void rewriteDestroy(PacketContainer packet, PacketSession state) {
        if (packet.getIntLists().size() == 0) {
            return;
        }
        List<Integer> ids = packet.getIntLists().read(0);
        if (ids == null || !ids.contains(state.realEntityId)) {
            return;
        }
        List<Integer> rewritten = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            rewritten.add(id != null && id == state.realEntityId ? state.virtualEntityId : id);
        }
        packet.getIntLists().write(0, rewritten);
        state.spawned.set(false);
    }

    private void rewriteSpawn(PacketContainer packet, PacketSession state) {
        PresentationTarget target = state.presentationTarget.get();
        if (packet.getUUIDs().size() > 0) {
            packet.getUUIDs().write(0, state.virtualEntityUuid);
        }
        if (target == null) {
            return;
        }
        if (packet.getDoubles().size() >= 3) {
            packet.getDoubles()
                .write(0, target.x)
                .write(1, target.y)
                .write(2, target.z);
        }
        if (packet.getVectors().size() > 0) {
            packet.getVectors().write(0, new Vector());
        }
        byte yaw = packedAngle(target.yaw);
        byte pitch = packedAngle(target.pitch);
        StructureModifier<Byte> bytes = packet.getBytes();
        if (bytes.size() > 0) {
            bytes.write(0, pitch);
        }
        if (bytes.size() > 1) {
            bytes.write(1, yaw);
        }
        if (bytes.size() > 2) {
            bytes.write(2, yaw);
        }
    }

    private void suppressVirtualInteraction(PacketEvent event) {
        PacketSession state = sessions.get(event.getPlayer().getUniqueId());
        if (state == null || !state.session.isActive()) {
            return;
        }
        try {
            StructureModifier<Integer> integers = event.getPacket().getIntegers();
            if (integers.size() > 0 && integers.read(0) == state.virtualEntityId) {
                event.setCancelled(true);
            }
        } catch (RuntimeException ex) {
            markBroken(state, "ProtocolLib inbound virtual-body filter failed", ex);
            event.setCancelled(true);
        }
    }

    private void publishPresentationTarget(PacketSession state, Player player, VesselSnapshot vessel) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null || !world.getUID().equals(vessel.worldId)) {
            return;
        }
        double offset = clamp(
            bodyOffsetBase + vessel.width * bodyOffsetScale,
            bodyOffsetMinimum,
            bodyOffsetMaximum
        );
        double yawRadians = Math.toRadians(player.getYaw());
        double x = eye.getX() + Math.sin(yawRadians) * offset;
        double z = eye.getZ() - Math.cos(yawRadians) * offset;
        double y = eye.getY() - vessel.eyeHeight;
        state.presentationTarget.set(new PresentationTarget(
            world.getUID(),
            x,
            y,
            z,
            player.getYaw(),
            player.getPitch()
        ));
    }

    private Location controllerDestination(VesselSnapshot snapshot, Player player) {
        World world = Bukkit.getWorld(snapshot.worldId);
        if (world == null) {
            return null;
        }
        return new Location(
            world,
            snapshot.x,
            snapshot.y + snapshot.eyeHeight - player.getEyeHeight() + verticalOffset,
            snapshot.z,
            player.getYaw(),
            player.getPitch()
        );
    }

    private void snapControllerToTarget(PacketSession state, Player player, Location target) {
        PossessionSession session = state.session;
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
                markBroken(state, "controller sync snap failed", ex);
            } finally {
                session.cameraTeleportInProgress(false);
                session.endCameraRigTeleport();
            }
            if (!success) {
                markBroken(state, "controller sync snap rejected", null);
            }
            return;
        }

        player.teleportAsync(target).whenComplete((success, error) -> {
            session.cameraTeleportInProgress(false);
            session.endCameraRigTeleport();
            if (session.isActive() && (error != null || !Boolean.TRUE.equals(success))) {
                markBroken(state, "controller async snap failed", error);
            }
        });
    }

    private void sendTransform(Player player, int virtualEntityId, PresentationTarget target) {
        PacketContainer teleport = protocol.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        teleport.getIntegers().write(0, virtualEntityId);
        writePositionMoveRotation(teleport, target);
        if (teleport.getBooleans().size() > 0) {
            teleport.getBooleans().write(0, false);
        }
        protocol.sendServerPacket(player, teleport, false);

        PacketContainer head = protocol.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, virtualEntityId);
        if (head.getBytes().size() > 0) {
            head.getBytes().write(0, packedAngle(target.yaw));
        }
        protocol.sendServerPacket(player, head, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void writePositionMoveRotation(PacketContainer packet, PresentationTarget target) {
        try {
            Object wrapper = createPositionMoveRotationMethod.invoke(
                null,
                new Vector(target.x, target.y, target.z),
                new Vector(),
                target.yaw,
                target.pitch
            );
            Object modifierObject = getPositionMoveRotationMethod.invoke(packet);
            StructureModifier modifier = (StructureModifier) modifierObject;
            modifier.write(0, wrapper);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            throw new IllegalStateException("Could not write ProtocolLib 26.2 PositionMoveRotation", ex);
        }
    }

    private void sendDestroy(Player player, int entityId) {
        PacketContainer destroy = protocol.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, List.of(entityId));
        protocol.sendServerPacket(player, destroy, false);
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
        PacketSession state = sessions.remove(session.playerId());
        if (state != null) {
            if (state.playerTask != null) {
                state.playerTask.cancel();
            }
            if (state.vesselTask != null) {
                state.vesselTask.cancel();
            }
            if (player.isOnline()) {
                try {
                    sendDestroy(player, state.virtualEntityId);
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.WARNING, "Could not remove Incarnate virtual body from client", ex);
                }
            }
        }

        ScheduledTask genericTask = session.cameraRigTask();
        if (genericTask != null) {
            genericTask.cancel();
            session.cameraRigTask(null);
        }
        session.endCameraRigTeleport();
        session.cameraTeleportInProgress(false);
        session.cameraRigId(null);
        clearThirdPersonDistance(player);
        restoreControllerState(session, player);

        Mob vessel = session.vessel();
        if (player.isOnline() && vessel.isValid()) {
            ScheduledTask refresh = vessel.getScheduler().run(
                plugin,
                ignored -> {
                    if (player.isOnline() && vessel.isValid()) {
                        try {
                            protocol.updateEntity(vessel, List.of(player));
                        } catch (RuntimeException ex) {
                            plugin.getLogger().log(Level.WARNING, "Could not restore real vessel tracking after ProtocolLib presentation", ex);
                        }
                    }
                },
                null
            );
            if (refresh == null) {
                plugin.getLogger().warning("Could not schedule real vessel tracking refresh after ProtocolLib presentation.");
            }
        }
    }

    private void restoreControllerState(PossessionSession session, Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.setVelocity(new Vector());
        player.setFallDistance(0.0f);
        player.setNoPhysics(session.playerState().noPhysics());
        player.setPose(session.playerState().pose(), session.playerState().fixedPose());
    }

    private void markBroken(PacketSession state, String reason, Throwable error) {
        if (!state.broken.compareAndSet(false, true)) {
            return;
        }
        if (error == null) {
            plugin.getLogger().warning("Incarnate ProtocolLib presentation failed: " + reason);
        } else {
            plugin.getLogger().log(Level.WARNING, "Incarnate ProtocolLib presentation failed: " + reason, error);
        }
    }

    private void fail(PossessionSession session) {
        if (session.isActive()) {
            failureHandler.accept(session);
        }
    }

    private static java.util.Optional<PacketType> optionalServerPacket(String fieldName) {
        try {
            Object value = PacketType.Play.Server.class.getField(fieldName).get(null);
            return value instanceof PacketType type ? java.util.Optional.of(type) : java.util.Optional.empty();
        } catch (ReflectiveOperationException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static int allocateVirtualEntityId() {
        int id = NEXT_VIRTUAL_ENTITY_ID.getAndDecrement();
        if (id < 1_500_000_000) {
            NEXT_VIRTUAL_ENTITY_ID.compareAndSet(id - 1, Integer.MAX_VALUE - 100_000);
        }
        return id;
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

    private static byte packedAngle(float degrees) {
        return (byte) Math.floor(degrees * 256.0f / 360.0f);
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

    private static final class PacketSession {
        private final PossessionSession session;
        private final int realEntityId;
        private final int virtualEntityId;
        private final UUID virtualEntityUuid;
        private final AtomicReference<VesselSnapshot> vesselSnapshot;
        private final AtomicReference<PresentationTarget> presentationTarget = new AtomicReference<>();
        private final AtomicBoolean spawned = new AtomicBoolean(false);
        private final AtomicBoolean broken = new AtomicBoolean(false);
        private final AtomicInteger unpairedTicks = new AtomicInteger();
        private final AtomicInteger pairingAttempts = new AtomicInteger();
        private volatile ScheduledTask playerTask;
        private volatile ScheduledTask vesselTask;
        private Location lastControllerTarget;

        private PacketSession(
            PossessionSession session,
            int realEntityId,
            int virtualEntityId,
            UUID virtualEntityUuid,
            VesselSnapshot initialSnapshot
        ) {
            this.session = session;
            this.realEntityId = realEntityId;
            this.virtualEntityId = virtualEntityId;
            this.virtualEntityUuid = virtualEntityUuid;
            this.vesselSnapshot = new AtomicReference<>(initialSnapshot);
        }
    }

    private record VesselSnapshot(
        UUID worldId,
        double x,
        double y,
        double z,
        double eyeHeight,
        double width,
        double height
    ) {
    }

    private record PresentationTarget(
        UUID worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
    }
}
