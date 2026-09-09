package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class ClimbingVesselController extends GroundVesselController {
    private final double climbVelocity;
    private final double probeDistance;

    public ClimbingVesselController(FileConfiguration config) {
        super(config);
        this.climbVelocity = config.getDouble("movement.climbing.climb-velocity", 0.24);
        this.probeDistance = Math.max(0.15, config.getDouble("movement.climbing.wall-probe-distance", 0.45));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        super.tick(session, vessel);
        if (session.isMovementControlLocked()) {
            return;
        }
        if (session.input().forwardAxis() <= 0.0 && !session.input().jump()) {
            return;
        }

        Vector forward = MovementMath.direction(session.view().yaw(), 0.0f).setY(0.0);
        if (forward.lengthSquared() < 0.0001) {
            return;
        }
        forward.normalize().multiply(probeDistance);

        Location probe = vessel.getLocation().clone().add(forward).add(0.0, Math.min(0.8, vessel.getHeight() * 0.4), 0.0);
        if (probe.getBlock().isPassable()) {
            return;
        }

        Vector velocity = vessel.getVelocity();
        velocity.setY(Math.max(velocity.getY(), climbVelocity));
        vessel.setVelocity(velocity);
    }
}
