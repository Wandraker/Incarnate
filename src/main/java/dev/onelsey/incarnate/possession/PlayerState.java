package dev.onelsey.incarnate.possession;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public record PlayerState(
    GameMode gameMode,
    Location location,
    float flySpeed,
    boolean allowFlight,
    boolean flying
) {
    public static PlayerState capture(Player player) {
        return new PlayerState(
            player.getGameMode(),
            player.getLocation().clone(),
            player.getFlySpeed(),
            player.getAllowFlight(),
            player.isFlying()
        );
    }
}
