package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class HoppingVesselController extends GroundVesselController {
    private final int hopIntervalTicks;
    private final double jumpMultiplier;
    private final double sprintMultiplier;
    private final double airControl;
    private final double idleDamping;
    private final double hurtControl;

    public HoppingVesselController(FileConfiguration config) {
        super(config);
        this.hopIntervalTicks = Math.max(1, config.getInt("movement.hopping.hop-interval-ticks", 5));
        this.jumpMultiplier = Math.max(0.1, config.getDouble("movement.hopping.jump-multiplier", 1.0));
        this.sprintMultiplier = Math.max(0.1, config.getDouble("movement.hopping.sprint-multiplier", 1.2));
        this.airControl = MovementMath.clamp01(config.getDouble("movement.hopping.air-control", 0.20));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.hopping.idle-horizontal-damping", 0.70));
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.hopping.hurt-input-control", 0.25));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        applyRotation(vessel, view);

        if (session.isMovementControlLocked()) {
            vessel.setJumping(false);
            session.lastKnownVesselLocation(vessel.getLocation());
            return;
        }

        Vector current = vessel.getVelocity();
        Vector direction = MovementMath.horizontal(input, view.yaw());
        boolean moving = direction.lengthSquared() > 0.0001;
        double speed = resolveWalkSpeed(vessel) * (input.sprint() ? sprintMultiplier : 1.0);

        if (vessel.isOnGround()) {
            boolean rhythmicHop = moving && session.controlTick() % hopIntervalTicks == 0;
            if (input.jump() || rhythmicHop) {
                if (moving) {
                    double control = vessel.getNoDamageTicks() > 0 ? hurtControl : 1.0;
                    current.setX(MovementMath.lerp(current.getX(), direction.getX() * speed, control));
                    current.setZ(MovementMath.lerp(current.getZ(), direction.getZ() * speed, control));
                }
                current.setY(Math.max(current.getY(), resolveJumpVelocity(vessel) * jumpMultiplier));
                vessel.setJumping(true);
            } else {
                vessel.setJumping(false);
                if (vessel.getNoDamageTicks() == 0) {
                    current.setX(current.getX() * idleDamping);
                    current.setZ(current.getZ() * idleDamping);
                }
            }
        } else {
            vessel.setJumping(false);
            if (moving) {
                double control = vessel.getNoDamageTicks() > 0 ? airControl * hurtControl : airControl;
                current.setX(MovementMath.lerp(current.getX(), direction.getX() * speed, control));
                current.setZ(MovementMath.lerp(current.getZ(), direction.getZ() * speed, control));
            }
        }

        vessel.setVelocity(current);
        session.lastKnownVesselLocation(vessel.getLocation());
    }
}
