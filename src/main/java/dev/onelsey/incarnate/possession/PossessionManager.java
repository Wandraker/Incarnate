package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import dev.onelsey.incarnate.ability.AbilityGesture;
import dev.onelsey.incarnate.ability.AbilityRegistry;
import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.message.MessageService;
import dev.onelsey.incarnate.movement.ControllerRegistry;
import dev.onelsey.incarnate.movement.VesselController;
import dev.onelsey.incarnate.permission.IncarnatePermissions;
import dev.onelsey.incarnate.sense.VesselPositionSnapshot;
import dev.onelsey.incarnate.sense.WardenSenseMath;
import dev.onelsey.incarnate.sense.WardenSenseSnapshot;
import dev.onelsey.incarnate.visibility.PossessionVisibilityManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PossessionManager {
    private final IncarnatePlugin plugin;
    private final ControllerRegistry controllers;
    private final AbilityRegistry abilities;
    private final PossessionVisibilityManager visibility;
    private final MessageService messages;
    private final Map<UUID, PossessionSession> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, PossessionSession> byVessel = new ConcurrentHashMap<>();
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingVessels = ConcurrentHashMap.newKeySet();
    private final Set<UUID> restoringPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> recoveryInFlight = ConcurrentHashMap.newKeySet();
    private final Set<EntityType> excluded;
    private final boolean removeCreatedOnRelease;
    private final boolean removeOrphanedCreated;
    private final boolean releaseAtVessel;
    private final CameraTransport cameraMode;
    private final int cameraAttachRetries;
    private final boolean cameraFallbackToSpectatorTarget;
    private final DirectCameraBridge directCameraBridge;
    private final CameraRigManager cameraRigManager;
    private final boolean hudEnabled;
    private final int hudIntervalTicks;
    private final boolean hudShowHealth;
    private final boolean hudShowAbilities;
    private final boolean hudShowSenses;
    private final boolean wardenSensesEnabled;
    private final double wardenSenseMaxRange;
    private final int wardenSenseMemoryTicks;
    private final boolean wardenSenseIgnoreSelf;
    private final boolean wardenSenseShowEventKind;
    private final PlayerRecoveryStore playerRecovery;
    private final VesselRecoveryStore vesselRecovery;

    public PossessionManager(
        IncarnatePlugin plugin,
        ControllerRegistry controllers,
        AbilityRegistry abilities,
        PossessionVisibilityManager visibility,
        MessageService messages,
        Set<EntityType> excluded
    ) {
        this.plugin = plugin;
        this.controllers = controllers;
        this.abilities = abilities;
        this.visibility = visibility;
        this.messages = messages;
        this.excluded = Set.copyOf(excluded);
        this.removeCreatedOnRelease = plugin.getConfig().getBoolean("created-vessels.remove-on-release", true);
        this.removeOrphanedCreated = plugin.getConfig().getBoolean("created-vessels.remove-orphaned-after-recovery", true);
        this.releaseAtVessel = plugin.getConfig().getBoolean("control.release-at-vessel", true);
        CameraTransport configuredCamera;
        try {
            configuredCamera = CameraTransport.valueOf(plugin.getConfig().getString("camera.mode", "CAMERA_RIG").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown camera.mode; using CAMERA_RIG.");
            configuredCamera = CameraTransport.CAMERA_RIG;
        }
        this.cameraMode = configuredCamera == CameraTransport.NONE ? CameraTransport.CAMERA_RIG : configuredCamera;
        this.cameraAttachRetries = Math.max(1, plugin.getConfig().getInt("camera.attach-retries", 8));
        this.cameraFallbackToSpectatorTarget = plugin.getConfig().getBoolean("camera.fallback-to-spectator-target", false);
        this.directCameraBridge = new DirectCameraBridge(plugin);
        this.cameraRigManager = new CameraRigManager(plugin, session -> requestRelease(session, ReleaseReason.INTERNAL_ERROR));
        this.hudEnabled = plugin.getConfig().getBoolean("hud.actionbar.enabled", true);
        this.hudIntervalTicks = Math.max(1, plugin.getConfig().getInt("hud.actionbar.interval-ticks", 4));
        this.hudShowHealth = plugin.getConfig().getBoolean("hud.actionbar.show-health", true);
        this.hudShowAbilities = plugin.getConfig().getBoolean("hud.actionbar.show-abilities", true);
        this.hudShowSenses = plugin.getConfig().getBoolean("hud.actionbar.show-senses", true);
        this.wardenSensesEnabled = plugin.getConfig().getBoolean("senses.warden.enabled", true);
        this.wardenSenseMaxRange = Math.max(1.0, plugin.getConfig().getDouble("senses.warden.max-range", 32.0));
        this.wardenSenseMemoryTicks = Math.max(1, plugin.getConfig().getInt("senses.warden.memory-ticks", 40));
        this.wardenSenseIgnoreSelf = plugin.getConfig().getBoolean("senses.warden.ignore-self", true);
        this.wardenSenseShowEventKind = plugin.getConfig().getBoolean("senses.warden.show-event-kind", true);
        this.playerRecovery = new PlayerRecoveryStore(plugin);
        this.vesselRecovery = new VesselRecoveryStore(plugin);
    }

    public PossessionSession session(Player player) {
        return byPlayer.get(player.getUniqueId());
    }

    public PossessionSession session(Mob vessel) {
        return byVessel.get(vessel.getUniqueId());
    }

    public boolean isPossessing(Player player) {
        return session(player) != null;
    }

    public boolean isRecoveryPending(Player player) {
        UUID playerId = player.getUniqueId();
        return restoringPlayers.contains(playerId) || playerRecovery.hasRecovery(player);
    }

    public void recoverPending(Player player) {
        recoverPlayerIfNeeded(player);
    }

    public boolean isControllerConcealed(UUID playerId) {
        return visibility.isConcealed(playerId);
    }

    public boolean isExcluded(EntityType type) {
        return excluded.contains(type);
    }

    public boolean canCreate(EntityType type) {
        if (isExcluded(type) || !type.isAlive() || !type.isSpawnable()) {
            return false;
        }
        Class<? extends Entity> entityClass = type.getEntityClass();
        return entityClass != null && Mob.class.isAssignableFrom(entityClass);
    }

    public void begin(Player player, Mob vessel, PossessionOrigin origin) {
        UUID playerId = player.getUniqueId();
        UUID vesselId = vessel.getUniqueId();
        EntityType vesselType = vessel.getType();

        if (origin == PossessionOrigin.CREATED && !player.hasPermission(IncarnatePermissions.CREATE)) {
            rejectBeforeStart(player, vessel, origin, "permission-create");
            return;
        }
        if (origin == PossessionOrigin.EXISTING && !player.hasPermission(IncarnatePermissions.POSSESS)) {
            rejectBeforeStart(player, vessel, origin, "permission-possess");
            return;
        }
        if (!IncarnatePermissions.canUseMob(player, vesselType)) {
            rejectBeforeStart(player, vessel, origin, "mob-access-denied", Map.of("entity", Component.text(vesselType.name().toLowerCase(java.util.Locale.ROOT))));
            return;
        }
        if (!player.isOnline() || player.isDead()) {
            rejectBeforeStart(player, vessel, origin, "start-state-invalid");
            return;
        }
        if (player.isInsideVehicle()) {
            rejectBeforeStart(player, vessel, origin, "leave-vehicle");
            return;
        }
        if (isPossessing(player) || byPlayer.containsKey(playerId)) {
            rejectBeforeStart(player, vessel, origin, "already-possessing");
            return;
        }
        if (restoringPlayers.contains(playerId) || playerRecovery.hasRecovery(player)) {
            rejectBeforeStart(player, vessel, origin, "previous-recovery-pending");
            recoverPlayerIfNeeded(player);
            return;
        }
        if (isExcluded(vessel.getType())) {
            rejectBeforeStart(player, vessel, origin, "entity-excluded");
            return;
        }
        if (byVessel.containsKey(vesselId)) {
            rejectBeforeStart(player, vessel, origin, "vessel-already-controlled");
            return;
        }
        if (!pendingPlayers.add(playerId)) {
            rejectBeforeStart(player, vessel, origin, "request-already-starting");
            return;
        }
        if (!pendingVessels.add(vesselId)) {
            pendingPlayers.remove(playerId);
            rejectBeforeStart(player, vessel, origin, "vessel-already-acquiring");
            return;
        }

        final PlayerState playerState;
        final InputSnapshot initialInput;
        final ViewSnapshot initialView;
        try {
            playerState = PlayerState.capture(player);
            initialInput = InputSnapshot.from(player.getCurrentInput());
            initialView = new ViewSnapshot(player.getYaw(), player.getPitch());
        } catch (RuntimeException ex) {
            clearPending(playerId, vesselId);
            discardCreatedVessel(vessel, origin);
            plugin.getLogger().log(Level.WARNING, "Failed to capture controller state before possession", ex);
            messages.send(player, "capture-failed");
            return;
        }

        ScheduledTask beginTask = vessel.getScheduler().run(plugin, task -> {
            VesselState vesselState = null;
            boolean committed = false;
            try {
                if (!vessel.isValid() || vessel.isDead()) {
                    notifyPlayer(player, "vessel-invalid");
                    discardCreatedVesselNow(vessel, origin);
                    return;
                }
                if (vessel.isInsideVehicle()) {
                    notifyPlayer(player, "vessel-riding");
                    discardCreatedVesselNow(vessel, origin);
                    return;
                }
                if (vesselRecovery.isMarked(vessel)) {
                    boolean recovered = vesselRecovery.recoverOrphan(vessel, removeOrphanedCreated);
                    if (recovered && vessel.isValid()) {
                        notifyPlayer(player, "vessel-recovered-retry");
                    } else {
                        notifyPlayer(player, "vessel-recovery-pending");
                    }
                    return;
                }

                if (byPlayer.containsKey(playerId) || byVessel.containsKey(vesselId)) {
                    notifyPlayer(player, "race-rejected");
                    discardCreatedVesselNow(vessel, origin);
                    return;
                }

                vesselState = VesselState.capture(vessel);
                vesselRecovery.save(vessel, origin, vesselState);
                vesselState.applyPossessionState(vessel);

                PossessionSession session = new PossessionSession(
                    playerId,
                    player,
                    vessel,
                    origin,
                    playerState,
                    vesselState,
                    initialInput,
                    initialView
                );
                session.abilityKeys(abilities.primaryLabel(vessel), abilities.secondaryLabel(vessel));

                if (byPlayer.putIfAbsent(playerId, session) != null) {
                    rollbackPreparedVessel(vessel, origin, vesselState);
                    notifyPlayer(player, "race-rejected");
                    return;
                }
                if (byVessel.putIfAbsent(vesselId, session) != null) {
                    byPlayer.remove(playerId, session);
                    rollbackPreparedVessel(vessel, origin, vesselState);
                    notifyPlayer(player, "race-rejected");
                    return;
                }

                committed = true;
                visibility.conceal(playerId, player);
                startControlLoop(session);
                attachPlayerCamera(session);
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed while starting possession for vessel " + vesselId, ex);
                if (!committed && vesselState != null) {
                    rollbackPreparedVessel(vessel, origin, vesselState);
                }
                PossessionSession session = byPlayer.get(playerId);
                if (session != null && session.vesselId().equals(vesselId)) {
                    requestRelease(session, ReleaseReason.INTERNAL_ERROR);
                } else {
                    notifyPlayer(player, "possession-internal-error");
                }
            } finally {
                clearPending(playerId, vesselId);
            }
        }, () -> deferFromRetired(() -> {
            clearPending(playerId, vesselId);
            notifyPlayer(player, "vessel-disappeared");
        }));

        if (beginTask == null) {
            clearPending(playerId, vesselId);
            notifyPlayer(player, "vessel-disappeared");
        }
    }

    private void attachPlayerCamera(PossessionSession session) {
        Player player = session.player();

        ScheduledTask attachTask = player.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !player.isOnline()) {
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
                return;
            }

            playerRecovery.save(player, session.playerState());
            startInputSampler(session);
            startHud(session);
            applyControllerIsolation(player);

            if (cameraMode == CameraTransport.SPECTATOR_TARGET) {
                attachSpectatorTargetCamera(session, false);
            } else if (cameraMode == CameraTransport.MOUNTED) {
                attachMountedCamera(session);
            } else if (cameraMode == CameraTransport.DIRECT_ENTITY) {
                attachDirectEntityCamera(session, 0);
            } else {
                if (!cameraRigManager.attach(session)) {
                    requestRelease(session, ReleaseReason.INTERNAL_ERROR);
                    return;
                }
                sendAcquiredMessage(session, "camera.rig-anchor");
            }
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (attachTask == null) {
            requestRelease(session, ReleaseReason.QUIT);
        }
    }

    private static void applyControllerIsolation(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            try {
                player.setSpectatorTarget(null);
            } catch (IllegalStateException | IllegalArgumentException ignored) {
            }
        }
        if (player.getGameMode() != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvulnerable(true);
        player.setCollidable(false);
        player.setInvisible(true);
        player.setAffectsSpawning(false);
    }

    private void attachDirectEntityCamera(PossessionSession session, int attempt) {
        Player player = session.player();
        Location destination = session.lastKnownVesselLocation();
        if (destination == null) {
            handleDirectCameraFailure(session, "camera-reason.no-position");
            return;
        }

        ViewSnapshot view = session.view();
        destination.setYaw(view.yaw());
        destination.setPitch(view.pitch());
        session.cameraTeleportInProgress(true);

        player.teleportAsync(destination).whenComplete((success, error) -> {
            ScheduledTask settleTask = player.getScheduler().run(plugin, task -> {
                session.cameraTeleportInProgress(false);
                if (!session.isActive() || !player.isOnline()) {
                    return;
                }
                if (error != null || !Boolean.TRUE.equals(success)) {
                    handleDirectCameraFailure(session, "camera-reason.move-failed");
                    return;
                }
                finishDirectEntityCameraAttach(session, attempt);
            }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
            if (settleTask == null && session.isActive()) {
                session.cameraTeleportInProgress(false);
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
            }
        });
    }

    private void finishDirectEntityCameraAttach(PossessionSession session, int attempt) {
        Player player = session.player();
        Mob vessel = session.vessel();
        if (!session.isActive() || !player.isOnline() || !vessel.isValid() || vessel.isDead()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
            return;
        }

        if (!Bukkit.isOwnedByCurrentRegion(vessel)) {
            retryDirectEntityCamera(session, attempt);
            return;
        }

        session.cameraTeleportInProgress(true);
        boolean attached;
        try {
            attached = directCameraBridge.attach(player, vessel);
        } finally {
            session.cameraTeleportInProgress(false);
        }
        if (!attached) {
            handleDirectCameraFailure(session, "camera-reason.direct-bridge-failed");
            return;
        }

        session.cameraTransport(CameraTransport.DIRECT_ENTITY);
        sendAcquiredMessage(session, "direct-entity");
    }

    private void retryDirectEntityCamera(PossessionSession session, int attempt) {
        if (attempt + 1 >= cameraAttachRetries) {
            handleDirectCameraFailure(session, "camera-reason.region-join-failed");
            return;
        }
        Player player = session.player();
        ScheduledTask retry = player.getScheduler().runDelayed(
            plugin,
            task -> attachDirectEntityCamera(session, attempt + 1),
            () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)),
            1L
        );
        if (retry == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void handleDirectCameraFailure(PossessionSession session, String reasonKey) {
        if (!session.isActive()) {
            return;
        }
        notifyPlayer(session.player(), "camera-direct-failed", Map.of(
            "reason", messages.render(session.player(), reasonKey)
        ));
        requestRelease(session, ReleaseReason.INTERNAL_ERROR);
    }

    private void attachMountedCamera(PossessionSession session) {
        Player player = session.player();
        Location destination = session.lastKnownVesselLocation();
        if (destination == null) {
            handleMountedCameraFailure(session, "camera-reason.no-position");
            return;
        }

        ViewSnapshot view = session.view();
        destination.setYaw(view.yaw());
        destination.setPitch(view.pitch());
        session.cameraTeleportInProgress(true);

        player.teleportAsync(destination).whenComplete((success, error) -> {
            ScheduledTask settleTask = player.getScheduler().run(plugin, task -> {
                session.cameraTeleportInProgress(false);
                if (!session.isActive() || !player.isOnline()) {
                    return;
                }
                if (error != null || !Boolean.TRUE.equals(success)) {
                    handleMountedCameraFailure(session, "camera-reason.move-failed");
                    return;
                }
                tryMountController(session, 0);
            }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
            if (settleTask == null && session.isActive()) {
                session.cameraTeleportInProgress(false);
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
            }
        });
    }

    private void tryMountController(PossessionSession session, int attempt) {
        Mob vessel = session.vessel();
        Player player = session.player();
        ScheduledTask mountTask = vessel.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
                return;
            }

            if (!Bukkit.isOwnedByCurrentRegion(player)) {
                retryMountedCamera(session, attempt);
                return;
            }

            if (player.isInsideVehicle() && player.getVehicle() != vessel) {
                handleMountedCameraFailure(session, "camera-reason.other-vehicle");
                return;
            }

            boolean mounted = player.getVehicle() == vessel || vessel.addPassenger(player);
            if (!mounted) {
                retryMountedCamera(session, attempt);
                return;
            }

            session.cameraTransport(CameraTransport.MOUNTED);
            if (session.vesselType() == EntityType.ENDER_DRAGON) {
                resyncMountedDragonView(session);
            }
            sendAcquiredMessage(session, "mounted-free-look");
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (mountTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void resyncMountedDragonView(PossessionSession session) {
        Player player = session.player();
        ViewSnapshot view = session.view();
        player.setRotation(view.yaw(), view.pitch());

        player.getScheduler().runDelayed(plugin, task -> {
            if (!session.isActive() || !player.isOnline() || !session.usesMountedCamera()) {
                return;
            }
            ViewSnapshot currentView = session.view();
            player.setRotation(currentView.yaw(), currentView.pitch());
        }, null, 1L);
    }

    private void retryMountedCamera(PossessionSession session, int attempt) {
        if (attempt + 1 >= cameraAttachRetries) {
            handleMountedCameraFailure(session, "camera-reason.region-join-failed");
            return;
        }
        Mob vessel = session.vessel();
        ScheduledTask retry = vessel.getScheduler().runDelayed(plugin, task -> tryMountController(session, attempt + 1), null, 1L);
        if (retry == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void handleMountedCameraFailure(PossessionSession session, String reasonKey) {
        if (!session.isActive()) {
            return;
        }
        if (cameraFallbackToSpectatorTarget) {
            notifyPlayer(session.player(), "camera-fallback", Map.of("reason", messages.render(session.player(), reasonKey)));
            attachSpectatorTargetCamera(session, true);
        } else {
            notifyPlayer(session.player(), "camera-attach-failed");
            requestRelease(session, ReleaseReason.INTERNAL_ERROR);
        }
    }

    private void attachSpectatorTargetCamera(PossessionSession session, boolean fallback) {
        Player player = session.player();
        Mob vessel = session.vessel();
        ScheduledTask cameraTask = player.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !player.isOnline()) {
                return;
            }
            session.cameraTransport(CameraTransport.SPECTATOR_TARGET);
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
            try {
                player.setSpectatorTarget(vessel);
            } catch (IllegalStateException | IllegalArgumentException ex) {
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
                return;
            }
            sendAcquiredMessage(session, fallback ? "legacy-spectator-fallback" : "spectator-target");
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (cameraTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void sendAcquiredMessage(PossessionSession session, String cameraKey) {
        Player player = session.player();
        notifyPlayer(player, "acquired", Map.of(
            "camera", messages.cameraLabel(player, cameraKey),
            "primary_action", messages.abilityLabel(player, session.primaryAbilityKey()),
            "secondary_action", messages.abilityLabel(player, session.secondaryAbilityKey())
        ));
    }

    private void startHud(PossessionSession session) {
        boolean baseHud = hudShowHealth || hudShowAbilities;
        boolean senseHud = hudShowSenses && wardenSensesEnabled && session.vesselType() == EntityType.WARDEN;
        if (!hudEnabled || (!baseHud && !senseHud)) {
            return;
        }
        Player player = session.player();
        ScheduledTask hudTask = player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!session.isActive() || !player.isOnline()) {
                task.cancel();
                return;
            }

            Component line = Component.empty();
            if (baseHud) {
                VesselTelemetry telemetry = session.telemetry();
                String messageKey = hudShowHealth && hudShowAbilities
                    ? "hud.line"
                    : hudShowHealth ? "hud.health-only" : "hud.abilities-only";

                line = messages.render(player, messageKey, Map.of(
                    "health", Component.text(formatHudNumber(telemetry.health())),
                    "max_health", Component.text(formatHudNumber(telemetry.maxHealth())),
                    "primary_action", messages.abilityLabel(player, session.primaryAbilityKey()),
                    "secondary_action", messages.abilityLabel(player, session.secondaryAbilityKey()),
                    "primary_state", cooldownState(player, session.primaryAbilityKey(), session.primaryCooldownRemainingTicks()),
                    "secondary_state", secondaryCooldownState(player, session)
                ));
            }

            Component sense = wardenSenseHud(player, session);
            if (sense != null) {
                if (baseHud) {
                    line = line.append(messages.render(player, "hud.separator"));
                }
                line = line.append(sense);
            }
            if (baseHud || sense != null) {
                player.sendActionBar(line);
            }
        }, null, 1L, hudIntervalTicks);
        session.hudTask(hudTask);
        if (hudTask == null) {
            requestRelease(session, ReleaseReason.QUIT);
        }
    }

    private Component wardenSenseHud(Player player, PossessionSession session) {
        if (!hudShowSenses || !wardenSensesEnabled || session.vesselType() != EntityType.WARDEN) {
            return null;
        }
        WardenSenseSnapshot sense = session.activeWardenSense();
        VesselPositionSnapshot position = session.vesselPosition();
        if (sense == null || position == null || !position.worldId().equals(sense.worldId())) {
            return null;
        }

        double dx = sense.x() - position.x();
        double dy = sense.y() - position.y();
        double dz = sense.z() - position.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        String direction = WardenSenseMath.directionKey(session.view().yaw(), dx, dz);
        String kind = wardenSenseShowEventKind ? sense.kind() : "vibration";

        return messages.render(player, "hud.sense", Map.of(
            "sense_kind", messages.render(player, "sense.kind." + kind),
            "sense_direction", messages.render(player, "sense.direction." + direction),
            "sense_distance", Component.text(formatHudNumber(distance))
        ));
    }

    private Component secondaryCooldownState(Player player, PossessionSession session) {
        int remainingTicks = session.secondaryCooldownRemainingTicks();
        if (remainingTicks <= 0
            && session.vesselType() == EntityType.WARDEN
            && "sonic-boom".equals(session.secondaryAbilityKey())
            && session.activeWardenSonicTargetId() == null) {
            return messages.render(player, "hud.no-target");
        }
        return cooldownState(player, session.secondaryAbilityKey(), remainingTicks);
    }

    private Component cooldownState(Player player, String abilityKey, int remainingTicks) {
        if ("none".equals(abilityKey)) {
            return messages.render(player, "hud.unavailable");
        }
        if (remainingTicks <= 0) {
            return messages.render(player, "hud.ready");
        }
        return messages.render(player, "hud.cooldown", Map.of(
            "seconds", Component.text(formatHudNumber(remainingTicks / 20.0))
        ));
    }

    private static String formatHudNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void startInputSampler(PossessionSession session) {
        Player player = session.player();
        ScheduledTask inputTask = player.getScheduler().runAtFixedRate(plugin, task -> {
            if (!session.isActive() || !player.isOnline()) {
                task.cancel();
                return;
            }
            session.input(InputSnapshot.from(player.getCurrentInput()));
            session.view(new ViewSnapshot(player.getYaw(), player.getPitch()));
        }, null, 1L, 1L);
        session.inputSamplerTask(inputTask);
        if (inputTask == null) {
            requestRelease(session, ReleaseReason.QUIT);
        }
    }

    private void startControlLoop(PossessionSession session) {
        Mob vessel = session.vessel();
        VesselController controller = controllers.controllerFor(vessel);

        ScheduledTask controlTask = vessel.getScheduler().runAtFixedRate(plugin, task -> {
            if (!session.isActive()) {
                task.cancel();
                return;
            }
            if (!vessel.isValid() || vessel.isDead()) {
                task.cancel();
                requestRelease(session, vessel.isDead() ? ReleaseReason.VESSEL_DIED : ReleaseReason.VESSEL_REMOVED);
                return;
            }

            session.advanceControlTick();
            updateVesselTelemetry(session, vessel);
            try {
                if (bodyStateLocksMovement(session, vessel)) {
                    vessel.setVelocity(new Vector());
                } else {
                    controller.tick(session, vessel);
                }
                session.lastKnownVesselLocation(vessel.getLocation());
            } catch (Throwable ex) {
                task.cancel();
                plugin.getLogger().log(Level.SEVERE, "Movement controller failed for " + vessel.getType() + " " + session.vesselId(), ex);
                requestRelease(session, ReleaseReason.INTERNAL_ERROR);
                return;
            }
            try {
                abilities.tick(session);
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.SEVERE, "Active ability tick failed for " + vessel.getType() + " " + session.vesselId(), ex);
                try {
                    abilities.abortActive(session);
                } catch (Throwable cleanupEx) {
                    plugin.getLogger().log(Level.WARNING, "Failed to abort active ability state for " + session.vesselId(), cleanupEx);
                }
            }
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)), 1L, 1L);

        session.controlTask(controlTask);
        if (controlTask == null) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private static boolean bodyStateLocksMovement(PossessionSession session, Mob vessel) {
        return vessel instanceof Axolotl axolotl && (session.axolotlPlayingDeadControlled() || axolotl.isPlayingDead())
            || vessel instanceof Fox fox && (session.foxSleepingControlled() || fox.isSleeping());
    }

    private static void updateVesselTelemetry(PossessionSession session, Mob vessel) {
        AttributeInstance maxHealth = vessel.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? Math.max(1.0, vessel.getHealth()) : maxHealth.getValue();
        session.updateTelemetry(vessel.getHealth(), max);
    }

    public void updateInput(Player player, InputSnapshot input) {
        PossessionSession session = session(player);
        if (session != null && session.isActive()) {
            session.input(input);
        }
    }

    public void updateView(Player player, float yaw, float pitch) {
        PossessionSession session = session(player);
        if (session != null && session.isActive()) {
            session.view(new ViewSnapshot(yaw, pitch));
        }
    }

    public void recordWardenGameEvent(
        UUID worldId,
        double x,
        double y,
        double z,
        String eventKey,
        int eventRadius,
        UUID sourceId,
        UUID sonicTargetId
    ) {
        if (!wardenSensesEnabled || eventRadius <= 0) {
            return;
        }
        double range = Math.min(wardenSenseMaxRange, eventRadius);
        double rangeSquared = range * range;
        String kind = WardenSenseMath.kindKey(eventKey);

        for (PossessionSession session : byPlayer.values()) {
            if (!session.isActive() || session.vesselType() != EntityType.WARDEN) {
                continue;
            }
            if (wardenSenseIgnoreSelf && sourceId != null
                && (sourceId.equals(session.playerId()) || sourceId.equals(session.vesselId()))) {
                continue;
            }
            VesselPositionSnapshot position = session.vesselPosition();
            if (position == null || !position.worldId().equals(worldId)) {
                continue;
            }
            if (position.distanceSquared(x, y, z) > rangeSquared) {
                continue;
            }
            UUID safeSonicTargetId = sonicTargetId;
            if (safeSonicTargetId != null
                && (safeSonicTargetId.equals(session.playerId()) || safeSonicTargetId.equals(session.vesselId()))) {
                safeSonicTargetId = null;
            }
            session.recordWardenSense(
                worldId, x, y, z, kind, eventKey, sourceId, safeSonicTargetId, wardenSenseMemoryTicks
            );
        }
    }

    public void triggerPrimary(Player player) {
        triggerGesture(player, AbilityGesture.PRIMARY);
    }

    public void triggerSecondary(Player player) {
        triggerGesture(player, AbilityGesture.SECONDARY);
    }

    public void triggerGesture(Player player, AbilityGesture gesture) {
        PossessionSession session = session(player);
        if (session == null || !session.isActive()) {
            return;
        }

        Mob vessel = session.vessel();
        ScheduledTask abilityTask = vessel.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
                return;
            }
            try {
                abilities.trigger(session, gesture);
            } catch (Throwable ex) {
                boolean secondary = gesture == AbilityGesture.SECONDARY;
                String kind = secondary ? "Secondary" : "Primary";
                plugin.getLogger().log(Level.SEVERE, kind + " ability gesture " + gesture + " failed for " + vessel.getType() + " " + session.vesselId(), ex);
                notifyPlayer(player, secondary ? "ability-secondary-failed" : "ability-primary-failed");
            }
        }, null);
        if (abilityTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    public void requestRelease(Player player, ReleaseReason reason) {
        PossessionSession session = session(player);
        if (session != null) {
            requestRelease(session, reason);
        }
    }

    public void requestRelease(PossessionSession session, ReleaseReason reason) {
        if (!session.deactivate()) {
            return;
        }

        restoringPlayers.add(session.playerId());
        byPlayer.remove(session.playerId(), session);
        byVessel.remove(session.vesselId(), session);
        cancelTasks(session);

        Mob vessel = session.vessel();
        ScheduledTask releaseTask = vessel.getScheduler().run(plugin, ignored -> {
            detachMountedCameraOnVesselThread(session, vessel);
            Location exit = vessel.isValid() ? findExitLocation(vessel) : session.lastKnownVesselLocation();

            if (session.origin() == PossessionOrigin.CREATED && removeCreatedOnRelease && vessel.isValid()) {
                vesselRecovery.clear(vessel);
                vessel.remove();
            } else if (vessel.isValid()) {
                restoreVesselAfterSession(session, vessel);
                vesselRecovery.clear(vessel);
            }

            restorePlayer(session, reason, exit);
        }, () -> deferFromRetired(() -> restorePlayer(session, reason, session.lastKnownVesselLocation())));
        if (releaseTask == null) {
            restorePlayer(session, reason, session.lastKnownVesselLocation());
        }
    }

    private void detachMountedCameraOnVesselThread(PossessionSession session, Mob vessel) {
        if (!session.usesMountedCamera()) {
            return;
        }
        Player player = session.player();
        if (!Bukkit.isOwnedByCurrentRegion(player)) {
            return;
        }
        if (player.getVehicle() == vessel) {
            vessel.removePassenger(player);
        }
    }

    private void cancelTasks(PossessionSession session) {
        ScheduledTask control = session.controlTask();
        if (control != null) {
            control.cancel();
        }
        ScheduledTask input = session.inputSamplerTask();
        if (input != null) {
            input.cancel();
        }
        ScheduledTask hud = session.hudTask();
        if (hud != null) {
            hud.cancel();
        }
        ScheduledTask rig = session.cameraRigTask();
        if (rig != null) {
            rig.cancel();
            session.cameraRigTask(null);
        }
    }

    private void restorePlayer(PossessionSession session, ReleaseReason reason, Location vesselLocation) {
        Player player = session.player();
        ScheduledTask restoreTask = player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }

            if (session.usesDirectEntityCamera()) {
                directCameraBridge.reset(player);
            }
            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                try {
                    player.setSpectatorTarget(null);
                } catch (IllegalStateException | IllegalArgumentException ignored) {
                }
            }
            session.cameraTeleportInProgress(false);
            session.cameraTransport(CameraTransport.NONE);

            restorePlayerState(player, session.playerState());

            Location destination = releaseAtVessel && vesselLocation != null ? vesselLocation : session.playerState().location();
            player.teleportAsync(destination).whenComplete((success, error) -> {
                ScheduledTask clearTask = player.getScheduler().run(plugin, ignored -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error == null && Boolean.TRUE.equals(success)) {
                        playerRecovery.clear(player);
                        restoringPlayers.remove(session.playerId());
                        visibility.reveal(session.playerId(), player);
                        if (reason != ReleaseReason.QUIT && reason != ReleaseReason.PLUGIN_DISABLE) {
                            messages.send(player, "released");
                        }
                    } else {
                        plugin.getLogger().warning("Release teleport failed for " + player.getUniqueId() + "; recovery marker was kept.");
                        messages.send(player, "release-teleport-failed");
                    }
                }, null);
                if (clearTask == null) {
                    // Keep recovery PDC and concealment. Recovery will finish on a safe player thread later.
                }
            });
        }, null);
        if (restoreTask == null) {
            visibility.forget(session.playerId());
        }
    }

    private void restorePlayerState(Player player, PlayerState state) {
        player.setGameMode(state.gameMode());
        player.setInvulnerable(state.invulnerable());
        player.setCollidable(state.collidable());
        player.setInvisible(state.invisible());
        player.setAffectsSpawning(state.affectsSpawning());
        player.setAllowFlight(state.allowFlight());
        player.setFlySpeed(state.flySpeed());
        player.setFlying(state.allowFlight() && state.flying());
    }

    public void releaseOnQuit(Player player) {
        PossessionSession session = session(player);
        if (session == null || !session.deactivate()) {
            return;
        }

        restoringPlayers.add(session.playerId());
        byPlayer.remove(session.playerId(), session);
        byVessel.remove(session.vesselId(), session);
        visibility.forget(session.playerId());
        cancelTasks(session);

        if (session.usesDirectEntityCamera()) {
            directCameraBridge.reset(player);
        }
        if (session.usesCameraRig()) {
            cameraRigManager.detach(session, player);
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        session.cameraTeleportInProgress(false);
        session.cameraTransport(CameraTransport.NONE);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            try {
                player.setSpectatorTarget(null);
            } catch (IllegalStateException | IllegalArgumentException ignored) {
            }
        }
        restorePlayerState(player, session.playerState());

        Mob vessel = session.vessel();
        vessel.getScheduler().run(plugin, ignored -> {
            if (session.origin() == PossessionOrigin.CREATED && removeCreatedOnRelease && vessel.isValid()) {
                vesselRecovery.clear(vessel);
                vessel.remove();
            } else if (vessel.isValid()) {
                restoreVesselAfterSession(session, vessel);
                vesselRecovery.clear(vessel);
            }
        }, null);
    }

    public void releaseOnControllerDeath(Player player) {
        PossessionSession session = session(player);
        if (session == null || !session.deactivate()) {
            return;
        }

        restoringPlayers.add(session.playerId());
        byPlayer.remove(session.playerId(), session);
        byVessel.remove(session.vesselId(), session);
        cancelTasks(session);

        if (session.usesDirectEntityCamera()) {
            directCameraBridge.reset(player);
        }
        if (session.usesCameraRig()) {
            cameraRigManager.detach(session, player);
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        session.cameraTeleportInProgress(false);
        session.cameraTransport(CameraTransport.NONE);

        Mob vessel = session.vessel();
        vessel.getScheduler().run(plugin, ignored -> {
            if (session.origin() == PossessionOrigin.CREATED && removeCreatedOnRelease && vessel.isValid()) {
                vesselRecovery.clear(vessel);
                vessel.remove();
            } else if (vessel.isValid()) {
                restoreVesselAfterSession(session, vessel);
                vesselRecovery.clear(vessel);
            }
        }, null);
    }

    public void onPlayerRespawn(Player player) {
        if (!restoringPlayers.contains(player.getUniqueId()) && !playerRecovery.hasRecovery(player)) {
            return;
        }
        player.getScheduler().runDelayed(plugin, task -> recoverPlayerIfNeeded(player), null, 1L);
    }

    public void onPlayerJoin(Player player) {
        visibility.applyToJoiningViewer(player);
        if (playerRecovery.hasRecovery(player)) {
            restoringPlayers.add(player.getUniqueId());
        }
        player.getScheduler().run(plugin, task -> recoverPlayerIfNeeded(player), null);
    }

    public void recoverPlayerIfNeeded(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerRecovery.hasRecovery(player)) {
            restoringPlayers.remove(playerId);
            recoveryInFlight.remove(playerId);
            if (visibility.isConcealed(playerId)) {
                visibility.reveal(playerId, player);
            }
            return;
        }

        restoringPlayers.add(playerId);
        if (!recoveryInFlight.add(playerId)) {
            return;
        }

        playerRecovery.recover(player).whenComplete((success, error) -> {
            recoveryInFlight.remove(playerId);
            if (error == null && Boolean.TRUE.equals(success)) {
                restoringPlayers.remove(playerId);
                visibility.reveal(playerId, player);
                notifyPlayer(player, "recovered");
            } else {
                plugin.getLogger().warning("Interrupted possession recovery is still pending for " + playerId + ".");
                notifyPlayer(player, "recovery-pending");
            }
        });
    }

    public void recoverOrphanVessel(Mob mob) {
        UUID vesselId = mob.getUniqueId();
        if (byVessel.containsKey(vesselId)) {
            return;
        }

        ScheduledTask recoveryTask = mob.getScheduler().run(plugin, task -> {
            if (byVessel.containsKey(vesselId)) {
                return;
            }
            if (vesselRecovery.recoverOrphan(mob, removeOrphanedCreated)) {
                plugin.getLogger().warning("Recovered orphaned Incarnate vessel " + vesselId + " (" + mob.getType() + ").");
            }
        }, null);
        if (recoveryTask == null) {
            // Keep the durable UUID index: an unloaded/retired entity may load again later.
        }
    }

    public void recoverIndexedVessels() {
        for (UUID vesselId : vesselRecovery.indexedVessels()) {
            Entity entity = Bukkit.getEntity(vesselId);
            if (entity instanceof Mob mob) {
                recoverOrphanVessel(mob);
            }
        }
    }

    public void recoverAlreadyOnlinePlayers() {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.getScheduler().run(plugin, task -> recoverPlayerIfNeeded(player), null);
            }
        });
    }

    public void shutdown() {
        for (PossessionSession session : List.copyOf(byPlayer.values())) {
            if (!session.deactivate()) {
                continue;
            }

            byPlayer.remove(session.playerId(), session);
            byVessel.remove(session.vesselId(), session);
            cancelTasks(session);

            Player player = session.player();
            if (Bukkit.isOwnedByCurrentRegion(player) && player.isOnline()) {
                if (session.usesDirectEntityCamera()) {
                    directCameraBridge.reset(player);
                }
                if (session.usesCameraRig()) {
                    cameraRigManager.detach(session, player);
                }
                if (player.isInsideVehicle()) {
                    player.leaveVehicle();
                }
                session.cameraTeleportInProgress(false);
                session.cameraTransport(CameraTransport.NONE);
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    try {
                        player.setSpectatorTarget(null);
                    } catch (IllegalStateException | IllegalArgumentException ignored) {
                    }
                }
                restorePlayerState(player, session.playerState());
            }

            Mob vessel = session.vessel();
            if (Bukkit.isOwnedByCurrentRegion(vessel) && vessel.isValid()) {
                if (session.origin() == PossessionOrigin.CREATED && removeCreatedOnRelease) {
                    vesselRecovery.clear(vessel);
                    vessel.remove();
                } else {
                    restoreVesselAfterSession(session, vessel);
                    vesselRecovery.clear(vessel);
                }
            }

            // JavaPlugin is already disabled before onDisable runs, so scheduling here is illegal on Folia.
            // Recovery PDC remains authoritative for state that the current region does not safely own.
            visibility.detachForDisable(session.playerId());
        }

        byPlayer.clear();
        byVessel.clear();
        pendingPlayers.clear();
        pendingVessels.clear();
        recoveryInFlight.clear();
    }

    private void deferFromRetired(Runnable action) {
        try {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, action);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.FINE, "Could not defer retired-entity recovery task", ex);
        }
    }

    private void notifyPlayer(Player player, String key) {
        notifyPlayer(player, key, Map.of());
    }

    private void notifyPlayer(Player player, String key, Map<String, Component> placeholders) {
        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                messages.send(player, key, placeholders);
            }
        }, null);
    }

    private void rejectBeforeStart(Player player, Mob vessel, PossessionOrigin origin, String key) {
        rejectBeforeStart(player, vessel, origin, key, Map.of());
    }

    private void rejectBeforeStart(Player player, Mob vessel, PossessionOrigin origin, String key, Map<String, Component> placeholders) {
        messages.send(player, key, placeholders);
        discardCreatedVessel(vessel, origin);
    }

    private void discardCreatedVessel(Mob vessel, PossessionOrigin origin) {
        if (origin != PossessionOrigin.CREATED) {
            return;
        }
        ScheduledTask task = vessel.getScheduler().run(plugin, ignored -> discardCreatedVesselNow(vessel, origin), null);
        if (task == null) {
        }
    }

    private void discardCreatedVesselNow(Mob vessel, PossessionOrigin origin) {
        if (origin == PossessionOrigin.CREATED && vessel.isValid()) {
            vesselRecovery.clear(vessel);
            vessel.remove();
        }
    }

    private static void restoreVesselAfterSession(PossessionSession session, Mob vessel) {
        if (vessel.isDead()) {
            return;
        }
        if (session.origin() == PossessionOrigin.CREATED) {
            session.vesselState().restoreRetainedCreated(vessel);
        } else {
            session.vesselState().restore(vessel);
        }
    }

    private void rollbackPreparedVessel(Mob vessel, PossessionOrigin origin, VesselState state) {
        if (origin == PossessionOrigin.CREATED) {
            discardCreatedVesselNow(vessel, origin);
            return;
        }
        if (vessel.isValid() && !vessel.isDead()) {
            state.restore(vessel);
        }
        vesselRecovery.clear(vessel);
    }

    private void clearPending(UUID playerId, UUID vesselId) {
        pendingPlayers.remove(playerId);
        pendingVessels.remove(vesselId);
    }

    private static Location findExitLocation(Mob vessel) {
        Location base = vessel.getLocation().clone();
        double[][] offsets = {
            {1.25, 0.0}, {-1.25, 0.0}, {0.0, 1.25}, {0.0, -1.25},
            {0.9, 0.9}, {-0.9, 0.9}, {0.9, -0.9}, {-0.9, -0.9}
        };

        for (double[] offset : offsets) {
            Location candidate = base.clone().add(offset[0], 0.0, offset[1]);
            if (candidate.getBlock().isPassable() && candidate.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()) {
                candidate.setYaw(base.getYaw());
                candidate.setPitch(base.getPitch());
                return candidate;
            }
        }
        return base.add(0.0, Math.max(0.25, vessel.getHeight() * 0.15), 0.0);
    }
}
