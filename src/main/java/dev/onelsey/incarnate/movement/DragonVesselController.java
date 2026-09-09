package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Mob;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public final class DragonVesselController implements VesselController {
    private static final double MOTION_EPSILON_SQUARED = 1.0E-6;

    private final double speed;
    private final double sprintMultiplier;
    private final double acceleration;
    private final double idleDamping;
    private final double hurtControl;

    public DragonVesselController(FileConfiguration config) {
        this.speed = Math.max(0.05, config.getDouble("movement.dragon.speed", 0.48));
        this.sprintMultiplier = Math.max(1.0, config.getDouble("movement.dragon.sprint-multiplier", 1.35));
        this.acceleration = MovementMath.clamp01(config.getDouble("movement.dragon.acceleration", 0.42));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.dragon.idle-damping", 0.78));
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.dragon.hurt-input-control", 0.35));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        if (!(vessel instanceof EnderDragon dragon)) {
            return;
        }

        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        float bodyYaw = MovementMath.dragonBodyYaw(view.yaw());
        float bodyPitch = MovementMath.clampPitch(view.pitch());

        if (dragon.getPhase() != EnderDragon.Phase.HOVER) {
            dragon.setPhase(EnderDragon.Phase.HOVER);
        }
        dragon.setAware(false);
        dragon.setTarget(null);
        dragon.setGravity(false);
        dragon.setRotation(bodyYaw, bodyPitch);
        dragon.setBodyYaw(bodyYaw);

        if (session.dragonTransferInProgress()) {
            dragon.setVelocity(new Vector());
            return;
        }

        if (session.isMovementControlLocked()) {
            session.dragonControlMotion(new Vector());
            dragon.setVelocity(new Vector());
            session.lastKnownVesselLocation(dragon.getLocation());
            return;
        }

        double vertical = (input.jump() ? 1.0 : 0.0) - (input.sneak() ? 1.0 : 0.0);
        Vector wanted = MovementMath.threeDimensional(input, view, vertical);
        Vector motion = session.dragonControlMotion();

        if (wanted.lengthSquared() > 0.0001) {
            double targetSpeed = speed * (input.sprint() ? sprintMultiplier : 1.0);
            double control = dragon.getNoDamageTicks() > 0 ? acceleration * hurtControl : acceleration;
            wanted.multiply(targetSpeed);
            motion.setX(MovementMath.lerp(motion.getX(), wanted.getX(), control));
            motion.setY(MovementMath.lerp(motion.getY(), wanted.getY(), control));
            motion.setZ(MovementMath.lerp(motion.getZ(), wanted.getZ(), control));
        } else {
            motion.multiply(idleDamping);
        }

        if (motion.lengthSquared() < MOTION_EPSILON_SQUARED) {
            motion.zero();
        }
        session.dragonControlMotion(motion);

        // HOVER intentionally remains active so vanilla podium/portal routing stays disabled.
        // Its own phase target is the current position, so Bukkit velocity alone is consumed by
        // the dragon flight controller. Move the real entity explicitly and keep vanilla delta
        // motion zero so the phase cannot apply the same displacement a second time.
        dragon.setVelocity(new Vector());
        if (motion.lengthSquared() < MOTION_EPSILON_SQUARED) {
            session.lastKnownVesselLocation(dragon.getLocation());
            return;
        }

        Location target = dragon.getLocation().add(motion);
        target.setYaw(bodyYaw);
        target.setPitch(bodyPitch);

        if (Bukkit.isOwnedByCurrentRegion(target)) {
            boolean moved = dragon.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
            if (!moved) {
                session.dragonControlMotion(new Vector());
            }
            dragon.setVelocity(new Vector());
            session.lastKnownVesselLocation(dragon.getLocation());
            return;
        }

        if (!session.beginDragonTransfer()) {
            return;
        }

        dragon.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                session.dragonControlMotion(new Vector());
            }
            session.endDragonTransfer();
        });
    }
}
