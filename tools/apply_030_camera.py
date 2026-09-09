from pathlib import Path

p = Path('src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java')
text = p.read_text()

old = '''    private final boolean removeCreatedOnRelease;
    private final boolean removeOrphanedCreated;
    private final boolean releaseAtVessel;
    private final PlayerRecoveryStore playerRecovery;
'''
new = '''    private final boolean removeCreatedOnRelease;
    private final boolean removeOrphanedCreated;
    private final boolean releaseAtVessel;
    private final CameraTransport cameraMode;
    private final int cameraMountRetries;
    private final boolean cameraFallbackToSpectatorTarget;
    private final PlayerRecoveryStore playerRecovery;
'''
if text.count(old) != 1:
    raise SystemExit('camera fields anchor mismatch')
text = text.replace(old, new, 1)

old = '''        this.removeCreatedOnRelease = plugin.getConfig().getBoolean("created-vessels.remove-on-release", true);
        this.removeOrphanedCreated = plugin.getConfig().getBoolean("created-vessels.remove-orphaned-after-recovery", true);
        this.releaseAtVessel = plugin.getConfig().getBoolean("control.release-at-vessel", true);
        this.playerRecovery = new PlayerRecoveryStore(plugin);
'''
new = '''        this.removeCreatedOnRelease = plugin.getConfig().getBoolean("created-vessels.remove-on-release", true);
        this.removeOrphanedCreated = plugin.getConfig().getBoolean("created-vessels.remove-orphaned-after-recovery", true);
        this.releaseAtVessel = plugin.getConfig().getBoolean("control.release-at-vessel", true);
        CameraTransport configuredCamera;
        try {
            configuredCamera = CameraTransport.valueOf(plugin.getConfig().getString("camera.mode", "MOUNTED").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown camera.mode; using MOUNTED.");
            configuredCamera = CameraTransport.MOUNTED;
        }
        this.cameraMode = configuredCamera == CameraTransport.NONE ? CameraTransport.MOUNTED : configuredCamera;
        this.cameraMountRetries = Math.max(1, plugin.getConfig().getInt("camera.mount-retries", 8));
        this.cameraFallbackToSpectatorTarget = plugin.getConfig().getBoolean("camera.fallback-to-spectator-target", true);
        this.playerRecovery = new PlayerRecoveryStore(plugin);
'''
if text.count(old) != 1:
    raise SystemExit('constructor anchor mismatch')
text = text.replace(old, new, 1)

start = text.index('    private void attachPlayerCamera(PossessionSession session) {')
end = text.index('    private void startInputSampler(PossessionSession session) {', start)
replacement = '''    private void attachPlayerCamera(PossessionSession session) {
        Player player = session.player();

        ScheduledTask attachTask = player.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !player.isOnline()) {
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
                return;
            }

            playerRecovery.save(player, session.playerState());
            startInputSampler(session);
            player.setGameMode(GameMode.SPECTATOR);
            try {
                player.setSpectatorTarget(null);
            } catch (IllegalStateException ignored) {
            }

            if (cameraMode == CameraTransport.SPECTATOR_TARGET) {
                attachSpectatorTargetCamera(session, false);
            } else {
                attachMountedCamera(session);
            }
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (attachTask == null) {
            requestRelease(session, ReleaseReason.QUIT);
        }
    }

    private void attachMountedCamera(PossessionSession session) {
        Player player = session.player();
        Location destination = session.lastKnownVesselLocation();
        if (destination == null) {
            handleMountedCameraFailure(session, "No vessel position was available for the free-look camera.");
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
                    handleMountedCameraFailure(session, "Could not move the controller camera to the vessel safely.");
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
                handleMountedCameraFailure(session, "The controller entered another vehicle while the camera was attaching.");
                return;
            }

            boolean mounted = player.getVehicle() == vessel || vessel.addPassenger(player);
            if (!mounted) {
                retryMountedCamera(session, attempt);
                return;
            }

            session.cameraTransport(CameraTransport.MOUNTED);
            sendAcquiredMessage(session, "mounted free-look");
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (mountTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void retryMountedCamera(PossessionSession session, int attempt) {
        if (attempt + 1 >= cameraMountRetries) {
            handleMountedCameraFailure(session, "The controller and vessel could not be joined on a safe entity region.");
            return;
        }
        Mob vessel = session.vessel();
        ScheduledTask retry = vessel.getScheduler().runDelayed(plugin, task -> tryMountController(session, attempt + 1), null, 1L);
        if (retry == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void handleMountedCameraFailure(PossessionSession session, String reason) {
        if (!session.isActive()) {
            return;
        }
        if (cameraFallbackToSpectatorTarget) {
            notifyPlayer(session.player(), "[Incarnate] Free-look camera fallback: " + reason);
            attachSpectatorTargetCamera(session, true);
        } else {
            notifyPlayer(session.player(), "[Incarnate] Could not attach the free-look camera safely.");
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
            try {
                player.setSpectatorTarget(vessel);
            } catch (IllegalStateException ex) {
                requestRelease(session, ReleaseReason.VESSEL_REMOVED);
                return;
            }
            sendAcquiredMessage(session, fallback ? "legacy spectator fallback" : "spectator target");
        }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));
        if (cameraTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    private void sendAcquiredMessage(PossessionSession session, String cameraLabel) {
        Mob vessel = session.vessel();
        notifyPlayer(session.player(), "[Incarnate] Vessel acquired. Camera: " + cameraLabel + ". Left-click: "
            + abilities.primaryLabel(vessel) + ", F: " + abilities.secondaryLabel(vessel) + ", Shift+F: release.");
    }

'''
text = text[:start] + replacement + text[end:]

old = '''            try {
                controller.tick(session, vessel);
            } catch (Throwable ex) {
'''
new = '''            try {
                controller.tick(session, vessel);
                session.lastKnownVesselLocation(vessel.getLocation());
            } catch (Throwable ex) {
'''
if text.count(old) != 1:
    raise SystemExit('control tick anchor mismatch')
text = text.replace(old, new, 1)

old = '''        ScheduledTask releaseTask = vessel.getScheduler().run(plugin, ignored -> {
            Location exit = vessel.isValid() ? findExitLocation(vessel) : session.lastKnownVesselLocation();
'''
new = '''        ScheduledTask releaseTask = vessel.getScheduler().run(plugin, ignored -> {
            detachMountedCameraOnVesselThread(session, vessel);
            Location exit = vessel.isValid() ? findExitLocation(vessel) : session.lastKnownVesselLocation();
'''
if text.count(old) != 1:
    raise SystemExit('release detach anchor mismatch')
text = text.replace(old, new, 1)

old = '''            if (player.getGameMode() == GameMode.SPECTATOR) {
                try {
                    player.setSpectatorTarget(null);
                } catch (IllegalStateException ignored) {
                }
            }

            restorePlayerState(player, session.playerState());
'''
new = '''            if (player.isInsideVehicle()) {
                player.leaveVehicle();
            }
            if (player.getGameMode() == GameMode.SPECTATOR) {
                try {
                    player.setSpectatorTarget(null);
                } catch (IllegalStateException ignored) {
                }
            }
            session.cameraTeleportInProgress(false);
            session.cameraTransport(CameraTransport.NONE);

            restorePlayerState(player, session.playerState());
'''
if text.count(old) != 1:
    raise SystemExit('restore camera anchor mismatch')
text = text.replace(old, new, 1)

old = '''        visibility.forget(session.playerId());
        cancelTasks(session);

        if (player.getGameMode() == GameMode.SPECTATOR) {
'''
new = '''        visibility.forget(session.playerId());
        cancelTasks(session);

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        session.cameraTeleportInProgress(false);
        session.cameraTransport(CameraTransport.NONE);
        if (player.getGameMode() == GameMode.SPECTATOR) {
'''
if text.count(old) != 1:
    raise SystemExit('quit camera anchor mismatch')
text = text.replace(old, new, 1)

old = '''        byPlayer.remove(session.playerId(), session);
        byVessel.remove(session.vesselId(), session);
        cancelTasks(session);

        Mob vessel = session.vessel();
'''
new = '''        byPlayer.remove(session.playerId(), session);
        byVessel.remove(session.vesselId(), session);
        cancelTasks(session);

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        session.cameraTeleportInProgress(false);
        session.cameraTransport(CameraTransport.NONE);

        Mob vessel = session.vessel();
'''
# This exact block should occur only in releaseOnControllerDeath because quit has visibility line.
if text.count(old) != 1:
    raise SystemExit(f'death camera anchor mismatch: {text.count(old)}')
text = text.replace(old, new, 1)

old = '''            Player player = session.player();
            if (Bukkit.isOwnedByCurrentRegion(player) && player.isOnline()) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
'''
new = '''            Player player = session.player();
            if (Bukkit.isOwnedByCurrentRegion(player) && player.isOnline()) {
                if (player.isInsideVehicle()) {
                    player.leaveVehicle();
                }
                session.cameraTeleportInProgress(false);
                session.cameraTransport(CameraTransport.NONE);
                if (player.getGameMode() == GameMode.SPECTATOR) {
'''
if text.count(old) != 1:
    raise SystemExit('shutdown camera anchor mismatch')
text = text.replace(old, new, 1)

insert_anchor = '''    private void cancelTasks(PossessionSession session) {
'''
helper = '''    private void detachMountedCameraOnVesselThread(PossessionSession session, Mob vessel) {
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

'''
if text.count(insert_anchor) != 1:
    raise SystemExit('detach helper anchor mismatch')
text = text.replace(insert_anchor, helper + insert_anchor, 1)

p.write_text(text)
print('0.3.0 mounted camera patch applied')
