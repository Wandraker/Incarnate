from pathlib import Path


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8")


def replace_exact(path, old, new, count=1):
    text = read(path)
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {actual}: {old[:100]!r}")
    write(path, text.replace(old, new, count))


replace_exact("build.gradle.kts", 'version = "0.3.0"', 'version = "0.4.0"')

replace_exact(
    "src/main/resources/plugin.yml",
    "version: 0.3.0",
    "version: 0.4.0",
)
replace_exact(
    "src/main/resources/plugin.yml",
    "description: Create a real mob body, trigger abilities, or inspect a controlled vessel.\n    usage: /incarnate <mob|primary|secondary|inspect>",
    "description: Create a real mob body, trigger abilities, inspect it, or view possession status.\n    usage: /incarnate <mob|primary|secondary|status|inspect>",
)

replace_exact(
    "src/main/resources/config.yml",
    "  ravager-roar:\n    enabled: true\n    cooldown-ticks: 80\n    movement-lock-ticks: 12\n",
    "  ravager-roar:\n    enabled: true\n    cooldown-ticks: 80\n    movement-lock-ticks: 12\n  evoker-fangs:\n    enabled: true\n    count: 5\n    spacing: 1.25\n    base-delay-ticks: 2\n    delay-step-ticks: 2\n    cooldown-ticks: 40\n",
)

# PossessionSession: immutable vessel/ability identity plus safe cooldown snapshots.
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "import org.bukkit.Location;\nimport org.bukkit.entity.Mob;",
    "import org.bukkit.Location;\nimport org.bukkit.entity.EntityType;\nimport org.bukkit.entity.Mob;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "    private final UUID vesselId;\n    private final Player player;\n    private final Mob vessel;\n    private final PossessionOrigin origin;",
    "    private final UUID vesselId;\n    private final EntityType vesselType;\n    private final Player player;\n    private final Mob vessel;\n    private final PossessionOrigin origin;\n    private final String primaryAbilityKey;\n    private final String secondaryAbilityKey;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "    private volatile long lastPrimaryAbilityTick = Long.MIN_VALUE;\n    private volatile long lastSecondaryAbilityTick = Long.MIN_VALUE;",
    "    private volatile long lastPrimaryAbilityTick = Long.MIN_VALUE;\n    private volatile long lastSecondaryAbilityTick = Long.MIN_VALUE;\n    private volatile int lastPrimaryCooldownTicks;\n    private volatile int lastSecondaryCooldownTicks;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "        VesselState vesselState,\n        InputSnapshot initialInput,\n        ViewSnapshot initialView\n    ) {",
    "        VesselState vesselState,\n        InputSnapshot initialInput,\n        ViewSnapshot initialView,\n        String primaryAbilityKey,\n        String secondaryAbilityKey\n    ) {",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "        this.vesselId = vessel.getUniqueId();\n        this.player = player;\n        this.vessel = vessel;\n        this.origin = origin;",
    "        this.vesselId = vessel.getUniqueId();\n        this.vesselType = vessel.getType();\n        this.player = player;\n        this.vessel = vessel;\n        this.origin = origin;\n        this.primaryAbilityKey = primaryAbilityKey;\n        this.secondaryAbilityKey = secondaryAbilityKey;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "    public UUID vesselId() { return vesselId; }\n    public Player player() { return player; }\n    public Mob vessel() { return vessel; }\n    public PossessionOrigin origin() { return origin; }",
    "    public UUID vesselId() { return vesselId; }\n    public EntityType vesselType() { return vesselType; }\n    public Player player() { return player; }\n    public Mob vessel() { return vessel; }\n    public PossessionOrigin origin() { return origin; }\n    public String primaryAbilityKey() { return primaryAbilityKey; }\n    public String secondaryAbilityKey() { return secondaryAbilityKey; }",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "        lastPrimaryAbilityTick = now;\n        return true;\n    }\n\n    public boolean acquireSecondaryCooldown(int cooldownTicks) {",
    "        lastPrimaryAbilityTick = now;\n        lastPrimaryCooldownTicks = Math.max(1, cooldownTicks);\n        return true;\n    }\n\n    public long primaryCooldownRemainingTicks() {\n        long last = lastPrimaryAbilityTick;\n        int duration = lastPrimaryCooldownTicks;\n        if (last == Long.MIN_VALUE || duration <= 0) {\n            return 0L;\n        }\n        return Math.max(0L, duration - (controlTick - last));\n    }\n\n    public boolean acquireSecondaryCooldown(int cooldownTicks) {",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java",
    "        lastSecondaryAbilityTick = now;\n        return true;\n    }\n\n    public void lockMovementControl(int ticks) {",
    "        lastSecondaryAbilityTick = now;\n        lastSecondaryCooldownTicks = Math.max(1, cooldownTicks);\n        return true;\n    }\n\n    public long secondaryCooldownRemainingTicks() {\n        long last = lastSecondaryAbilityTick;\n        int duration = lastSecondaryCooldownTicks;\n        if (last == Long.MIN_VALUE || duration <= 0) {\n            return 0L;\n        }\n        return Math.max(0L, duration - (controlTick - last));\n    }\n\n    public void lockMovementControl(int ticks) {",
)

# Manager: calculate ability identity once on the vessel thread and never read it cross-region for UI.
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    "                    vesselState,\n                    initialInput,\n                    initialView\n                );",
    "                    vesselState,\n                    initialInput,\n                    initialView,\n                    abilities.primaryLabel(vessel),\n                    abilities.secondaryLabel(vessel)\n                );",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java",
    "        Player player = session.player();\n        Mob vessel = session.vessel();\n        notifyPlayer(player, \"acquired\", Map.of(\n            \"camera\", messages.cameraLabel(player, cameraKey),\n            \"primary_action\", messages.abilityLabel(player, abilities.primaryLabel(vessel)),\n            \"secondary_action\", messages.abilityLabel(player, abilities.secondaryLabel(vessel))\n        ));",
    "        Player player = session.player();\n        notifyPlayer(player, \"acquired\", Map.of(\n            \"camera\", messages.cameraLabel(player, cameraKey),\n            \"primary_action\", messages.abilityLabel(player, session.primaryAbilityKey()),\n            \"secondary_action\", messages.abilityLabel(player, session.secondaryAbilityKey())\n        ));",
)

# Evoker gets true EvokerFangs entities with the possessed Evoker as owner.
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "import org.bukkit.entity.Enderman;\nimport org.bukkit.entity.Ghast;",
    "import org.bukkit.entity.Enderman;\nimport org.bukkit.entity.Evoker;\nimport org.bukkit.entity.EvokerFangs;\nimport org.bukkit.entity.Ghast;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "    private final boolean ravagerRoarEnabled;\n    private final int ravagerRoarCooldownTicks;\n    private final int ravagerRoarLockTicks;",
    "    private final boolean ravagerRoarEnabled;\n    private final int ravagerRoarCooldownTicks;\n    private final int ravagerRoarLockTicks;\n    private final boolean evokerFangsEnabled;\n    private final int evokerFangsCount;\n    private final double evokerFangsSpacing;\n    private final int evokerFangsBaseDelayTicks;\n    private final int evokerFangsDelayStepTicks;\n    private final int evokerFangsCooldownTicks;",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "        this.ravagerRoarEnabled = config.getBoolean(\"abilities.ravager-roar.enabled\", true);\n        this.ravagerRoarCooldownTicks = Math.max(1, config.getInt(\"abilities.ravager-roar.cooldown-ticks\", 80));\n        this.ravagerRoarLockTicks = Math.max(1, config.getInt(\"abilities.ravager-roar.movement-lock-ticks\", 12));",
    "        this.ravagerRoarEnabled = config.getBoolean(\"abilities.ravager-roar.enabled\", true);\n        this.ravagerRoarCooldownTicks = Math.max(1, config.getInt(\"abilities.ravager-roar.cooldown-ticks\", 80));\n        this.ravagerRoarLockTicks = Math.max(1, config.getInt(\"abilities.ravager-roar.movement-lock-ticks\", 12));\n        this.evokerFangsEnabled = config.getBoolean(\"abilities.evoker-fangs.enabled\", true);\n        this.evokerFangsCount = Math.max(1, config.getInt(\"abilities.evoker-fangs.count\", 5));\n        this.evokerFangsSpacing = Math.max(0.5, config.getDouble(\"abilities.evoker-fangs.spacing\", 1.25));\n        this.evokerFangsBaseDelayTicks = Math.max(1, config.getInt(\"abilities.evoker-fangs.base-delay-ticks\", 2));\n        this.evokerFangsDelayStepTicks = Math.max(0, config.getInt(\"abilities.evoker-fangs.delay-step-ticks\", 2));\n        this.evokerFangsCooldownTicks = Math.max(1, config.getInt(\"abilities.evoker-fangs.cooldown-ticks\", 40));",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "        if (vessel instanceof Creeper creeper && creeperEnabled) {\n            return toggleCreeper(session, creeper);\n        }\n        if (vessel instanceof AbstractSkeleton skeleton && skeletonEnabled) {",
    "        if (vessel instanceof Creeper creeper && creeperEnabled) {\n            return toggleCreeper(session, creeper);\n        }\n        if (vessel instanceof Evoker evoker && evokerFangsEnabled) {\n            return castEvokerFangs(session, evoker);\n        }\n        if (vessel instanceof AbstractSkeleton skeleton && skeletonEnabled) {",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "        if (vessel instanceof Creeper && creeperEnabled) return \"fuse\";\n        if (vessel instanceof AbstractSkeleton && skeletonEnabled) return \"arrow-melee\";",
    "        if (vessel instanceof Creeper && creeperEnabled) return \"fuse\";\n        if (vessel instanceof Evoker && evokerFangsEnabled) return \"evoker-fangs\";\n        if (vessel instanceof AbstractSkeleton && skeletonEnabled) return \"arrow-melee\";",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java",
    "    private boolean toggleCreeper(PossessionSession session, Creeper creeper) {",
    "    private boolean castEvokerFangs(PossessionSession session, Evoker evoker) {\n        Vector horizontal = direction(session.view());\n        horizontal.setY(0.0);\n        if (horizontal.lengthSquared() < 1.0E-6) {\n            return false;\n        }\n        horizontal.normalize();\n\n        java.util.List<Location> locations = new java.util.ArrayList<>();\n        Location base = evoker.getLocation();\n        for (int index = 0; index < evokerFangsCount; index++) {\n            double distance = evokerFangsSpacing * (index + 1);\n            Location candidate = base.clone().add(horizontal.clone().multiply(distance));\n            Location spawn = findFangSpawn(candidate);\n            if (spawn != null) {\n                locations.add(spawn);\n            }\n        }\n        if (locations.isEmpty() || !session.acquirePrimaryCooldown(evokerFangsCooldownTicks)) {\n            return false;\n        }\n\n        evoker.swingMainHand();\n        for (int index = 0; index < locations.size(); index++) {\n            Location spawn = locations.get(index);\n            org.bukkit.entity.Entity spawned = evoker.getWorld().spawnEntity(spawn, org.bukkit.entity.EntityType.EVOKER_FANGS);\n            if (!(spawned instanceof EvokerFangs fangs)) {\n                spawned.remove();\n                continue;\n            }\n            fangs.setOwner(evoker);\n            fangs.setAttackDelay(Math.max(1, evokerFangsBaseDelayTicks + index * evokerFangsDelayStepTicks));\n        }\n        return true;\n    }\n\n    private static Location findFangSpawn(Location candidate) {\n        Location probe = candidate.clone().add(0.0, 2.5, 0.0);\n        RayTraceResult ground = candidate.getWorld().rayTraceBlocks(\n            probe,\n            new Vector(0.0, -1.0, 0.0),\n            5.0,\n            FluidCollisionMode.NEVER,\n            true\n        );\n        if (ground == null || ground.getHitBlock() == null) {\n            return null;\n        }\n        Location spawn = candidate.clone();\n        spawn.setY(ground.getHitBlock().getY() + 1.0);\n        return Bukkit.isOwnedByCurrentRegion(spawn) ? spawn : null;\n    }\n\n    private boolean toggleCreeper(PossessionSession session, Creeper creeper) {",
)

# /incarnate status is safe: it reads only immutable/volatile session snapshots, never the live vessel cross-region.
replace_exact(
    "src/main/java/dev/onelsey/incarnate/command/IncarnateCommand.java",
    "        if (action.equals(\"secondary\")) {\n            possessions.triggerSecondary(player);\n            return true;\n        }\n        if (action.equals(\"inspect\")) {",
    "        if (action.equals(\"secondary\")) {\n            possessions.triggerSecondary(player);\n            return true;\n        }\n        if (action.equals(\"status\")) {\n            status(player);\n            return true;\n        }\n        if (action.equals(\"inspect\")) {",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/command/IncarnateCommand.java",
    "    private void inspect(Player player) {",
    "    private void status(Player player) {\n        PossessionSession session = possessions.session(player);\n        if (session == null || !session.isActive()) {\n            messages.send(player, \"not-possessing\");\n            return;\n        }\n\n        String cameraKey = switch (session.cameraTransport()) {\n            case MOUNTED -> \"mounted-free-look\";\n            case SPECTATOR_TARGET -> \"spectator-target\";\n            case NONE -> \"pending\";\n        };\n        String originKey = \"origin.\" + session.origin().name().toLowerCase(Locale.ROOT);\n        messages.send(player, \"status-result\", Map.of(\n            \"mob\", Component.text(session.vesselType().name().toLowerCase(Locale.ROOT)),\n            \"camera\", messages.cameraLabel(player, cameraKey),\n            \"origin\", messages.render(player, originKey),\n            \"primary_action\", messages.abilityLabel(player, session.primaryAbilityKey()),\n            \"secondary_action\", messages.abilityLabel(player, session.secondaryAbilityKey()),\n            \"primary_cooldown\", cooldownText(player, session.primaryCooldownRemainingTicks()),\n            \"secondary_cooldown\", cooldownText(player, session.secondaryCooldownRemainingTicks())\n        ));\n    }\n\n    private Component cooldownText(Player player, long ticks) {\n        if (ticks <= 0L) {\n            return messages.render(player, \"status.ready\");\n        }\n        return Component.text(String.format(Locale.ROOT, \"%.1fs\", ticks / 20.0));\n    }\n\n    private void inspect(Player player) {",
)
replace_exact(
    "src/main/java/dev/onelsey/incarnate/command/IncarnateCommand.java",
    "            if (\"SECONDARY\".startsWith(prefix)) {\n                out.add(\"secondary\");\n            }\n        }",
    "            if (\"SECONDARY\".startsWith(prefix)) {\n                out.add(\"secondary\");\n            }\n            if (\"STATUS\".startsWith(prefix)) {\n                out.add(\"status\");\n            }\n        }",
)

# Localization.
for locale, usage_old, usage_new, status_line, ready, pending, fangs in [
    (
        "en_US",
        "  usage-incarnate: '<prefix> <muted>Usage:</muted> <text>/incarnate &lt;mob|primary|secondary|inspect&gt;</text>'",
        "  usage-incarnate: '<prefix> <muted>Usage:</muted> <text>/incarnate &lt;mob|primary|secondary|status|inspect&gt;</text>'",
        "  status-result: '<prefix> <primary><mob></primary> <dark_gray>•</dark_gray> <muted>camera</muted> <secondary><camera></secondary> <dark_gray>•</dark_gray> <muted>origin</muted> <text><origin></text> <dark_gray>•</dark_gray> <muted>Left click</muted> <accent><primary_action></accent> <dark_gray>[<primary_cooldown>]</dark_gray> <dark_gray>•</dark_gray> <muted>F</muted> <accent><secondary_action></accent> <dark_gray>[<secondary_cooldown>]</dark_gray>'",
        "ready",
        "attaching",
        "evoker fangs",
    ),
    (
        "ru_RU",
        "  usage-incarnate: '<prefix> <muted>Использование:</muted> <text>/incarnate &lt;моб|primary|secondary|inspect&gt;</text>'",
        "  usage-incarnate: '<prefix> <muted>Использование:</muted> <text>/incarnate &lt;моб|primary|secondary|status|inspect&gt;</text>'",
        "  status-result: '<prefix> <primary><mob></primary> <dark_gray>•</dark_gray> <muted>камера</muted> <secondary><camera></secondary> <dark_gray>•</dark_gray> <muted>источник</muted> <text><origin></text> <dark_gray>•</dark_gray> <muted>ЛКМ</muted> <accent><primary_action></accent> <dark_gray>[<primary_cooldown>]</dark_gray> <dark_gray>•</dark_gray> <muted>F</muted> <accent><secondary_action></accent> <dark_gray>[<secondary_cooldown>]</dark_gray>'",
        "готово",
        "подключается",
        "клыки заклинателя",
    ),
]:
    path = f"src/main/resources/locales/{locale}.yml"
    replace_exact(path, usage_old, usage_new)
    replace_exact(
        path,
        "  permission-create:",
        status_line + "\n  status:\n    ready: '" + ready + "'\n  permission-create:",
    )
    replace_exact(
        path,
        "    spectator-target: '" + ("spectator target" if locale == "en_US" else "spectator-камера") + "'",
        "    spectator-target: '" + ("spectator target" if locale == "en_US" else "spectator-камера") + "'\n    pending: '" + pending + "'",
    )
    replace_exact(
        path,
        "    guardian-laser: '" + ("guardian laser" if locale == "en_US" else "лазер хранителя") + "'",
        "    guardian-laser: '" + ("guardian laser" if locale == "en_US" else "лазер хранителя") + "'\n    evoker-fangs: '" + fangs + "'",
    )

# README and changelog.
replace_exact(
    "README.md",
    "- `/incarnate secondary` - fallback secondary-ability trigger for testing.\n- `/incarnate inspect`",
    "- `/incarnate secondary` - fallback secondary-ability trigger for testing.\n- `/incarnate status` - show the current vessel, camera transport, ability mapping and live cooldowns without cross-region vessel reads.\n- `/incarnate inspect`",
)
replace_exact("README.md", "## What 0.3.0 implements", "## What 0.4.0 implements")
replace_exact(
    "README.md",
    "### Real bodies\n",
    "0.4.0 adds a true Evoker-fangs cast and a Folia-safe possession status snapshot while retaining the 0.3.0 mounted free-look camera architecture.\n\n### Real bodies\n",
)
replace_exact(
    "README.md",
    "- Witch - native ranged attack through the Mob implementation.\n",
    "- Witch - native ranged attack through the Mob implementation;\n- Evoker - a real line of `EvokerFangs` entities owned by the possessed Evoker, with configurable count, spacing, delays and cooldown.\n",
)
replace_exact("README.md", "0.3.0 keeps the real Mob AI flag enabled", "0.4.0 keeps the real Mob AI flag enabled")

change = """## 0.4.0\n\nMob Identity & Control Update.\n\n- Added `/incarnate status`, showing the current vessel type, camera transport, possession origin, mapped primary/secondary abilities and live cooldown remaining values.\n- Status reads immutable/volatile session snapshots rather than touching the live vessel from the Player region, keeping the UI path Folia-safe.\n- Possession sessions now snapshot vessel type and ability identity on the vessel-owned thread when acquisition commits.\n- Primary and secondary cooldown duration/remaining state is tracked explicitly in the session for UI and future feedback without cross-region entity reads.\n- Added a dedicated Evoker primary: a configurable line of real `EvokerFangs` entities owned by the possessed Evoker, with staggered native attack delays.\n- Evoker fang placement fails closed outside the currently owned Folia region instead of spawning cross-region from the vessel thread.\n- Acquired-body messages now use the ability identity cached in the session, removing a previous cross-region live-vessel read in spectator fallback UI.\n- Added `abilities.evoker-fangs` configuration without resetting existing configuration values; the existing non-destructive config migrator adds only missing keys.\n- Added English/Russian localization for status, cooldown readiness and Evoker fangs.\n- Retains the 0.3.0 mounted free-look camera, Ravager roar, 0.2.0 native abilities, permissions, concealment and crash recovery.\n\n"""
replace_exact("CHANGELOG.md", "# Changelog\n\n", "# Changelog\n\n" + change)

print("Incarnate 0.4.0 patch applied successfully")
