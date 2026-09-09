from pathlib import Path

p = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = p.read_text()

replacements = [
    (
        "import dev.onelsey.incarnate.input.ViewSnapshot;\n",
        "import dev.onelsey.incarnate.input.ViewSnapshot;\nimport dev.onelsey.incarnate.integration.ElysiumPrivateTagIntegration;\n",
        "integration import",
    ),
    (
        "    private final PossessionVisibilityManager visibility;\n",
        "    private final PossessionVisibilityManager visibility;\n    private final ElysiumPrivateTagIntegration privateTags;\n",
        "integration field",
    ),
    (
        "        AbilityRegistry abilities,\n        PossessionVisibilityManager visibility,\n        Set<EntityType> excluded\n",
        "        AbilityRegistry abilities,\n        PossessionVisibilityManager visibility,\n        ElysiumPrivateTagIntegration privateTags,\n        Set<EntityType> excluded\n",
        "constructor parameter",
    ),
    (
        "        this.abilities = abilities;\n        this.visibility = visibility;\n        this.excluded = Set.copyOf(excluded);\n",
        "        this.abilities = abilities;\n        this.visibility = visibility;\n        this.privateTags = privateTags;\n        this.excluded = Set.copyOf(excluded);\n",
        "constructor assignment",
    ),
    (
        "            playerRecovery.save(player, session.playerState());\n            startInputSampler(session);\n            player.setGameMode(GameMode.SPECTATOR);\n",
        "            playerRecovery.save(player, session.playerState());\n            privateTags.suppress(player);\n            startInputSampler(session);\n            player.setGameMode(GameMode.SPECTATOR);\n",
        "suppress on possession",
    ),
    (
        "                        visibility.reveal(session.playerId(), player);\n                        if (reason != ReleaseReason.QUIT && reason != ReleaseReason.PLUGIN_DISABLE) {\n",
        "                        visibility.reveal(session.playerId(), player);\n                        privateTags.restore(player);\n                        if (reason != ReleaseReason.QUIT && reason != ReleaseReason.PLUGIN_DISABLE) {\n",
        "restore on normal release",
    ),
    (
        "            if (visibility.isConcealed(playerId)) {\n                visibility.reveal(playerId, player);\n            }\n            return;\n",
        "            if (visibility.isConcealed(playerId)) {\n                visibility.reveal(playerId, player);\n            }\n            privateTags.restore(player);\n            return;\n",
        "restore stale suppression without PDC",
    ),
    (
        "                restoringPlayers.remove(playerId);\n                visibility.reveal(playerId, player);\n                notifyPlayer(player, \"[Incarnate] Recovered from an interrupted possession session.\");\n",
        "                restoringPlayers.remove(playerId);\n                visibility.reveal(playerId, player);\n                privateTags.restore(player);\n                notifyPlayer(player, \"[Incarnate] Recovered from an interrupted possession session.\");\n",
        "restore after interrupted recovery",
    ),
    (
        "                restorePlayerState(player, session.playerState());\n            }\n\n            Mob vessel = session.vessel();\n",
        "                restorePlayerState(player, session.playerState());\n                privateTags.restore(player);\n            }\n\n            Mob vessel = session.vessel();\n",
        "best effort restore on disable",
    ),
]

for old, new, label in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, got {count}")
    text = text.replace(old, new, 1)

p.write_text(text)
print("Private Tag integration patch applied")
