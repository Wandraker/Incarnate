from pathlib import Path


def replace(path, old, new, count=1):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"pattern not found in {path}: {old[:160]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")


replace("build.gradle.kts", 'version = "0.9.0-dev.2"', 'version = "0.9.0-dev.3"')
replace("src/main/resources/plugin.yml", "version: 0.9.0-dev.2", "version: 0.9.0-dev.3")

replace(
    "src/main/java/dev/onelsey/incarnate/sense/WardenSenseSnapshot.java",
    """    String eventKey,\n    UUID sourceId,\n    long expiresAfterTick\n) {\n    public boolean isActive(long controlTick) {\n        return controlTick <= expiresAfterTick;\n    }\n}\n""",
    """    String eventKey,\n    UUID sourceId,\n    UUID sonicTargetId,\n    long sonicTargetExpiresAfterTick,\n    long expiresAfterTick\n) {\n    public boolean isActive(long controlTick) {\n        return controlTick <= expiresAfterTick;\n    }\n\n    public UUID activeSonicTarget(long controlTick) {\n        return sonicTargetId != null && controlTick <= sonicTargetExpiresAfterTick ? sonicTargetId : null;\n    }\n}\n"""
)

replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    """    public void recordWardenSense(UUID worldId, double x, double y, double z, String kind, String eventKey, UUID sourceId, int memoryTicks) {\n        long expires = controlTick + Math.max(1, memoryTicks);\n        this.wardenSense = new WardenSenseSnapshot(worldId, x, y, z, kind, eventKey, sourceId, expires);\n    }\n\n    public WardenSenseSnapshot activeWardenSense() {\n""",
    """    public synchronized void recordWardenSense(\n        UUID worldId,\n        double x,\n        double y,\n        double z,\n        String kind,\n        String eventKey,\n        UUID sourceId,\n        UUID sonicTargetId,\n        int memoryTicks\n    ) {\n        long expires = controlTick + Math.max(1, memoryTicks);\n        UUID candidate = sonicTargetId;\n        long candidateExpires = sonicTargetId == null ? Long.MIN_VALUE : expires;\n        WardenSenseSnapshot previous = this.wardenSense;\n        if (candidate == null && previous != null) {\n            UUID previousCandidate = previous.activeSonicTarget(controlTick);\n            if (previousCandidate != null) {\n                candidate = previousCandidate;\n                candidateExpires = previous.sonicTargetExpiresAfterTick();\n            }\n        }\n        this.wardenSense = new WardenSenseSnapshot(\n            worldId, x, y, z, kind, eventKey, sourceId, candidate, candidateExpires, expires\n        );\n    }\n\n    public WardenSenseSnapshot activeWardenSense() {\n"""
)

replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    """    public void clearWardenSense() {\n        this.wardenSense = null;\n    }\n\n    public ScheduledTask controlTask() { return controlTask; }\n""",
    """    public void clearWardenSense() {\n        this.wardenSense = null;\n    }\n\n    public UUID activeWardenSonicTargetId() {\n        WardenSenseSnapshot snapshot = wardenSense;\n        return snapshot == null ? null : snapshot.activeSonicTarget(controlTick);\n    }\n\n    public synchronized void clearWardenSonicTargetCandidate() {\n        WardenSenseSnapshot snapshot = wardenSense;\n        if (snapshot == null || snapshot.sonicTargetId() == null) {\n            return;\n        }\n        wardenSense = new WardenSenseSnapshot(\n            snapshot.worldId(),\n            snapshot.x(),\n            snapshot.y(),\n            snapshot.z(),\n            snapshot.kind(),\n            snapshot.eventKey(),\n            snapshot.sourceId(),\n            null,\n            Long.MIN_VALUE,\n            snapshot.expiresAfterTick()\n        );\n    }\n\n    public ScheduledTask controlTask() { return controlTask; }\n"""
)

replace(
    "src/main/java/dev/onelsey/incarnate/listener/SessionListener.java",
    """import org.bukkit.entity.Entity;\nimport org.bukkit.entity.Mob;\nimport org.bukkit.entity.Player;\n""",
    """import org.bukkit.entity.Entity;\nimport org.bukkit.entity.LivingEntity;\nimport org.bukkit.entity.Mob;\nimport org.bukkit.entity.Player;\nimport org.bukkit.entity.Projectile;\n"""
)
replace(
    "src/main/java/dev/onelsey/incarnate/listener/SessionListener.java",
    "import org.bukkit.inventory.EquipmentSlot;\n",
    "import org.bukkit.inventory.EquipmentSlot;\nimport org.bukkit.projectiles.ProjectileSource;\n"
)
replace(
    "src/main/java/dev/onelsey/incarnate/listener/SessionListener.java",
    """        Entity source = event.getEntity();\n        java.util.UUID sourceId = null;\n        if (source != null && Bukkit.isOwnedByCurrentRegion(source)) {\n            if (source.isSneaking() && Tag.GAME_EVENT_IGNORE_VIBRATIONS_SNEAKING.isTagged(event.getEvent())) {\n                return;\n            }\n            sourceId = source.getUniqueId();\n        }\n\n        org.bukkit.Location location = event.getLocation();\n        possessions.recordWardenGameEvent(\n            location.getWorld().getUID(),\n            location.getX(),\n            location.getY(),\n            location.getZ(),\n            event.getEvent().getKey().getKey(),\n            event.getRadius(),\n            sourceId\n        );\n""",
    """        Entity source = event.getEntity();\n        java.util.UUID sourceId = null;\n        java.util.UUID sonicTargetId = null;\n        if (source != null && Bukkit.isOwnedByCurrentRegion(source)) {\n            if (source.isSneaking() && Tag.GAME_EVENT_IGNORE_VIBRATIONS_SNEAKING.isTagged(event.getEvent())) {\n                return;\n            }\n            sourceId = source.getUniqueId();\n            if (source instanceof LivingEntity living) {\n                sonicTargetId = living.getUniqueId();\n            } else if (source instanceof Projectile projectile) {\n                ProjectileSource shooter = projectile.getShooter();\n                if (shooter instanceof LivingEntity living && Bukkit.isOwnedByCurrentRegion(living)) {\n                    sonicTargetId = living.getUniqueId();\n                }\n            }\n        }\n\n        org.bukkit.Location location = event.getLocation();\n        possessions.recordWardenGameEvent(\n            location.getWorld().getUID(),\n            location.getX(),\n            location.getY(),\n            location.getZ(),\n            event.getEvent().getKey().getKey(),\n            event.getRadius(),\n            sourceId,\n            sonicTargetId\n        );\n"""
)

replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    '                    "secondary_state", cooldownState(player, session.secondaryAbilityKey(), session.secondaryCooldownRemainingTicks())\n',
    '                    "secondary_state", secondaryCooldownState(player, session)\n'
)
replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    """    private Component cooldownState(Player player, String abilityKey, int remainingTicks) {\n""",
    """    private Component secondaryCooldownState(Player player, PossessionSession session) {\n        int remainingTicks = session.secondaryCooldownRemainingTicks();\n        if (remainingTicks <= 0\n            && session.vesselType() == EntityType.WARDEN\n            && \"sonic-boom\".equals(session.secondaryAbilityKey())\n            && session.activeWardenSonicTargetId() == null) {\n            return messages.render(player, \"hud.no-target\");\n        }\n        return cooldownState(player, session.secondaryAbilityKey(), remainingTicks);\n    }\n\n    private Component cooldownState(Player player, String abilityKey, int remainingTicks) {\n"""
)
replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    """        int eventRadius,\n        UUID sourceId\n    ) {\n""",
    """        int eventRadius,\n        UUID sourceId,\n        UUID sonicTargetId\n    ) {\n"""
)
replace(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    """            session.recordWardenSense(worldId, x, y, z, kind, eventKey, sourceId, wardenSenseMemoryTicks);\n""",
    """            UUID safeSonicTargetId = sonicTargetId;\n            if (safeSonicTargetId != null\n                && (safeSonicTargetId.equals(session.playerId()) || safeSonicTargetId.equals(session.vesselId()))) {\n                safeSonicTargetId = null;\n            }\n            session.recordWardenSense(\n                worldId, x, y, z, kind, eventKey, sourceId, safeSonicTargetId, wardenSenseMemoryTicks\n            );\n"""
)

replace(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    """    private boolean startWardenSonic(PossessionSession session, Warden warden) {\n        if (session.wardenSonicTracked()) {\n            return false;\n        }\n        var sense = session.activeWardenSense();\n        if (sense == null || sense.sourceId() == null) {\n            return false;\n        }\n\n        Entity rawTarget = Bukkit.getEntity(sense.sourceId());\n        if (!(rawTarget instanceof LivingEntity target)\n            || !Bukkit.isOwnedByCurrentRegion(target)\n            || !target.isValid()\n            || target.isDead()\n            || target.getUniqueId().equals(session.playerId())\n            || target.getUniqueId().equals(session.vesselId())\n            || target.getWorld() != warden.getWorld()\n            || warden.getEyeLocation().distanceSquared(target.getEyeLocation()) > wardenSonicRange * wardenSonicRange\n            || !session.acquireSecondaryCooldown(Math.max(wardenSonicCooldownTicks, wardenSonicChargeTicks))) {\n            return false;\n        }\n\n        session.startWardenSonic(target.getUniqueId(), wardenSonicChargeTicks);\n        session.lockMovementControl(wardenSonicChargeTicks + 2);\n        warden.setVelocity(new Vector());\n        warden.getWorld().playSound(\n            warden.getLocation(),\n            Sound.ENTITY_WARDEN_SONIC_CHARGE,\n            SoundCategory.HOSTILE,\n            3.0f,\n            1.0f\n        );\n        return true;\n    }\n""",
    """    private boolean startWardenSonic(PossessionSession session, Warden warden) {\n        if (session.wardenSonicTracked()) {\n            return false;\n        }\n        UUID targetId = session.activeWardenSonicTargetId();\n        if (targetId == null) {\n            return false;\n        }\n\n        Entity rawTarget = Bukkit.getEntity(targetId);\n        if (!(rawTarget instanceof LivingEntity target)\n            || !Bukkit.isOwnedByCurrentRegion(target)\n            || !target.isValid()\n            || target.isDead()\n            || target.getUniqueId().equals(session.playerId())\n            || target.getUniqueId().equals(session.vesselId())\n            || target.getWorld() != warden.getWorld()\n            || warden.getEyeLocation().distanceSquared(target.getEyeLocation()) > wardenSonicRange * wardenSonicRange) {\n            session.clearWardenSonicTargetCandidate();\n            return false;\n        }\n        if (!session.acquireSecondaryCooldown(Math.max(wardenSonicCooldownTicks, wardenSonicChargeTicks))) {\n            return false;\n        }\n\n        session.clearWardenSonicTargetCandidate();\n        session.startWardenSonic(target.getUniqueId(), wardenSonicChargeTicks);\n        session.lockMovementControl(wardenSonicChargeTicks + 2);\n        warden.setVelocity(new Vector());\n        warden.getWorld().playSound(\n            warden.getLocation(),\n            Sound.ENTITY_WARDEN_SONIC_CHARGE,\n            SoundCategory.HOSTILE,\n            3.0f,\n            1.0f\n        );\n        return true;\n    }\n"""
)

for locale, ghast, sonic, no_target in (
    ("src/main/resources/locales/en_US.yml", "ghast fireball", "sonic boom", "NO TARGET"),
    ("src/main/resources/locales/ru_RU.yml", "огненный шар гаста", "звуковой удар", "НЕТ ЦЕЛИ"),
):
    replace(locale, "    unavailable: '<muted>—</muted>'\n", f"    unavailable: '<muted>—</muted>'\n    no-target: '<muted>{no_target}</muted>'\n")
    replace(locale, "    fangs: '" + ("fangs" if "en_US" in locale else "клыки") + "'\n", "    fangs: '" + ("fangs" if "en_US" in locale else "клыки") + f"'\n    ghast-fireball: '{ghast}'\n    sonic-boom: '{sonic}'\n")

replace(
    "src/test/java/dev/onelsey/incarnate/sense/WardenSenseMathTest.java",
    "import static org.junit.jupiter.api.Assertions.assertFalse;\n",
    "import static org.junit.jupiter.api.Assertions.assertFalse;\nimport static org.junit.jupiter.api.Assertions.assertNull;\n"
)
replace(
    "src/test/java/dev/onelsey/incarnate/sense/WardenSenseMathTest.java",
    """        WardenSenseSnapshot snapshot = new WardenSenseSnapshot(\n            UUID.randomUUID(), 1.0, 2.0, 3.0, \"movement\", \"step\", null, 42L\n        );\n        assertTrue(snapshot.isActive(42L));\n        assertFalse(snapshot.isActive(43L));\n    }\n}\n""",
    """        WardenSenseSnapshot snapshot = new WardenSenseSnapshot(\n            UUID.randomUUID(), 1.0, 2.0, 3.0, \"movement\", \"step\", null, null, Long.MIN_VALUE, 42L\n        );\n        assertTrue(snapshot.isActive(42L));\n        assertFalse(snapshot.isActive(43L));\n    }\n\n    @Test\n    void sonicTargetExpiryIsIndependentFromLatestHudVibration() {\n        UUID targetId = UUID.randomUUID();\n        WardenSenseSnapshot snapshot = new WardenSenseSnapshot(\n            UUID.randomUUID(), 1.0, 2.0, 3.0, \"block\", \"block_destroy\", null, targetId, 42L, 60L\n        );\n        assertEquals(targetId, snapshot.activeSonicTarget(42L));\n        assertNull(snapshot.activeSonicTarget(43L));\n        assertTrue(snapshot.isActive(60L));\n    }\n}\n"""
)

replace(
    "src/test/java/dev/onelsey/incarnate/message/MessageResourcesTest.java",
    """    @Test\n    void bundledMiniMessageTemplatesParse() {\n""",
    """    @Test\n    void wardenAndGhastAbilityPresentationIsBundled() {\n        for (String locale : Set.of(\"en_US\", \"ru_RU\")) {\n            YamlConfiguration localized = load(\"src/main/resources/locales/\" + locale + \".yml\");\n            assertNotNull(localized.getString(\"messages.ability.ghast-fireball\"));\n            assertNotNull(localized.getString(\"messages.ability.sonic-boom\"));\n            assertNotNull(localized.getString(\"messages.hud.no-target\"));\n        }\n    }\n\n    @Test\n    void bundledMiniMessageTemplatesParse() {\n"""
)

changelog = Path("CHANGELOG.md")
text = changelog.read_text(encoding="utf-8")
header = "# Changelog\n\n"
if not text.startswith(header):
    raise SystemExit("unexpected changelog header")
entry = """## 0.9.0-dev.3 - Warden Sonic Target Memory\n\n- Separates the latest Warden HUD vibration from the most recent living entity that can actually be used as a sonic-boom target.\n- Unrelated block/action vibrations no longer erase a still-valid sonic target; the target keeps its original sense expiry instead of being refreshed by unrelated noise.\n- Projectile vibrations can resolve to their living shooter when both entities are safely owned by the current Folia region.\n- Consumes the sensed target when sonic charging starts and clears stale/invalid candidates fail-closed.\n- Shows a localized `NO TARGET` state instead of `READY` when Warden sonic boom has no recent living target.\n- Adds the missing English/Russian labels for Ghast fireball and Warden sonic boom.\n\n## 0.9.0-dev.2 - Warden Sonic & Ghast Fidelity\n\n- Adds controlled Warden sonic boom without mutating Warden anger or Brain state. The attack uses the real `SONIC_BOOM` damage type, charge/boom sounds, particles and knockback resistance.\n- Adds a native-feeling Ghast attack sequence: charging state, warning sound, delayed real `LargeFireball`, explosion power and shoot sound.\n- Persists/restores pre-possession Ghast charging state through normal and interrupted-session recovery.\n- Advances the gameplay configuration schema to 7 non-destructively.\n\n"""
changelog.write_text(header + entry + text[len(header):], encoding="utf-8")
