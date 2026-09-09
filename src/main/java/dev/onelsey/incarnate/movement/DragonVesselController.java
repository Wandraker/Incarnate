package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class DragonVesselController implements VesselController {
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

        if (dragon.getPhase() != EnderDragon.Phase.HOVER) {
            dragon.setPhase(EnderDragon.Phase.HOVER);
        }
        dragon.setAware(false);
        dragon.setTarget(null);
        dragon.setGravity(false);
        dragon.setRotation(view.yaw(), MovementMath.clampPitch(view.pitch()));
        dragon.setBodyYaw(view.yaw());

        if (session.isMovementControlLocked()) {
            dragon.setVelocity(new Vector());
            session.lastKnownVesselLocation(dragon.getLocation());
            return;
        }

        double vertical = (input.jump() ? 1.0 : 0.0) - (input.sneak() ? 1.0 : 0.0);
        Vector wanted = MovementMath.threeDimensional(input, view, vertical);
        Vector current = dragon.getVelocity();

        if (wanted.lengthSquared() > 0.0001) {
            double targetSpeed = speed * (input.sprint() ? sprintMultiplier : 1.0);
            double control = dragon.getNoDamageTicks() > 0 ? acceleration * hurtControl : acceleration;
            wanted.multiply(targetSpeed);
            current.setX(MovementMath.lerp(current.getX(), wanted.getX(), control));
            current.setY(MovementMath.lerp(current.getY(), wanted.getY(), control));
            current.setZ(MovementMath.lerp(current.getZ(), wanted.getZ(), control));
        } else if (dragon.getNoDamageTicks() == 0) {
            current.multiply(idleDamping);
        }

        dragon.setVelocity(current);
        session.lastKnownVesselLocation(dragon.getLocation());
    }
}
