package dev.onelsey.incarnate.possession;

import org.bukkit.Bukkit;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Sittable;

public record VesselState(
    boolean aware,
    boolean ai,
    boolean persistent,
    boolean removeWhenFarAway,
    boolean aggressive,
    boolean gravity,
    LivingEntity target,
    Boolean sitting,
    Boolean camelDashing,
    Boolean batAwake,
    Boolean creeperIgnited,
    Integer creeperFuseTicks
) {
    public static VesselState capture(Mob mob) {
        Boolean sitting = mob instanceof Sittable sittable ? sittable.isSitting() : null;
        Boolean camelDashing = mob instanceof Camel camel ? camel.isDashing() : null;
        Boolean batAwake = mob instanceof Bat bat ? bat.isAwake() : null;
        Boolean creeperIgnited = mob instanceof Creeper creeper ? creeper.isIgnited() : null;
        Integer creeperFuseTicks = mob instanceof Creeper creeper ? creeper.getFuseTicks() : null;

        return new VesselState(
            mob.isAware(),
            mob.hasAI(),
            mob.isPersistent(),
            mob.getRemoveWhenFarAway(),
            mob.isAggressive(),
            mob.hasGravity(),
            mob.getTarget(),
            sitting,
            camelDashing,
            batAwake,
            creeperIgnited,
            creeperFuseTicks
        );
    }

    public void applyPossessionState(Mob mob) {
        mob.getPathfinder().stopPathfinding();
        mob.setTarget(null);
        mob.setJumping(false);
        mob.setAggressive(false);
        mob.setAI(true);
        mob.setAware(false);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);

        if (mob instanceof Sittable sittable) {
            sittable.setSitting(false);
        }
        if (mob instanceof Camel camel) {
            camel.setDashing(false);
        }
        if (mob instanceof Bat bat) {
            bat.setAwake(true);
            bat.setTargetLocation(null);
        }
    }

    public void restore(Mob mob) {
        mob.getPathfinder().stopPathfinding();
        mob.setTarget(null);
        mob.setJumping(false);
        mob.setAI(ai);
        mob.setAware(aware);
        mob.setPersistent(persistent);
        mob.setRemoveWhenFarAway(removeWhenFarAway);
        mob.setAggressive(aggressive);
        mob.setGravity(gravity);

        if (target != null && target.isValid() && !target.isDead() && Bukkit.isOwnedByCurrentRegion(target)) {
            mob.setTarget(target);
        }
        if (mob instanceof Sittable sittable && sitting != null) {
            sittable.setSitting(sitting);
        }
        if (mob instanceof Camel camel && camelDashing != null) {
            camel.setDashing(camelDashing);
        }
        if (mob instanceof Bat bat && batAwake != null) {
            bat.setAwake(batAwake);
            bat.setTargetLocation(null);
        }
        if (mob instanceof Creeper creeper && creeperIgnited != null && creeperFuseTicks != null) {
            restoreCreeper(creeper, creeperIgnited, creeperFuseTicks);
        }
    }

    static void restoreCreeper(Creeper creeper, boolean ignited, int fuseTicks) {
        creeper.setIgnited(false);
        creeper.setFuseTicks(Math.max(0, Math.min(creeper.getMaxFuseTicks(), fuseTicks)));
        if (ignited) {
            creeper.setIgnited(true);
        }
    }
}
