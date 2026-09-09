from pathlib import Path


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, got {count}")
    return text.replace(old, new, 1)

p = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = p.read_text()

replacements = [
    ("import dev.onelsey.incarnate.movement.ControllerRegistry;\n", "import dev.onelsey.incarnate.message.MessageService;\nimport dev.onelsey.incarnate.movement.ControllerRegistry;\n", "message import"),
    ("    private final PossessionVisibilityManager visibility;\n", "    private final PossessionVisibilityManager visibility;\n    private final MessageService messages;\n", "message field"),
    ("        PossessionVisibilityManager visibility,\n        Set<EntityType> excluded\n", "        PossessionVisibilityManager visibility,\n        MessageService messages,\n        Set<EntityType> excluded\n", "constructor parameter"),
    ("        this.visibility = visibility;\n        this.excluded = Set.copyOf(excluded);\n", "        this.visibility = visibility;\n        this.messages = messages;\n        this.excluded = Set.copyOf(excluded);\n", "constructor assignment"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have permission to create vessels.");', '            rejectBeforeStart(player, vessel, origin, "permission-create");', "create permission"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have permission to possess existing mobs.");', '            rejectBeforeStart(player, vessel, origin, "permission-possess");', "possess permission"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have access to the " + vesselType + " vessel.");', '            rejectBeforeStart(player, vessel, origin, "mob-access-denied", Map.of("entity", Component.text(vesselType.name().toLowerCase(java.util.Locale.ROOT))));', "mob permission"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] You cannot start possession in your current state.");', '            rejectBeforeStart(player, vessel, origin, "start-state-invalid");', "invalid state"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] Leave your current vehicle before possessing a mob.");', '            rejectBeforeStart(player, vessel, origin, "leave-vehicle");', "vehicle"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] You are already possessing a vessel.");', '            rejectBeforeStart(player, vessel, origin, "already-possessing");', "already possessing"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] Your previous vessel release is still being recovered.");', '            rejectBeforeStart(player, vessel, origin, "previous-recovery-pending");', "previous recovery"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] This entity type is excluded.");', '            rejectBeforeStart(player, vessel, origin, "entity-excluded");', "excluded"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] That vessel is already controlled.");', '            rejectBeforeStart(player, vessel, origin, "vessel-already-controlled");', "controlled"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] A possession request is already starting for you.");', '            rejectBeforeStart(player, vessel, origin, "request-already-starting");', "request pending"),
    ('            rejectBeforeStart(player, vessel, origin, "[Incarnate] That vessel is already being acquired.");', '            rejectBeforeStart(player, vessel, origin, "vessel-already-acquiring");', "vessel acquiring"),
    ('            player.sendMessage(Component.text("[Incarnate] Failed to capture your current state safely."));', '            messages.send(player, "capture-failed");', "capture failure"),
    ('                    notifyPlayer(player, "[Incarnate] The vessel is no longer valid.");', '                    notifyPlayer(player, "vessel-invalid");', "vessel invalid"),
    ('                    notifyPlayer(player, "[Incarnate] A mob riding another entity cannot be possessed yet.");', '                    notifyPlayer(player, "vessel-riding");', "vessel riding"),
    ('                        notifyPlayer(player, "[Incarnate] This mob was recovered from an interrupted session. Try possessing it again.");', '                        notifyPlayer(player, "vessel-recovered-retry");', "recovered retry"),
    ('                        notifyPlayer(player, "[Incarnate] This vessel was pending interrupted-session recovery.");', '                        notifyPlayer(player, "vessel-recovery-pending");', "vessel recovery pending"),
    ('                    notifyPlayer(player, "[Incarnate] Possession race was rejected safely.");', '                    notifyPlayer(player, "race-rejected");', "race 1"),
    ('                    notifyPlayer(player, "[Incarnate] Possession race was rejected safely.");', '                    notifyPlayer(player, "race-rejected");', "race 2"),
    ('                    notifyPlayer(player, "[Incarnate] Possession race was rejected safely.");', '                    notifyPlayer(player, "race-rejected");', "race 3"),
    ('                    notifyPlayer(player, "[Incarnate] Possession failed safely due to an internal error.");', '                    notifyPlayer(player, "possession-internal-error");', "internal failure"),
    ('            notifyPlayer(player, "[Incarnate] The vessel disappeared before possession started.");', '            notifyPlayer(player, "vessel-disappeared");', "disappeared retired"),
    ('            notifyPlayer(player, "[Incarnate] The vessel disappeared before possession started.");', '            notifyPlayer(player, "vessel-disappeared");', "disappeared null"),
    ('            handleMountedCameraFailure(session, "No vessel position was available for the free-look camera.");', '            handleMountedCameraFailure(session, "camera-reason.no-position");', "camera no position"),
    ('                    handleMountedCameraFailure(session, "Could not move the controller camera to the vessel safely.");', '                    handleMountedCameraFailure(session, "camera-reason.move-failed");', "camera move"),
    ('                handleMountedCameraFailure(session, "The controller entered another vehicle while the camera was attaching.");', '                handleMountedCameraFailure(session, "camera-reason.other-vehicle");', "camera vehicle"),
    ('            sendAcquiredMessage(session, "mounted free-look");', '            sendAcquiredMessage(session, "mounted-free-look");', "mounted label"),
    ('            handleMountedCameraFailure(session, "The controller and vessel could not be joined on a safe entity region.");', '            handleMountedCameraFailure(session, "camera-reason.region-join-failed");', "camera region"),
    ('    private void handleMountedCameraFailure(PossessionSession session, String reason) {\n', '    private void handleMountedCameraFailure(PossessionSession session, String reasonKey) {\n', "camera reason signature"),
    ('            notifyPlayer(session.player(), "[Incarnate] Free-look camera fallback: " + reason);\n            attachSpectatorTargetCamera(session, true);', '            notifyPlayer(session.player(), "camera-fallback", Map.of("reason", messages.render(session.player(), reasonKey)));\n            attachSpectatorTargetCamera(session, true);', "camera fallback"),
    ('            notifyPlayer(session.player(), "[Incarnate] Could not attach the free-look camera safely.");', '            notifyPlayer(session.player(), "camera-attach-failed");', "camera attach failed"),
    ('            sendAcquiredMessage(session, fallback ? "legacy spectator fallback" : "spectator target");', '            sendAcquiredMessage(session, fallback ? "legacy-spectator-fallback" : "spectator-target");', "spectator label"),
    ('    private void sendAcquiredMessage(PossessionSession session, String cameraLabel) {\n        Mob vessel = session.vessel();\n        notifyPlayer(session.player(), "[Incarnate] Vessel acquired. Camera: " + cameraLabel + ". Left-click: "\n            + abilities.primaryLabel(vessel) + ", F: " + abilities.secondaryLabel(vessel) + ", Shift+F: release.");\n    }', '    private void sendAcquiredMessage(PossessionSession session, String cameraKey) {\n        Player player = session.player();\n        Mob vessel = session.vessel();\n        notifyPlayer(player, "acquired", Map.of(\n            "camera", messages.cameraLabel(player, cameraKey),\n            "primary_action", messages.abilityLabel(player, abilities.primaryLabel(vessel)),\n            "secondary_action", messages.abilityLabel(player, abilities.secondaryLabel(vessel))\n        ));\n    }', "acquired message"),
    ('                notifyPlayer(player, "[Incarnate] This vessel ability failed; possession was kept active.");', '                notifyPlayer(player, "ability-primary-failed");', "primary error"),
    ('                notifyPlayer(player, "[Incarnate] This vessel secondary ability failed; possession was kept active.");', '                notifyPlayer(player, "ability-secondary-failed");', "secondary error"),
    ('                            player.sendMessage(Component.text("[Incarnate] Released from vessel."));', '                            messages.send(player, "released");', "released"),
    ('                        player.sendMessage(Component.text("[Incarnate] Release teleport failed; recovery state was kept for safety."));', '                        messages.send(player, "release-teleport-failed");', "release teleport"),
    ('                notifyPlayer(player, "[Incarnate] Recovered from an interrupted possession session.");', '                notifyPlayer(player, "recovered");', "recovered player"),
    ('                notifyPlayer(player, "[Incarnate] Recovery is still pending; new possession is blocked for safety.");', '                notifyPlayer(player, "recovery-pending");', "recovery pending player"),
]

for old, new, label in replacements:
    text = replace_once(text, old, new, label)

old_helpers = '''    private void notifyPlayer(Player player, String message) {\n        player.getScheduler().run(plugin, task -> {\n            if (player.isOnline()) {\n                player.sendMessage(Component.text(message));\n            }\n        }, null);\n    }\n\n    private void rejectBeforeStart(Player player, Mob vessel, PossessionOrigin origin, String message) {\n        player.sendMessage(Component.text(message));\n        discardCreatedVessel(vessel, origin);\n    }\n'''
new_helpers = '''    private void notifyPlayer(Player player, String key) {\n        notifyPlayer(player, key, Map.of());\n    }\n\n    private void notifyPlayer(Player player, String key, Map<String, Component> placeholders) {\n        player.getScheduler().run(plugin, task -> {\n            if (player.isOnline()) {\n                messages.send(player, key, placeholders);\n            }\n        }, null);\n    }\n\n    private void rejectBeforeStart(Player player, Mob vessel, PossessionOrigin origin, String key) {\n        rejectBeforeStart(player, vessel, origin, key, Map.of());\n    }\n\n    private void rejectBeforeStart(Player player, Mob vessel, PossessionOrigin origin, String key, Map<String, Component> placeholders) {\n        messages.send(player, key, placeholders);\n        discardCreatedVessel(vessel, origin);\n    }\n'''
text = replace_once(text, old_helpers, new_helpers, "message helpers")
p.write_text(text)

p = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = p.read_text()
label_replacements = [
    ('return "guardian laser";', 'return "guardian-laser";'),
    ('return "arrow / melee";', 'return "arrow-melee";'),
    ('return "ranged attack / melee";', 'return "ranged-melee";'),
]
for old, new in label_replacements:
    text = text.replace(old, new)
p.write_text(text)

print("Incarnate message layer patch applied")
