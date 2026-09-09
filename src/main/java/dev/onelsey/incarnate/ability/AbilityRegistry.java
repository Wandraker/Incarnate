package dev.onelsey.incarnate.ability;

import dev.onelsey.incarnate.input.ViewSnapshot;
import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.BreezeWindCharge;
import org.bukkit.entity.Creeper;
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
