package dev.onelsey.incarnate.listener;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import dev.onelsey.incarnate.IncarnatePlugin;
import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionSession;
import dev.onelsey.incarnate.possession.ReleaseReason;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.concurrent.TimeUnit;

public final class SessionListener implements Listener {
    private final PossessionManager possessions;
    private final boolean releaseHotkey;
    private final boolean secondaryHotkey;
    private final long spectatorShiftLatchNanos;

    public SessionListener(IncarnatePlugin plugin, PossessionManager possessions) {
        this.possessions = possessions;
        this.releaseHotkey = plugin.getConfig().getBoolean("release-key.shift-swap-offhand", true);
        this.secondaryHotkey = plugin.getConfig().getBoolean("secondary-key.swap-offhand", true);
        long latchMillis = Math.max(100L, plugin.getConfig().getLong("release-key.spectator-shift-latch-ms", 650L));
        this.spectatorShiftLatchNanos = TimeUnit.MILLISECONDS.toNanos(latchMillis);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session != null && session.isActive() && event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInput(PlayerInputEvent event) {
        possessions.updateInput(event.getPlayer(), InputSnapshot.from(event.getInput()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTeleport(PlayerTeleportEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session == null || !session.isActive()) {
            return;
        }
        if (session.cameraTeleportInProgress()) {
            return;
        }
        if (session.usesSpectatorTargetCamera() && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onView(PlayerMoveEvent event) {
        if (event.getTo() == null || !possessions.isPossessing(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getYaw() == event.getTo().getYaw() && event.getFrom().getPitch() == event.getTo().getPitch()) {
            return;
        }
        possessions.updateView(event.getPlayer(), event.getTo().getYaw(), event.getTo().getPitch());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PossessionSession session = possessions.session(player);
        if (session == null || !session.isActive() || !session.usesMountedCamera()) {
            return;
        }
        if (event.getDismounted().getUniqueId().equals(session.vesselId()) && event.isCancellable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session == null || !session.isActive() || !session.usesSpectatorTargetCamera()) {
            return;
        }
        if (event.getSpectatorTarget().getUniqueId().equals(session.vesselId())) {
            session.markSpectatorShiftAttempt();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStartSpectating(PlayerStartSpectatingEntityEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session == null || !session.isActive()) {
            return;
        }
        if (session.usesMountedCamera()) {
            event.setCancelled(true);
            possessions.triggerPrimary(event.getPlayer());
            return;
        }
        if (!event.getNewSpectatorTarget().getUniqueId().equals(session.vesselId())) {
            event.setCancelled(true);
            possessions.triggerPrimary(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session == null || !session.isActive()) {
            return;
        }

        boolean sneak = session.input().sneak()
            || event.getPlayer().isSneaking()
            || session.hasRecentSpectatorShiftAttempt(spectatorShiftLatchNanos);

        if (releaseHotkey && sneak) {
            event.setCancelled(true);
            possessions.requestRelease(session, ReleaseReason.HOTKEY);
            return;
        }

        if (secondaryHotkey) {
            event.setCancelled(true);
            possessions.triggerSecondary(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrimary(PlayerArmSwingEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && possessions.isPossessing(event.getPlayer())) {
            possessions.triggerPrimary(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTrackEntity(PlayerTrackEntityEvent event) {
        if (event.getEntity() instanceof Player controller
            && possessions.isControllerConcealed(controller.getUniqueId())
            && !event.getPlayer().getUniqueId().equals(controller.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onControllerDeath(PlayerDeathEvent event) {
        possessions.releaseOnControllerDeath(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        possessions.onPlayerRespawn(event.getPlayer());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        PossessionSession session = possessions.session(mob);
        if (session != null) {
            possessions.requestRelease(session, ReleaseReason.VESSEL_DIED);
        }
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        PossessionSession session = possessions.session(mob);
        if (session != null) {
            possessions.requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Mob mob) {
                possessions.recoverOrphanVessel(mob);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        possessions.releaseOnQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        possessions.onPlayerJoin(event.getPlayer());
    }
}
