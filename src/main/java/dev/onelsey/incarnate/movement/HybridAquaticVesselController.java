package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;

public final class HybridAquaticVesselController implements VesselController {
    private final GroundVesselController ground;
    private final AquaticVesselController aquatic;

    public HybridAquaticVesselController(FileConfiguration config) {
        this.ground = new GroundVesselController(config);
        this.aquatic = new AquaticVesselController(config);
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        if (vessel.isInWater()) {
            aquatic.tick(session, vessel);
        } else {
            ground.tick(session, vessel);
        }
    }
}
