package dev.onelsey.incarnate.possession;

import org.bukkit.entity.Bat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Mob;

public record VesselState(
    boolean aware,
    boolean ai,
    boolean persistent,
    boolean removeWhenFarAway,
    boolean aggressive,
    boolean gravity,
    Boolean batAwake,
    Boolean creeperIgnited,
    Integer creeperFuseTicks
) {
    public static VesselState capture(Mob mob) {
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
