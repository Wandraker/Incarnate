from pathlib import Path

path = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = path.read_text()


def replace_once(old: str, new: str, name: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{name}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import net.kyori.adventure.text.Component;\nimport org.bukkit.GameMode;",
    "import net.kyori.adventure.text.Component;\nimport org.bukkit.Bukkit;\nimport org.bukkit.GameMode;",
    "Bukkit import",
)

replace_once(
    '''            restorePlayerState(player, session.playerState());
            visibility.reveal(session.playerId(), player);

            Location destination = releaseAtVessel && vesselLocation != null ? vesselLocation : session.playerState().location();
            player.teleportAsync(destination).whenComplete((success, error) -> {
                ScheduledTask clearTask = player.getScheduler().run(plugin, ignored -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error == null && Boolean.TRUE.equals(success)) {
                        playerRecovery.clear(player);
                        restoringPlayers.remove(session.playerId());
                    } else {
                        plugin.getLogger().warning("Release teleport failed for " + player.getUniqueId() + "; recovery marker was kept.");
                        player.sendMessage(Component.text("[Incarnate] Release teleport failed; recovery state was kept for safety."));
                    }
                }, null);
                if (clearTask == null) {
                }
            });

            if (reason != ReleaseReason.QUIT && reason != ReleaseReason.PLUGIN_DISABLE) {
                player.sendMessage(Component.text("[Incarnate] Released from vessel."));
            }''',
    '''            restorePlayerState(player, session.playerState());

            Location destination = releaseAtVessel && vesselLocation != null ? vesselLocation : session.playerState().location();
            player.teleportAsync(destination).whenComplete((success, error) -> {
                ScheduledTask clearTask = player.getScheduler().run(plugin, ignored -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error == null && Boolean.TRUE.equals(success)) {
                        playerRecovery.clear(player);
                        restoringPlayers.remove(session.playerId());
                        visibility.reveal(session.playerId(), player);
                        if (reason != ReleaseReason.QUIT && reason != ReleaseReason.PLUGIN_DISABLE) {
                            player.sendMessage(Component.text("[Incarnate] Released from vessel."));
                        }
                    } else {
                        plugin.getLogger().warning("Release teleport failed for " + player.getUniqueId() + "; recovery marker was kept.");
                        player.sendMessage(Component.text("[Incarnate] Release teleport failed; recovery state was kept for safety."));
                    }
                }, null);
                if (clearTask == null) {
                    // Keep recovery PDC and concealment. Recovery will finish on a safe player thread later.
                }
            });''',
    "release reveal ordering",
)

replace_once(
    '''        player.setFlySpeed(state.flySpeed());
        if (state.allowFlight()) {
            player.setFlying(state.flying());
        }''',
    '''        player.setFlySpeed(state.flySpeed());
        player.setFlying(state.allowFlight() && state.flying());''',
    "flight restore",
)

replace_once(
    '''        player.getScheduler().runDelayed(plugin, task -> {
            recoverPlayerIfNeeded(player);
            visibility.reveal(player.getUniqueId(), player);
        }, null, 1L);''',
    '''        player.getScheduler().runDelayed(plugin, task -> recoverPlayerIfNeeded(player), null, 1L);''',
    "respawn reveal ordering",
)

replace_once(
    '''        player.getScheduler().run(plugin, task -> {
            recoverPlayerIfNeeded(player);
            visibility.reveal(player.getUniqueId(), player);
        }, null);''',
    '''        player.getScheduler().run(plugin, task -> recoverPlayerIfNeeded(player), null);''',
    "join reveal ordering",
)

replace_once(
    '''            if (error == null && Boolean.TRUE.equals(success)) {
                restoringPlayers.remove(playerId);
                notifyPlayer(player, "[Incarnate] Recovered from an interrupted possession session.");''',
    '''            if (error == null && Boolean.TRUE.equals(success)) {
                restoringPlayers.remove(playerId);
                visibility.reveal(playerId, player);
                notifyPlayer(player, "[Incarnate] Recovered from an interrupted possession session.");''',
    "recovery reveal ordering",
)

replace_once(
    '''    public void shutdown() {
        for (PossessionSession session : List.copyOf(byPlayer.values())) {
            requestRelease(session, ReleaseReason.PLUGIN_DISABLE);
        }
        pendingPlayers.clear();
        pendingVessels.clear();
    }''',
    '''    public void shutdown() {
        for (PossessionSession session : List.copyOf(byPlayer.values())) {
            if (!session.deactivate()) {
                continue;
            }

            byPlayer.remove(session.playerId(), session);
            byVessel.remove(session.vesselId(), session);
            cancelTasks(session);

            Player player = session.player();
            if (player.isOnline() && Bukkit.isOwnedByCurrentRegion(player)) {
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    try {
                        player.setSpectatorTarget(null);
                    } catch (IllegalStateException ignored) {
                    }
                }
                restorePlayerState(player, session.playerState());
            }

            Mob vessel = session.vessel();
            if (vessel.isValid() && Bukkit.isOwnedByCurrentRegion(vessel)) {
                if (session.origin() == PossessionOrigin.CREATED && removeCreatedOnRelease) {
                    vesselRecovery.clear(vessel);
                    vessel.remove();
                } else {
                    if (!vessel.isDead()) {
                        session.vesselState().restore(vessel);
                    }
                    vesselRecovery.clear(vessel);
                }
            }

            // JavaPlugin is already disabled before onDisable runs, so scheduling here is illegal on Folia.
            // Recovery PDC remains authoritative for state that the current region does not safely own.
            visibility.forget(session.playerId());
        }

        byPlayer.clear();
        byVessel.clear();
        pendingPlayers.clear();
        pendingVessels.clear();
        recoveryInFlight.clear();
    }''',
    "disabled-plugin shutdown",
)

path.write_text(text)
print("PossessionManager lifecycle patch applied successfully")
