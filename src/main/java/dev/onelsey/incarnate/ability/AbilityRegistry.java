package dev.onelsey.incarnate.ability;

import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.BreezeWindCharge;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class AbilityRegistry {
    private final boolean skeletonEnabled;
    private final boolean skeletonRequireBow;
    private final double skeletonArrowSpeed;
    private final int skeletonCooldownTicks;
    private final boolean nativeProjectilesEnabled;
    private final double nativeProjectileSpeed;
    private final int nativeProjectileCooldownTicks;
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

    public AbilityRegistry(FileConfiguration config) {
        this.skeletonEnabled = config.getBoolean("abilities.skeleton.enabled", true);
        this.skeletonRequireBow = config.getBoolean("abilities.skeleton.require-bow", true);
        this.skeletonArrowSpeed = config.getDouble("abilities.skeleton.arrow-speed", 3.0);
        this.skeletonCooldownTicks = Math.max(1, config.getInt("abilities.skeleton.cooldown-ticks", 12));
        this.nativeProjectilesEnabled = config.getBoolean("abilities.native-projectiles.enabled", true);
        this.nativeProjectileSpeed = config.getDouble("abilities.native-projectiles.speed", 1.5);
        this.nativeProjectileCooldownTicks = Math.max(1, config.getInt("abilities.native-projectiles.cooldown-ticks", 14));
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
    }

    public boolean triggerPrimary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
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
        return melee(session, vessel);
    }

    public boolean triggerSecondary(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return false;
        }

        if (vessel instanceof Enderman enderman && endermanTeleportEnabled) {
            return teleportEnderman(session, enderman);
        }
        if (vessel instanceof Spider spider && spiderPounceEnabled) {
            return pounceSpider(session, spider);
        }
        return false;
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

    private boolean melee(PossessionSession session, Mob vessel) {
        if (!meleeEnabled) {
            return false;
        }

        Vector direction = direction(session.view());
        RayTraceResult hit = vessel.getWorld().rayTrace(
            vessel.getEyeLocation(),
            direction,
            meleeRange,
            FluidCollisionMode.NEVER,
            true,
            meleeRaySize,
            entity -> entity instanceof LivingEntity
                && !entity.getUniqueId().equals(vessel.getUniqueId())
                && (!(entity instanceof Player player) || !player.getUniqueId().equals(session.playerId()))
        );
        if (hit == null || hit.getHitEntity() == null) {
            return false;
        }
        if (!session.acquirePrimaryCooldown(meleeCooldownTicks)) {
            return false;
        }

        vessel.swingMainHand();
        vessel.attack(hit.getHitEntity());
        return true;
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
        enderman.teleportAsync(safe);
        return true;
    }

    private boolean pounceSpider(PossessionSession session, Spider spider) {
        if (!spider.isOnGround() || !session.acquireSecondaryCooldown(spiderPounceCooldownTicks)) {
            return false;
        }

        Vector horizontal = direction(session.view());
        horizontal.setY(0.0);
        if (horizontal.lengthSquared() < 1.0E-6) {
            return false;
        }
        horizontal.normalize().multiply(spiderPounceHorizontalSpeed);
        horizontal.setY(spiderPounceVerticalVelocity);
        spider.setVelocity(horizontal);
        return true;
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
