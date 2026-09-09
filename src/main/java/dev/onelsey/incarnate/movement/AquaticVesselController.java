package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class AquaticVesselController implements VesselController {
    private final double speed;
    private final double sprintMultiplier;
    private final double acceleration;
    private final double idleDamping;
    private final double beachedHopVelocity;
    private final double hurtControl;

    public AquaticVesselController(FileConfiguration config) {
        this.speed = config.getDouble("movement.aquatic.speed", 0.27);
        this.sprintMultiplier = config.getDouble("movement.aquatic.sprint-multiplier", 1.25);
        this.acceleration = MovementMath.clamp01(config.getDouble("movement.aquatic.acceleration", 0.45));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.aquatic.idle-damping", 0.82));
        this.beachedHopVelocity = config.getDouble("movement.aquatic.beached-hop-velocity", 0.28);
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.aquatic.hurt-input-control", 0.35));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        vessel.setRotation(view.yaw(), MovementMath.clampPitch(view.pitch()));
        vessel.setBodyYaw(view.yaw());
        vessel.setJumping(false);

        if (session.isMovementControlLocked()) {
            session.lastKnownVesselLocation(vessel.getLocation());
            return;
        }

        if (!vessel.isInWater()) {
            Vector beached = vessel.getVelocity();
            if (vessel.getNoDamageTicks() == 0) {
                beached.setX(beached.getX() * idleDamping);
                beached.setZ(beached.getZ() * idleDamping);
            }
            boolean wantsMove = Math.abs(input.forwardAxis()) > 0.01 || Math.abs(input.strafeAxis()) > 0.01;
            if (vessel.isOnGround() && (input.jump() || (wantsMove && session.controlTick() % 8 == 0))) {
                beached.setY(Math.max(beached.getY(), beachedHopVelocity));
                vessel.setJumping(true);
            }
            vessel.setVelocity(beached);
            session.lastKnownVesselLocation(vessel.getLocation());
            return;
        }

        double vertical = (input.jump() ? 1.0 : 0.0) - (input.sneak() ? 1.0 : 0.0);
        Vector wanted = MovementMath.threeDimensional(input, view, vertical);
        Vector current = vessel.getVelocity();

        if (wanted.lengthSquared() > 0.0001) {
            double control = vessel.getNoDamageTicks() > 0 ? acceleration * hurtControl : acceleration;
            wanted.multiply(speed * (input.sprint() ? sprintMultiplier : 1.0));
            current.setX(MovementMath.lerp(current.getX(), wanted.getX(), control));
            current.setY(MovementMath.lerp(current.getY(), wanted.getY(), control));
            current.setZ(MovementMath.lerp(current.getZ(), wanted.getZ(), control));
        } else if (vessel.getNoDamageTicks() == 0) {
            current.multiply(idleDamping);
        }

        vessel.setVelocity(current);
        session.lastKnownVesselLocation(vessel.getLocation());
    }
}
