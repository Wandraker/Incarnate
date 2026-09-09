package dev.onelsey.incarnate.visibility;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PossessionVisibilityManager {
    private final IncarnatePlugin plugin;
    private final boolean hideFromTab;
    private final Set<UUID> concealedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> tabUnlistedByIncarnate = new ConcurrentHashMap<>();
    private final TabVisibilityIndex tabIndex;

    public PossessionVisibilityManager(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.hideFromTab = plugin.getConfig().getBoolean("visibility.hide-controller-from-tab", true);
        this.tabIndex = new TabVisibilityIndex(plugin);
        for (TabVisibilityIndex.Entry entry : tabIndex.snapshot()) {
            tabUnlistedByIncarnate.computeIfAbsent(entry.controllerId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(entry.viewerId());
        }
    }

    public boolean isConcealed(UUID playerId) {
        return concealedPlayers.contains(playerId);
    }

    public void conceal(UUID controllerId, Player controller) {
        concealedPlayers.add(controllerId);
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(controllerId)) {
                    hideFrom(viewer, controllerId);
                }
            }
        });
    }

    public void reveal(UUID controllerId, Player controller) {
        concealedPlayers.remove(controllerId);
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.getUniqueId().equals(controllerId)) {
                    showTo(viewer, controllerId);
                }
            }
        });
    }

    public void recoverStaleTabEntries() {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            for (Map.Entry<UUID, Set<UUID>> ownership : Map.copyOf(tabUnlistedByIncarnate).entrySet()) {
                UUID controllerId = ownership.getKey();
                Player controller = Bukkit.getPlayer(controllerId);
                if (controller == null) {
                    forgetTabController(controllerId);
                    continue;
                }

                for (UUID viewerId : Set.copyOf(ownership.getValue())) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer == null) {
                        forgetTabPair(controllerId, viewerId);
                        continue;
                    }
                    showTo(viewer, controllerId);
                }
            }
        });
    }

    public void forget(UUID controllerId) {
        concealedPlayers.remove(controllerId);
        forgetTabController(controllerId);
    }

    public void applyToJoiningViewer(Player viewer) {
        viewer.getScheduler().run(plugin, task -> {
            UUID viewerId = viewer.getUniqueId();
            for (UUID controllerId : concealedPlayers) {
                if (!controllerId.equals(viewerId)) {
                    hideNow(viewer, controllerId);
                }
            }
        }, null);
    }

    private void hideFrom(Player viewer, UUID controllerId) {
        viewer.getScheduler().run(plugin, task -> hideNow(viewer, controllerId), null);
    }

    private void hideNow(Player viewer, UUID controllerId) {
        if (!concealedPlayers.contains(controllerId)) {
            return;
        }
        Player controller = Bukkit.getPlayer(controllerId);
        if (controller == null) {
            return;
        }

        if (hideFromTab && viewer.isListed(controller) && viewer.unlistPlayer(controller)) {
            UUID viewerId = viewer.getUniqueId();
            tabUnlistedByIncarnate.computeIfAbsent(controllerId, ignored -> ConcurrentHashMap.newKeySet())
                .add(viewerId);
            tabIndex.mark(controllerId, viewerId);
        }

        pulseTracking(viewer, controller);
    }

    private void showTo(Player viewer, UUID controllerId) {
        viewer.getScheduler().run(plugin, task -> {
            if (concealedPlayers.contains(controllerId)) {
                return;
            }
            Player controller = Bukkit.getPlayer(controllerId);
            if (controller == null) {
                forgetTabPair(controllerId, viewer.getUniqueId());
                return;
            }

            pulseTracking(viewer, controller);

            UUID viewerId = viewer.getUniqueId();
            Set<UUID> viewers = tabUnlistedByIncarnate.get(controllerId);
            boolean owned = viewers != null && viewers.remove(viewerId);
            if (owned) {
                if (viewer.canSee(controller)) {
                    viewer.listPlayer(controller);
                }
                tabIndex.forget(controllerId, viewerId);
            }
            if (viewers != null && viewers.isEmpty()) {
                tabUnlistedByIncarnate.remove(controllerId, viewers);
            }
        }, null);
    }

    private void forgetTabPair(UUID controllerId, UUID viewerId) {
        Set<UUID> viewers = tabUnlistedByIncarnate.get(controllerId);
        if (viewers != null) {
            viewers.remove(viewerId);
            if (viewers.isEmpty()) {
                tabUnlistedByIncarnate.remove(controllerId, viewers);
            }
        }
        tabIndex.forget(controllerId, viewerId);
    }

    private void forgetTabController(UUID controllerId) {
        tabUnlistedByIncarnate.remove(controllerId);
        tabIndex.forgetController(controllerId);
    }

    private void pulseTracking(Player viewer, Player controller) {
        viewer.hidePlayer(plugin, controller);
        viewer.showPlayer(plugin, controller);
    }
}
