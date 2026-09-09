package dev.onelsey.incarnate.command;

import dev.onelsey.incarnate.message.MessageService;
import dev.onelsey.incarnate.permission.IncarnatePermissions;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionOrigin;
import dev.onelsey.incarnate.possession.PossessionSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import java.util.Map;

public final class IncarnateCommand implements CommandExecutor, TabCompleter {
    private final PossessionManager possessions;
    private final MessageService messages;
    private final double inspectDistance;

    public IncarnateCommand(PossessionManager possessions, MessageService messages, double inspectDistance) {
        this.possessions = possessions;
        this.messages = messages;
        this.inspectDistance = Math.max(1.0, inspectDistance);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (args.length != 1) {
            messages.send(player, "usage-incarnate");
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

        if (!player.hasPermission(IncarnatePermissions.CREATE)) {
            messages.send(player, "permission-create");
            return true;
        }
        if (possessions.isPossessing(player)) {
            messages.send(player, "release-current-first");
            return true;
        }

        EntityType type;
        try {
            type = EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            messages.send(player, "entity-unknown", Map.of("entity", Component.text(args[0])));
            return true;
        }

        Component entityName = Component.text(type.name().toLowerCase(Locale.ROOT));
        if (!possessions.canCreate(type)) {
            messages.send(player, "entity-created-unsupported", Map.of("entity", entityName));
            return true;
        }
        if (!IncarnatePermissions.canUseMob(player, type)) {
            messages.send(player, "mob-access-denied", Map.of("entity", entityName));
            return true;
        }

        Location spawn = player.getLocation().clone();
        Entity entity;
        try {
            entity = player.getWorld().spawnEntity(spawn, type);
        } catch (RuntimeException ex) {
            messages.send(player, "create-failed");
            return true;
        }

        if (!(entity instanceof Mob mob)) {
            entity.remove();
            messages.send(player, "internal-type-rejected");
            return true;
        }

        possessions.begin(player, mob, PossessionOrigin.CREATED);
        return true;
    }

    private void inspect(Player player) {
        if (!player.hasPermission(IncarnatePermissions.INSPECT)) {
            messages.send(player, "permission-inspect");
            return;
        }

        RayTraceResult hit = player.getWorld().rayTrace(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            inspectDistance,
            FluidCollisionMode.NEVER,
            true,
            0.35,
            entity -> Bukkit.isOwnedByCurrentRegion(entity) && entity instanceof Mob
        );
        Entity entity = hit == null ? null : hit.getHitEntity();
        if (!(entity instanceof Mob mob)) {
            messages.send(player, "inspect-look", Map.of(
                "distance", Component.text(String.format(Locale.ROOT, "%.1f", inspectDistance))
            ));
            return;
        }

        PossessionSession session = possessions.session(mob);
        if (session == null || !session.isActive()) {
            messages.send(player, "inspect-not-controlled");
            return;
        }

        String originKey = "origin." + session.origin().name().toLowerCase(Locale.ROOT);
        messages.send(player, "inspect-result", Map.of(
            "mob", Component.text(mob.getType().name().toLowerCase(Locale.ROOT)),
            "vessel", Component.text(session.vesselId().toString()),
            "controller", Component.text(session.playerName()),
            "controller_uuid", Component.text(session.playerId().toString()),
            "origin", messages.render(player, originKey)
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
        if (sender.hasPermission(IncarnatePermissions.INSPECT) && "INSPECT".startsWith(prefix)) {
            out.add("inspect");
        }

        if (sender instanceof Player player && player.hasPermission(IncarnatePermissions.CREATE)) {
            for (EntityType type : Registry.ENTITY_TYPE) {
                if (!possessions.canCreate(type)
                    || !IncarnatePermissions.canUseMob(player, type)
                    || !type.name().startsWith(prefix)) {
                    continue;
                }
                out.add(type.name().toLowerCase(Locale.ROOT));
            }
        }

        out.sort(Comparator.naturalOrder());
        return out;
    }
}
