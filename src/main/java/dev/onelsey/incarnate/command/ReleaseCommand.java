package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.ReleaseReason;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReleaseCommand implements CommandExecutor {
    private final PossessionManager possessions;

    public ReleaseCommand(PossessionManager possessions) {
        this.possessions = possessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (possessions.isPossessing(player)) {
            possessions.requestRelease(player, ReleaseReason.COMMAND);
            return true;
        }

        if (possessions.isRecoveryPending(player)) {
            possessions.recoverPending(player);
            player.sendMessage(Component.text("[Incarnate] Recovery retry started."));
            return true;
        }

        player.sendMessage(Component.text("[Incarnate] You are not possessing a vessel."));
        return true;
    }
}
