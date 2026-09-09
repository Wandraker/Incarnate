from pathlib import Path


def replace_once(path, old, new, label):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    p.write_text(text.replace(old, new, 1))


# Version metadata.
replace_once(
    "build.gradle.kts",
    'version = "0.1.0"',
    'version = "0.2.0"',
    "Gradle version",
)
replace_once(
    "src/main/resources/plugin.yml",
    "version: 0.1.0",
    "version: 0.2.0",
    "plugin.yml version",
)

# Runtime log derives version from plugin metadata from now on.
replace_once(
    "src/main/java/dev/onelsey/incarnate/IncarnatePlugin.java",
    'getLogger().info("Incarnate 0.1.0 enabled for Minecraft 26.2+ (Paper/Purpur/Leaf/Folia).");',
    'getLogger().info("Incarnate " + getPluginMeta().getVersion() + " enabled for Minecraft 26.2+ (Paper/Purpur/Leaf/Folia).");',
    "dynamic startup version",
)

# PossessionSession: active native ability state.
replace_once(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "    private volatile long movementControlLockedUntilTick = Long.MIN_VALUE;\n    private volatile long lastSpectatorShiftAttemptNanos = Long.MIN_VALUE;",
    "    private volatile long movementControlLockedUntilTick = Long.MIN_VALUE;\n    private volatile boolean guardianLaserActive;\n    private volatile long vexChargeUntilTick = Long.MIN_VALUE;\n    private volatile long lastSpectatorShiftAttemptNanos = Long.MIN_VALUE;",
    "session native ability fields",
)
replace_once(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "    public boolean isMovementControlLocked() {\n        return movementControlLockedUntilTick != Long.MIN_VALUE && controlTick <= movementControlLockedUntilTick;\n    }\n\n    public void markSpectatorShiftAttempt() {",
    "    public boolean isMovementControlLocked() {\n        return movementControlLockedUntilTick != Long.MIN_VALUE && controlTick <= movementControlLockedUntilTick;\n    }\n\n    public boolean guardianLaserActive() { return guardianLaserActive; }\n    public void guardianLaserActive(boolean active) { this.guardianLaserActive = active; }\n\n    public void startVexCharge(int ticks) {\n        vexChargeUntilTick = controlTick + Math.max(1, ticks);\n    }\n\n    public boolean vexChargeActive() {\n        return vexChargeUntilTick != Long.MIN_VALUE && controlTick <= vexChargeUntilTick;\n    }\n\n    public void clearVexCharge() {\n        vexChargeUntilTick = Long.MIN_VALUE;\n    }\n\n    public void markSpectatorShiftAttempt() {",
    "session native ability methods",
)

# VesselState: preserve Guardian/Vex/PufferFish state around possession.
p = Path("src/main/java/dev/onelsey/incarnate/possession/VesselState.java")
text = p.read_text()
text = text.replace("import org.bukkit.entity.Creeper;\n", "import org.bukkit.entity.Creeper;\nimport org.bukkit.entity.Guardian;\n", 1)
text = text.replace("import org.bukkit.entity.Mob;\n", "import org.bukkit.entity.Mob;\nimport org.bukkit.entity.PufferFish;\n", 1)
text = text.replace("import org.bukkit.entity.Sittable;\n", "import org.bukkit.entity.Sittable;\nimport org.bukkit.entity.Vex;\n", 1)
old = "    Boolean batAwake,\n    Boolean creeperIgnited,\n    Integer creeperFuseTicks\n) {"
new = "    Boolean batAwake,\n    Boolean creeperIgnited,\n    Integer creeperFuseTicks,\n    Boolean guardianLaserActive,\n    Integer guardianLaserTicks,\n    Integer pufferFishPuffState,\n    Boolean vexCharging\n) {"
if text.count(old) != 1:
    raise SystemExit("VesselState record fields mismatch")
text = text.replace(old, new, 1)
old = "        Boolean creeperIgnited = mob instanceof Creeper creeper ? creeper.isIgnited() : null;\n        Integer creeperFuseTicks = mob instanceof Creeper creeper ? creeper.getFuseTicks() : null;"
new = "        Boolean creeperIgnited = mob instanceof Creeper creeper ? creeper.isIgnited() : null;\n        Integer creeperFuseTicks = mob instanceof Creeper creeper ? creeper.getFuseTicks() : null;\n        Boolean guardianLaserActive = mob instanceof Guardian guardian ? guardian.hasLaser() : null;\n        Integer guardianLaserTicks = mob instanceof Guardian guardian ? guardian.getLaserTicks() : null;\n        Integer pufferFishPuffState = mob instanceof PufferFish pufferFish ? pufferFish.getPuffState() : null;\n        Boolean vexCharging = mob instanceof Vex vex ? vex.isCharging() : null;"
if text.count(old) != 1:
    raise SystemExit("VesselState capture locals mismatch")
text = text.replace(old, new, 1)
old = "            batAwake,\n            creeperIgnited,\n            creeperFuseTicks\n        );"
new = "            batAwake,\n            creeperIgnited,\n            creeperFuseTicks,\n            guardianLaserActive,\n            guardianLaserTicks,\n            pufferFishPuffState,\n            vexCharging\n        );"
if text.count(old) != 1:
    raise SystemExit("VesselState constructor values mismatch")
text = text.replace(old, new, 1)
old = "        if (mob instanceof Bat bat) {\n            bat.setAwake(true);\n            bat.setTargetLocation(null);\n        }"
new = "        if (mob instanceof Bat bat) {\n            bat.setAwake(true);\n            bat.setTargetLocation(null);\n        }\n        if (mob instanceof Guardian guardian) {\n            guardian.setLaser(false);\n        }\n        if (mob instanceof Vex vex) {\n            vex.setCharging(false);\n        }"
if text.count(old) != 1:
    raise SystemExit("VesselState apply special state mismatch")
text = text.replace(old, new, 1)
old = "        if (target != null && Bukkit.isOwnedByCurrentRegion(target) && target.isValid() && !target.isDead()) {\n            mob.setTarget(target);\n        }"
new = "        boolean targetRestored = false;\n        if (target != null && Bukkit.isOwnedByCurrentRegion(target) && target.isValid() && !target.isDead()) {\n            mob.setTarget(target);\n            targetRestored = true;\n        }"
if text.count(old) != 1:
    raise SystemExit("VesselState target restore mismatch")
text = text.replace(old, new, 1)
old = "        if (mob instanceof Creeper creeper && creeperIgnited != null && creeperFuseTicks != null) {\n            restoreCreeper(creeper, creeperIgnited, creeperFuseTicks);\n        }\n    }"
new = "        if (mob instanceof Creeper creeper && creeperIgnited != null && creeperFuseTicks != null) {\n            restoreCreeper(creeper, creeperIgnited, creeperFuseTicks);\n        }\n        if (mob instanceof PufferFish pufferFish && pufferFishPuffState != null) {\n            pufferFish.setPuffState(Math.max(0, Math.min(2, pufferFishPuffState)));\n        }\n        if (mob instanceof Vex vex && vexCharging != null) {\n            vex.setCharging(vexCharging);\n        }\n        if (mob instanceof Guardian guardian) {\n            guardian.setLaser(false);\n            if (Boolean.TRUE.equals(guardianLaserActive) && guardianLaserTicks != null && targetRestored && guardian.setLaser(true)) {\n                int safeTicks = Math.max(-10, Math.min(Math.max(-10, guardian.getLaserDuration() - 1), guardianLaserTicks));\n                guardian.setLaserTicks(safeTicks);\n            }\n        }\n    }"
if text.count(old) != 1:
    raise SystemExit("VesselState restore special state mismatch")
text = text.replace(old, new, 1)
p.write_text(text)

# Crash recovery: Vex and PufferFish restore exactly; Guardian laser is safely cancelled because its old target cannot be persisted safely.
p = Path("src/main/java/dev/onelsey/incarnate/possession/VesselRecoveryStore.java")
text = p.read_text()
text = text.replace("import org.bukkit.entity.Creeper;\n", "import org.bukkit.entity.Creeper;\nimport org.bukkit.entity.Guardian;\n", 1)
text = text.replace("import org.bukkit.entity.Mob;\n", "import org.bukkit.entity.Mob;\nimport org.bukkit.entity.PufferFish;\n", 1)
text = text.replace("import org.bukkit.entity.Sittable;\n", "import org.bukkit.entity.Sittable;\nimport org.bukkit.entity.Vex;\n", 1)
old = "    private final NamespacedKey creeperIgnitedKey;\n    private final NamespacedKey creeperFuseTicksKey;"
new = "    private final NamespacedKey creeperIgnitedKey;\n    private final NamespacedKey creeperFuseTicksKey;\n    private final NamespacedKey pufferFishPuffStateKey;\n    private final NamespacedKey vexChargingKey;"
if text.count(old) != 1:
    raise SystemExit("VesselRecoveryStore key fields mismatch")
text = text.replace(old, new, 1)
old = "        this.creeperIgnitedKey = new NamespacedKey(plugin, \"vessel_recovery_creeper_ignited\");\n        this.creeperFuseTicksKey = new NamespacedKey(plugin, \"vessel_recovery_creeper_fuse_ticks\");"
new = "        this.creeperIgnitedKey = new NamespacedKey(plugin, \"vessel_recovery_creeper_ignited\");\n        this.creeperFuseTicksKey = new NamespacedKey(plugin, \"vessel_recovery_creeper_fuse_ticks\");\n        this.pufferFishPuffStateKey = new NamespacedKey(plugin, \"vessel_recovery_pufferfish_puff_state\");\n        this.vexChargingKey = new NamespacedKey(plugin, \"vessel_recovery_vex_charging\");"
if text.count(old) != 1:
    raise SystemExit("VesselRecoveryStore key ctor mismatch")
text = text.replace(old, new, 1)
old = "            if (state.creeperFuseTicks() != null) {\n                data.set(creeperFuseTicksKey, PersistentDataType.INTEGER, state.creeperFuseTicks());\n            } else {\n                data.remove(creeperFuseTicksKey);\n            }\n            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);"
new = "            if (state.creeperFuseTicks() != null) {\n                data.set(creeperFuseTicksKey, PersistentDataType.INTEGER, state.creeperFuseTicks());\n            } else {\n                data.remove(creeperFuseTicksKey);\n            }\n            if (state.pufferFishPuffState() != null) {\n                data.set(pufferFishPuffStateKey, PersistentDataType.INTEGER, state.pufferFishPuffState());\n            } else {\n                data.remove(pufferFishPuffStateKey);\n            }\n            setNullableBool(data, vexChargingKey, state.vexCharging());\n            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);"
if text.count(old) != 1:
    raise SystemExit("VesselRecoveryStore save specials mismatch")
text = text.replace(old, new, 1)
old = "        if (mob instanceof Creeper creeper) {\n            Boolean ignited = readNullableBool(data, creeperIgnitedKey);\n            Integer fuseTicks = data.get(creeperFuseTicksKey, PersistentDataType.INTEGER);\n            if (ignited != null && fuseTicks != null) {\n                VesselState.restoreCreeper(creeper, ignited, fuseTicks);\n            }\n        }\n\n        clear(mob);"
new = "        if (mob instanceof Creeper creeper) {\n            Boolean ignited = readNullableBool(data, creeperIgnitedKey);\n            Integer fuseTicks = data.get(creeperFuseTicksKey, PersistentDataType.INTEGER);\n            if (ignited != null && fuseTicks != null) {\n                VesselState.restoreCreeper(creeper, ignited, fuseTicks);\n            }\n        }\n        if (mob instanceof PufferFish pufferFish) {\n            Integer puffState = data.get(pufferFishPuffStateKey, PersistentDataType.INTEGER);\n            if (puffState != null) {\n                pufferFish.setPuffState(Math.max(0, Math.min(2, puffState)));\n            }\n        }\n        if (mob instanceof Vex vex) {\n            Boolean charging = readNullableBool(data, vexChargingKey);\n            if (charging != null) {\n                vex.setCharging(charging);\n            }\n        }\n        if (mob instanceof Guardian guardian) {\n            guardian.setLaser(false);\n        }\n\n        clear(mob);"
if text.count(old) != 1:
    raise SystemExit("VesselRecoveryStore recover specials mismatch")
text = text.replace(old, new, 1)
old = "        data.remove(creeperIgnitedKey);\n        data.remove(creeperFuseTicksKey);\n        index.forget(vesselId);"
new = "        data.remove(creeperIgnitedKey);\n        data.remove(creeperFuseTicksKey);\n        data.remove(pufferFishPuffStateKey);\n        data.remove(vexChargingKey);\n        index.forget(vesselId);"
if text.count(old) != 1:
    raise SystemExit("VesselRecoveryStore clear specials mismatch")
text = text.replace(old, new, 1)
p.write_text(text)

# AbilityRegistry: Guardian laser, PufferFish puff, Vex charge and display labels.
p = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = p.read_text()
text = text.replace("import org.bukkit.entity.Ghast;\n", "import org.bukkit.entity.Ghast;\nimport org.bukkit.entity.Guardian;\n", 1)
text = text.replace("import org.bukkit.entity.Player;\n", "import org.bukkit.entity.Player;\nimport org.bukkit.entity.PufferFish;\n", 1)
text = text.replace("import org.bukkit.entity.Witch;\n", "import org.bukkit.entity.Vex;\nimport org.bukkit.entity.Witch;\n", 1)
old = "    private final int camelDashCooldownTicks;\n    private final int camelDashLockTicks;"
new = "    private final int camelDashCooldownTicks;\n    private final int camelDashLockTicks;\n    private final boolean guardianLaserEnabled;\n    private final double guardianLaserRange;\n    private final double guardianLaserRaySize;\n    private final int guardianLaserCooldownTicks;\n    private final boolean pufferFishPuffEnabled;\n    private final int pufferFishPuffCooldownTicks;\n    private final boolean vexChargeEnabled;\n    private final double vexChargeSpeed;\n    private final int vexChargeDurationTicks;\n    private final int vexChargeCooldownTicks;\n    private final int vexChargeLockTicks;"
if text.count(old) != 1:
    raise SystemExit("AbilityRegistry fields mismatch")
text = text.replace(old, new, 1)
old = "        this.camelDashCooldownTicks = Math.max(1, config.getInt(\"abilities.camel-dash.cooldown-ticks\", 30));\n        this.camelDashLockTicks = Math.max(1, config.getInt(\"abilities.camel-dash.movement-lock-ticks\", 8));"
new = "        this.camelDashCooldownTicks = Math.max(1, config.getInt(\"abilities.camel-dash.cooldown-ticks\", 30));\n        this.camelDashLockTicks = Math.max(1, config.getInt(\"abilities.camel-dash.movement-lock-ticks\", 8));\n        this.guardianLaserEnabled = config.getBoolean(\"abilities.guardian-laser.enabled\", true);\n        this.guardianLaserRange = Math.max(2.0, config.getDouble(\"abilities.guardian-laser.range\", 20.0));\n        this.guardianLaserRaySize = Math.max(0.0, config.getDouble(\"abilities.guardian-laser.ray-size\", 0.35));\n        this.guardianLaserCooldownTicks = Math.max(1, config.getInt(\"abilities.guardian-laser.cooldown-ticks\", 100));\n        this.pufferFishPuffEnabled = config.getBoolean(\"abilities.pufferfish-puff.enabled\", true);\n        this.pufferFishPuffCooldownTicks = Math.max(1, config.getInt(\"abilities.pufferfish-puff.cooldown-ticks\", 8));\n        this.vexChargeEnabled = config.getBoolean(\"abilities.vex-charge.enabled\", true);\n        this.vexChargeSpeed = Math.max(0.05, config.getDouble(\"abilities.vex-charge.speed\", 1.25));\n        this.vexChargeDurationTicks = Math.max(1, config.getInt(\"abilities.vex-charge.duration-ticks\", 8));\n        this.vexChargeCooldownTicks = Math.max(1, config.getInt(\"abilities.vex-charge.cooldown-ticks\", 24));\n        this.vexChargeLockTicks = Math.max(1, config.getInt(\"abilities.vex-charge.movement-lock-ticks\", 8));"
if text.count(old) != 1:
    raise SystemExit("AbilityRegistry config mismatch")
text = text.replace(old, new, 1)
old = "        if (vessel instanceof Creeper creeper && creeperEnabled) {\n            return toggleCreeper(session, creeper);\n        }"
new = "        if (vessel instanceof Guardian guardian && guardianLaserEnabled) {\n            return startGuardianLaser(session, guardian);\n        }\n        if (vessel instanceof Creeper creeper && creeperEnabled) {\n            return toggleCreeper(session, creeper);\n        }"
if text.count(old) != 1:
    raise SystemExit("AbilityRegistry Guardian primary insertion mismatch")
text = text.replace(old, new, 1)
old = "        if (vessel instanceof Enderman enderman && endermanTeleportEnabled) {\n            return teleportEnderman(session, enderman);\n        }"
new = "        if (vessel instanceof PufferFish pufferFish && pufferFishPuffEnabled) {\n            return togglePufferFish(session, pufferFish);\n        }\n        if (vessel instanceof Vex vex && vexChargeEnabled) {\n            return chargeVex(session, vex);\n        }\n        if (vessel instanceof Enderman enderman && endermanTeleportEnabled) {\n            return teleportEnderman(session, enderman);\n        }"
if text.count(old) != 1:
    raise SystemExit("AbilityRegistry secondary insert mismatch")
text = text.replace(old, new, 1)
marker = "    private boolean toggleCreeper(PossessionSession session, Creeper creeper) {"
if text.count(marker) != 1:
    raise SystemExit("AbilityRegistry method insertion marker mismatch")
methods = '''    public void tick(PossessionSession session) {
        Mob vessel = session.vessel();
        if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
            return;
        }
        if (vessel instanceof Guardian guardian) {
            tickGuardianLaser(session, guardian);
        }
        if (vessel instanceof Vex vex) {
            tickVexCharge(session, vex);
        }
    }

    public void abortActive(PossessionSession session) {
        Mob vessel = session.vessel();
        if (vessel instanceof Guardian guardian && session.guardianLaserActive()) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            session.guardianLaserActive(false);
        }
        if (vessel instanceof Vex vex && session.vexChargeActive()) {
            vex.setCharging(false);
            session.clearVexCharge();
        }
    }

    public String primaryLabel(Mob vessel) {
        if (vessel instanceof Guardian && guardianLaserEnabled) return "guardian laser";
        if (vessel instanceof Creeper && creeperEnabled) return "fuse";
        if (vessel instanceof AbstractSkeleton && skeletonEnabled) return "arrow / melee";
        if (nativeProjectilesEnabled && projectileFor(vessel) != null) return "projectile";
        if (nativeRangedEnabled && vessel instanceof RangedEntity && supportsNativeRanged(vessel)) return "ranged attack / melee";
        return meleeEnabled ? "melee" : "none";
    }

    public String secondaryLabel(Mob vessel) {
        if (vessel instanceof PufferFish && pufferFishPuffEnabled) return "puff";
        if (vessel instanceof Vex && vexChargeEnabled) return "charge";
        if (vessel instanceof Enderman && endermanTeleportEnabled) return "teleport";
        if (vessel instanceof Spider && spiderPounceEnabled) return "pounce";
        if (vessel instanceof Camel && camelDashEnabled) return "dash";
        return "none";
    }

    private boolean startGuardianLaser(PossessionSession session, Guardian guardian) {
        if (session.guardianLaserActive()) {
            return false;
        }
        LivingEntity target = findLivingTarget(session, guardian, guardianLaserRange, guardianLaserRaySize);
        if (target == null || !guardian.hasLineOfSight(target)) {
            return false;
        }
        int cooldown = Math.max(guardianLaserCooldownTicks, guardian.getLaserDuration() + 10);
        if (!session.acquirePrimaryCooldown(cooldown)) {
            return false;
        }

        guardian.setTarget(target);
        if (!guardian.setLaser(true)) {
            guardian.setTarget(null);
            return false;
        }
        guardian.setLaserTicks(-10);
        guardian.setVelocity(new Vector());
        session.guardianLaserActive(true);
        session.lockMovementControl(guardian.getLaserDuration() + 12);
        return true;
    }

    private void tickGuardianLaser(PossessionSession session, Guardian guardian) {
        if (!session.guardianLaserActive()) {
            return;
        }
        LivingEntity target = guardian.getTarget();
        if (!guardian.hasLaser()
            || target == null
            || !Bukkit.isOwnedByCurrentRegion(target)
            || !target.isValid()
            || target.isDead()
            || !guardian.hasLineOfSight(target)) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            session.guardianLaserActive(false);
            return;
        }

        int duration = Math.max(1, guardian.getLaserDuration());
        int nextTicks = Math.max(-10, guardian.getLaserTicks()) + 1;
        if (nextTicks >= duration) {
            guardian.setLaserTicks(duration);
            guardian.setLaser(false);
            guardian.setTarget(null);
            session.guardianLaserActive(false);
            return;
        }
        guardian.setLaserTicks(nextTicks);
    }

    private boolean togglePufferFish(PossessionSession session, PufferFish pufferFish) {
        if (!session.acquireSecondaryCooldown(pufferFishPuffCooldownTicks)) {
            return false;
        }
        pufferFish.setPuffState(pufferFish.getPuffState() >= 2 ? 0 : 2);
        return true;
    }

    private boolean chargeVex(PossessionSession session, Vex vex) {
        Vector charge = direction(session.view());
        if (charge.lengthSquared() < 1.0E-6 || !session.acquireSecondaryCooldown(vexChargeCooldownTicks)) {
            return false;
        }
        vex.setCharging(true);
        session.startVexCharge(vexChargeDurationTicks);
        session.lockMovementControl(vexChargeLockTicks);
        vex.setVelocity(charge.normalize().multiply(vexChargeSpeed));
        return true;
    }

    private void tickVexCharge(PossessionSession session, Vex vex) {
        if (session.vexChargeActive()) {
            return;
        }
        if (vex.isCharging()) {
            vex.setCharging(false);
        }
        session.clearVexCharge();
    }

'''
text = text.replace(marker, methods + marker, 1)
p.write_text(text)

# Control loop ticks active native abilities separately from movement failures, and acquisition message exposes actual controls for the body.
p = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = p.read_text()
old = '''            try {
                controller.tick(session, vessel);
            } catch (Throwable ex) {
                task.cancel();
                plugin.getLogger().log(Level.SEVERE, "Movement controller failed for " + vessel.getType() + " " + session.vesselId(), ex);
                requestRelease(session, ReleaseReason.INTERNAL_ERROR);
            }
'''
new = '''            try {
                controller.tick(session, vessel);
            } catch (Throwable ex) {
                task.cancel();
                plugin.getLogger().log(Level.SEVERE, "Movement controller failed for " + vessel.getType() + " " + session.vesselId(), ex);
                requestRelease(session, ReleaseReason.INTERNAL_ERROR);
                return;
            }
            try {
                abilities.tick(session);
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.SEVERE, "Active ability tick failed for " + vessel.getType() + " " + session.vesselId(), ex);
                try {
                    abilities.abortActive(session);
                } catch (Throwable cleanupEx) {
                    plugin.getLogger().log(Level.WARNING, "Failed to abort active ability state for " + session.vesselId(), cleanupEx);
                }
            }
'''
if text.count(old) != 1:
    raise SystemExit("PossessionManager control loop mismatch")
text = text.replace(old, new, 1)
old = '            player.sendMessage(Component.text("[Incarnate] Vessel acquired. Left-click: primary, F: secondary, Shift+F: release."));'
new = '            player.sendMessage(Component.text("[Incarnate] Vessel acquired. Left-click: " + abilities.primaryLabel(vessel) + ", F: " + abilities.secondaryLabel(vessel) + ", Shift+F: release."));'
if text.count(old) != 1:
    raise SystemExit("PossessionManager acquisition message mismatch")
text = text.replace(old, new, 1)
p.write_text(text)

# Config additions.
replace_once(
    "src/main/resources/config.yml",
    "  camel-dash:\n    enabled: true\n    horizontal-speed: 1.15\n    vertical-velocity: 0.12\n    cooldown-ticks: 30\n    movement-lock-ticks: 8\n",
    "  camel-dash:\n    enabled: true\n    horizontal-speed: 1.15\n    vertical-velocity: 0.12\n    cooldown-ticks: 30\n    movement-lock-ticks: 8\n  guardian-laser:\n    enabled: true\n    range: 20.0\n    ray-size: 0.35\n    cooldown-ticks: 100\n  pufferfish-puff:\n    enabled: true\n    cooldown-ticks: 8\n  vex-charge:\n    enabled: true\n    speed: 1.25\n    duration-ticks: 8\n    cooldown-ticks: 24\n    movement-lock-ticks: 8\n",
    "0.2.0 ability config",
)

# CI derives plugin version from plugin.yml, so future version bumps cannot silently desync the smoke test.
p = Path(".github/workflows/ci.yml")
text = p.read_text()
text = text.replace("USER_AGENT='Incarnate-CI/0.1.0 (GitHub Actions)'", "USER_AGENT='Incarnate-CI (GitHub Actions)'", 1)
old = "          mkdir -p ci-server/plugins\n          cp build/libs/Incarnate-0.1.0.jar ci-server/plugins/Incarnate.jar"
new = "          PLUGIN_VERSION=$(sed -n 's/^version: //p' src/main/resources/plugin.yml | head -n1 | tr -d '\"\\047')\n          test -n \"$PLUGIN_VERSION\"\n          PLUGIN_JAR=\"build/libs/Incarnate-${PLUGIN_VERSION}.jar\"\n          test -f \"$PLUGIN_JAR\"\n          mkdir -p ci-server/plugins\n          cp \"$PLUGIN_JAR\" ci-server/plugins/Incarnate.jar"
if text.count(old) != 1:
    raise SystemExit("CI plugin jar mismatch")
text = text.replace(old, new, 1)
text = text.replace("grep -Fq 'Incarnate 0.1.0 enabled for Minecraft 26.2+'", "grep -Fq \"Incarnate ${PLUGIN_VERSION} enabled for Minecraft 26.2+\"", 1)
text = text.replace("grep -F 'Incarnate 0.1.0 enabled for Minecraft 26.2+' paper-smoke.log", "grep -F \"Incarnate ${PLUGIN_VERSION} enabled for Minecraft 26.2+\" paper-smoke.log", 1)
text = text.replace("grep -F 'Disabling Incarnate v0.1.0' paper-smoke.log", "grep -F \"Disabling Incarnate v${PLUGIN_VERSION}\" paper-smoke.log", 1)
p.write_text(text)

# README version/features.
p = Path("README.md")
text = p.read_text()
text = text.replace("## What 0.1.0 implements", "## What 0.2.0 implements", 1)
text = text.replace("Ender Dragon and Shulker are deliberately excluded in 0.1.0.", "Ender Dragon and Shulker are deliberately excluded in 0.2.0.", 1)
text = text.replace("0.1.0 has dedicated controllers for:", "0.2.0 has dedicated controllers for:", 1)
text = text.replace("Implemented in 0.1.0:\n", "Implemented in 0.2.0:\n", 2)
text = text.replace("- Creeper: primary toggles the real creeper fuse;", "- Guardian / Elder Guardian: a real charging Guardian laser, advanced tick-by-tick through the Guardian API until native damage is applied;\n- Creeper: primary toggles the real creeper fuse;", 1)
text = text.replace("- Camel: real dashing state plus a physical forward dash impulse.", "- Camel: real dashing state plus a physical forward dash impulse;\n- PufferFish: toggles the real puff state;\n- Vex: native charging state plus a 3D physical charge impulse.", 1)
text = text.replace("Special-state restoration currently includes sitting state, Bat awake/hanging state, Camel dashing state and Creeper ignition/fuse progress.", "Special-state restoration currently includes sitting state, Bat awake/hanging state, Camel dashing state, Creeper ignition/fuse progress, PufferFish puff state and Vex charging state. Normal release also restores a Guardian's pre-possession laser state when its original target is still safely restorable; crash recovery safely cancels an orphaned laser because its old target cannot be persisted Folia-safely.", 1)
p.write_text(text)

# Changelog new release section.
p = Path("CHANGELOG.md")
text = p.read_text()
insert = '''# Changelog

## 0.2.0

Native Abilities Update.

- Added real Guardian / Elder Guardian laser attacks using the Paper Guardian API. The laser charges over native laser ticks and lets the Guardian API apply the actual hit rather than calling manual damage.
- Guardian laser targets are validated on the vessel-owned Folia region and the attack cancels safely if the target dies, crosses regions, or leaves line of sight.
- Added PufferFish secondary puff toggle using the real puff state, with normal and crash recovery of the original puff state.
- Added Vex secondary charge using the real charging state plus a 3D physical impulse and movement-control lock; original charging state is recovered.
- Added active-ability ticking on the vessel EntityScheduler, separate from movement-controller failure handling.
- Possession acquisition now tells the controller the actual primary and secondary actions available for that body.
- Guardian pre-possession laser state is restored on normal release when its original target is still safely restorable; interrupted recovery cancels orphaned laser state rather than guessing a target.
- CI smoke tests now derive the plugin version from plugin.yml/JAR metadata instead of hardcoding the previous release number.
- Retains default-deny create/possess/per-mob permissions and unrestricted emergency `/release`.

'''
if not text.startswith("# Changelog\n\n## 0.1.0"):
    raise SystemExit("CHANGELOG header mismatch")
text = insert + text[len("# Changelog\n\n"):]
p.write_text(text)

print("Incarnate 0.2.0 patch applied")
