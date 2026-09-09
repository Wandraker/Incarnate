package dev.onelsey.incarnate.command;

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

public final class PossessCommand implements CommandExecutor {
    private final PossessionManager possessions;
    private final double distance;

    public PossessCommand(PossessionManager possessions, double distance) {
        this.possessions = possessions;
        this.distance = Math.max(0.5, distance);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (!player.hasPermission(IncarnatePermissions.POSSESS)) {
            player.sendMessage(Component.text("[Incarnate] You do not have permission to possess existing mobs."));
            return true;
        }
        if (possessions.isPossessing(player)) {
            player.sendMessage(Component.text("[Incarnate] You are already possessing a vessel."));
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
            player.sendMessage(Component.text("[Incarnate] Look at a mob within " + distance + " blocks."));
            return true;
        }
        if (!IncarnatePermissions.canUseMob(player, mob.getType())) {
            player.sendMessage(Component.text("[Incarnate] You do not have access to the " + mob.getType() + " vessel."));
            return true;
        }

        possessions.begin(player, mob, PossessionOrigin.EXISTING);
        return true;
    }
}
