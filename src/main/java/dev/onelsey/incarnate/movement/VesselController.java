package dev.onelsey.incarnate.movement;

import dev.onelsey.incarnate.possession.PossessionSession;
import org.bukkit.entity.Mob;

public interface VesselController {
    void tick(PossessionSession session, Mob vessel);
}
