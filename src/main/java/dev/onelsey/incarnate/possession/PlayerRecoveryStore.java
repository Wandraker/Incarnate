package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Pose;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerRecoveryStore {
    private final IncarnatePlugin plugin;
    private final NamespacedKey activeKey;
    private final NamespacedKey gameModeKey;
    private final NamespacedKey allowFlightKey;
    private final NamespacedKey flyingKey;
    private final NamespacedKey flySpeedKey;
    private final NamespacedKey invulnerableKey;
    private final NamespacedKey collidableKey;
    private final NamespacedKey invisibleKey;
    private final NamespacedKey affectsSpawningKey;
    private final NamespacedKey poseKey;
    private final NamespacedKey fixedPoseKey;
    private final NamespacedKey noPhysicsKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey xKey;
    private final NamespacedKey yKey;
    private final NamespacedKey zKey;
    private final NamespacedKey yawKey;
    private final NamespacedKey pitchKey;

    public PlayerRecoveryStore(IncarnatePlugin plugin) {
        this.plugin = plugin;
        this.activeKey = new NamespacedKey(plugin, "recovery_active");
        this.gameModeKey = new NamespacedKey(plugin, "recovery_game_mode");
        this.allowFlightKey = new NamespacedKey(plugin, "recovery_allow_flight");
        this.flyingKey = new NamespacedKey(plugin, "recovery_flying");
        this.flySpeedKey = new NamespacedKey(plugin, "recovery_fly_speed");
        this.invulnerableKey = new NamespacedKey(plugin, "recovery_invulnerable");
        this.collidableKey = new NamespacedKey(plugin, "recovery_collidable");
        this.invisibleKey = new NamespacedKey(plugin, "recovery_invisible");
        this.affectsSpawningKey = new NamespacedKey(plugin, "recovery_affects_spawning");
        this.poseKey = new NamespacedKey(plugin, "recovery_pose");
        this.fixedPoseKey = new NamespacedKey(plugin, "recovery_fixed_pose");
        this.noPhysicsKey = new NamespacedKey(plugin, "recovery_no_physics");
        this.worldKey = new NamespacedKey(plugin, "recovery_world");
        this.xKey = new NamespacedKey(plugin, "recovery_x");
        this.yKey = new NamespacedKey(plugin, "recovery_y");
        this.zKey = new NamespacedKey(plugin, "recovery_z");
        this.yawKey = new NamespacedKey(plugin, "recovery_yaw");
        this.pitchKey = new NamespacedKey(plugin, "recovery_pitch");
    }

    public void save(Player player, PlayerState state) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(activeKey);
        data.set(gameModeKey, PersistentDataType.STRING, state.gameMode().name());
        data.set(allowFlightKey, PersistentDataType.BYTE, bool(state.allowFlight()));
        data.set(flyingKey, PersistentDataType.BYTE, bool(state.flying()));
        data.set(flySpeedKey, PersistentDataType.FLOAT, state.flySpeed());
        data.set(invulnerableKey, PersistentDataType.BYTE, bool(state.invulnerable()));
        data.set(collidableKey, PersistentDataType.BYTE, bool(state.collidable()));
        data.set(invisibleKey, PersistentDataType.BYTE, bool(state.invisible()));
        data.set(affectsSpawningKey, PersistentDataType.BYTE, bool(state.affectsSpawning()));
        data.set(poseKey, PersistentDataType.STRING, state.pose().name());
        data.set(fixedPoseKey, PersistentDataType.BYTE, bool(state.fixedPose()));
        data.set(noPhysicsKey, PersistentDataType.BYTE, bool(state.noPhysics()));

        Location location = state.location();
        if (location.getWorld() != null) {
            data.set(worldKey, PersistentDataType.STRING, location.getWorld().getUID().toString());
            data.set(xKey, PersistentDataType.DOUBLE, location.getX());
            data.set(yKey, PersistentDataType.DOUBLE, location.getY());
            data.set(zKey, PersistentDataType.DOUBLE, location.getZ());
            data.set(yawKey, PersistentDataType.FLOAT, location.getYaw());
            data.set(pitchKey, PersistentDataType.FLOAT, location.getPitch());
        } else {
            data.remove(worldKey);
            data.remove(xKey);
            data.remove(yKey);
            data.remove(zKey);
            data.remove(yawKey);
            data.remove(pitchKey);
        }

        data.set(activeKey, PersistentDataType.BYTE, (byte) 1);
    }

    public boolean hasRecovery(Player player) {
        Byte active = player.getPersistentDataContainer().get(activeKey, PersistentDataType.BYTE);
        return active != null && active != 0;
    }

    public Location savedLocation(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        String rawWorld = data.get(worldKey, PersistentDataType.STRING);
        Double x = data.get(xKey, PersistentDataType.DOUBLE);
        Double y = data.get(yKey, PersistentDataType.DOUBLE);
        Double z = data.get(zKey, PersistentDataType.DOUBLE);
        Float yaw = data.get(yawKey, PersistentDataType.FLOAT);
        Float pitch = data.get(pitchKey, PersistentDataType.FLOAT);
        if (rawWorld == null || x == null || y == null || z == null) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(UUID.fromString(rawWorld));
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z, yaw == null ? 0.0f : yaw, pitch == null ? 0.0f : pitch);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public CompletableFuture<Boolean> recover(Player player) {
        if (!hasRecovery(player)) {
            return CompletableFuture.completedFuture(true);
        }

        PersistentDataContainer data = player.getPersistentDataContainer();
        String rawMode = data.get(gameModeKey, PersistentDataType.STRING);
        boolean allowFlight = readBool(data, allowFlightKey, false);
        boolean flying = readBool(data, flyingKey, false);
        Float flySpeed = data.get(flySpeedKey, PersistentDataType.FLOAT);
        boolean invulnerable = readBool(data, invulnerableKey, false);
        boolean collidable = readBool(data, collidableKey, true);
        boolean invisible = readBool(data, invisibleKey, false);
        boolean affectsSpawning = readBool(data, affectsSpawningKey, true);
        String rawPose = data.get(poseKey, PersistentDataType.STRING);
        boolean fixedPose = readBool(data, fixedPoseKey, player.hasFixedPose());
        boolean noPhysics = readBool(data, noPhysicsKey, player.hasNoPhysics());
        String rawWorld = data.get(worldKey, PersistentDataType.STRING);
        Double x = data.get(xKey, PersistentDataType.DOUBLE);
        Double y = data.get(yKey, PersistentDataType.DOUBLE);
        Double z = data.get(zKey, PersistentDataType.DOUBLE);
        Location location = savedLocation(player);

        if (location == null && rawWorld != null && x != null && y != null && z != null) {
            try {
                UUID worldId = UUID.fromString(rawWorld);
                if (Bukkit.getWorld(worldId) == null) {
                    plugin.getLogger().warning("Recovery world " + worldId + " is not currently loaded for " + player.getUniqueId() + "; recovery marker was kept.");
                    return CompletableFuture.completedFuture(false);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (player.getGameMode() == GameMode.SPECTATOR) {
            try {
                player.setSpectatorTarget(null);
            } catch (IllegalStateException | IllegalArgumentException ignored) {
            }
        }

        GameMode mode = GameMode.SURVIVAL;
        if (rawMode != null) {
            try {
                mode = GameMode.valueOf(rawMode);
            } catch (IllegalArgumentException ignored) {
            }
        }

        player.setGameMode(mode);
        player.setInvulnerable(invulnerable);
        player.setCollidable(collidable);
        player.setInvisible(invisible);
        player.setAffectsSpawning(affectsSpawning);
        player.setNoPhysics(noPhysics);
        if (rawPose != null) {
            try {
                player.setPose(Pose.valueOf(rawPose), fixedPose);
            } catch (IllegalArgumentException ignored) {
            }
        }
        player.setAllowFlight(allowFlight);
        if (flySpeed != null && flySpeed >= -1.0f && flySpeed <= 1.0f) {
            player.setFlySpeed(flySpeed);
        }
        player.setFlying(allowFlight && flying);

        if (location == null) {
            plugin.getLogger().warning("Recovery location data was missing or invalid for " + player.getUniqueId() + "; restored player state in place and cleared the stale marker.");
            clear(player);
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean> completion = new CompletableFuture<>();
        player.teleportAsync(location).whenComplete((success, error) -> {
            if (error != null || !Boolean.TRUE.equals(success)) {
                plugin.getLogger().warning("Could not complete interrupted-possession recovery teleport for " + player.getUniqueId() + ".");
                completion.complete(false);
                return;
            }

            var clearTask = player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) {
                    completion.complete(false);
                    return;
                }
                clear(player);
                completion.complete(true);
            }, () -> completion.complete(false));
            if (clearTask == null) {
                completion.complete(false);
            }
        });
        return completion;
    }

    public void clear(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.remove(activeKey);
        data.remove(gameModeKey);
        data.remove(allowFlightKey);
        data.remove(flyingKey);
        data.remove(flySpeedKey);
        data.remove(invulnerableKey);
        data.remove(collidableKey);
        data.remove(invisibleKey);
        data.remove(affectsSpawningKey);
        data.remove(poseKey);
        data.remove(fixedPoseKey);
        data.remove(noPhysicsKey);
        data.remove(worldKey);
        data.remove(xKey);
        data.remove(yKey);
        data.remove(zKey);
        data.remove(yawKey);
        data.remove(pitchKey);
    }

    private static byte bool(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    private static boolean readBool(PersistentDataContainer data, NamespacedKey key, boolean fallback) {
        Byte value = data.get(key, PersistentDataType.BYTE);
        return value == null ? fallback : value != 0;
    }
}
