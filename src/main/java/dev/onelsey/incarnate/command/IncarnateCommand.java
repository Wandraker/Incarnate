package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionOrigin;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class IncarnateCommand implements CommandExecutor, TabCompleter {
    private final PossessionManager possessions;

    public IncarnateCommand(PossessionManager possessions) {
        this.possessions = possessions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("[Incarnate] Usage: /incarnate <mob|primary>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("primary")) {
            possessions.triggerPrimary(player);
            return true;
        }

        if (possessions.isPossessing(player)) {
            player.sendMessage(Component.text("[Incarnate] Release your current vessel first."));
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text("[Incarnate] Unknown entity type: " + args[0]));
            return true;
        }

        if (!possessions.canCreate(type)) {
            player.sendMessage(Component.text("[Incarnate] " + type + " cannot be used as a created vessel in this build."));
            return true;
        }

        Location spawn = player.getLocation().clone();
        Entity entity;
        try {
            entity = player.getWorld().spawnEntity(spawn, type);
        } catch (RuntimeException ex) {
            player.sendMessage(Component.text("[Incarnate] Failed to create that vessel type."));
            return true;
        }

        if (!(entity instanceof Mob mob)) {
            entity.remove();
            player.sendMessage(Component.text("[Incarnate] Internal type check rejected that vessel."));
            return true;
        }

        possessions.begin(player, mob, PossessionOrigin.CREATED);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toUpperCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if ("PRIMARY".startsWith(prefix)) {
            out.add("primary");
        }

        for (EntityType type : Registry.ENTITY_TYPE) {
            if (!possessions.canCreate(type) || !type.name().startsWith(prefix)) {
                continue;
            }
            out.add(type.name().toLowerCase(Locale.ROOT));
        }

        out.sort(Comparator.naturalOrder());
        return out;
    }
}
