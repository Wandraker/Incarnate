package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Mob;

public final class HoppingAquaticVesselController implements VesselController {
    private final HoppingVesselController hopping;
    private final AquaticVesselController aquatic;

    public HoppingAquaticVesselController(FileConfiguration config) {
        this.hopping = new HoppingVesselController(config);
        this.aquatic = new AquaticVesselController(config);
    }

    @Override
    public void tick(PossessionSession session, Mob vessel) {
        if (vessel.isInWater()) {
            aquatic.tick(session, vessel);
        } else {
            hopping.tick(session, vessel);
        }
    }
}
