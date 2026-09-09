from pathlib import Path

# AbilityRegistry
p = Path('src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java')
text = p.read_text()
repls = [
    ('import org.bukkit.entity.PufferFish;\n', 'import org.bukkit.entity.PufferFish;\nimport org.bukkit.entity.Ravager;\n', 'ravager import'),
    ('    private final int vexChargeLockTicks;\n', '    private final int vexChargeLockTicks;\n    private final boolean ravagerRoarEnabled;\n    private final int ravagerRoarCooldownTicks;\n    private final int ravagerRoarLockTicks;\n', 'ability fields'),
    ('        this.vexChargeLockTicks = Math.max(1, config.getInt("abilities.vex-charge.movement-lock-ticks", 8));\n', '        this.vexChargeLockTicks = Math.max(1, config.getInt("abilities.vex-charge.movement-lock-ticks", 8));\n        this.ravagerRoarEnabled = config.getBoolean("abilities.ravager-roar.enabled", true);\n        this.ravagerRoarCooldownTicks = Math.max(1, config.getInt("abilities.ravager-roar.cooldown-ticks", 80));\n        this.ravagerRoarLockTicks = Math.max(1, config.getInt("abilities.ravager-roar.movement-lock-ticks", 12));\n', 'ability config'),
    ('        if (vessel instanceof Camel camel && camelDashEnabled) {\n            return dashCamel(session, camel);\n        }\n        return false;\n', '        if (vessel instanceof Camel camel && camelDashEnabled) {\n            return dashCamel(session, camel);\n        }\n        if (vessel instanceof Ravager ravager && ravagerRoarEnabled) {\n            return roarRavager(session, ravager);\n        }\n        return false;\n', 'secondary dispatch'),
    ('        if (vessel instanceof Camel && camelDashEnabled) return "dash";\n        return "none";\n', '        if (vessel instanceof Camel && camelDashEnabled) return "dash";\n        if (vessel instanceof Ravager && ravagerRoarEnabled) return "roar";\n        return "none";\n', 'secondary label'),
]
for old, new, label in repls:
    if text.count(old) != 1:
        raise SystemExit(f'AbilityRegistry {label}: expected 1, got {text.count(old)}')
    text = text.replace(old, new, 1)
anchor = '''    private static boolean supportsNativeRanged(Mob vessel) {\n'''
method = '''    private boolean roarRavager(PossessionSession session, Ravager ravager) {\n        if (!session.acquireSecondaryCooldown(ravagerRoarCooldownTicks)) {\n            return false;\n        }\n        ravager.setStunnedTicks(-1);\n        ravager.setAttackTicks(-1);\n        ravager.setRoarTicks(11);\n        session.lockMovementControl(ravagerRoarLockTicks);\n        return true;\n    }\n\n'''
if text.count(anchor) != 1:
    raise SystemExit('AbilityRegistry method anchor mismatch')
text = text.replace(anchor, method + anchor, 1)
p.write_text(text)

# VesselState
p = Path('src/main/java/dev/onelsey/incarnate/possession/VesselState.java')
text = p.read_text()
repls = [
    ('import org.bukkit.entity.PufferFish;\n', 'import org.bukkit.entity.PufferFish;\nimport org.bukkit.entity.Ravager;\n', 'ravager import'),
    ('    Integer pufferFishPuffState,\n    Boolean vexCharging\n) {\n', '    Integer pufferFishPuffState,\n    Boolean vexCharging,\n    Integer ravagerAttackTicks,\n    Integer ravagerStunnedTicks,\n    Integer ravagerRoarTicks\n) {\n', 'record fields'),
    ('        Boolean vexCharging = mob instanceof Vex vex ? vex.isCharging() : null;\n', '        Boolean vexCharging = mob instanceof Vex vex ? vex.isCharging() : null;\n        Integer ravagerAttackTicks = mob instanceof Ravager ravager ? ravager.getAttackTicks() : null;\n        Integer ravagerStunnedTicks = mob instanceof Ravager ravager ? ravager.getStunnedTicks() : null;\n        Integer ravagerRoarTicks = mob instanceof Ravager ravager ? ravager.getRoarTicks() : null;\n', 'capture values'),
    ('            guardianLaserTicks,\n            pufferFishPuffState,\n            vexCharging\n        );\n', '            guardianLaserTicks,\n            pufferFishPuffState,\n            vexCharging,\n            ravagerAttackTicks,\n            ravagerStunnedTicks,\n            ravagerRoarTicks\n        );\n', 'record constructor'),
    ('        if (mob instanceof Vex vex) {\n            vex.setCharging(false);\n        }\n', '        if (mob instanceof Vex vex) {\n            vex.setCharging(false);\n        }\n        if (mob instanceof Ravager ravager) {\n            ravager.setAttackTicks(-1);\n            ravager.setStunnedTicks(-1);\n            ravager.setRoarTicks(-1);\n        }\n', 'possession reset'),
    ('        if (mob instanceof Vex vex && vexCharging != null) {\n            vex.setCharging(vexCharging);\n        }\n        if (mob instanceof Guardian guardian) {\n', '        if (mob instanceof Vex vex && vexCharging != null) {\n            vex.setCharging(vexCharging);\n        }\n        if (mob instanceof Ravager ravager && ravagerAttackTicks != null && ravagerStunnedTicks != null && ravagerRoarTicks != null) {\n            ravager.setAttackTicks(ravagerAttackTicks);\n            ravager.setStunnedTicks(ravagerStunnedTicks);\n            ravager.setRoarTicks(ravagerRoarTicks);\n        }\n        if (mob instanceof Guardian guardian) {\n', 'restore values'),
]
for old, new, label in repls:
    if text.count(old) != 1:
        raise SystemExit(f'VesselState {label}: expected 1, got {text.count(old)}')
    text = text.replace(old, new, 1)
p.write_text(text)

# VesselRecoveryStore
p = Path('src/main/java/dev/onelsey/incarnate/possession/VesselRecoveryStore.java')
text = p.read_text()
repls = [
    ('import org.bukkit.entity.PufferFish;\n', 'import org.bukkit.entity.PufferFish;\nimport org.bukkit.entity.Ravager;\n', 'ravager import'),
    ('    private final NamespacedKey vexChargingKey;\n', '    private final NamespacedKey vexChargingKey;\n    private final NamespacedKey ravagerAttackTicksKey;\n    private final NamespacedKey ravagerStunnedTicksKey;\n    private final NamespacedKey ravagerRoarTicksKey;\n', 'keys fields'),
    ('        this.vexChargingKey = new NamespacedKey(plugin, "vessel_recovery_vex_charging");\n', '        this.vexChargingKey = new NamespacedKey(plugin, "vessel_recovery_vex_charging");\n        this.ravagerAttackTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_attack_ticks");\n        this.ravagerStunnedTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_stunned_ticks");\n        this.ravagerRoarTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_roar_ticks");\n', 'keys init'),
    ('            setNullableBool(data, vexChargingKey, state.vexCharging());\n            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);\n', '            setNullableBool(data, vexChargingKey, state.vexCharging());\n            setNullableInt(data, ravagerAttackTicksKey, state.ravagerAttackTicks());\n            setNullableInt(data, ravagerStunnedTicksKey, state.ravagerStunnedTicks());\n            setNullableInt(data, ravagerRoarTicksKey, state.ravagerRoarTicks());\n            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);\n', 'save values'),
    ('        if (mob instanceof Vex vex) {\n            Boolean charging = readNullableBool(data, vexChargingKey);\n            if (charging != null) {\n                vex.setCharging(charging);\n            }\n        }\n        if (mob instanceof Guardian guardian) {\n', '        if (mob instanceof Vex vex) {\n            Boolean charging = readNullableBool(data, vexChargingKey);\n            if (charging != null) {\n                vex.setCharging(charging);\n            }\n        }\n        if (mob instanceof Ravager ravager) {\n            Integer attackTicks = data.get(ravagerAttackTicksKey, PersistentDataType.INTEGER);\n            Integer stunnedTicks = data.get(ravagerStunnedTicksKey, PersistentDataType.INTEGER);\n            Integer roarTicks = data.get(ravagerRoarTicksKey, PersistentDataType.INTEGER);\n            if (attackTicks != null) ravager.setAttackTicks(attackTicks);\n            if (stunnedTicks != null) ravager.setStunnedTicks(stunnedTicks);\n            if (roarTicks != null) ravager.setRoarTicks(roarTicks);\n        }\n        if (mob instanceof Guardian guardian) {\n', 'recover values'),
    ('        data.remove(vexChargingKey);\n        index.forget(vesselId);\n', '        data.remove(vexChargingKey);\n        data.remove(ravagerAttackTicksKey);\n        data.remove(ravagerStunnedTicksKey);\n        data.remove(ravagerRoarTicksKey);\n        index.forget(vesselId);\n', 'clear values'),
]
for old, new, label in repls:
    if text.count(old) != 1:
        raise SystemExit(f'VesselRecoveryStore {label}: expected 1, got {text.count(old)}')
    text = text.replace(old, new, 1)
anchor = '''    private static boolean readBool(PersistentDataContainer data, NamespacedKey key, boolean fallback) {\n'''
helper = '''    private static void setNullableInt(PersistentDataContainer data, NamespacedKey key, Integer value) {\n        if (value == null) {\n            data.remove(key);\n        } else {\n            data.set(key, PersistentDataType.INTEGER, value);\n        }\n    }\n\n'''
if text.count(anchor) != 1:
    raise SystemExit('VesselRecoveryStore helper anchor mismatch')
text = text.replace(anchor, helper + anchor, 1)
p.write_text(text)

print('Ravager 0.3.0 patch applied')
