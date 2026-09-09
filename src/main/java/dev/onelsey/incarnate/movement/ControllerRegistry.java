package dev.onelsey.incarnate.movement;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;

import java.util.EnumSet;
import java.util.Set;

public final class ControllerRegistry {
    private static final Set<EntityType> FLIGHT = EnumSet.of(
        EntityType.ALLAY,
        EntityType.BAT,
        EntityType.BEE,
        EntityType.BLAZE,
        EntityType.GHAST,
        EntityType.HAPPY_GHAST,
        EntityType.PARROT,
        EntityType.PHANTOM,
        EntityType.VEX,
        EntityType.WITHER
    );

    private static final Set<EntityType> AQUATIC = EnumSet.of(
        EntityType.COD,
        EntityType.DOLPHIN,
        EntityType.ELDER_GUARDIAN,
        EntityType.GLOW_SQUID,
        EntityType.GUARDIAN,
        EntityType.NAUTILUS,
        EntityType.PUFFERFISH,
        EntityType.SALMON,
        EntityType.SQUID,
        EntityType.TADPOLE,
        EntityType.TROPICAL_FISH,
        EntityType.ZOMBIE_NAUTILUS
    );

    private static final Set<EntityType> HYBRID_AQUATIC = EnumSet.of(
        EntityType.AXOLOTL,
        EntityType.DROWNED,
        EntityType.TURTLE
    );

    private static final Set<EntityType> HOPPING = EnumSet.of(
        EntityType.RABBIT
    );

    private static final Set<EntityType> CLIMBING = EnumSet.of(
        EntityType.SPIDER,
        EntityType.CAVE_SPIDER
    );

    private static final Set<EntityType> CUBES = EnumSet.of(
        EntityType.SLIME,
        EntityType.MAGMA_CUBE,
        EntityType.SULFUR_CUBE
    );

    private final VesselController ground;
    private final VesselController flight;
    private final VesselController aquatic;
    private final VesselController hybridAquatic;
    private final VesselController hopping;
    private final VesselController frog;
    private final VesselController climbing;
    private final VesselController cube;

    public ControllerRegistry(FileConfiguration config) {
        this.ground = new GroundVesselController(config);
        this.flight = new FlightVesselController(config);
        this.aquatic = new AquaticVesselController(config);
        this.hybridAquatic = new HybridAquaticVesselController(config);
        this.hopping = new HoppingVesselController(config);
        this.frog = new HoppingAquaticVesselController(config);
        this.climbing = new ClimbingVesselController(config);
        this.cube = new CubeVesselController(config);
    }

    public VesselController controllerFor(Mob mob) {
        EntityType type = mob.getType();
        if (FLIGHT.contains(type)) {
            return flight;
        }
        if (AQUATIC.contains(type)) {
            return aquatic;
        }
        if (HYBRID_AQUATIC.contains(type)) {
            return hybridAquatic;
        }
        if (type == EntityType.FROG) {
            return frog;
        }
        if (HOPPING.contains(type)) {
            return hopping;
        }
        if (CLIMBING.contains(type)) {
            return climbing;
        }
        if (CUBES.contains(type)) {
            return cube;
        }
        return ground;
    }
}
