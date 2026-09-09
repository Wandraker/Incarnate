package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;

public class GroundVesselController implements VesselController {
    private final double fallbackWalkSpeed;
    private final double movementAttributeScale;
    private final double minimumWalkSpeed;
    private final double maximumWalkSpeed;
    private final double sprintMultiplier;
    private final double airControl;
    private final double fallbackJumpVelocity;
    private final boolean preserveVerticalVelocity;
    private final double hurtControl;
    private final double idleDamping;

    public GroundVesselController(FileConfiguration config) {
        this.fallbackWalkSpeed = config.getDouble("movement.ground.fallback-walk-speed", 0.24);
        this.movementAttributeScale = config.getDouble("movement.ground.attribute-scale", 1.0);
        this.minimumWalkSpeed = config.getDouble("movement.ground.minimum-walk-speed", 0.06);
        this.maximumWalkSpeed = config.getDouble("movement.ground.maximum-walk-speed", 0.55);
        this.sprintMultiplier = config.getDouble("movement.ground.sprint-multiplier", 1.30);
        this.airControl = MovementMath.clamp01(config.getDouble("movement.ground.air-control", 0.35));
        this.fallbackJumpVelocity = config.getDouble("movement.ground.fallback-jump-velocity", 0.42);
        this.preserveVerticalVelocity = config.getBoolean("movement.ground.preserve-existing-vertical-velocity", true);
        this.hurtControl = MovementMath.clamp01(config.getDouble("movement.ground.hurt-input-control", 0.25));
        this.idleDamping = MovementMath.clamp01(config.getDouble("movement.ground.idle-horizontal-damping", 0.65));
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        InputSnapshot input = session.input();
        ViewSnapshot view = session.view();
        applyRotation(vessel, view);

        Vector current = vessel.getVelocity();
        double vertical = preserveVerticalVelocity ? current.getY() : 0.0;
        Vector direction = MovementMath.horizontal(input, view.yaw());

        if (direction.lengthSquared() > 0.0001) {
            double speed = resolveWalkSpeed(vessel) * (input.sprint() ? sprintMultiplier : 1.0);
            double control = vessel.isOnGround() ? 1.0 : airControl;
            if (vessel.getNoDamageTicks() > 0) {
                control *= hurtControl;
            }

            current.setX(MovementMath.lerp(current.getX(), direction.getX() * speed, control));
            current.setZ(MovementMath.lerp(current.getZ(), direction.getZ() * speed, control));
        } else if (vessel.isOnGround() && vessel.getNoDamageTicks() == 0) {
            current.setX(current.getX() * idleDamping);
            current.setZ(current.getZ() * idleDamping);
        }

        current.setY(vertical);
        if (input.jump() && vessel.isOnGround()) {
            current.setY(resolveJumpVelocity(vessel));
            vessel.setJumping(true);
        } else {
            vessel.setJumping(false);
        }

        vessel.setVelocity(current);
        session.lastKnownVesselLocation(vessel.getLocation());
    }

    protected void applyRotation(Mob vessel, ViewSnapshot view) {
        float pitch = MovementMath.clampPitch(view.pitch());
        vessel.setRotation(view.yaw(), pitch);
        vessel.setBodyYaw(view.yaw());
    }

    protected double resolveWalkSpeed(Mob vessel) {
        AttributeInstance movement = vessel.getAttribute(Attribute.MOVEMENT_SPEED);
        double speed = movement == null ? fallbackWalkSpeed : movement.getValue() * movementAttributeScale;
        return Math.max(minimumWalkSpeed, Math.min(maximumWalkSpeed, speed));
    }

    protected double resolveJumpVelocity(Mob vessel) {
        AttributeInstance jump = vessel.getAttribute(Attribute.JUMP_STRENGTH);
        if (jump == null || jump.getValue() <= 0.0) {
            return fallbackJumpVelocity;
        }
        return jump.getValue();
    }
}
