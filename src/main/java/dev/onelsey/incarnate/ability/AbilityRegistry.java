package dev.onelsey.incarnate.ability;

import com.destroystokyo.paper.entity.RangedEntity;
import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.BreezeWindCharge;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
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
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Spellcaster;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

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
    }

    public boolean triggerPrimary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }

        if (vessel instanceof Guardian guardian && guardianLaserEnabled) {
            return startGuardianLaser(session, guardian);
        }
        if (vessel instanceof Creeper creeper && creeperEnabled) {
            return toggleCreeper(session, creeper);
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

    public boolean triggerSecondary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
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

    public void tick(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return;
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
    }

    public String primaryLabel(Mob vessel) {
        if (vessel instanceof Guardian && guardianLaserEnabled) return "guardian-laser";
        if (vessel instanceof Creeper && creeperEnabled) return "fuse";
        if (vessel instanceof AbstractSkeleton && skeletonEnabled) return "arrow-melee";
        if (nativeProjectilesEnabled && projectileFor(vessel) != null) return "projectile";
        if (nativeRangedEnabled && vessel instanceof RangedEntity && supportsNativeRanged(vessel)) return "ranged-melee";
        return meleeEnabled ? "melee" : "none";
    }

    public String secondaryLabel(Mob vessel) {
        if (vessel instanceof PufferFish && pufferFishPuffEnabled) return "puff";
        if (vessel instanceof Vex && vexChargeEnabled) return "charge";
        if (vessel instanceof Evoker && evokerFangsEnabled) return "fangs";
        if (vessel instanceof Enderman && endermanTeleportEnabled) return "teleport";
        if (vessel instanceof Spider && spiderPounceEnabled) return "pounce";
        if (vessel instanceof Camel && camelDashEnabled) return "dash";
        if (vessel instanceof Ravager && ravagerRoarEnabled) return "roar";
        return "none";
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
        if (vessel instanceof Ghast) {
            return LargeFireball.class;
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
