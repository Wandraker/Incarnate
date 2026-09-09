package dev.onelsey.incarnate.integration;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class ElysiumPrivateTagIntegration {
    private final IncarnatePlugin plugin;
    private final boolean enabled;
    private final String pluginName;
    private final String source;
    private final AtomicBoolean incompatibleWarned = new AtomicBoolean();
    private final AtomicBoolean connectedLogged = new AtomicBoolean();
    private volatile Binding binding;

    public ElysiumPrivateTagIntegration(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("integrations.elysium-private-tag.enabled", true);
        this.pluginName = plugin.getConfig().getString("integrations.elysium-private-tag.plugin-name", "Elysium_Private_Tag");
        this.source = plugin.getConfig().getString("integrations.elysium-private-tag.source", "Incarnate");
    }

    public void suppress(Player player) {
        invoke(player, true);
    }

    public void restore(Player player) {
        invoke(player, false);
    }

    public void recoverStaleOnlineSuppressions() {
        if (!enabled || resolve() == null) {
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(plugin, task -> restore(player), null);
            }
        });
    }

    private void invoke(Player player, boolean hide) {
        Binding current = resolve();
        if (current == null) {
            return;
        }

        try {
            Method method = hide ? current.hideMethod() : current.showMethod();
            method.invoke(current.plugin(), player, source);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            Throwable cause = ex instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : ex;
            plugin.getLogger().log(Level.WARNING,
                "Elysium Private Tag integration failed while " + (hide ? "hiding" : "restoring") + " a tag for " + player.getUniqueId(),
                cause);
            binding = null;
        }
    }

    private Binding resolve() {
        if (!enabled) {
            return null;
        }

        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        if (target == null || !target.isEnabled()) {
            binding = null;
            return null;
        }

        Binding current = binding;
        if (current != null && current.plugin() == target) {
            return current;
        }

        try {
            Method hideMethod = target.getClass().getMethod("hideTag", Player.class, String.class);
            Method showMethod = target.getClass().getMethod("showTag", Player.class, String.class);
            Binding resolved = new Binding(target, hideMethod, showMethod);
            binding = resolved;
            incompatibleWarned.set(false);
            if (connectedLogged.compareAndSet(false, true)) {
                plugin.getLogger().info("Elysium Private Tag integration enabled through source-aware tag suppression.");
            }
            return resolved;
        } catch (NoSuchMethodException ex) {
            binding = null;
            if (incompatibleWarned.compareAndSet(false, true)) {
                plugin.getLogger().warning(
                    "Plugin '" + pluginName + "' is present but does not expose hideTag/showTag(Player, String); tag integration was disabled."
                );
            }
            return null;
        }
    }

    private record Binding(Plugin plugin, Method hideMethod, Method showMethod) {
    }
}
