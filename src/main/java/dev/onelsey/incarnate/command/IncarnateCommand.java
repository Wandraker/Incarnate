package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionOrigin;
import dev.onelsey.incarnate.possession.PossessionSession;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
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
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class IncarnateCommand implements CommandExecutor, TabCompleter {
    private static final String MORPH_PERMISSION = "incarnate.use.incarnate";
    private static final String INSPECT_PERMISSION = "incarnate.admin.inspect";

    private final PossessionManager possessions;
    private final double inspectDistance;

    public IncarnateCommand(PossessionManager possessions, double inspectDistance) {
        this.possessions = possessions;
        this.inspectDistance = Math.max(1.0, inspectDistance);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is player-only.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("[Incarnate] Usage: /incarnate <mob|primary|secondary|inspect>"));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("primary")) {
            possessions.triggerPrimary(player);
            return true;
        }
        if (action.equals("secondary")) {
            possessions.triggerSecondary(player);
            return true;
        }
        if (action.equals("inspect")) {
            inspect(player);
            return true;
        }

        if (!player.hasPermission(MORPH_PERMISSION)) {
            player.sendMessage(Component.text("[Incarnate] You do not have permission to create a vessel."));
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

    private void inspect(Player player) {
        if (!player.hasPermission(INSPECT_PERMISSION)) {
            player.sendMessage(Component.text("[Incarnate] You do not have permission to inspect vessels."));
            return;
        }

        RayTraceResult hit = player.getWorld().rayTrace(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            inspectDistance,
            FluidCollisionMode.NEVER,
            true,
            0.35,
            entity -> entity instanceof Mob
        );
        Entity entity = hit == null ? null : hit.getHitEntity();
        if (!(entity instanceof Mob mob)) {
            player.sendMessage(Component.text("[Incarnate] Look at a mob within " + inspectDistance + " blocks."));
            return;
        }

        PossessionSession session = possessions.session(mob);
        if (session == null || !session.isActive()) {
            player.sendMessage(Component.text("[Incarnate] That mob is not currently controlled by Incarnate."));
            return;
        }

        player.sendMessage(Component.text(
            "[Incarnate] " + mob.getType()
                + " vessel=" + session.vesselId()
                + " controller=" + session.playerName()
                + " (" + session.playerId() + ")"
                + " origin=" + session.origin().name().toLowerCase(Locale.ROOT)
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toUpperCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        if (sender instanceof Player player && possessions.isPossessing(player)) {
            if ("PRIMARY".startsWith(prefix)) {
                out.add("primary");
            }
            if ("SECONDARY".startsWith(prefix)) {
                out.add("secondary");
            }
        }
        if (sender.hasPermission(INSPECT_PERMISSION) && "INSPECT".startsWith(prefix)) {
            out.add("inspect");
        }

        if (sender.hasPermission(MORPH_PERMISSION)) {
            for (EntityType type : Registry.ENTITY_TYPE) {
                if (!possessions.canCreate(type) || !type.name().startsWith(prefix)) {
                    continue;
                }
                out.add(type.name().toLowerCase(Locale.ROOT));
            }
        }

        out.sort(Comparator.naturalOrder());
        return out;
    }
}
