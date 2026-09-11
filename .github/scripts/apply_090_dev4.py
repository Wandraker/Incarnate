from pathlib import Path


def replace(path, old, new):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing marker in {path}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

replace("build.gradle.kts", 'version = "0.9.0-dev.3"', 'version = "0.9.0-dev.4"')
replace("src/main/resources/plugin.yml", "version: 0.9.0-dev.3", "version: 0.9.0-dev.4")

bridge = Path("src/main/java/dev/onelsey/incarnate/input/SpectatorPrimaryInputBridge.java")
text = bridge.read_text(encoding="utf-8")
old = '''    static boolean isSwapOffhandAction(Object packet) {
        if (packet == null || !PLAYER_ACTION_PACKET_SIMPLE_NAME.equals(packet.getClass().getSimpleName())) {
            return false;
        }
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isEnum() || !field.trySetAccessible()) {
                    continue;
                }
                try {
                    Object value = field.get(packet);
                    if (value instanceof Enum<?> action && SWAP_OFFHAND_ACTION.equals(action.name())) {
                        return true;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return false;
    }
'''
new = '''    static boolean isSwapOffhandAction(Object packet) {
        if (packet == null || !PLAYER_ACTION_PACKET_SIMPLE_NAME.equals(packet.getClass().getSimpleName())) {
            return false;
        }
        return SWAP_OFFHAND_ACTION.equals(playerActionName(packet));
    }

    static String playerActionName(Object packet) {
        if (packet == null) {
            return null;
        }
        for (String accessorName : new String[]{"getAction", "action"}) {
            try {
                Method accessor = packet.getClass().getMethod(accessorName);
                if (!accessor.trySetAccessible()) {
                    continue;
                }
                Object value = accessor.invoke(packet);
                if (value instanceof Enum<?> action) {
                    return action.name();
                }
            } catch (ReflectiveOperationException | SecurityException ignored) {
            }
        }
        for (Class<?> type = packet.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().isEnum() || !field.trySetAccessible()) {
                    continue;
                }
                try {
                    Object value = field.get(packet);
                    if (value instanceof Enum<?> action) {
                        return action.name();
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return null;
    }
'''
if old not in text:
    raise SystemExit("bridge swap decoder marker missing")
bridge.write_text(text.replace(old, new, 1), encoding="utf-8")

vision = Path("src/main/java/dev/onelsey/incarnate/vision/WardenVisionManager.java")
text = vision.read_text(encoding="utf-8")
text = text.replace("    private final int visualRefreshTicks;\n    private final int visualEffectDurationTicks;\n", "")
text = text.replace('        this.visualRefreshTicks = Math.max(20, plugin.getConfig().getInt("vision.warden.visual-refresh-ticks", 100));\n        this.visualEffectDurationTicks = Math.max(40, visualRefreshTicks + 40);\n', "")
text = text.replace('''            state.guardTicks += 5;
            if (visualDarkness && state.guardTicks >= visualRefreshTicks) {
                state.guardTicks = 0;
                applyVisualDarkness(session.player());
            }
''', "")
text = text.replace("            visualEffectDurationTicks,\n", "            PotionEffect.INFINITE_DURATION,\n")
text = text.replace("        private int guardTicks;\n", "")
marker = '''    public void deactivate(Player player, boolean restoreClient) {
'''
insert = '''    public boolean suppressesNativeDarkness(PossessionSession session) {
        return visualDarkness && applies(session);
    }

    public void deactivate(Player player, boolean restoreClient) {
'''
if marker not in text:
    raise SystemExit("vision deactivate marker missing")
text = text.replace(marker, insert, 1)
vision.write_text(text, encoding="utf-8")

listener = Path("src/main/java/dev/onelsey/incarnate/listener/SessionListener.java")
text = listener.read_text(encoding="utf-8")
text = text.replace("import org.bukkit.event.entity.EntityRemoveEvent;\n", "import org.bukkit.event.entity.EntityRemoveEvent;\nimport org.bukkit.event.entity.EntityPotionEffectEvent;\n")
text = text.replace("import org.bukkit.projectiles.ProjectileSource;\n", "import org.bukkit.projectiles.ProjectileSource;\nimport org.bukkit.potion.PotionEffectType;\n")
marker = '''    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent event) {
'''
insert = '''    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
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
'''
if marker not in text:
    raise SystemExit("listener game event marker missing")
listener.write_text(text.replace(marker, insert, 1), encoding="utf-8")

abilities = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = abilities.read_text(encoding="utf-8")
text = text.replace("    private final double meleeRaySize;\n", "    private final double meleeRaySize;\n    private final double meleeAimAssistRadius;\n", 1)
text = text.replace('        this.meleeRaySize = Math.max(0.0, config.getDouble("abilities.melee.ray-size", 0.30));\n', '        this.meleeRaySize = Math.max(0.0, config.getDouble("abilities.melee.ray-size", 0.30));\n        this.meleeAimAssistRadius = Math.max(0.0, config.getDouble("abilities.melee.aim-assist-radius", 0.35));\n', 1)
text = text.replace("        LivingEntity target = findLivingTarget(session, vessel, meleeRange, meleeRaySize);\n", "        LivingEntity target = findLivingTarget(session, vessel, meleeRange, meleeRaySize + meleeAimAssistRadius);\n", 1)
abilities.write_text(text, encoding="utf-8")

config = Path("src/main/resources/config.yml")
text = config.read_text(encoding="utf-8")
text = text.replace("config-version: 7", "config-version: 8", 1)
text = text.replace("    ray-size: 0.30\n    cooldown-ticks: 10\n", "    ray-size: 0.30\n    aim-assist-radius: 0.35\n    cooldown-ticks: 10\n", 1)
config.write_text(text, encoding="utf-8")

replace("src/main/java/dev/onelsey/incarnate/config/ConfigMigrator.java", "private static final int CURRENT_SCHEMA = 7;", "private static final int CURRENT_SCHEMA = 8;")

cfgtest = Path("src/test/java/dev/onelsey/incarnate/config/ConfigMigratorTest.java")
text = cfgtest.read_text(encoding="utf-8")
text = text.replace('defaults.set("config-version", 7);', 'defaults.set("config-version", 8);')
text = text.replace('user.set("config-version", 7);', 'user.set("config-version", 8);')
text = text.replace('assertEquals(7, user.getInt("config-version"));', 'assertEquals(8, user.getInt("config-version"));')
text = text.replace('        defaults.set("vision.warden.entity-hard-limit", 12.0);\n', '        defaults.set("vision.warden.entity-hard-limit", 12.0);\n        defaults.set("abilities.melee.aim-assist-radius", 0.35);\n', 1)
text = text.replace('        assertEquals(12.0, user.getDouble("vision.warden.entity-hard-limit"));\n', '        assertEquals(12.0, user.getDouble("vision.warden.entity-hard-limit"));\n        assertEquals(0.35, user.getDouble("abilities.melee.aim-assist-radius"));\n', 1)
cfgtest.write_text(text, encoding="utf-8")

msgtest = Path("src/test/java/dev/onelsey/incarnate/message/MessageResourcesTest.java")
text = msgtest.read_text(encoding="utf-8").replace('assertEquals(7, config.getInt("config-version"));', 'assertEquals(8, config.getInt("config-version"));')
msgtest.write_text(text, encoding="utf-8")

bridgetest = Path("src/test/java/dev/onelsey/incarnate/input/SpectatorPrimaryInputBridgeTest.java")
text = bridgetest.read_text(encoding="utf-8")
text = text.replace('import static org.junit.jupiter.api.Assertions.assertFalse;\n', 'import static org.junit.jupiter.api.Assertions.assertEquals;\nimport static org.junit.jupiter.api.Assertions.assertFalse;\n')
marker = '''    @Test
    void ignoresOtherPlayerActions() {
'''
insert = '''    @Test
    void readsPublicPlayerActionAccessorWithoutPrivateFieldAccess() {
        assertEquals("SWAP_ITEM_WITH_OFFHAND", SpectatorPrimaryInputBridge.playerActionName(
            new AccessorOnlyPacket(PlayerAction.SWAP_ITEM_WITH_OFFHAND)
        ));
    }

    @Test
    void ignoresOtherPlayerActions() {
'''
if marker not in text:
    raise SystemExit("bridge test marker missing")
text = text.replace(marker, insert, 1)
class_marker = '''    private static final class ServerboundPlayerActionPacket {
'''
accessor_class = '''    public static final class AccessorOnlyPacket {
        private final PlayerAction action;

        private AccessorOnlyPacket(PlayerAction action) {
            this.action = action;
        }

        public PlayerAction getAction() {
            return action;
        }
    }

    private static final class ServerboundPlayerActionPacket {
'''
if class_marker not in text:
    raise SystemExit("bridge test class marker missing")
text = text.replace(class_marker, accessor_class, 1)
bridgetest.write_text(text, encoding="utf-8")

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
section = '''# Changelog

## 0.9.0-dev.4 - Runtime Input & Vision Polish

- Fixes spectator `F` decoding against Minecraft 26.2 by using the packet's public action accessor before the compatibility field fallback.
- Keeps the existing packet/Bukkit secondary-input deduplication and the `F` / `Shift + F` control mapping unchanged.
- Makes possessed-Warden visual darkness client-stable instead of periodically refreshing a short fake Darkness effect.
- Suppresses vanilla Warden-caused Darkness updates on the hidden Warden controller while Incarnate's Warden vision is active, preventing the body's own Darkness pulse from fighting the possession vision layer.
- Adds configurable melee aim assistance through `abilities.melee.aim-assist-radius` so real-mob melee is less pixel-perfect without changing the underlying real `Mob#attack` execution.
- Advances gameplay config schema to 8 additively; existing server values remain preserved.

'''
if not text.startswith("# Changelog\n\n"):
    raise SystemExit("changelog header missing")
changelog.write_text(section + text[len("# Changelog\n\n"):], encoding="utf-8")
