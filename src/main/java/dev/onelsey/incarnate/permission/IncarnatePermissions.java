package dev.onelsey.incarnate.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import java.util.Locale;

public final class IncarnatePermissions {
    public static final String CREATE = "incarnate.use.incarnate";
    public static final String POSSESS = "incarnate.use.possess";
    public static final String ALL_MOBS = "incarnate.mob.*";
    public static final String INSPECT = "incarnate.admin.inspect";

    private static final String MOB_PREFIX = "incarnate.mob.";

    private IncarnatePermissions() {
    }

    public static String mobNode(EntityType type) {
        return MOB_PREFIX + type.name().toLowerCase(Locale.ROOT);
    }

    public static boolean canUseMob(CommandSender sender, EntityType type) {
        return sender.hasPermission(ALL_MOBS) || sender.hasPermission(mobNode(type));
    }
}
