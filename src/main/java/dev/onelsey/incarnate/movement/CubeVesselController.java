package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractCubeMob;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class CubeVesselController implements VesselController {
    private final double speed;
    private final double sprintMultiplier;
    private final double baseHopVelocity;
    private final double sizeHopBonus;
    private final int hopIntervalTicks;
    private final double idleDamping;
    private final double hurtControl;

    public CubeVesselController(FileConfiguration config) {
        this.speed = config.getDouble("movement.cube.speed", 0.23);
        this.sprintMultiplier = config.getDouble("movement.cube.sprint-multiplier", 1.25);
        this.baseHopVelocity = config.getDouble("movement.cube.base-hop-velocity", 0.42);
        this.sizeHopBonus = config.getDouble("movement.cube.size-hop-bonus", 0.015);
        this.hopIntervalTicks = Math.max(1, config.getInt("movement.cube.hop-interval-ticks", 6));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.cube.idle-damping", 0.72));
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.cube.hurt-input-control", 0.25));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        vessel.setRotation(view.yaw(), MovementMath.clampPitch(view.pitch()));
        vessel.setBodyYaw(view.yaw());

        if (session.isMovementControlLocked()) {
            vessel.setJumping(false);
            session.lastKnownVesselLocation(vessel.getLocation());
            return;
        }

        Vector current = vessel.getVelocity();
        Vector direction = MovementMath.horizontal(input, view.yaw());
        boolean moving = direction.lengthSquared() > 0.0001;
        if (moving) {
            double target = speed * (input.sprint() ? sprintMultiplier : 1.0);
            if (vessel.getNoDamageTicks() > 0) {
                current.setX(MovementMath.lerp(current.getX(), direction.getX() * target, hurtControl));
                current.setZ(MovementMath.lerp(current.getZ(), direction.getZ() * target, hurtControl));
            } else {
                current.setX(direction.getX() * target);
                current.setZ(direction.getZ() * target);
            }
        } else if (vessel.isOnGround() && vessel.getNoDamageTicks() == 0) {
            current.setX(current.getX() * idleDamping);
            current.setZ(current.getZ() * idleDamping);
        }

        boolean manualJump = input.jump();
        boolean rhythmicHop = moving && session.controlTick() % hopIntervalTicks == 0;
        if (vessel.isOnGround() && (manualJump || rhythmicHop)) {
            int size = vessel instanceof AbstractCubeMob cube ? cube.getSize() : 1;
            current.setY(baseHopVelocity + Math.max(0, size - 1) * sizeHopBonus);
            vessel.setJumping(true);
        } else {
            vessel.setJumping(false);
        }

        vessel.setVelocity(current);
        session.lastKnownVesselLocation(vessel.getLocation());
    }
}
