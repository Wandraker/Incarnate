package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PossessionSession {
    private final UUID playerId;
    private final String playerName;
    private final UUID vesselId;
    private final Player player;
    private final Mob vessel;
    private final PossessionOrigin origin;
    private final PlayerState playerState;
    private final VesselState vesselState;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final CooldownWindow primaryCooldown = new CooldownWindow();
    private final CooldownWindow secondaryCooldown = new CooldownWindow();

    private volatile InputSnapshot input;
    private volatile ViewSnapshot view;
    private volatile Location lastKnownVesselLocation;
    private volatile ScheduledTask controlTask;
    private volatile ScheduledTask inputSamplerTask;
    private volatile ScheduledTask hudTask;
    private volatile long controlTick;
    private volatile long movementControlLockedUntilTick = Long.MIN_VALUE;
    private volatile boolean guardianLaserActive;
    private volatile boolean guardianLaserSeenActive;
    private volatile UUID guardianLaserTargetId;
    private volatile long guardianLaserDeadlineTick = Long.MIN_VALUE;
    private volatile long vexChargeUntilTick = Long.MIN_VALUE;
    private volatile long evokerCastUntilTick = Long.MIN_VALUE;
    private volatile long lastSpectatorShiftAttemptNanos = Long.MIN_VALUE;
    private volatile CameraTransport cameraTransport = CameraTransport.NONE;
    private volatile boolean cameraTeleportInProgress;
    private volatile String primaryAbilityKey = "none";
    private volatile String secondaryAbilityKey = "none";
    private volatile VesselTelemetry telemetry;

    public PossessionSession(
        UUID playerId,
        Player player,
        Mob vessel,
        PossessionOrigin origin,
        PlayerState playerState,
        VesselState vesselState,
        InputSnapshot initialInput,
        ViewSnapshot initialView
    ) {
        this.playerId = playerId;
        this.playerName = player.getName();
        this.vesselId = vessel.getUniqueId();
        this.player = player;
        this.vessel = vessel;
        this.origin = origin;
        this.playerState = playerState;
        this.vesselState = vesselState;
        this.input = initialInput;
        this.view = initialView;
        this.lastKnownVesselLocation = vessel.getLocation().clone();
        this.telemetry = new VesselTelemetry(vessel.getHealth(), maxHealth(vessel));
    }

    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public UUID vesselId() { return vesselId; }
    public Player player() { return player; }
    public Mob vessel() { return vessel; }
    public PossessionOrigin origin() { return origin; }
    public PlayerState playerState() { return playerState; }
    public VesselState vesselState() { return vesselState; }
    public boolean isActive() { return active.get(); }
    public boolean deactivate() { return active.compareAndSet(true, false); }
    public InputSnapshot input() { return input; }
    public void input(InputSnapshot input) { this.input = input; }
    public ViewSnapshot view() { return view; }
    public void view(ViewSnapshot view) { this.view = view; }

    public Location lastKnownVesselLocation() {
        Location location = lastKnownVesselLocation;
        return location == null ? null : location.clone();
    }

    public void lastKnownVesselLocation(Location location) {
        if (location != null) {
            this.lastKnownVesselLocation = location.clone();
        }
    }

    public ScheduledTask controlTask() { return controlTask; }
    public void controlTask(ScheduledTask controlTask) { this.controlTask = controlTask; }
    public ScheduledTask inputSamplerTask() { return inputSamplerTask; }
    public void inputSamplerTask(ScheduledTask task) { this.inputSamplerTask = task; }
    public ScheduledTask hudTask() { return hudTask; }
    public void hudTask(ScheduledTask task) { this.hudTask = task; }

    public CameraTransport cameraTransport() { return cameraTransport; }
    public void cameraTransport(CameraTransport cameraTransport) { this.cameraTransport = cameraTransport; }
    public boolean usesMountedCamera() { return cameraTransport == CameraTransport.MOUNTED; }
    public boolean usesSpectatorTargetCamera() { return cameraTransport == CameraTransport.SPECTATOR_TARGET; }
    public boolean cameraTeleportInProgress() { return cameraTeleportInProgress; }
    public void cameraTeleportInProgress(boolean value) { this.cameraTeleportInProgress = value; }

    public long controlTick() { return controlTick; }
    public long advanceControlTick() { return ++controlTick; }

    public boolean acquirePrimaryCooldown(int cooldownTicks) {
        return primaryCooldown.tryAcquire(controlTick, cooldownTicks);
    }

    public boolean acquireSecondaryCooldown(int cooldownTicks) {
        return secondaryCooldown.tryAcquire(controlTick, cooldownTicks);
    }

    public int primaryCooldownRemainingTicks() {
        return primaryCooldown.remaining(controlTick);
    }

    public int secondaryCooldownRemainingTicks() {
        return secondaryCooldown.remaining(controlTick);
    }

    public void abilityKeys(String primary, String secondary) {
        this.primaryAbilityKey = primary == null ? "none" : primary;
        this.secondaryAbilityKey = secondary == null ? "none" : secondary;
    }

    public String primaryAbilityKey() { return primaryAbilityKey; }
    public String secondaryAbilityKey() { return secondaryAbilityKey; }

    public VesselTelemetry telemetry() { return telemetry; }

    public void updateTelemetry(double health, double maxHealth) {
        telemetry = new VesselTelemetry(health, maxHealth);
    }

    public void lockMovementControl(int ticks) {
        long until = controlTick + Math.max(1, ticks);
        if (movementControlLockedUntilTick == Long.MIN_VALUE || until > movementControlLockedUntilTick) {
            movementControlLockedUntilTick = until;
        }
    }

    public boolean isMovementControlLocked() {
        return movementControlLockedUntilTick != Long.MIN_VALUE && controlTick <= movementControlLockedUntilTick;
    }

    public boolean guardianLaserActive() { return guardianLaserActive; }

    public void startGuardianLaser(UUID targetId, int timeoutTicks) {
        guardianLaserActive = true;
        guardianLaserSeenActive = false;
        guardianLaserTargetId = targetId;
        guardianLaserDeadlineTick = controlTick + Math.max(1, timeoutTicks);
    }

    public UUID guardianLaserTargetId() { return guardianLaserTargetId; }
    public boolean guardianLaserSeenActive() { return guardianLaserSeenActive; }
    public void markGuardianLaserSeenActive() { guardianLaserSeenActive = true; }

    public boolean guardianLaserExpired() {
        return guardianLaserActive && guardianLaserDeadlineTick != Long.MIN_VALUE && controlTick > guardianLaserDeadlineTick;
    }

    public void clearGuardianLaser() {
        guardianLaserActive = false;
        guardianLaserSeenActive = false;
        guardianLaserTargetId = null;
        guardianLaserDeadlineTick = Long.MIN_VALUE;
    }

    public void startVexCharge(int ticks) {
        vexChargeUntilTick = controlTick + Math.max(1, ticks);
    }

    public boolean vexChargeActive() {
        return vexChargeUntilTick != Long.MIN_VALUE && controlTick <= vexChargeUntilTick;
    }

    public void clearVexCharge() {
        vexChargeUntilTick = Long.MIN_VALUE;
    }

    public void startEvokerCast(int ticks) {
        evokerCastUntilTick = controlTick + Math.max(1, ticks);
    }

    public boolean evokerCastTracked() {
        return evokerCastUntilTick != Long.MIN_VALUE;
    }

    public boolean evokerCastExpired() {
        return evokerCastTracked() && controlTick > evokerCastUntilTick;
    }

    public void clearEvokerCast() {
        evokerCastUntilTick = Long.MIN_VALUE;
    }

    public void markSpectatorShiftAttempt() {
        this.lastSpectatorShiftAttemptNanos = System.nanoTime();
    }

    public boolean hasRecentSpectatorShiftAttempt(long windowNanos) {
        long last = lastSpectatorShiftAttemptNanos;
        return last != Long.MIN_VALUE && System.nanoTime() - last <= windowNanos;
    }

    private static double maxHealth(Mob vessel) {
        AttributeInstance attribute = vessel.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? Math.max(vessel.getHealth(), 1.0) : attribute.getValue();
    }
}
