from pathlib import Path


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    if old not in text:
        raise RuntimeError(f"Expected text not found in {path}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


replace_once("build.gradle.kts", 'version = "0.9.0-dev.5"', 'version = "0.9.0-dev.6"')
replace_once("src/main/resources/plugin.yml", "version: 0.9.0-dev.5", "version: 0.9.0-dev.6")

replace_once("src/main/resources/config.yml", "config-version: 9", "config-version: 10")
replace_once("src/main/resources/config.yml", "  mounted-client-game-mode: ADVENTURE\n", "")
replace_once("src/main/resources/config.yml", "  fallback-to-spectator-target: true", "  fallback-to-spectator-target: false")

replace_once(
    "src/main/java/dev/onelsey/incarnate/config/ConfigMigrator.java",
    "    private static final int CURRENT_SCHEMA = 9;",
    "    private static final int CURRENT_SCHEMA = 10;"
)
replace_once(
    "src/main/java/dev/onelsey/incarnate/config/ConfigMigrator.java",
    "        if (target.getInt(\"config-version\", 0) != CURRENT_SCHEMA) {\n",
    "        if (sourceSchema < 10 && target.contains(\"camera.mounted-client-game-mode\")) {\n"
    "            target.set(\"camera.mounted-client-game-mode\", null);\n"
    "            changed = true;\n"
    "        }\n"
    "        if (target.getInt(\"config-version\", 0) != CURRENT_SCHEMA) {\n"
)

write(
    "src/main/java/dev/onelsey/incarnate/possession/PlayerState.java",
    '''package dev.onelsey.incarnate.possession;\n\nimport org.bukkit.GameMode;\nimport org.bukkit.Location;\nimport org.bukkit.entity.Player;\n\npublic record PlayerState(\n    GameMode gameMode,\n    Location location,\n    float flySpeed,\n    boolean allowFlight,\n    boolean flying,\n    boolean invulnerable,\n    boolean collidable,\n    boolean affectsSpawning\n) {\n    public static PlayerState capture(Player player) {\n        return new PlayerState(\n            player.getGameMode(),\n            player.getLocation().clone(),\n            player.getFlySpeed(),\n            player.getAllowFlight(),\n            player.isFlying(),\n            player.isInvulnerable(),\n            player.isCollidable(),\n            player.getAffectsSpawning()\n        );\n    }\n}\n'''
)

recovery = "src/main/java/dev/onelsey/incarnate/possession/PlayerRecoveryStore.java"
replace_once(
    recovery,
    "    private final NamespacedKey flySpeedKey;\n",
    "    private final NamespacedKey flySpeedKey;\n"
    "    private final NamespacedKey invulnerableKey;\n"
    "    private final NamespacedKey collidableKey;\n"
    "    private final NamespacedKey affectsSpawningKey;\n"
)
replace_once(
    recovery,
    '        this.flySpeedKey = new NamespacedKey(plugin, "recovery_fly_speed");\n',
    '        this.flySpeedKey = new NamespacedKey(plugin, "recovery_fly_speed");\n'
    '        this.invulnerableKey = new NamespacedKey(plugin, "recovery_invulnerable");\n'
    '        this.collidableKey = new NamespacedKey(plugin, "recovery_collidable");\n'
    '        this.affectsSpawningKey = new NamespacedKey(plugin, "recovery_affects_spawning");\n'
)
replace_once(
    recovery,
    "        data.set(flySpeedKey, PersistentDataType.FLOAT, state.flySpeed());\n",
    "        data.set(flySpeedKey, PersistentDataType.FLOAT, state.flySpeed());\n"
    "        data.set(invulnerableKey, PersistentDataType.BYTE, bool(state.invulnerable()));\n"
    "        data.set(collidableKey, PersistentDataType.BYTE, bool(state.collidable()));\n"
    "        data.set(affectsSpawningKey, PersistentDataType.BYTE, bool(state.affectsSpawning()));\n"
)
replace_once(
    recovery,
    "        Float flySpeed = data.get(flySpeedKey, PersistentDataType.FLOAT);\n",
    "        Float flySpeed = data.get(flySpeedKey, PersistentDataType.FLOAT);\n"
    "        boolean invulnerable = readBool(data, invulnerableKey, false);\n"
    "        boolean collidable = readBool(data, collidableKey, true);\n"
    "        boolean affectsSpawning = readBool(data, affectsSpawningKey, true);\n"
)
replace_once(
    recovery,
    "        player.setGameMode(mode);\n        player.setAllowFlight(allowFlight);\n",
    "        player.setGameMode(mode);\n"
    "        player.setInvulnerable(invulnerable);\n"
    "        player.setCollidable(collidable);\n"
    "        player.setAffectsSpawning(affectsSpawning);\n"
    "        player.setAllowFlight(allowFlight);\n"
)
replace_once(
    recovery,
    "        data.remove(flySpeedKey);\n",
    "        data.remove(flySpeedKey);\n"
    "        data.remove(invulnerableKey);\n"
    "        data.remove(collidableKey);\n"
    "        data.remove(affectsSpawningKey);\n"
)

manager = "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java"
replace_once(manager, "    private final ClientGameModeBridge clientGameModeBridge;\n", "")
replace_once(manager, "        this.clientGameModeBridge = new ClientGameModeBridge(plugin);\n", "")
replace_once(
    manager,
    "            player.setGameMode(GameMode.SPECTATOR);\n"
    "            try {\n"
    "                player.setSpectatorTarget(null);\n"
    "            } catch (IllegalStateException | IllegalArgumentException ignored) {\n"
    "            }\n\n",
    "            applyControllerIsolation(player);\n\n"
)
replace_once(manager, "            clientGameModeBridge.presentMounted(player);\n", "")
replace_once(
    manager,
    "            clientGameModeBridge.present(player, GameMode.SPECTATOR);\n"
    "            session.cameraTransport(CameraTransport.SPECTATOR_TARGET);\n"
    "            try {\n",
    "            session.cameraTransport(CameraTransport.SPECTATOR_TARGET);\n"
    "            if (player.getGameMode() != GameMode.SPECTATOR) {\n"
    "                player.setGameMode(GameMode.SPECTATOR);\n"
    "            }\n"
    "            try {\n"
)
replace_once(
    manager,
    "        if (attachTask == null) {\n"
    "            requestRelease(session, ReleaseReason.QUIT);\n"
    "        }\n"
    "    }\n\n"
    "    private void attachMountedCamera(PossessionSession session) {\n",
    "        if (attachTask == null) {\n"
    "            requestRelease(session, ReleaseReason.QUIT);\n"
    "        }\n"
    "    }\n\n"
    "    private static void applyControllerIsolation(Player player) {\n"
    "        if (player.getGameMode() == GameMode.SPECTATOR) {\n"
    "            try {\n"
    "                player.setSpectatorTarget(null);\n"
    "            } catch (IllegalStateException | IllegalArgumentException ignored) {\n"
    "            }\n"
    "        }\n"
    "        if (player.getGameMode() != GameMode.ADVENTURE) {\n"
    "            player.setGameMode(GameMode.ADVENTURE);\n"
    "        }\n"
    "        player.setFlying(false);\n"
    "        player.setAllowFlight(false);\n"
    "        player.setInvulnerable(true);\n"
    "        player.setCollidable(false);\n"
    "        player.setAffectsSpawning(false);\n"
    "    }\n\n"
    "    private void attachMountedCamera(PossessionSession session) {\n"
)
replace_once(
    manager,
    "    private void restorePlayerState(Player player, PlayerState state) {\n"
    "        player.setGameMode(state.gameMode());\n"
    "        player.setAllowFlight(state.allowFlight());\n"
    "        player.setFlySpeed(state.flySpeed());\n"
    "        player.setFlying(state.allowFlight() && state.flying());\n"
    "        clientGameModeBridge.present(player, state.gameMode());\n"
    "    }\n",
    "    private void restorePlayerState(Player player, PlayerState state) {\n"
    "        player.setGameMode(state.gameMode());\n"
    "        player.setInvulnerable(state.invulnerable());\n"
    "        player.setCollidable(state.collidable());\n"
    "        player.setAffectsSpawning(state.affectsSpawning());\n"
    "        player.setAllowFlight(state.allowFlight());\n"
    "        player.setFlySpeed(state.flySpeed());\n"
    "        player.setFlying(state.allowFlight() && state.flying());\n"
    "    }\n"
)

listener = "src/main/java/dev/onelsey/incarnate/listener/SessionListener.java"
replace_once(
    listener,
    "            if (event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {\n"
    "                event.setCancelled(true);\n"
    "                return;\n"
    "            }\n",
    "            org.bukkit.GameMode expected = session.usesSpectatorTargetCamera()\n"
    "                ? org.bukkit.GameMode.SPECTATOR\n"
    "                : org.bukkit.GameMode.ADVENTURE;\n"
    "            if (event.getNewGameMode() != expected) {\n"
    "                event.setCancelled(true);\n"
    "                return;\n"
    "            }\n"
)
replace_once(
    listener,
    "    public void onPrimaryInteract(PlayerInteractEvent event) {\n"
    "        if (event.getHand() != EquipmentSlot.HAND) {\n"
    "            return;\n"
    "        }\n"
    "        Action action = event.getAction();\n"
    "        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {\n"
    "            return;\n"
    "        }\n"
    "        triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.INTERACT);\n"
    "    }\n",
    "    public void onPrimaryInteract(PlayerInteractEvent event) {\n"
    "        PossessionSession session = possessions.session(event.getPlayer());\n"
    "        if (session == null || !session.isActive()) {\n"
    "            return;\n"
    "        }\n"
    "        if (event.getHand() == EquipmentSlot.HAND) {\n"
    "            Action action = event.getAction();\n"
    "            if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {\n"
    "                triggerPrimaryFromTransport(event.getPlayer(), PrimaryInputTransport.INTERACT);\n"
    "            }\n"
    "        }\n"
    "        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);\n"
    "        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);\n"
    "        event.setCancelled(true);\n"
    "    }\n"
)

plugin = "src/main/java/dev/onelsey/incarnate/IncarnatePlugin.java"
replace_once(
    plugin,
    "import dev.onelsey.incarnate.listener.SessionListener;\n",
    "import dev.onelsey.incarnate.listener.ControllerIsolationListener;\n"
    "import dev.onelsey.incarnate.listener.SessionListener;\n"
)
replace_once(
    plugin,
    "        getServer().getPluginManager().registerEvents(sessionListener, this);\n",
    "        getServer().getPluginManager().registerEvents(sessionListener, this);\n"
    "        getServer().getPluginManager().registerEvents(new ControllerIsolationListener(possessions), this);\n"
)

write(
    "src/main/java/dev/onelsey/incarnate/listener/ControllerIsolationListener.java",
    '''package dev.onelsey.incarnate.listener;\n\nimport dev.onelsey.incarnate.possession.PossessionManager;\nimport dev.onelsey.incarnate.possession.PossessionSession;\nimport org.bukkit.entity.Player;\nimport org.bukkit.entity.Projectile;\nimport org.bukkit.event.EventHandler;\nimport org.bukkit.event.EventPriority;\nimport org.bukkit.event.Listener;\nimport org.bukkit.event.block.BlockBreakEvent;\nimport org.bukkit.event.block.BlockPlaceEvent;\nimport org.bukkit.event.entity.EntityCombustEvent;\nimport org.bukkit.event.entity.EntityDamageByEntityEvent;\nimport org.bukkit.event.entity.EntityDamageEvent;\nimport org.bukkit.event.entity.EntityPickupItemEvent;\nimport org.bukkit.event.entity.EntityShootBowEvent;\nimport org.bukkit.event.entity.EntityTargetLivingEntityEvent;\nimport org.bukkit.event.entity.FoodLevelChangeEvent;\nimport org.bukkit.event.entity.ProjectileLaunchEvent;\nimport org.bukkit.event.inventory.InventoryClickEvent;\nimport org.bukkit.event.inventory.InventoryDragEvent;\nimport org.bukkit.event.inventory.InventoryOpenEvent;\nimport org.bukkit.event.player.PlayerArmorStandManipulateEvent;\nimport org.bukkit.event.player.PlayerAttemptPickupItemEvent;\nimport org.bukkit.event.player.PlayerDropItemEvent;\nimport org.bukkit.event.player.PlayerFishEvent;\nimport org.bukkit.event.player.PlayerInteractAtEntityEvent;\nimport org.bukkit.event.player.PlayerInteractEntityEvent;\nimport org.bukkit.event.player.PlayerItemConsumeEvent;\nimport org.bukkit.projectiles.ProjectileSource;\n\npublic final class ControllerIsolationListener implements Listener {\n    private final PossessionManager possessions;\n\n    public ControllerIsolationListener(PossessionManager possessions) {\n        this.possessions = possessions;\n    }\n\n    private boolean active(Player player) {\n        PossessionSession session = possessions.session(player);\n        return session != null && session.isActive();\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onDamage(EntityDamageEvent event) {\n        if (event.getEntity() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onOutgoingDamage(EntityDamageByEntityEvent event) {\n        if (event.getDamager() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n            return;\n        }\n        if (event.getDamager() instanceof Projectile projectile) {\n            ProjectileSource shooter = projectile.getShooter();\n            if (shooter instanceof Player player && active(player)) {\n                event.setCancelled(true);\n            }\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onBreak(BlockBreakEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onPlace(BlockPlaceEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onInteractEntity(PlayerInteractEntityEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onArmorStand(PlayerArmorStandManipulateEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onDrop(PlayerDropItemEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onAttemptPickup(PlayerAttemptPickupItemEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onPickup(EntityPickupItemEvent event) {\n        if (event.getEntity() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onConsume(PlayerItemConsumeEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onFish(PlayerFishEvent event) {\n        if (active(event.getPlayer())) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onProjectileLaunch(ProjectileLaunchEvent event) {\n        ProjectileSource shooter = event.getEntity().getShooter();\n        if (shooter instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onShootBow(EntityShootBowEvent event) {\n        if (event.getEntity() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onFoodLevel(FoodLevelChangeEvent event) {\n        if (event.getEntity() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onCombust(EntityCombustEvent event) {\n        if (event.getEntity() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onTarget(EntityTargetLivingEntityEvent event) {\n        if (event.getTarget() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onInventoryOpen(InventoryOpenEvent event) {\n        if (event.getPlayer() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onInventoryClick(InventoryClickEvent event) {\n        if (event.getWhoClicked() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n\n    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n    public void onInventoryDrag(InventoryDragEvent event) {\n        if (event.getWhoClicked() instanceof Player player && active(player)) {\n            event.setCancelled(true);\n        }\n    }\n}\n'''
)

replace_once(
    "README.md",
    "Instead of directly attaching the client camera to the Mob through `setSpectatorTarget`, the hidden spectator Player is moved to the vessel and mounted on the real Mob while remaining its own camera. This is designed to preserve normal mouse/touch yaw and pitch input while the Mob body follows that view direction.",
    "Instead of directly attaching the client camera to the Mob through `setSpectatorTarget`, the hidden controller is moved to the vessel and mounted on the real Mob while remaining its own camera. Mounted possession now uses a real Adventure controller rather than a Spectator controller so vanilla can deliver ordinary swap-offhand (`F`) and sprint input. Incarnate isolates that controller from world gameplay with invulnerability, collision/spawn suppression and event-level interaction guards."
)
replace_once(
    "README.md",
    "  fallback-to-spectator-target: true",
    "  fallback-to-spectator-target: false"
)
replace_once(
    "README.md",
    "`SPECTATOR_TARGET` remains available as a compatibility fallback. Mounted acquisition uses Player and Mob EntitySchedulers and retries region-safe attachment before falling back or releasing safely.",
    "`SPECTATOR_TARGET` remains available as a legacy compatibility fallback, but the fresh default no longer falls back to it automatically because true Spectator mode suppresses some vanilla input used by Incarnate. Mounted acquisition uses Player and Mob EntitySchedulers and retries region-safe attachment before falling back or releasing safely."
)
replace_once(
    "README.md",
    "The Player's original game mode, flight permission/state, fly speed, world, location and rotation are stored for interrupted recovery. Recovery remains pending if the saved world is temporarily unavailable.",
    "The Player's original game mode, flight permission/state, fly speed, invulnerability, collision state, mob-spawn influence, world, location and rotation are stored for interrupted recovery. Recovery remains pending if the saved world is temporarily unavailable."
)

changelog = read("CHANGELOG.md")
entry = '''## 0.9.0-dev.6 - Adventure Controller Isolation\n\n- Replaces the mounted possession foundation with a real server-side Adventure controller instead of keeping the server in Spectator and spoofing a client-only game mode.\n- Removes the dev.5 client game-mode packet bridge; mounted `F`, `Shift + F` and sprint input now use normal vanilla Adventure input paths.\n- Adds a controller isolation layer: possessed players are invulnerable, non-collidable, excluded from mob-spawn influence, hidden by the existing visibility system, protected from incoming damage and prevented from directly damaging, breaking, placing, interacting, dropping, picking up, consuming, fishing, launching projectiles or modifying inventories.\n- Prevents mobs from targeting the hidden controller body while possession is active.\n- Extends Player recovery state with the original invulnerability, collision and spawn-influence flags so normal release, quit recovery, restart recovery and plugin shutdown restore the pre-possession state.\n- Keeps `SPECTATOR_TARGET` only as a legacy compatibility camera; new configs default its automatic fallback to disabled because true Spectator mode can suppress Incarnate controls.\n- Removes obsolete `camera.mounted-client-game-mode` during schema migration and advances gameplay config schema to 10.\n\n'''
if not changelog.startswith("# Changelog\n\n"):
    raise RuntimeError("Unexpected CHANGELOG header")
write("CHANGELOG.md", "# Changelog\n\n" + entry + changelog[len("# Changelog\n\n"):])

config_test = "src/test/java/dev/onelsey/incarnate/config/ConfigMigratorTest.java"
text = read(config_test)
text = text.replace('defaults.set("config-version", 9);', 'defaults.set("config-version", 10);')
text = text.replace('user.set("config-version", 9);', 'user.set("config-version", 10);')
text = text.replace('assertEquals(9, user.getInt("config-version"));', 'assertEquals(10, user.getInt("config-version"));')
if 'assertEquals(9, user.getInt("config-version"));' in text:
    raise RuntimeError("ConfigMigratorTest still contains schema 9 assertion")
write(config_test, text)

message_test = "src/test/java/dev/onelsey/incarnate/message/MessageResourcesTest.java"
replace_once(message_test, '        assertEquals(9, config.getInt("config-version"));\n        assertEquals("ADVENTURE", config.getString("camera.mounted-client-game-mode"));\n', '        assertEquals(10, config.getInt("config-version"));\n        assertTrue(!config.contains("camera.mounted-client-game-mode"));\n        assertTrue(!config.getBoolean("camera.fallback-to-spectator-target"));\n')

Path("src/main/java/dev/onelsey/incarnate/possession/ClientGameModeBridge.java").unlink()
Path("src/test/java/dev/onelsey/incarnate/possession/ClientGameModeBridgeTest.java").unlink()
