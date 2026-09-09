package dev.onelsey.incarnate.possession;

import org.bukkit.Bukkit;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.Vex;

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
    Integer creeperFuseTicks,
    Boolean guardianLaserActive,
    Integer guardianLaserTicks,
    Integer pufferFishPuffState,
    Boolean vexCharging,
    Integer ravagerAttackTicks,
    Integer ravagerStunnedTicks,
    Integer ravagerRoarTicks,
    Spellcaster.Spell spellcasterSpell
) {
    public static VesselState capture(Mob mob) {
        Boolean sitting = mob instanceof Sittable sittable ? sittable.isSitting() : null;
        Boolean camelDashing = mob instanceof Camel camel ? camel.isDashing() : null;
        Boolean batAwake = mob instanceof Bat bat ? bat.isAwake() : null;
        Boolean creeperIgnited = mob instanceof Creeper creeper ? creeper.isIgnited() : null;
        Integer creeperFuseTicks = mob instanceof Creeper creeper ? creeper.getFuseTicks() : null;
        Boolean guardianLaserActive = mob instanceof Guardian guardian ? guardian.hasLaser() : null;
        Integer guardianLaserTicks = mob instanceof Guardian guardian ? guardian.getLaserTicks() : null;
        Integer pufferFishPuffState = mob instanceof PufferFish pufferFish ? pufferFish.getPuffState() : null;
        Boolean vexCharging = mob instanceof Vex vex ? vex.isCharging() : null;
        Integer ravagerAttackTicks = mob instanceof Ravager ravager ? ravager.getAttackTicks() : null;
        Integer ravagerStunnedTicks = mob instanceof Ravager ravager ? ravager.getStunnedTicks() : null;
        Integer ravagerRoarTicks = mob instanceof Ravager ravager ? ravager.getRoarTicks() : null;
        Spellcaster.Spell spellcasterSpell = mob instanceof Spellcaster spellcaster ? spellcaster.getSpell() : null;

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
            creeperFuseTicks,
            guardianLaserActive,
            guardianLaserTicks,
            pufferFishPuffState,
            vexCharging,
            ravagerAttackTicks,
            ravagerStunnedTicks,
            ravagerRoarTicks,
            spellcasterSpell
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
        if (mob instanceof Guardian guardian) {
            guardian.setLaser(false);
        }
        if (mob instanceof Vex vex) {
            vex.setCharging(false);
        }
        if (mob instanceof Ravager ravager) {
            ravager.setAttackTicks(-1);
            ravager.setStunnedTicks(-1);
            ravager.setRoarTicks(-1);
        }
        if (mob instanceof Spellcaster spellcaster) {
            spellcaster.setSpell(Spellcaster.Spell.NONE);
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

        boolean targetRestored = false;
        if (target != null && Bukkit.isOwnedByCurrentRegion(target) && target.isValid() && !target.isDead()) {
            mob.setTarget(target);
            targetRestored = true;
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
        if (mob instanceof PufferFish pufferFish && pufferFishPuffState != null) {
            pufferFish.setPuffState(Math.max(0, Math.min(2, pufferFishPuffState)));
        }
        if (mob instanceof Vex vex && vexCharging != null) {
            vex.setCharging(vexCharging);
        }
        if (mob instanceof Ravager ravager && ravagerAttackTicks != null && ravagerStunnedTicks != null && ravagerRoarTicks != null) {
            ravager.setAttackTicks(ravagerAttackTicks);
            ravager.setStunnedTicks(ravagerStunnedTicks);
            ravager.setRoarTicks(ravagerRoarTicks);
        }
        if (mob instanceof Spellcaster spellcaster && spellcasterSpell != null) {
            spellcaster.setSpell(spellcasterSpell);
        }
        if (mob instanceof Guardian guardian) {
            guardian.setLaser(false);
            if (Boolean.TRUE.equals(guardianLaserActive) && guardianLaserTicks != null && targetRestored && guardian.setLaser(true)) {
                int safeTicks = Math.max(-10, Math.min(Math.max(-10, guardian.getLaserDuration() - 1), guardianLaserTicks));
                guardian.setLaserTicks(safeTicks);
            }
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
