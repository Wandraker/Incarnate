package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public final class FlightVesselController implements VesselController {
    private final double fallbackSpeed;
    private final double attributeScale;
    private final double minimumSpeed;
    private final double maximumSpeed;
    private final double sprintMultiplier;
    private final double acceleration;
    private final double idleDamping;
    private final double hurtControl;

    public FlightVesselController(FileConfiguration config) {
        this.fallbackSpeed = config.getDouble("movement.flight.speed", 0.34);
        this.attributeScale = config.getDouble("movement.flight.attribute-scale", 1.0);
        this.minimumSpeed = config.getDouble("movement.flight.minimum-speed", 0.12);
        this.maximumSpeed = config.getDouble("movement.flight.maximum-speed", 0.65);
        this.sprintMultiplier = config.getDouble("movement.flight.sprint-multiplier", 1.35);
        this.acceleration = MovementMath.clamp01(config.getDouble("movement.flight.acceleration", 0.55));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.flight.idle-damping", 0.72));
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.flight.hurt-input-control", 0.30));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        vessel.setRotation(view.yaw(), MovementMath.clampPitch(view.pitch()));
        vessel.setBodyYaw(view.yaw());
        vessel.setJumping(false);
        vessel.setGravity(false);

        double vertical = (input.jump() ? 1.0 : 0.0) - (input.sneak() ? 1.0 : 0.0);
        Vector wanted = MovementMath.threeDimensional(input, view, vertical);
        Vector current = vessel.getVelocity();

        if (wanted.lengthSquared() > 0.0001) {
            double targetSpeed = resolveFlyingSpeed(vessel) * (input.sprint() ? sprintMultiplier : 1.0);
            double control = vessel.getNoDamageTicks() > 0 ? acceleration * hurtControl : acceleration;
            wanted.multiply(targetSpeed);
            current.setX(MovementMath.lerp(current.getX(), wanted.getX(), control));
            current.setY(MovementMath.lerp(current.getY(), wanted.getY(), control));
            current.setZ(MovementMath.lerp(current.getZ(), wanted.getZ(), control));
        } else if (vessel.getNoDamageTicks() == 0) {
            current.multiply(idleDamping);
        }

        vessel.setVelocity(current);
        session.lastKnownVesselLocation(vessel.getLocation());
    }

    private double resolveFlyingSpeed(Mob vessel) {
        AttributeInstance flying = vessel.getAttribute(Attribute.FLYING_SPEED);
        double resolved = flying == null || flying.getValue() <= 0.0
            ? fallbackSpeed
            : flying.getValue() * attributeScale;
        return Math.max(minimumSpeed, Math.min(maximumSpeed, resolved));
    }
}
