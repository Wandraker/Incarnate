package dev.onelsey.incarnate.ability;

import com.destroystokyo.paper.entity.RangedEntity;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.BreezeWindCharge;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AbilityRegistry {
    private final boolean skeletonEnabled;
    private final boolean skeletonRequireBow;
    private final double skeletonArrowSpeed;
    private final int skeletonCooldownTicks;
    private final boolean nativeProjectilesEnabled;
    private final double nativeProjectileSpeed;
    private final int nativeProjectileCooldownTicks;
    private final boolean nativeRangedEnabled;
    private final double nativeRangedRange;
    private final double nativeRangedRaySize;
    private final float nativeRangedCharge;
    private final int nativeRangedCooldownTicks;
    private final boolean creeperEnabled;
    private final int creeperCooldownTicks;
    private final boolean meleeEnabled;
    private final double meleeRange;
    private final double meleeRaySize;
    private final int meleeCooldownTicks;
    private final boolean endermanTeleportEnabled;
    private final double endermanTeleportDistance;
    private final int endermanTeleportCooldownTicks;
    private final boolean spiderPounceEnabled;
    private final double spiderPounceHorizontalSpeed;
    private final double spiderPounceVerticalVelocity;
    private final int spiderPounceCooldownTicks;
    private final int spiderPounceLockTicks;
    private final boolean camelDashEnabled;
    private final double camelDashHorizontalSpeed;
    private final double camelDashVerticalVelocity;
    private final int camelDashCooldownTicks;
    private final int camelDashLockTicks;
    private final boolean guardianLaserEnabled;
    private final double guardianLaserRange;
    private final double guardianLaserRaySize;
    private final int guardianLaserCooldownTicks;
    private final boolean pufferFishPuffEnabled;
    private final int pufferFishPuffCooldownTicks;
    private final boolean vexChargeEnabled;
    private final double vexChargeSpeed;
    private final int vexChargeDurationTicks;
    private final int vexChargeCooldownTicks;
    private final int vexChargeLockTicks;
    private final boolean ravagerRoarEnabled;
    private final int ravagerRoarCooldownTicks;
    private final int ravagerRoarLockTicks;
    private final boolean evokerFangsEnabled;
    private final int evokerFangsCount;
    private final double evokerFangsSpacing;
    private final int evokerFangsCooldownTicks;
    private final int evokerFangsCastTicks;
    private final int evokerFangsAttackDelayStep;
    private final int evokerFangsGroundSearchBlocks;
    private final boolean dragonFireballEnabled;
    private final double dragonFireballSpeed;
    private final int dragonFireballCooldownTicks;
    private final boolean shulkerBulletEnabled;
    private final double shulkerBulletRange;
    private final double shulkerBulletRaySize;
    private final int shulkerBulletCooldownTicks;
    private final boolean shulkerShellEnabled;
    private final int shulkerShellCooldownTicks;
    private final boolean frogTongueEnabled;
    private final double frogTongueRange;
    private final double frogTongueRaySize;
    private final int frogTongueDurationTicks;
    private final int frogTongueCooldownTicks;
    private final boolean axolotlPlayDeadEnabled;
    private final int axolotlPlayDeadCooldownTicks;
    private final boolean foxPounceEnabled;
    private final double foxPounceHorizontalSpeed;
    private final double foxPounceVerticalVelocity;
    private final int foxPounceDurationTicks;
    private final int foxPounceCooldownTicks;
    private final int foxPounceLockTicks;
    private final boolean foxSleepEnabled;
    private final int foxSleepCooldownTicks;
    private final boolean pandaRollEnabled;
    private final double pandaRollSpeed;
    private final double pandaRollVerticalVelocity;
    private final int pandaRollDurationTicks;
    private final int pandaRollCooldownTicks;
    private final int pandaRollLockTicks;
    private final boolean wardenSonicEnabled;
    private final double wardenSonicRange;
    private final double wardenSonicDamage;
    private final double wardenSonicHorizontalKnockback;
    private final double wardenSonicVerticalKnockback;
    private final double wardenSonicParticleStep;
    private final int wardenSonicChargeTicks;
    private final int wardenSonicCooldownTicks;
    private final boolean ghastFireballEnabled;
    private final double ghastFireballSpeed;
    private final int ghastFireballChargeTicks;
    private final int ghastFireballCooldownTicks;

    public AbilityRegistry(FileConfiguration config) {
        this.skeletonEnabled = config.getBoolean("abilities.skeleton.enabled", true);
        this.skeletonRequireBow = config.getBoolean("abilities.skeleton.require-bow", true);
        this.skeletonArrowSpeed = config.getDouble("abilities.skeleton.arrow-speed", 3.0);
        this.skeletonCooldownTicks = Math.max(1, config.getInt("abilities.skeleton.cooldown-ticks", 12));
        this.nativeProjectilesEnabled = config.getBoolean("abilities.native-projectiles.enabled", true);
        this.nativeProjectileSpeed = config.getDouble("abilities.native-projectiles.speed", 1.5);
        this.nativeProjectileCooldownTicks = Math.max(1, config.getInt("abilities.native-projectiles.cooldown-ticks", 14));
        this.nativeRangedEnabled = config.getBoolean("abilities.native-ranged.enabled", true);
        this.nativeRangedRange = Math.max(2.0, config.getDouble("abilities.native-ranged.range", 24.0));
        this.nativeRangedRaySize = Math.max(0.0, config.getDouble("abilities.native-ranged.ray-size", 0.35));
        this.nativeRangedCharge = (float) Math.max(0.0, Math.min(1.0, config.getDouble("abilities.native-ranged.charge", 1.0)));
        this.nativeRangedCooldownTicks = Math.max(1, config.getInt("abilities.native-ranged.cooldown-ticks", 20));
        this.creeperEnabled = config.getBoolean("abilities.creeper.enabled", true);
        this.creeperCooldownTicks = Math.max(1, config.getInt("abilities.creeper.cooldown-ticks", 5));
        this.meleeEnabled = config.getBoolean("abilities.melee.enabled", true);
        this.meleeRange = Math.max(1.0, config.getDouble("abilities.melee.range", 3.25));
        this.meleeRaySize = Math.max(0.0, config.getDouble("abilities.melee.ray-size", 0.30));
        this.meleeCooldownTicks = Math.max(1, config.getInt("abilities.melee.cooldown-ticks", 10));
        this.endermanTeleportEnabled = config.getBoolean("abilities.enderman-teleport.enabled", true);
        this.endermanTeleportDistance = Math.max(2.0, config.getDouble("abilities.enderman-teleport.max-distance", 12.0));
        this.endermanTeleportCooldownTicks = Math.max(1, config.getInt("abilities.enderman-teleport.cooldown-ticks", 30));
        this.spiderPounceEnabled = config.getBoolean("abilities.spider-pounce.enabled", true);
        this.spiderPounceHorizontalSpeed = Math.max(0.05, config.getDouble("abilities.spider-pounce.horizontal-speed", 0.85));
        this.spiderPounceVerticalVelocity = Math.max(0.05, config.getDouble("abilities.spider-pounce.vertical-velocity", 0.42));
        this.spiderPounceCooldownTicks = Math.max(1, config.getInt("abilities.spider-pounce.cooldown-ticks", 18));
        this.spiderPounceLockTicks = Math.max(1, config.getInt("abilities.spider-pounce.movement-lock-ticks", 5));
        this.camelDashEnabled = config.getBoolean("abilities.camel-dash.enabled", true);
        this.camelDashHorizontalSpeed = Math.max(0.05, config.getDouble("abilities.camel-dash.horizontal-speed", 1.15));
        this.camelDashVerticalVelocity = Math.max(0.0, config.getDouble("abilities.camel-dash.vertical-velocity", 0.12));
        this.camelDashCooldownTicks = Math.max(1, config.getInt("abilities.camel-dash.cooldown-ticks", 30));
        this.camelDashLockTicks = Math.max(1, config.getInt("abilities.camel-dash.movement-lock-ticks", 8));
        this.guardianLaserEnabled = config.getBoolean("abilities.guardian-laser.enabled", true);
        this.guardianLaserRange = Math.max(2.0, config.getDouble("abilities.guardian-laser.range", 20.0));
        this.guardianLaserRaySize = Math.max(0.0, config.getDouble("abilities.guardian-laser.ray-size", 0.35));
        this.guardianLaserCooldownTicks = Math.max(1, config.getInt("abilities.guardian-laser.cooldown-ticks", 100));
        this.pufferFishPuffEnabled = config.getBoolean("abilities.pufferfish-puff.enabled", true);
        this.pufferFishPuffCooldownTicks = Math.max(1, config.getInt("abilities.pufferfish-puff.cooldown-ticks", 8));
        this.vexChargeEnabled = config.getBoolean("abilities.vex-charge.enabled", true);
        this.vexChargeSpeed = Math.max(0.05, config.getDouble("abilities.vex-charge.speed", 1.25));
        this.vexChargeDurationTicks = Math.max(1, config.getInt("abilities.vex-charge.duration-ticks", 8));
        this.vexChargeCooldownTicks = Math.max(1, config.getInt("abilities.vex-charge.cooldown-ticks", 24));
        this.vexChargeLockTicks = Math.max(1, config.getInt("abilities.vex-charge.movement-lock-ticks", 8));
        this.ravagerRoarEnabled = config.getBoolean("abilities.ravager-roar.enabled", true);
        this.ravagerRoarCooldownTicks = Math.max(1, config.getInt("abilities.ravager-roar.cooldown-ticks", 80));
        this.ravagerRoarLockTicks = Math.max(1, config.getInt("abilities.ravager-roar.movement-lock-ticks", 12));
        this.evokerFangsEnabled = config.getBoolean("abilities.evoker-fangs.enabled", true);
        this.evokerFangsCount = Math.max(1, Math.min(16, config.getInt("abilities.evoker-fangs.count", 8)));
        this.evokerFangsSpacing = Math.max(0.25, config.getDouble("abilities.evoker-fangs.spacing", 1.25));
        this.evokerFangsCooldownTicks = Math.max(1, config.getInt("abilities.evoker-fangs.cooldown-ticks", 50));
        this.evokerFangsCastTicks = Math.max(1, config.getInt("abilities.evoker-fangs.cast-ticks", 20));
        this.evokerFangsAttackDelayStep = Math.max(0, config.getInt("abilities.evoker-fangs.attack-delay-step", 2));
        this.evokerFangsGroundSearchBlocks = Math.max(1, Math.min(8, config.getInt("abilities.evoker-fangs.ground-search-blocks", 3)));
        this.dragonFireballEnabled = config.getBoolean("abilities.dragon-fireball.enabled", true);
        this.dragonFireballSpeed = Math.max(0.1, config.getDouble("abilities.dragon-fireball.speed", 1.35));
        this.dragonFireballCooldownTicks = Math.max(1, config.getInt("abilities.dragon-fireball.cooldown-ticks", 30));
        this.shulkerBulletEnabled = config.getBoolean("abilities.shulker-bullet.enabled", true);
        this.shulkerBulletRange = Math.max(2.0, config.getDouble("abilities.shulker-bullet.range", 24.0));
        this.shulkerBulletRaySize = Math.max(0.0, config.getDouble("abilities.shulker-bullet.ray-size", 0.35));
        this.shulkerBulletCooldownTicks = Math.max(1, config.getInt("abilities.shulker-bullet.cooldown-ticks", 40));
        this.shulkerShellEnabled = config.getBoolean("abilities.shulker-shell.enabled", true);
        this.shulkerShellCooldownTicks = Math.max(1, config.getInt("abilities.shulker-shell.cooldown-ticks", 6));
        this.frogTongueEnabled = config.getBoolean("abilities.frog-tongue.enabled", true);
        this.frogTongueRange = Math.max(2.0, config.getDouble("abilities.frog-tongue.range", 10.0));
        this.frogTongueRaySize = Math.max(0.0, config.getDouble("abilities.frog-tongue.ray-size", 0.30));
        this.frogTongueDurationTicks = Math.max(1, config.getInt("abilities.frog-tongue.duration-ticks", 12));
        this.frogTongueCooldownTicks = Math.max(1, config.getInt("abilities.frog-tongue.cooldown-ticks", 24));
        this.axolotlPlayDeadEnabled = config.getBoolean("abilities.axolotl-play-dead.enabled", true);
        this.axolotlPlayDeadCooldownTicks = Math.max(1, config.getInt("abilities.axolotl-play-dead.cooldown-ticks", 8));
        this.foxPounceEnabled = config.getBoolean("abilities.fox-pounce.enabled", true);
        this.foxPounceHorizontalSpeed = Math.max(0.05, config.getDouble("abilities.fox-pounce.horizontal-speed", 0.95));
        this.foxPounceVerticalVelocity = Math.max(0.05, config.getDouble("abilities.fox-pounce.vertical-velocity", 0.45));
        this.foxPounceDurationTicks = Math.max(1, config.getInt("abilities.fox-pounce.duration-ticks", 10));
        this.foxPounceCooldownTicks = Math.max(1, config.getInt("abilities.fox-pounce.cooldown-ticks", 24));
        this.foxPounceLockTicks = Math.max(1, config.getInt("abilities.fox-pounce.movement-lock-ticks", 6));
        this.foxSleepEnabled = config.getBoolean("abilities.fox-sleep.enabled", true);
        this.foxSleepCooldownTicks = Math.max(1, config.getInt("abilities.fox-sleep.cooldown-ticks", 8));
        this.pandaRollEnabled = config.getBoolean("abilities.panda-roll.enabled", true);
        this.pandaRollSpeed = Math.max(0.05, config.getDouble("abilities.panda-roll.speed", 0.60));
        this.pandaRollVerticalVelocity = Math.max(0.0, config.getDouble("abilities.panda-roll.vertical-velocity", 0.10));
        this.pandaRollDurationTicks = Math.max(1, config.getInt("abilities.panda-roll.duration-ticks", 12));
        this.pandaRollCooldownTicks = Math.max(1, config.getInt("abilities.panda-roll.cooldown-ticks", 30));
        this.pandaRollLockTicks = Math.max(1, config.getInt("abilities.panda-roll.movement-lock-ticks", 12));
        this.wardenSonicEnabled = config.getBoolean("abilities.warden-sonic.enabled", true);
        this.wardenSonicRange = Math.max(2.0, config.getDouble("abilities.warden-sonic.range", 20.0));
        this.wardenSonicDamage = Math.max(0.0, config.getDouble("abilities.warden-sonic.damage", 10.0));
        this.wardenSonicHorizontalKnockback = Math.max(0.0, config.getDouble("abilities.warden-sonic.horizontal-knockback", 2.5));
        this.wardenSonicVerticalKnockback = Math.max(0.0, config.getDouble("abilities.warden-sonic.vertical-knockback", 0.5));
        this.wardenSonicParticleStep = Math.max(0.25, config.getDouble("abilities.warden-sonic.particle-step", 0.5));
        this.wardenSonicChargeTicks = Math.max(1, config.getInt("abilities.warden-sonic.charge-ticks", 34));
        this.wardenSonicCooldownTicks = Math.max(1, config.getInt("abilities.warden-sonic.cooldown-ticks", 80));
        this.ghastFireballEnabled = config.getBoolean("abilities.ghast-fireball.enabled", true);
        this.ghastFireballSpeed = Math.max(0.1, config.getDouble("abilities.ghast-fireball.speed", 1.0));
        this.ghastFireballChargeTicks = Math.max(1, config.getInt("abilities.ghast-fireball.charge-ticks", 20));
        this.ghastFireballCooldownTicks = Math.max(1, config.getInt("abilities.ghast-fireball.cooldown-ticks", 60));
    }

    public boolean trigger(PossessionSession session, AbilityGesture gesture) {
        return switch (gesture) {
            case PRIMARY -> triggerPrimaryInternal(session);
            case SECONDARY -> triggerSecondaryInternal(session);
            case SNEAK_PRIMARY -> triggerSneakPrimary(session);
            case SPRINT_PRIMARY -> triggerSprintPrimary(session);
        };
    }

    public boolean triggerPrimary(PossessionSession session) {
        return trigger(session, AbilityGesture.PRIMARY);
    }

    public boolean triggerSecondary(PossessionSession session) {
        return trigger(session, AbilityGesture.SECONDARY);
    }

    private boolean triggerPrimaryInternal(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }

        if (vessel instanceof Axolotl axolotl && (session.axolotlPlayingDeadControlled() || axolotl.isPlayingDead())) {
            return false;
        }
        if (vessel instanceof Fox fox && (session.foxSleepingControlled() || fox.isSleeping())) {
            return false;
        }
        if (vessel instanceof EnderDragon dragon && dragonFireballEnabled) {
            return shootDragonFireball(session, dragon);
        }
        if (vessel instanceof Shulker shulker && shulkerBulletEnabled) {
            return shootShulkerBullet(session, shulker);
        }
        if (vessel instanceof Guardian guardian && guardianLaserEnabled) {
            return startGuardianLaser(session, guardian);
        }
        if (vessel instanceof Creeper creeper && creeperEnabled) {
            return toggleCreeper(session, creeper);
        }
        if (vessel instanceof Ghast ghast && ghastFireballEnabled) {
            return startGhastFireball(session, ghast);
        }
        if (vessel instanceof AbstractSkeleton skeleton && skeletonEnabled) {
            if (!skeletonRequireBow || hasBow(skeleton)) {
                return shootSkeleton(session, skeleton);
            }
            return melee(session, vessel);
        }
        if (nativeProjectilesEnabled && projectileFor(vessel) != null) {
            return shootNativeProjectile(session, vessel);
        }
        if (nativeRangedEnabled && vessel instanceof RangedEntity ranged && supportsNativeRanged(vessel)) {
            if (nativeRanged(session, vessel, ranged)) {
                return true;
            }
        }
        return melee(session, vessel);
    }

    private boolean triggerSecondaryInternal(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }

        if (vessel instanceof Axolotl axolotl && axolotlPlayDeadEnabled) {
            return toggleAxolotlPlayDead(session, axolotl);
        }
        if (vessel instanceof Fox fox && foxSleepEnabled) {
            return toggleFoxSleep(session, fox);
        }
        if (vessel instanceof Panda panda && pandaRollEnabled) {
            return rollPanda(session, panda);
        }
        if (vessel instanceof Warden warden && wardenSonicEnabled) {
            return startWardenSonic(session, warden);
        }
        if (vessel instanceof Shulker shulker && shulkerShellEnabled) {
            return toggleShulkerShell(session, shulker);
        }
        if (vessel instanceof PufferFish pufferFish && pufferFishPuffEnabled) {
            return togglePufferFish(session, pufferFish);
        }
        if (vessel instanceof Vex vex && vexChargeEnabled) {
            return chargeVex(session, vex);
        }
        if (vessel instanceof Evoker evoker && evokerFangsEnabled) {
            return castEvokerFangs(session, evoker);
        }
        if (vessel instanceof Enderman enderman && endermanTeleportEnabled) {
            return teleportEnderman(session, enderman);
        }
        if (vessel instanceof Spider spider && spiderPounceEnabled) {
            return pounceSpider(session, spider);
        }
        if (vessel instanceof Camel camel && camelDashEnabled) {
            return dashCamel(session, camel);
        }
        if (vessel instanceof Ravager ravager && ravagerRoarEnabled) {
            return roarRavager(session, ravager);
        }
        return false;
    }

    private boolean triggerSprintPrimary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }
        if (vessel instanceof Fox fox && foxPounceEnabled) {
            return pounceFox(session, fox);
        }
        return triggerPrimaryInternal(session);
    }

    private boolean triggerSneakPrimary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }
        if (vessel instanceof Frog frog && frogTongueEnabled) {
            return startFrogTongue(session, frog);
        }
        return triggerPrimaryInternal(session);
    }

    public void tick(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return;
        }
        if (vessel instanceof Axolotl axolotl) {
            maintainAxolotlPlayDead(session, axolotl);
        }
        if (vessel instanceof Fox fox) {
            maintainFoxSleep(session, fox);
        }
        if (vessel instanceof Guardian guardian) {
            tickGuardianLaser(session, guardian);
        }
        if (vessel instanceof Vex vex) {
            tickVexCharge(session, vex);
        }
        if (vessel instanceof Evoker evoker) {
            tickEvokerCast(session, evoker);
        }
        if (vessel instanceof Frog frog) {
            tickFrogTongue(session, frog);
        }
        if (vessel instanceof Fox fox) {
            tickFoxPounce(session, fox);
        }
        if (vessel instanceof Panda panda) {
            tickPandaRoll(session, panda);
        }
        if (vessel instanceof Warden warden) {
            tickWardenSonic(session, warden);
        }
        if (vessel instanceof Ghast ghast) {
            tickGhastFireball(session, ghast);
        }
    }

    public void abortActive(PossessionSession session) {
        Mob vessel = session.vessel();
        if (vessel instanceof Guardian guardian && session.guardianLaserActive()) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            guardian.setAware(false);
            session.clearGuardianLaser();
        }
        if (vessel instanceof Vex vex && session.vexChargeActive()) {
            vex.setCharging(false);
            session.clearVexCharge();
        }
        if (vessel instanceof Evoker evoker && session.evokerCastTracked()) {
            if (evoker.getSpell() == Spellcaster.Spell.FANGS) {
                evoker.setSpell(Spellcaster.Spell.NONE);
            }
            session.clearEvokerCast();
        }
        if (vessel instanceof Frog frog && session.frogTongueTracked()) {
            frog.setTongueTarget(null);
            session.clearFrogTongue();
        }
        if (vessel instanceof Fox fox && session.foxPounceTracked()) {
            fox.setLeaping(false);
            session.clearFoxPounce();
        }
        if (vessel instanceof Panda panda && session.pandaRollTracked()) {
            panda.setRolling(false);
            session.clearPandaRoll();
        }
        if (vessel instanceof Warden && session.wardenSonicTracked()) {
            session.clearWardenSonic();
        }
        if (vessel instanceof Ghast ghast && session.ghastFireballTracked()) {
            ghast.setCharging(false);
            session.clearGhastFireball();
        }
    }

    public String primaryLabel(Mob vessel) {
        if (vessel instanceof EnderDragon && dragonFireballEnabled) return "dragon-fireball";
        if (vessel instanceof Shulker && shulkerBulletEnabled) return "shulker-bullet";
        if (vessel instanceof Frog && frogTongueEnabled) return "frog-melee-tongue";
        if (vessel instanceof Fox && foxPounceEnabled) return "fox-melee-pounce";
        if (vessel instanceof Guardian && guardianLaserEnabled) return "guardian-laser";
        if (vessel instanceof Creeper && creeperEnabled) return "fuse";
        if (vessel instanceof Ghast && ghastFireballEnabled) return "ghast-fireball";
        if (vessel instanceof AbstractSkeleton && skeletonEnabled) return "arrow-melee";
        if (nativeProjectilesEnabled && projectileFor(vessel) != null) return "projectile";
        if (nativeRangedEnabled && vessel instanceof RangedEntity && supportsNativeRanged(vessel)) return "ranged-melee";
        return meleeEnabled ? "melee" : "none";
    }

    public String secondaryLabel(Mob vessel) {
        if (vessel instanceof Axolotl && axolotlPlayDeadEnabled) return "axolotl-play-dead";
        if (vessel instanceof Fox && foxSleepEnabled) return "fox-sleep";
        if (vessel instanceof Panda && pandaRollEnabled) return "panda-roll";
        if (vessel instanceof Warden && wardenSonicEnabled) return "sonic-boom";
        if (vessel instanceof Shulker && shulkerShellEnabled) return "shulker-shell";
        if (vessel instanceof PufferFish && pufferFishPuffEnabled) return "puff";
        if (vessel instanceof Vex && vexChargeEnabled) return "charge";
        if (vessel instanceof Evoker && evokerFangsEnabled) return "fangs";
        if (vessel instanceof Enderman && endermanTeleportEnabled) return "teleport";
        if (vessel instanceof Spider && spiderPounceEnabled) return "pounce";
        if (vessel instanceof Camel && camelDashEnabled) return "dash";
        if (vessel instanceof Ravager && ravagerRoarEnabled) return "roar";
        return "none";
    }

    private boolean startWardenSonic(PossessionSession session, Warden warden) {
        if (session.wardenSonicTracked()) {
            return false;
        }
        var sense = session.activeWardenSense();
        if (sense == null || sense.sourceId() == null) {
            return false;
        }

        Entity rawTarget = Bukkit.getEntity(sense.sourceId());
        if (!(rawTarget instanceof LivingEntity target)
            || !Bukkit.isOwnedByCurrentRegion(target)
            || !target.isValid()
            || target.isDead()
            || target.getUniqueId().equals(session.playerId())
            || target.getUniqueId().equals(session.vesselId())
            || target.getWorld() != warden.getWorld()
            || warden.getEyeLocation().distanceSquared(target.getEyeLocation()) > wardenSonicRange * wardenSonicRange
            || !session.acquireSecondaryCooldown(Math.max(wardenSonicCooldownTicks, wardenSonicChargeTicks))) {
            return false;
        }

        session.startWardenSonic(target.getUniqueId(), wardenSonicChargeTicks);
        session.lockMovementControl(wardenSonicChargeTicks + 2);
        warden.setVelocity(new Vector());
        warden.getWorld().playSound(
            warden.getLocation(),
            Sound.ENTITY_WARDEN_SONIC_CHARGE,
            SoundCategory.HOSTILE,
            3.0f,
            1.0f
        );
        return true;
    }

    private void tickWardenSonic(PossessionSession session, Warden warden) {
        if (!session.wardenSonicTracked()) {
            return;
        }

        UUID targetId = session.wardenSonicTargetId();
        Entity rawTarget = targetId == null ? null : Bukkit.getEntity(targetId);
        if (!(rawTarget instanceof LivingEntity target)
            || !Bukkit.isOwnedByCurrentRegion(target)
            || !target.isValid()
            || target.isDead()
            || target.getWorld() != warden.getWorld()
            || warden.getEyeLocation().distanceSquared(target.getEyeLocation()) > wardenSonicRange * wardenSonicRange) {
            session.clearWardenSonic();
            return;
        }
        if (!session.wardenSonicReady()) {
            warden.setVelocity(new Vector());
            return;
        }

        emitWardenSonic(warden, target);
        session.clearWardenSonic();
    }

    private void emitWardenSonic(Warden warden, LivingEntity target) {
        Location start = warden.getEyeLocation();
        Location end = target.getEyeLocation();
        Vector delta = end.toVector().subtract(start.toVector());
        double distance = delta.length();
        if (distance < 1.0E-6) {
            return;
        }

        Vector normalized = delta.clone().multiply(1.0 / distance);
        for (double travel = 1.0; travel < distance; travel += wardenSonicParticleStep) {
            Location point = start.clone().add(normalized.clone().multiply(travel));
            if (!Bukkit.isOwnedByCurrentRegion(point)) {
                break;
            }
            warden.getWorld().spawnParticle(Particle.SONIC_BOOM, point, 1);
        }
        warden.getWorld().playSound(
            warden.getLocation(),
            Sound.ENTITY_WARDEN_SONIC_BOOM,
            SoundCategory.HOSTILE,
            3.0f,
            1.0f
        );

        DamageSource source = DamageSource.builder(DamageType.SONIC_BOOM)
            .withCausingEntity(warden)
            .withDirectEntity(warden)
            .build();
        target.damage(wardenSonicDamage, source);

        AttributeInstance resistanceAttribute = target.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        double resistance = resistanceAttribute == null ? 0.0 : resistanceAttribute.getValue();
        double scale = WardenSonicMath.resistanceScale(resistance);
        Vector knockback = new Vector(
            normalized.getX() * wardenSonicHorizontalKnockback * scale,
            normalized.getY() * wardenSonicVerticalKnockback * scale,
            normalized.getZ() * wardenSonicHorizontalKnockback * scale
        );
        target.setVelocity(target.getVelocity().add(knockback));
    }

    private boolean startGhastFireball(PossessionSession session, Ghast ghast) {
        if (session.ghastFireballTracked()
            || !session.acquirePrimaryCooldown(Math.max(ghastFireballCooldownTicks, ghastFireballChargeTicks))) {
            return false;
        }

        ghast.setCharging(true);
        ghast.setVelocity(new Vector());
        session.startGhastFireball(ghastFireballChargeTicks);
        session.lockMovementControl(ghastFireballChargeTicks);
        ghast.getWorld().playSound(
            ghast.getLocation(),
            Sound.ENTITY_GHAST_WARN,
            SoundCategory.HOSTILE,
            10.0f,
            1.0f
        );
        return true;
    }

    private void tickGhastFireball(PossessionSession session, Ghast ghast) {
        if (!session.ghastFireballTracked()) {
            return;
        }
        if (!ghast.isCharging()) {
            ghast.setCharging(true);
        }
        if (!session.ghastFireballReady()) {
            ghast.setVelocity(new Vector());
            return;
        }

        Vector velocity = direction(session.view()).multiply(ghastFireballSpeed);
        LargeFireball fireball = ghast.launchProjectile(LargeFireball.class, velocity);
        fireball.setYield(ghast.getExplosionPower());
        ghast.getWorld().playSound(
            ghast.getLocation(),
            Sound.ENTITY_GHAST_SHOOT,
            SoundCategory.HOSTILE,
            10.0f,
            1.0f
        );
        ghast.setCharging(false);
        session.clearGhastFireball();
    }

    private boolean shootDragonFireball(PossessionSession session, EnderDragon dragon) {
        if (!session.acquirePrimaryCooldown(dragonFireballCooldownTicks)) {
            return false;
        }
        Vector velocity = direction(session.view()).multiply(dragonFireballSpeed);
        dragon.launchProjectile(DragonFireball.class, velocity);
        return true;
    }

    private boolean shootShulkerBullet(PossessionSession session, Shulker shulker) {
        LivingEntity target = findLivingTarget(session, shulker, shulkerBulletRange, shulkerBulletRaySize);
        if (target == null || !session.acquirePrimaryCooldown(shulkerBulletCooldownTicks)) {
            return false;
        }
        shulker.getWorld().spawn(shulker.getEyeLocation(), ShulkerBullet.class, bullet -> {
            bullet.setShooter(shulker);
            bullet.setTarget(target);
        });
        return true;
    }

    private boolean toggleShulkerShell(PossessionSession session, Shulker shulker) {
        if (!session.acquireSecondaryCooldown(shulkerShellCooldownTicks)) {
            return false;
        }
        shulker.setPeek(shulker.getPeek() >= 0.5f ? 0.0f : 1.0f);
        return true;
    }

    private boolean startFrogTongue(PossessionSession session, Frog frog) {
        if (session.frogTongueTracked()) {
            return false;
        }
        LivingEntity target = findLivingTarget(session, frog, frogTongueRange, frogTongueRaySize);
        if (target == null || !session.acquirePrimaryCooldown(frogTongueCooldownTicks)) {
            return false;
        }
        frog.setTongueTarget(target);
        session.startFrogTongue(target.getUniqueId(), frogTongueDurationTicks);
        return true;
    }

    private void tickFrogTongue(PossessionSession session, Frog frog) {
        if (!session.frogTongueTracked()) {
            return;
        }
        Entity target = frog.getTongueTarget();
        if (session.frogTongueExpired()
            || target == null
            || !Bukkit.isOwnedByCurrentRegion(target)
            || session.frogTongueTargetId() == null
            || !target.getUniqueId().equals(session.frogTongueTargetId())
            || !target.isValid()
            || target.getWorld() != frog.getWorld()
            || frog.getLocation().distanceSquared(target.getLocation()) > frogTongueRange * frogTongueRange) {
            frog.setTongueTarget(null);
            session.clearFrogTongue();
        }
    }

    private boolean toggleAxolotlPlayDead(PossessionSession session, Axolotl axolotl) {
        if (!session.acquireSecondaryCooldown(axolotlPlayDeadCooldownTicks)) return false;
        boolean playingDead = !session.axolotlPlayingDeadControlled();
        axolotl.setPlayingDead(playingDead);
        session.axolotlPlayingDeadControlled(playingDead);
        if (playingDead) axolotl.setVelocity(new Vector());
        return true;
    }

    private static void maintainAxolotlPlayDead(PossessionSession session, Axolotl axolotl) {
        boolean expected = session.axolotlPlayingDeadControlled();
        if (axolotl.isPlayingDead() != expected) {
            axolotl.setPlayingDead(expected);
        }
    }

    private boolean toggleFoxSleep(PossessionSession session, Fox fox) {
        if (!session.acquireSecondaryCooldown(foxSleepCooldownTicks)) return false;
        if (session.foxPounceTracked()) { fox.setLeaping(false); session.clearFoxPounce(); }
        boolean sleeping = !session.foxSleepingControlled();
        fox.setCrouching(false);
        fox.setInterested(false);
        fox.setFaceplanted(false);
        fox.setSleeping(sleeping);
        session.foxSleepingControlled(sleeping);
        if (sleeping) fox.setVelocity(new Vector());
        return true;
    }

    private static void maintainFoxSleep(PossessionSession session, Fox fox) {
        boolean expected = session.foxSleepingControlled();
        if (fox.isSleeping() != expected) {
            fox.setSleeping(expected);
        }
    }

    private boolean pounceFox(PossessionSession session, Fox fox) {
        if (session.foxSleepingControlled() || fox.isSleeping() || !fox.isOnGround() || session.foxPounceTracked()) return false;
        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6 || !session.acquirePrimaryCooldown(foxPounceCooldownTicks)) return false;
        horizontal.normalize().multiply(foxPounceHorizontalSpeed);
        horizontal.setY(foxPounceVerticalVelocity);
        fox.setCrouching(false);
        fox.setInterested(false);
        fox.setFaceplanted(false);
        fox.setLeaping(true);
        session.startFoxPounce(foxPounceDurationTicks);
        session.lockMovementControl(foxPounceLockTicks);
        fox.setVelocity(horizontal);
        return true;
    }

    private void tickFoxPounce(PossessionSession session, Fox fox) {
        if (!session.foxPounceTracked()) return;
        if (session.foxPounceExpired()) {
            fox.setLeaping(false);
            session.clearFoxPounce();
            return;
        }
        if (!fox.isLeaping()) {
            fox.setLeaping(true);
        }
    }

    private boolean rollPanda(PossessionSession session, Panda panda) {
        if (!panda.isOnGround() || session.pandaRollTracked()) return false;
        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6 || !session.acquireSecondaryCooldown(pandaRollCooldownTicks)) return false;
        horizontal.normalize().multiply(pandaRollSpeed);
        horizontal.setY(Math.max(panda.getVelocity().getY(), pandaRollVerticalVelocity));
        panda.setSneezing(false);
        panda.setOnBack(false);
        panda.setRolling(true);
        session.startPandaRoll(pandaRollDurationTicks);
        session.lockMovementControl(pandaRollLockTicks);
        panda.setVelocity(horizontal);
        return true;
    }

    private void tickPandaRoll(PossessionSession session, Panda panda) {
        if (!session.pandaRollTracked()) return;
        if (session.pandaRollExpired()) {
            panda.setRolling(false);
            session.clearPandaRoll();
            return;
        }
        if (!panda.isRolling()) {
            panda.setRolling(true);
        }
    }

    private boolean startGuardianLaser(PossessionSession session, Guardian guardian) {
        if (session.guardianLaserActive()) {
            return false;
        }
        LivingEntity target = findLivingTarget(session, guardian, guardianLaserRange, guardianLaserRaySize);
        if (target == null || !guardian.hasLineOfSight(target)) {
            return false;
        }
        if (!guardian.getWorld().equals(target.getWorld())) {
            return false;
        }
        double distanceSquared = guardian.getLocation().distanceSquared(target.getLocation());
        if (distanceSquared <= 9.0) {
            return melee(session, guardian);
        }

        int timeoutTicks = Math.max(20, guardian.getLaserDuration() + 40);
        int cooldown = Math.max(guardianLaserCooldownTicks, timeoutTicks);
        if (!session.acquirePrimaryCooldown(cooldown)) {
            return false;
        }

        guardian.setLaser(false);
        guardian.setTarget(target);
        guardian.setAware(true);
        guardian.getPathfinder().stopPathfinding();
        guardian.setVelocity(new Vector());
        session.startGuardianLaser(target.getUniqueId(), timeoutTicks);
        session.lockMovementControl(timeoutTicks);
        return true;
    }

    private void tickGuardianLaser(PossessionSession session, Guardian guardian) {
        if (!session.guardianLaserActive()) {
            return;
        }

        LivingEntity target = guardian.getTarget();
        if (target == null) {
            finishGuardianLaser(session, guardian);
            return;
        }

        if (session.guardianLaserExpired()
            || !Bukkit.isOwnedByCurrentRegion(target)
            || session.guardianLaserTargetId() == null
            || !target.getUniqueId().equals(session.guardianLaserTargetId())
            || !target.isValid()
            || target.isDead()
            || !guardian.getWorld().equals(target.getWorld())
            || guardian.getLocation().distanceSquared(target.getLocation()) > guardianLaserRange * guardianLaserRange
            || !guardian.hasLineOfSight(target)) {
            finishGuardianLaser(session, guardian);
            return;
        }

        if (guardian.hasLaser()) {
            session.markGuardianLaserSeenActive();
        } else if (session.guardianLaserSeenActive()) {
            finishGuardianLaser(session, guardian);
            return;
        }

        guardian.setAware(true);
        guardian.getPathfinder().stopPathfinding();
    }

    private static void finishGuardianLaser(PossessionSession session, Guardian guardian) {
        guardian.setLaser(false);
        guardian.setTarget(null);
        guardian.setAware(false);
        session.clearGuardianLaser();
    }

    private boolean togglePufferFish(PossessionSession session, PufferFish pufferFish) {
        if (!session.acquireSecondaryCooldown(pufferFishPuffCooldownTicks)) {
            return false;
        }
        pufferFish.setPuffState(pufferFish.getPuffState() >= 2 ? 0 : 2);
        return true;
    }

    private boolean chargeVex(PossessionSession session, Vex vex) {
        Vector charge = direction(session.view());
        if (charge.lengthSquared() < 1.0E-6 || !session.acquireSecondaryCooldown(vexChargeCooldownTicks)) {
            return false;
        }
        vex.setCharging(true);
        session.startVexCharge(vexChargeDurationTicks);
        session.lockMovementControl(vexChargeLockTicks);
        vex.setVelocity(charge.normalize().multiply(vexChargeSpeed));
        return true;
    }

    private void tickVexCharge(PossessionSession session, Vex vex) {
        if (session.vexChargeActive()) {
            return;
        }
        if (vex.isCharging()) {
            vex.setCharging(false);
        }
        session.clearVexCharge();
    }

    private boolean castEvokerFangs(PossessionSession session, Evoker evoker) {
        if (session.evokerCastTracked()) {
            return false;
        }

        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return false;
        }
        horizontal.normalize();

        List<Location> spawnLocations = new ArrayList<>();
        Location origin = evoker.getLocation();
        for (int i = 1; i <= evokerFangsCount; i++) {
            Location sample = origin.clone().add(horizontal.clone().multiply(evokerFangsSpacing * i));
            if (!Bukkit.isOwnedByCurrentRegion(sample)) {
                break;
            }
            Location fangLocation = findFangSpawnLocation(sample);
            if (fangLocation != null) {
                spawnLocations.add(fangLocation);
            }
        }

        if (spawnLocations.isEmpty() || !session.acquireSecondaryCooldown(evokerFangsCooldownTicks)) {
            return false;
        }

        evoker.setSpell(Spellcaster.Spell.FANGS);
        evoker.getPathfinder().stopPathfinding();
        evoker.setVelocity(new Vector());
        session.startEvokerCast(evokerFangsCastTicks);
        session.lockMovementControl(evokerFangsCastTicks);

        int delay = 1;
        for (Location location : spawnLocations) {
            int attackDelay = delay;
            evoker.getWorld().spawn(location, EvokerFangs.class, fangs -> {
                fangs.setOwner(evoker);
                fangs.setAttackDelay(attackDelay);
            });
            delay += evokerFangsAttackDelayStep;
        }
        return true;
    }

    private void tickEvokerCast(PossessionSession session, Evoker evoker) {
        if (!session.evokerCastTracked()) {
            return;
        }
        if (session.evokerCastExpired()) {
            if (evoker.getSpell() == Spellcaster.Spell.FANGS) {
                evoker.setSpell(Spellcaster.Spell.NONE);
            }
            session.clearEvokerCast();
            return;
        }
        evoker.setSpell(Spellcaster.Spell.FANGS);
        evoker.getPathfinder().stopPathfinding();
        evoker.setVelocity(new Vector());
    }

    private Location findFangSpawnLocation(Location sample) {
        Location cursor = sample.clone();
        cursor.setY(sample.getBlockY() + 1.0);
        for (int step = 0; step <= evokerFangsGroundSearchBlocks; step++) {
            Location check = cursor.clone().subtract(0.0, step, 0.0);
            if (!Bukkit.isOwnedByCurrentRegion(check)) {
                return null;
            }
            Block feet = check.getBlock();
            Block below = feet.getRelative(BlockFace.DOWN);
            if (feet.isPassable() && below.getType().isSolid()) {
                return feet.getLocation().add(0.5, 0.05, 0.5);
            }
        }
        return null;
    }

    private boolean toggleCreeper(PossessionSession session, Creeper creeper) {
        if (!session.acquirePrimaryCooldown(creeperCooldownTicks)) {
            return false;
        }
        creeper.setIgnited(!creeper.isIgnited());
        return true;
    }

    private boolean shootSkeleton(PossessionSession session, AbstractSkeleton skeleton) {
        if (!session.acquirePrimaryCooldown(skeletonCooldownTicks)) {
            return false;
        }

        Vector velocity = direction(session.view()).multiply(skeletonArrowSpeed);
        skeleton.swingMainHand();
        skeleton.launchProjectile(Arrow.class, velocity);
        return true;
    }

    private boolean shootNativeProjectile(PossessionSession session, Mob vessel) {
        Class<? extends Projectile> projectileClass = projectileFor(vessel);
        if (projectileClass == null || !session.acquirePrimaryCooldown(nativeProjectileCooldownTicks)) {
            return false;
        }

        Vector velocity = direction(session.view()).multiply(nativeProjectileSpeed);
        vessel.swingMainHand();
        vessel.launchProjectile(projectileClass, velocity);
        return true;
    }

    private boolean nativeRanged(PossessionSession session, Mob vessel, RangedEntity ranged) {
        if (!hasRequiredRangedWeapon(vessel)) {
            return false;
        }

        LivingEntity target = findLivingTarget(session, vessel, nativeRangedRange, nativeRangedRaySize);
        if (target == null || !session.acquirePrimaryCooldown(nativeRangedCooldownTicks)) {
            return false;
        }

        ranged.rangedAttack(target, nativeRangedCharge);
        return true;
    }

    private boolean melee(PossessionSession session, Mob vessel) {
        if (!meleeEnabled) {
            return false;
        }

        LivingEntity target = findLivingTarget(session, vessel, meleeRange, meleeRaySize);
        if (target == null || !session.acquirePrimaryCooldown(meleeCooldownTicks)) {
            return false;
        }

        vessel.swingMainHand();
        vessel.attack(target);
        return true;
    }

    private LivingEntity findLivingTarget(PossessionSession session, Mob vessel, double range, double raySize) {
        RayTraceResult hit = vessel.getWorld().rayTrace(
            vessel.getEyeLocation(),
            direction(session.view()),
            range,
            FluidCollisionMode.NEVER,
            true,
            raySize,
            entity -> Bukkit.isOwnedByCurrentRegion(entity)
                && entity instanceof LivingEntity
                && !entity.getUniqueId().equals(vessel.getUniqueId())
                && (!(entity instanceof Player player) || !player.getUniqueId().equals(session.playerId()))
        );
        return hit != null && hit.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    private boolean teleportEnderman(PossessionSession session, Enderman enderman) {
        Vector direction = direction(session.view());
        Location eye = enderman.getEyeLocation();
        RayTraceResult blockHit = enderman.getWorld().rayTraceBlocks(
            eye,
            direction,
            endermanTeleportDistance,
            FluidCollisionMode.NEVER,
            true
        );

        double travel = endermanTeleportDistance;
        if (blockHit != null) {
            travel = Math.max(1.0, Math.min(
                endermanTeleportDistance,
                blockHit.getHitPosition().distance(eye.toVector()) - 1.0
            ));
        }

        Location target = enderman.getLocation().clone().add(direction.clone().multiply(travel));
        Location safe = findEndermanLanding(target);
        if (safe == null || !session.acquireSecondaryCooldown(endermanTeleportCooldownTicks)) {
            return false;
        }

        enderman.setVelocity(new Vector());
        session.lockMovementControl(3);
        enderman.teleportAsync(safe);
        return true;
    }

    private boolean pounceSpider(PossessionSession session, Spider spider) {
        if (!spider.isOnGround()) {
            return false;
        }

        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return false;
        }
        if (!session.acquireSecondaryCooldown(spiderPounceCooldownTicks)) {
            return false;
        }

        horizontal.normalize().multiply(spiderPounceHorizontalSpeed);
        horizontal.setY(spiderPounceVerticalVelocity);
        session.lockMovementControl(spiderPounceLockTicks);
        spider.setVelocity(horizontal);
        return true;
    }

    private boolean dashCamel(PossessionSession session, Camel camel) {
        if (!camel.isOnGround()) {
            return false;
        }

        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return false;
        }
        if (!session.acquireSecondaryCooldown(camelDashCooldownTicks)) {
            return false;
        }

        horizontal.normalize().multiply(camelDashHorizontalSpeed);
        horizontal.setY(Math.max(camel.getVelocity().getY(), camelDashVerticalVelocity));
        camel.setDashing(true);
        session.lockMovementControl(camelDashLockTicks);
        camel.setVelocity(horizontal);
        return true;
    }

    private boolean roarRavager(PossessionSession session, Ravager ravager) {
        if (!session.acquireSecondaryCooldown(ravagerRoarCooldownTicks)) {
            return false;
        }
        ravager.setStunnedTicks(-1);
        ravager.setAttackTicks(-1);
        ravager.setRoarTicks(11);
        session.lockMovementControl(ravagerRoarLockTicks);
        return true;
    }

    private static boolean supportsNativeRanged(Mob vessel) {
        return vessel instanceof Pillager
            || vessel instanceof Piglin
            || vessel instanceof Drowned
            || vessel instanceof Witch
            || vessel instanceof Illusioner;
    }

    private static boolean hasRequiredRangedWeapon(Mob vessel) {
        if (vessel instanceof Witch) {
            return true;
        }
        if (vessel instanceof Pillager || vessel instanceof Piglin) {
            return holds(vessel, Material.CROSSBOW);
        }
        if (vessel instanceof Drowned) {
            return holds(vessel, Material.TRIDENT);
        }
        if (vessel instanceof Illusioner) {
            return holds(vessel, Material.BOW);
        }
        return false;
    }

    private static boolean holds(Mob vessel, Material material) {
        EntityEquipment equipment = vessel.getEquipment();
        if (equipment == null) {
            return false;
        }
        ItemStack main = equipment.getItemInMainHand();
        ItemStack off = equipment.getItemInOffHand();
        return main.getType() == material || off.getType() == material;
    }

    private static Location findEndermanLanding(Location target) {
        for (int delta = 0; delta <= 4; delta++) {
            Location down = target.clone().add(0.0, -delta, 0.0);
            if (isEndermanSafe(down)) {
                return centered(down);
            }
            if (delta != 0) {
                Location up = target.clone().add(0.0, delta, 0.0);
                if (isEndermanSafe(up)) {
                    return centered(up);
                }
            }
        }
        return null;
    }

    private static boolean isEndermanSafe(Location location) {
        return location.getBlock().isPassable()
            && location.clone().add(0.0, 1.0, 0.0).getBlock().isPassable()
            && location.clone().add(0.0, 2.0, 0.0).getBlock().isPassable()
            && !location.clone().add(0.0, -1.0, 0.0).getBlock().isPassable();
    }

    private static Location centered(Location location) {
        Location centered = location.clone();
        centered.setX(Math.floor(centered.getX()) + 0.5);
        centered.setZ(Math.floor(centered.getZ()) + 0.5);
        return centered;
    }

    private static Class<? extends Projectile> projectileFor(Mob vessel) {
        if (vessel instanceof Blaze) {
            return SmallFireball.class;
        }
        if (vessel instanceof Wither) {
            return WitherSkull.class;
        }
        if (vessel instanceof Snowman) {
            return Snowball.class;
        }
        if (vessel instanceof Llama || vessel instanceof TraderLlama) {
            return LlamaSpit.class;
        }
        if (vessel instanceof Breeze) {
            return BreezeWindCharge.class;
        }
        return null;
    }

    private static boolean hasBow(AbstractSkeleton skeleton) {
        EntityEquipment equipment = skeleton.getEquipment();
        return equipment != null && equipment.getItemInMainHand().getType() == Material.BOW;
    }

    private static Vector direction(ViewSnapshot view) {
        double yaw = Math.toRadians(view.yaw());
        double pitch = Math.toRadians(Math.max(-90.0f, Math.min(90.0f, view.pitch())));
        double cosPitch = Math.cos(pitch);
        return new Vector(
            -Math.sin(yaw) * cosPitch,
            -Math.sin(pitch),
            Math.cos(yaw) * cosPitch
        ).normalize();
    }
}
