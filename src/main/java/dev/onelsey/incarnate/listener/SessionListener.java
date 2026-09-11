package dev.onelsey.incarnate.listener;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import dev.onelsey.incarnate.IncarnatePlugin;
import dev.onelsey.incarnate.ability.AbilityGesture;
import dev.onelsey.incarnate.input.InputSnapshot;
import dev.onelsey.incarnate.input.PrimaryInputDeduplicator;
import dev.onelsey.incarnate.input.PrimaryInputTransport;
import dev.onelsey.incarnate.input.SecondaryInputDeduplicator;
import dev.onelsey.incarnate.input.SecondaryInputTransport;
import dev.onelsey.incarnate.possession.PossessionManager;
import dev.onelsey.incarnate.possession.PossessionSession;
import dev.onelsey.incarnate.possession.ReleaseReason;
import dev.onelsey.incarnate.vision.WardenVisionManager;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.TimeUnit;

public final class SessionListener implements Listener {
    private final PossessionManager possessions;
    private final PrimaryInputDeduplicator primaryInputDeduplicator;
    private final SecondaryInputDeduplicator secondaryInputDeduplicator;
    private final WardenVisionManager wardenVision;
    private final boolean releaseHotkey;
    private final boolean secondaryHotkey;
    private final boolean sneakPrimaryGesture;
    private final boolean sprintPrimaryGesture;
    private final long spectatorShiftLatchNanos;

    public SessionListener(
        IncarnatePlugin plugin,
        PossessionManager possessions,
        PrimaryInputDeduplicator primaryInputDeduplicator,
        SecondaryInputDeduplicator secondaryInputDeduplicator,
        WardenVisionManager wardenVision
    ) {
        this.possessions = possessions;
        this.primaryInputDeduplicator = primaryInputDeduplicator;
        this.secondaryInputDeduplicator = secondaryInputDeduplicator;
        this.wardenVision = wardenVision;
        this.releaseHotkey = plugin.getConfig().getBoolean("release-key.shift-swap-offhand", true);
        this.secondaryHotkey = plugin.getConfig().getBoolean("secondary-key.swap-offhand", true);
        this.sneakPrimaryGesture = plugin.getConfig().getBoolean("input.gestures.sneak-primary", true);
        this.sprintPrimaryGesture = plugin.getConfig().getBoolean("input.gestures.sprint-primary", true);
        long latchMillis = Math.max(100L, plugin.getConfig().getLong("release-key.spectator-shift-latch-ms", 650L));
        this.spectatorShiftLatchNanos = TimeUnit.MILLISECONDS.toNanos(latchMillis);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session != null && session.isActive()) {
            if (event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
                event.setCancelled(true);
                return;
            }
            wardenVision.ensureActive(session);
            return;
        }
        wardenVision.deactivate(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInput(PlayerInputEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session != null && session.isActive()) {
            wardenVision.ensureActive(session);
        }
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
        PossessionSession session = possessions.session(event.getPlayer());
        if (session != null && session.isActive()) {
            wardenVision.ensureActive(session);
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
            session.markSpectatorShiftAttempt();
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
            triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.SPECTATE_ENTITY);
            return;
        }
        if (!event.getNewSpectatorTarget().getUniqueId().equals(session.vesselId())) {
            event.setCancelled(true);
            triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.SPECTATE_ENTITY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        PossessionSession session = possessions.session(event.getPlayer());
        if (session == null || !session.isActive()) {
            return;
        }
        event.setCancelled(true);
        triggerSwapFromTransport(event.getPlayer(), SecondaryInputTransport.BUKKIT_SWAP);
    }

    public void triggerSwapFromPacket(Player player) {
        triggerSwapFromTransport(player, SecondaryInputTransport.PLAYER_ACTION_PACKET);
    }

    private void triggerSwapFromTransport(Player player, SecondaryInputTransport transport) {
        PossessionSession session = possessions.session(player);
        if (session == null || !session.isActive()) {
            return;
        }
        if (!secondaryInputDeduplicator.accept(player.getUniqueId(), transport, System.nanoTime())) {
            return;
        }

        boolean sneak = session.input().sneak()
            || player.isSneaking()
            || session.hasRecentSpectatorShiftAttempt(spectatorShiftLatchNanos);

        if (releaseHotkey && sneak) {
            possessions.requestRelease(session, ReleaseReason.HOTKEY);
            return;
        }

        if (secondaryHotkey) {
            possessions.triggerSecondary(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrimary(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.ARM_SWING);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrimaryInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.INTERACT);
    }

    public void triggerPrimaryFromPacket(Player player) {
        triggerPrimaryFromTransport(player, PrimaryInputTransport.SPECTATOR_PACKET);
    }

    private void triggerPrimaryFromTransport(Player player, PrimaryInputTransport transport) {
        PossessionSession session = possessions.session(player);
        if (session == null || !session.isActive()) {
            return;
        }
        if (!primaryInputDeduplicator.accept(player.getUniqueId(), transport, System.nanoTime())) {
            return;
        }
        possessions.triggerGesture(player, primaryGesture(player, session));
    }

    private AbilityGesture primaryGesture(Player player, PossessionSession session) {
        InputSnapshot input = session.input();
        if (sneakPrimaryGesture && (input.sneak() || player.isSneaking())) {
            return AbilityGesture.SNEAK_PRIMARY;
        }
        if (sprintPrimaryGesture && (input.sprint() || player.isSprinting())) {
            return AbilityGesture.SPRINT_PRIMARY;
        }
        return AbilityGesture.PRIMARY;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)
            || event.getCause() != EntityPotionEffectEvent.Cause.WARDEN
            || event.getModifiedType() != PotionEffectType.DARKNESS) {
            return;
        }
        PossessionSession session = possessions.session(player);
        if (session != null && session.isActive() && wardenVision.suppressesNativeDarkness(session)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent event) {
        if (!Tag.GAME_EVENT_WARDEN_CAN_LISTEN.isTagged(event.getEvent())) {
            return;
        }

        Entity source = event.getEntity();
        java.util.UUID sourceId = null;
        java.util.UUID sonicTargetId = null;
        if (source != null && Bukkit.isOwnedByCurrentRegion(source)) {
            if (source.isSneaking() && Tag.GAME_EVENT_IGNORE_VIBRATIONS_SNEAKING.isTagged(event.getEvent())) {
                return;
            }
            sourceId = source.getUniqueId();
            if (source instanceof LivingEntity living) {
                sonicTargetId = living.getUniqueId();
            } else if (source instanceof Projectile projectile) {
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof LivingEntity living && Bukkit.isOwnedByCurrentRegion(living)) {
                    sonicTargetId = living.getUniqueId();
                }
            }
        }

        org.bukkit.Location location = event.getLocation();
        possessions.recordWardenGameEvent(
            location.getWorld().getUID(),
            location.getX(),
            location.getY(),
            location.getZ(),
            event.getEvent().getKey().getKey(),
            event.getRadius(),
            sourceId,
            sonicTargetId
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTrackEntity(PlayerTrackEntityEvent event) {
        if (event.getEntity() instanceof Player controller
            && possessions.isControllerConcealed(controller.getUniqueId())
            && !event.getPlayer().getUniqueId().equals(controller.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        PossessionSession session = possessions.session(event.getPlayer());
        if (session != null && session.isActive()
            && wardenVision.filterTrack(session, event.getPlayer(), event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrackEntity(PlayerUntrackEntityEvent event) {
        wardenVision.onUntrack(event.getPlayer(), event.getEntity());
    }

    @EventHandler
    public void onControllerDeath(PlayerDeathEvent event) {
        wardenVision.deactivate(event.getEntity(), true);
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
        primaryInputDeduplicator.clear(event.getPlayer().getUniqueId());
        secondaryInputDeduplicator.clear(event.getPlayer().getUniqueId());
        wardenVision.deactivate(event.getPlayer(), false);
        possessions.releaseOnQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        possessions.onPlayerJoin(event.getPlayer());
    }
}
