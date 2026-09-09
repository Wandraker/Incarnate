from pathlib import Path

path = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = path.read_text()


def replace_once(old: str, new: str, name: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{name}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    '            player.sendMessage(Component.text("[Incarnate] Vessel acquired. Shift+F or /release to leave it."));',
    '            player.sendMessage(Component.text("[Incarnate] Vessel acquired. Left-click: primary, F: secondary, Shift+F: release."));',
    "control hint",
)

primary_method = '''    public void triggerPrimary(Player player) {
        PossessionSession session = session(player);
        if (session == null || !session.isActive()) {
            return;
        }

        Mob vessel = session.vessel();
        ScheduledTask abilityTask = vessel.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
                return;
            }
            try {
                abilities.triggerPrimary(session);
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.SEVERE, "Primary ability failed for " + vessel.getType() + " " + session.vesselId(), ex);
                notifyPlayer(player, "[Incarnate] This vessel ability failed; possession was kept active.");
            }
        }, null);
        if (abilityTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }
'''

secondary_method = primary_method + '''
    public void triggerSecondary(Player player) {
        PossessionSession session = session(player);
        if (session == null || !session.isActive()) {
            return;
        }

        Mob vessel = session.vessel();
        ScheduledTask abilityTask = vessel.getScheduler().run(plugin, task -> {
            if (!session.isActive() || !vessel.isValid() || vessel.isDead()) {
                return;
            }
            try {
                abilities.triggerSecondary(session);
            } catch (Throwable ex) {
                plugin.getLogger().log(Level.SEVERE, "Secondary ability failed for " + vessel.getType() + " " + session.vesselId(), ex);
                notifyPlayer(player, "[Incarnate] This vessel secondary ability failed; possession was kept active.");
            }
        }, null);
        if (abilityTask == null && session.isActive()) {
            requestRelease(session, ReleaseReason.VESSEL_REMOVED);
        }
    }
'''

replace_once(primary_method, secondary_method, "secondary ability dispatcher")

path.write_text(text)
print("PossessionManager secondary patch applied successfully")
