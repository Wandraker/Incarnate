from pathlib import Path


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_once(path, old, new):
    text = read(path)
    if old not in text:
        raise RuntimeError(f"Expected text not found in {path}: {old[:160]!r}")
    write(path, text.replace(old, new, 1))


replace_once("build.gradle.kts", 'version = "0.9.0-dev.6"', 'version = "0.9.0-dev.7"')
replace_once("src/main/resources/plugin.yml", "version: 0.9.0-dev.6", "version: 0.9.0-dev.7")

replace_once("src/main/resources/config.yml", "config-version: 10", "config-version: 11")
replace_once(
    "src/main/resources/config.yml",
    "camera:\n  mode: MOUNTED\n  mount-retries: 8\n  fallback-to-spectator-target: false",
    "camera:\n  mode: DIRECT_ENTITY\n  attach-retries: 8\n  fallback-to-spectator-target: false"
)

replace_once(
    "src/main/java/dev/onelsey/incarnate/config/ConfigMigrator.java",
    "    private static final int CURRENT_SCHEMA = 10;",
    "    private static final int CURRENT_SCHEMA = 11;"
)
replace_once(
    "src/main/java/dev/onelsey/incarnate/config/ConfigMigrator.java",
    "        if (sourceSchema < 10 && target.contains(\"camera.mounted-client-game-mode\")) {\n"
    "            target.set(\"camera.mounted-client-game-mode\", null);\n"
    "            changed = true;\n"
    "        }\n",
    "        if (sourceSchema < 10 && target.contains(\"camera.mounted-client-game-mode\")) {\n"
    "            target.set(\"camera.mounted-client-game-mode\", null);\n"
    "            changed = true;\n"
    "        }\n"
    "        if (sourceSchema < 11) {\n"
    "            String cameraMode = target.getString(\"camera.mode\");\n"
    "            if (cameraMode != null && cameraMode.equalsIgnoreCase(\"MOUNTED\")) {\n"
    "                target.set(\"camera.mode\", \"DIRECT_ENTITY\");\n"
    "                changed = true;\n"
    "            }\n"
    "            if (target.contains(\"camera.mount-retries\")) {\n"
    "                target.set(\"camera.attach-retries\", target.getInt(\"camera.mount-retries\", 8));\n"
    "                target.set(\"camera.mount-retries\", null);\n"
    "                changed = true;\n"
    "            }\n"
    "        }\n"
)

write(
    "src/main/java/dev/onelsey/incarnate/possession/DirectCameraBridge.java",
    '''package dev.onelsey.incarnate.possession;\n\nimport dev.onelsey.incarnate.IncarnatePlugin;\nimport org.bukkit.entity.Entity;\nimport org.bukkit.entity.Player;\n\nimport java.lang.reflect.Method;\nimport java.util.concurrent.atomic.AtomicBoolean;\nimport java.util.logging.Level;\n\npublic final class DirectCameraBridge {\n    private final IncarnatePlugin plugin;\n    private final AtomicBoolean warnedUnavailable = new AtomicBoolean(false);\n\n    public DirectCameraBridge(IncarnatePlugin plugin) {\n        this.plugin = plugin;\n    }\n\n    public boolean attach(Player player, Entity target) {\n        return setCamera(player, target);\n    }\n\n    public boolean reset(Player player) {\n        return setCamera(player, player);\n    }\n\n    private boolean setCamera(Player player, Entity target) {\n        if (player == null || target == null || !player.isOnline()) {\n            return false;\n        }\n        try {\n            Object serverPlayer = handle(player);\n            Object targetHandle = handle(target);\n            Method setter = findCameraSetter(serverPlayer.getClass(), targetHandle.getClass());\n            setter.invoke(serverPlayer, targetHandle);\n\n            Method getter = findCameraGetter(serverPlayer.getClass());\n            if (getter == null) {\n                return true;\n            }\n            Object actual = getter.invoke(serverPlayer);\n            return actual == targetHandle;\n        } catch (Throwable ex) {\n            warnUnavailable(ex);\n            return false;\n        }\n    }\n\n    private static Object handle(Entity entity) throws ReflectiveOperationException {\n        Method getHandle = entity.getClass().getMethod(\"getHandle\");\n        return getHandle.invoke(entity);\n    }\n\n    private static Method findCameraSetter(Class<?> ownerClass, Class<?> targetClass) throws NoSuchMethodException {\n        for (Class<?> type = ownerClass; type != null; type = type.getSuperclass()) {\n            for (Method method : type.getDeclaredMethods()) {\n                Class<?>[] parameters = method.getParameterTypes();\n                if (!method.getName().equals(\"setCamera\")\n                    || parameters.length != 1\n                    || !parameters[0].isAssignableFrom(targetClass)\n                    || !method.trySetAccessible()) {\n                    continue;\n                }\n                return method;\n            }\n        }\n        throw new NoSuchMethodException(ownerClass.getName() + \"#setCamera(Entity)\");\n    }\n\n    private static Method findCameraGetter(Class<?> ownerClass) {\n        for (Class<?> type = ownerClass; type != null; type = type.getSuperclass()) {\n            for (Method method : type.getDeclaredMethods()) {\n                if (method.getName().equals(\"getCamera\")\n                    && method.getParameterCount() == 0\n                    && method.trySetAccessible()) {\n                    return method;\n                }\n            }\n        }\n        return null;\n    }\n\n    private void warnUnavailable(Throwable throwable) {\n        if (!warnedUnavailable.compareAndSet(false, true)) {\n            return;\n        }\n        plugin.getLogger().log(\n            Level.WARNING,\n            \"Direct entity camera is unavailable on this server build. Incarnate will fail closed instead of degrading possession input.\",\n            throwable\n        );\n    }\n}\n'''
)

write(
    "src/main/java/dev/onelsey/incarnate/possession/CameraTransport.java",
    '''package dev.onelsey.incarnate.possession;\n\npublic enum CameraTransport {\n    NONE,\n    DIRECT_ENTITY,\n    MOUNTED,\n    SPECTATOR_TARGET\n}\n'''
)

session = "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java"
replace_once(
    session,
    "    public boolean usesMountedCamera() { return cameraTransport == CameraTransport.MOUNTED; }\n",
    "    public boolean usesDirectEntityCamera() { return cameraTransport == CameraTransport.DIRECT_ENTITY; }\n"
    "    public boolean usesMountedCamera() { return cameraTransport == CameraTransport.MOUNTED; }\n"
)

manager = "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java"
replace_once(manager, "    private final int cameraMountRetries;\n", "    private final int cameraAttachRetries;\n")
replace_once(
    manager,
    "    private final boolean cameraFallbackToSpectatorTarget;\n",
    "    private final boolean cameraFallbackToSpectatorTarget;\n"
    "    private final DirectCameraBridge directCameraBridge;\n"
)
replace_once(
    manager,
    '            configuredCamera = CameraTransport.valueOf(plugin.getConfig().getString("camera.mode", "MOUNTED").toUpperCase(java.util.Locale.ROOT));\n'
    '        } catch (IllegalArgumentException ex) {\n'
    '            plugin.getLogger().warning("Unknown camera.mode; using MOUNTED.");\n'
    '            configuredCamera = CameraTransport.MOUNTED;\n'
    '        }\n'
    '        this.cameraMode = configuredCamera == CameraTransport.NONE ? CameraTransport.MOUNTED : configuredCamera;\n'
    '        this.cameraMountRetries = Math.max(1, plugin.getConfig().getInt("camera.mount-retries", 8));\n'
    '        this.cameraFallbackToSpectatorTarget = plugin.getConfig().getBoolean("camera.fallback-to-spectator-target", true);\n',
    '            configuredCamera = CameraTransport.valueOf(plugin.getConfig().getString("camera.mode", "DIRECT_ENTITY").toUpperCase(java.util.Locale.ROOT));\n'
    '        } catch (IllegalArgumentException ex) {\n'
    '            plugin.getLogger().warning("Unknown camera.mode; using DIRECT_ENTITY.");\n'
    '            configuredCamera = CameraTransport.DIRECT_ENTITY;\n'
    '        }\n'
    '        this.cameraMode = configuredCamera == CameraTransport.NONE ? CameraTransport.DIRECT_ENTITY : configuredCamera;\n'
    '        this.cameraAttachRetries = Math.max(1, plugin.getConfig().getInt("camera.attach-retries", 8));\n'
    '        this.cameraFallbackToSpectatorTarget = plugin.getConfig().getBoolean("camera.fallback-to-spectator-target", false);\n'
    '        this.directCameraBridge = new DirectCameraBridge(plugin);\n'
)
replace_once(
    manager,
    "            if (cameraMode == CameraTransport.SPECTATOR_TARGET) {\n"
    "                attachSpectatorTargetCamera(session, false);\n"
    "            } else {\n"
    "                attachMountedCamera(session);\n"
    "            }\n",
    "            if (cameraMode == CameraTransport.SPECTATOR_TARGET) {\n"
    "                attachSpectatorTargetCamera(session, false);\n"
    "            } else if (cameraMode == CameraTransport.MOUNTED) {\n"
    "                attachMountedCamera(session);\n"
    "            } else {\n"
    "                attachDirectEntityCamera(session, 0);\n"
    "            }\n"
)

anchor = "    private void attachMountedCamera(PossessionSession session) {\n"
direct_methods = '''    private void attachDirectEntityCamera(PossessionSession session, int attempt) {\n        Player player = session.player();\n        Location destination = session.lastKnownVesselLocation();\n        if (destination == null) {\n            handleDirectCameraFailure(session, \"camera-reason.no-position\");\n            return;\n        }\n\n        ViewSnapshot view = session.view();\n        destination.setYaw(view.yaw());\n        destination.setPitch(view.pitch());\n        session.cameraTeleportInProgress(true);\n\n        player.teleportAsync(destination).whenComplete((success, error) -> {\n            ScheduledTask settleTask = player.getScheduler().run(plugin, task -> {\n                session.cameraTeleportInProgress(false);\n                if (!session.isActive() || !player.isOnline()) {\n                    return;\n                }\n                if (error != null || !Boolean.TRUE.equals(success)) {\n                    handleDirectCameraFailure(session, \"camera-reason.move-failed\");\n                    return;\n                }\n                finishDirectEntityCameraAttach(session, attempt);\n            }, () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)));\n            if (settleTask == null && session.isActive()) {\n                session.cameraTeleportInProgress(false);\n                requestRelease(session, ReleaseReason.VESSEL_REMOVED);\n            }\n        });\n    }\n\n    private void finishDirectEntityCameraAttach(PossessionSession session, int attempt) {\n        Player player = session.player();\n        Mob vessel = session.vessel();\n        if (!session.isActive() || !player.isOnline() || !vessel.isValid() || vessel.isDead()) {\n            requestRelease(session, ReleaseReason.VESSEL_REMOVED);\n            return;\n        }\n\n        if (!Bukkit.isOwnedByCurrentRegion(vessel)) {\n            retryDirectEntityCamera(session, attempt);\n            return;\n        }\n\n        session.cameraTeleportInProgress(true);\n        boolean attached;\n        try {\n            attached = directCameraBridge.attach(player, vessel);\n        } finally {\n            session.cameraTeleportInProgress(false);\n        }\n        if (!attached) {\n            handleDirectCameraFailure(session, \"camera-reason.direct-bridge-failed\");\n            return;\n        }\n\n        session.cameraTransport(CameraTransport.DIRECT_ENTITY);\n        sendAcquiredMessage(session, \"direct-entity\");\n    }\n\n    private void retryDirectEntityCamera(PossessionSession session, int attempt) {\n        if (attempt + 1 >= cameraAttachRetries) {\n            handleDirectCameraFailure(session, \"camera-reason.region-join-failed\");\n            return;\n        }\n        Player player = session.player();\n        ScheduledTask retry = player.getScheduler().runDelayed(\n            plugin,\n            task -> attachDirectEntityCamera(session, attempt + 1),\n            () -> deferFromRetired(() -> requestRelease(session, ReleaseReason.VESSEL_REMOVED)),\n            1L\n        );\n        if (retry == null && session.isActive()) {\n            requestRelease(session, ReleaseReason.VESSEL_REMOVED);\n        }\n    }\n\n    private void handleDirectCameraFailure(PossessionSession session, String reasonKey) {\n        if (!session.isActive()) {\n            return;\n        }\n        notifyPlayer(session.player(), \"camera-direct-failed\", Map.of(\n            \"reason\", messages.render(session.player(), reasonKey)\n        ));\n        requestRelease(session, ReleaseReason.INTERNAL_ERROR);\n    }\n\n'''
replace_once(manager, anchor, direct_methods + anchor)
replace_once(manager, "        if (attempt + 1 >= cameraMountRetries) {\n", "        if (attempt + 1 >= cameraAttachRetries) {\n")

replace_once(
    manager,
    "            if (player.isInsideVehicle()) {\n                player.leaveVehicle();\n            }\n"
    "            if (player.getGameMode() == GameMode.SPECTATOR) {\n",
    "            if (session.usesDirectEntityCamera()) {\n"
    "                directCameraBridge.reset(player);\n"
    "            }\n"
    "            if (player.isInsideVehicle()) {\n                player.leaveVehicle();\n            }\n"
    "            if (player.getGameMode() == GameMode.SPECTATOR) {\n"
)

replace_once(
    manager,
    "        if (player.isInsideVehicle()) {\n            player.leaveVehicle();\n        }\n"
    "        session.cameraTeleportInProgress(false);\n"
    "        session.cameraTransport(CameraTransport.NONE);\n"
    "        if (player.getGameMode() == GameMode.SPECTATOR) {\n",
    "        if (session.usesDirectEntityCamera()) {\n"
    "            directCameraBridge.reset(player);\n"
    "        }\n"
    "        if (player.isInsideVehicle()) {\n            player.leaveVehicle();\n        }\n"
    "        session.cameraTeleportInProgress(false);\n"
    "        session.cameraTransport(CameraTransport.NONE);\n"
    "        if (player.getGameMode() == GameMode.SPECTATOR) {\n"
)

replace_once(
    manager,
    "        if (player.isInsideVehicle()) {\n            player.leaveVehicle();\n        }\n"
    "        session.cameraTeleportInProgress(false);\n"
    "        session.cameraTransport(CameraTransport.NONE);\n\n"
    "        Mob vessel = session.vessel();\n",
    "        if (session.usesDirectEntityCamera()) {\n"
    "            directCameraBridge.reset(player);\n"
    "        }\n"
    "        if (player.isInsideVehicle()) {\n            player.leaveVehicle();\n        }\n"
    "        session.cameraTeleportInProgress(false);\n"
    "        session.cameraTransport(CameraTransport.NONE);\n\n"
    "        Mob vessel = session.vessel();\n"
)

replace_once(
    manager,
    "                if (player.isInsideVehicle()) {\n                    player.leaveVehicle();\n                }\n"
    "                session.cameraTeleportInProgress(false);\n",
    "                if (session.usesDirectEntityCamera()) {\n"
    "                    directCameraBridge.reset(player);\n"
    "                }\n"
    "                if (player.isInsideVehicle()) {\n                    player.leaveVehicle();\n                }\n"
    "                session.cameraTeleportInProgress(false);\n"
)

listener = "src/main/java/dev/onelsey/incarnate/listener/SessionListener.java"
replace_once(
    listener,
    "    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)\n"
    "    public void onView(PlayerMoveEvent event) {\n",
    "    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)\n"
    "    public void onDirectCameraControllerMove(PlayerMoveEvent event) {\n"
    "        PossessionSession session = possessions.session(event.getPlayer());\n"
    "        if (session == null || !session.isActive() || !session.usesDirectEntityCamera() || event.getTo() == null) {\n"
    "            return;\n"
    "        }\n"
    "        org.bukkit.Location from = event.getFrom();\n"
    "        org.bukkit.Location to = event.getTo();\n"
    "        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {\n"
    "            return;\n"
    "        }\n"
    "        org.bukkit.Location locked = from.clone();\n"
    "        locked.setYaw(to.getYaw());\n"
    "        locked.setPitch(to.getPitch());\n"
    "        event.setTo(locked);\n"
    "    }\n\n"
    "    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)\n"
    "    public void onView(PlayerMoveEvent event) {\n"
)

replace_once(
    "src/main/resources/locales/en_US.yml",
    "  camera:\n    mounted-free-look: 'mounted free-look'\n",
    "  camera:\n    direct-entity: 'native entity camera'\n    mounted-free-look: 'legacy mounted free-look'\n"
)
replace_once(
    "src/main/resources/locales/en_US.yml",
    "    region-join-failed: 'The controller and vessel could not be joined on a safe entity region.'\n",
    "    region-join-failed: 'The controller and vessel could not be joined on a safe entity region.'\n"
    "    direct-bridge-failed: 'The server could not bind the native camera to the vessel.'\n"
)
replace_once(
    "src/main/resources/locales/en_US.yml",
    "  camera-attach-failed: '<prefix> <error>Could not attach the free-look camera safely.</error>'\n",
    "  camera-attach-failed: '<prefix> <error>Could not attach the free-look camera safely.</error>'\n"
    "  camera-direct-failed: '<prefix> <error>Could not attach the native vessel camera safely.</error> <muted><reason></muted>'\n"
)
replace_once(
    "src/main/resources/locales/ru_RU.yml",
    "  camera:\n    mounted-free-look: 'свободная камера'\n",
    "  camera:\n    direct-entity: 'камера от лица сущности'\n    mounted-free-look: 'старая камера с посадкой'\n"
)
replace_once(
    "src/main/resources/locales/ru_RU.yml",
    "    region-join-failed: 'Не удалось безопасно объединить игрока и тело в одном регионе сущностей.'\n",
    "    region-join-failed: 'Не удалось безопасно объединить игрока и тело в одном регионе сущностей.'\n"
    "    direct-bridge-failed: 'Сервер не смог привязать нативную камеру к телу.'\n"
)
replace_once(
    "src/main/resources/locales/ru_RU.yml",
    "  camera-attach-failed: '<prefix> <error>Не удалось безопасно подключить свободную камеру.</error>'\n",
    "  camera-attach-failed: '<prefix> <error>Не удалось безопасно подключить свободную камеру.</error>'\n"
    "  camera-direct-failed: '<prefix> <error>Не удалось безопасно подключить камеру тела.</error> <muted><reason></muted>'\n"
)

config_test = "src/test/java/dev/onelsey/incarnate/config/ConfigMigratorTest.java"
text = read(config_test)
text = text.replace('defaults.set("config-version", 10);', 'defaults.set("config-version", 11);')
text = text.replace('defaults.set("camera.mode", "MOUNTED");', 'defaults.set("camera.mode", "DIRECT_ENTITY");')
text = text.replace('defaults.set("camera.mount-retries", 8);', 'defaults.set("camera.attach-retries", 8);')
text = text.replace('assertEquals("MOUNTED", user.getString("camera.mode"));', 'assertEquals("DIRECT_ENTITY", user.getString("camera.mode"));')
text = text.replace('assertEquals(8, user.getInt("camera.mount-retries"));', 'assertEquals(8, user.getInt("camera.attach-retries"));')
text = text.replace('assertEquals(10, user.getInt("config-version"));', 'assertEquals(11, user.getInt("config-version"));')
text = text.replace('user.set("config-version", 10);', 'user.set("config-version", 11);')
text = text.replace('defaults.set("config-version", 10);', 'defaults.set("config-version", 11);')
marker = "    @Test\n    void schemaMigrationPreservesCustomizedExclusions() {\n"
new_test = '''    @Test\n    void schemaTenMovesMountedDefaultToDirectEntityCamera() {\n        YamlConfiguration user = new YamlConfiguration();\n        user.set(\"config-version\", 10);\n        user.set(\"camera.mode\", \"MOUNTED\");\n        user.set(\"camera.mount-retries\", 13);\n\n        assertTrue(ConfigMigrator.migrateSchema(user, 10));\n        assertEquals(11, user.getInt(\"config-version\"));\n        assertEquals(\"DIRECT_ENTITY\", user.getString(\"camera.mode\"));\n        assertEquals(13, user.getInt(\"camera.attach-retries\"));\n        assertFalse(user.contains(\"camera.mount-retries\"));\n    }\n\n'''
if marker not in text:
    raise RuntimeError("ConfigMigratorTest insertion marker missing")
text = text.replace(marker, new_test + marker, 1)
write(config_test, text)

message_test = "src/test/java/dev/onelsey/incarnate/message/MessageResourcesTest.java"
replace_once(message_test, "        assertEquals(10, config.getInt(\"config-version\"));\n", "        assertEquals(11, config.getInt(\"config-version\"));\n")
replace_once(
    message_test,
    "        assertTrue(!config.contains(\"camera.mounted-client-game-mode\"));\n",
    "        assertTrue(!config.contains(\"camera.mounted-client-game-mode\"));\n"
    "        assertEquals(\"DIRECT_ENTITY\", config.getString(\"camera.mode\"));\n"
    "        assertEquals(8, config.getInt(\"camera.attach-retries\"));\n"
)

changelog = read("CHANGELOG.md")
entry = '''## 0.9.0-dev.7 - Native Entity Camera\n\n- Replaces the default passenger-based camera with a direct vanilla entity camera while keeping the controller in real Adventure mode, preserving the working `F`, `Shift + F`, sprint and ordinary input path from dev.6.\n- Uses the server's native `ServerPlayer#setCamera` path through a reflection bridge instead of a client-only fake camera packet, so the server remains aware of the active camera target for normal entity/chunk tracking.\n- Removes the visible/physical passenger requirement from the default possession flow. The hidden controller is positioned at acquisition, then positional movement is frozen while yaw/pitch input remains available to steer the vessel.\n- Vanilla F5 perspective switching is intentionally left client-native. Because the camera entity is the real Mob, third person should render the controlled body rather than the hidden Player; giant/complex bodies such as Ender Dragon remain a live-test target because vanilla owns third-person distance and clipping.\n- Keeps `MOUNTED` and `SPECTATOR_TARGET` only as explicit legacy camera modes. Fresh configs use `DIRECT_ENTITY`, and schema 10's former default `MOUNTED` is migrated to `DIRECT_ENTITY`.\n- Renames `camera.mount-retries` to `camera.attach-retries` and advances the gameplay config schema to 11.\n\n'''
if "# Changelog\n\n" not in changelog:
    raise RuntimeError("CHANGELOG header missing")
write("CHANGELOG.md", changelog.replace("# Changelog\n\n", "# Changelog\n\n" + entry, 1))

readme = read("README.md")
old_camera = '''### Free-look camera\n\nThe default camera transport remains the mounted free-look architecture introduced in 0.3.0.\n\nInstead of directly attaching the client camera to the Mob through `setSpectatorTarget`, the hidden controller is moved to the vessel and mounted on the real Mob while remaining its own camera. Mounted possession now uses a real Adventure controller rather than a Spectator controller so vanilla can deliver ordinary swap-offhand (`F`) and sprint input. Incarnate isolates that controller from world gameplay with invulnerability, collision/spawn suppression and event-level interaction guards.\n\n```yaml\ncamera:\n  mode: MOUNTED\n  mount-retries: 8\n  fallback-to-spectator-target: false\n```\n\n`SPECTATOR_TARGET` remains available as a legacy compatibility fallback, but the fresh default no longer falls back to it automatically because true Spectator mode suppresses some vanilla input used by Incarnate. Mounted acquisition uses Player and Mob EntitySchedulers and retries region-safe attachment before falling back or releasing safely.\n\nDismount input is guarded during possession, and release/quit/death/plugin-disable paths explicitly detach the hidden Player before state restoration or recovery teleportation.\n\nThe mounted camera still requires live client testing across representative mob sizes and server implementations before being considered gameplay-final.\n'''
new_camera = '''### Native entity camera\n\nThe default camera transport is now `DIRECT_ENTITY`. The controller remains a real Adventure-mode Player for normal vanilla input, but the server camera is bound directly to the real controlled Mob instead of mounting the Player as a passenger.\n\nIncarnate uses the vanilla server `ServerPlayer#setCamera` path through a reflection bridge. This keeps the server aware of the camera target rather than faking only the client view. The hidden controller is moved to the vessel during acquisition, world interaction remains isolated, and positional Player movement is frozen while look/input updates continue driving the Mob.\n\n```yaml\ncamera:\n  mode: DIRECT_ENTITY\n  attach-retries: 8\n  fallback-to-spectator-target: false\n```\n\nBecause the actual camera entity is the Mob, first person uses the Mob's native camera/eye position and vanilla F5 remains available. In third person the client should render the real Mob body rather than a seated hidden Player. Vanilla still owns third-person distance and collision clipping, so very large/complex bodies such as Ender Dragon require live tuning before their F5 presentation can be called final.\n\n`MOUNTED` and `SPECTATOR_TARGET` remain explicit legacy modes for diagnosis/compatibility. They are not the fresh default. True Spectator remains unsuitable for the primary path because it suppresses vanilla inputs that Incarnate uses.\n'''
if old_camera not in readme:
    raise RuntimeError("README camera section did not match expected dev6 text")
write("README.md", readme.replace(old_camera, new_camera, 1))
