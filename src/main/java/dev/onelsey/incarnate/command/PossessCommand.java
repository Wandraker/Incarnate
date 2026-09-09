package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.message.MessageService;
import dev.onelsey.incarnate.permission.IncarnatePermissions;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionOrigin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Locale;
import java.util.Map;

public final class PossessCommand implements CommandExecutor {
    private final PossessionManager possessions;
    private final MessageService messages;
    private final double distance;

    public PossessCommand(PossessionManager possessions, MessageService messages, double distance) {
        this.possessions = possessions;
        this.messages = messages;
        this.distance = Math.max(0.5, distance);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission(IncarnatePermissions.POSSESS)) {
            messages.send(player, "permission-possess");
            return true;
        }
        if (possessions.isPossessing(player)) {
            messages.send(player, "already-possessing");
            return true;
        }

        RayTraceResult hit = player.getWorld().rayTrace(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            distance,
            FluidCollisionMode.NEVER,
            true,
            0.35,
            entity -> Bukkit.isOwnedByCurrentRegion(entity)
                && entity instanceof Mob
                && entity != player
        );

        Entity entity = hit == null ? null : hit.getHitEntity();
        if (!(entity instanceof Mob mob)) {
            messages.send(player, "possess-look", Map.of(
                "distance", Component.text(String.format(Locale.ROOT, "%.1f", distance))
            ));
            return true;
        }
        if (!IncarnatePermissions.canUseMob(player, mob.getType())) {
            messages.send(player, "mob-access-denied", Map.of(
                "entity", Component.text(mob.getType().name().toLowerCase(Locale.ROOT))
            ));
            return true;
        }

        possessions.begin(player, mob, PossessionOrigin.EXISTING);
        return true;
    }
}
