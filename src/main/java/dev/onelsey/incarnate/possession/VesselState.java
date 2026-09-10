package dev.onelsey.incarnate.possession;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Panda;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Sniffer;
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
    Spellcaster.Spell spellcasterSpell,
    EnderDragon.Phase dragonPhase,
    Location dragonPodium,
    Float shulkerPeek,
    BlockFace shulkerAttachedFace,
    Entity frogTongueTarget,
    Sniffer.State snifferState,
    Boolean axolotlPlayingDead,
    Integer foxBodyState,
    Integer pandaBodyState
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
        EnderDragon.Phase dragonPhase = mob instanceof EnderDragon dragon ? dragon.getPhase() : null;
        Location dragonPodium = mob instanceof EnderDragon dragon ? dragon.getPodium().clone() : null;
        Float shulkerPeek = mob instanceof Shulker shulker ? shulker.getPeek() : null;
        BlockFace shulkerAttachedFace = mob instanceof Shulker shulker ? shulker.getAttachedFace() : null;
        Entity frogTongueTarget = mob instanceof Frog frog ? frog.getTongueTarget() : null;
        Sniffer.State snifferState = mob instanceof Sniffer sniffer ? sniffer.getState() : null;
        Boolean axolotlPlayingDead = mob instanceof Axolotl axolotl ? axolotl.isPlayingDead() : null;
        Integer foxBodyState = mob instanceof Fox fox ? BodyStateCodec.captureFox(fox) : null;
        Integer pandaBodyState = mob instanceof Panda panda ? BodyStateCodec.capturePanda(panda) : null;

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
            spellcasterSpell,
            dragonPhase,
            dragonPodium,
            shulkerPeek,
            shulkerAttachedFace,
            frogTongueTarget,
            snifferState,
            axolotlPlayingDead,
            foxBodyState,
            pandaBodyState
        );
    }

    public void applyPossessionState(Mob mob) {
        stopPathfinding(mob);
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
        if (mob instanceof EnderDragon dragon) {
            dragon.setPhase(EnderDragon.Phase.HOVER);
            dragon.setVelocity(new org.bukkit.util.Vector());
            dragon.setGravity(false);
        }
        if (mob instanceof Shulker shulker) {
            shulker.setVelocity(new org.bukkit.util.Vector());
            shulker.setGravity(false);
        }
        if (mob instanceof Frog frog) {
            frog.setTongueTarget(null);
        }
        if (mob instanceof Sniffer sniffer) {
            sniffer.setState(Sniffer.State.IDLING);
        }
        if (mob instanceof Axolotl axolotl) {
            axolotl.setPlayingDead(false);
        }
        if (mob instanceof Fox fox) {
            BodyStateCodec.clearFoxForPossession(fox);
        }
        if (mob instanceof Panda panda) {
            BodyStateCodec.clearPandaForPossession(panda);
        }
    }

    public void restore(Mob mob) {
        restoreCommon(mob);
        restoreSpecial(mob);
    }

    public void restoreRetainedCreated(Mob mob) {
        restoreCommon(mob);
        restoreSpecial(mob);
        if (mob instanceof EnderDragon dragon) {
            dragon.setVelocity(new org.bukkit.util.Vector());
            dragon.setPhase(EnderDragon.Phase.HOVER);
            dragon.setPodium(dragon.getLocation());
        }
    }

    private void restoreCommon(Mob mob) {
        stopPathfinding(mob);
        mob.setTarget(null);
        mob.setJumping(false);
        mob.setAI(ai);
        mob.setAware(aware);
        mob.setPersistent(persistent);
        mob.setRemoveWhenFarAway(removeWhenFarAway);
        mob.setAggressive(aggressive);
        mob.setGravity(gravity);
    }

    private void restoreSpecial(Mob mob) {
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
        if (mob instanceof EnderDragon dragon) {
            if (dragonPodium != null && dragonPodium.getWorld() == dragon.getWorld()) {
                dragon.setPodium(dragonPodium.clone());
            }
            if (dragonPhase != null && dragonPhase != EnderDragon.Phase.DYING) {
                dragon.setPhase(dragonPhase);
            }
        }
        if (mob instanceof Shulker shulker) {
            if (shulkerAttachedFace != null) {
                shulker.setAttachedFace(shulkerAttachedFace);
            }
            if (shulkerPeek != null) {
                shulker.setPeek(Math.max(0.0f, Math.min(1.0f, shulkerPeek)));
            }
        }
        if (mob instanceof Frog frog) {
            if (frogTongueTarget != null
                && Bukkit.isOwnedByCurrentRegion(frogTongueTarget)
                && frogTongueTarget.isValid()
                && frogTongueTarget.getWorld() == frog.getWorld()) {
                frog.setTongueTarget(frogTongueTarget);
            } else {
                frog.setTongueTarget(null);
            }
        }
        if (mob instanceof Sniffer sniffer && snifferState != null) {
            sniffer.setState(snifferState);
        }
        if (mob instanceof Axolotl axolotl && axolotlPlayingDead != null) {
            axolotl.setPlayingDead(axolotlPlayingDead);
        }
        if (mob instanceof Fox fox && foxBodyState != null) {
            BodyStateCodec.restoreFox(fox, foxBodyState);
        }
        if (mob instanceof Panda panda && pandaBodyState != null) {
            BodyStateCodec.restorePanda(panda, pandaBodyState);
        }
    }

    private static void stopPathfinding(Mob mob) {
        if (mob instanceof EnderDragon) {
            return;
        }
        mob.getPathfinder().stopPathfinding();
    }

    static void restoreCreeper(Creeper creeper, boolean ignited, int fuseTicks) {
        creeper.setIgnited(false);
        creeper.setFuseTicks(Math.max(0, Math.min(creeper.getMaxFuseTicks(), fuseTicks)));
        if (ignited) {
            creeper.setIgnited(true);
        }
    }
}
