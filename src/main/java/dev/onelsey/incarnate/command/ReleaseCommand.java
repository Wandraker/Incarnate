package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.message.MessageService;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.ReleaseReason;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReleaseCommand implements CommandExecutor {
    private final PossessionManager possessions;
    private final MessageService messages;

    public ReleaseCommand(PossessionManager possessions, MessageService messages) {
        this.possessions = possessions;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (possessions.isPossessing(player)) {
            possessions.requestRelease(player, ReleaseReason.COMMAND);
            return true;
        }

        if (possessions.isRecoveryPending(player)) {
            possessions.recoverPending(player);
            messages.send(player, "recovery-retry-started");
            return true;
        }

        messages.send(player, "not-possessing");
        return true;
    }
}
