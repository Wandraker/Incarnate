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
    '''                if (vessel.isInsideVehicle()) {
                    notifyPlayer(player, "[Incarnate] A mob riding another entity cannot be possessed yet.");
                    discardCreatedVesselNow(vessel, origin);
                    return;
                }

                if (byPlayer.containsKey(playerId) || byVessel.containsKey(vesselId)) {''',
    '''                if (vessel.isInsideVehicle()) {
                    notifyPlayer(player, "[Incarnate] A mob riding another entity cannot be possessed yet.");
                    discardCreatedVesselNow(vessel, origin);
                    return;
                }
                if (vesselRecovery.isMarked(vessel)) {
                    boolean recovered = vesselRecovery.recoverOrphan(vessel, removeOrphanedCreated);
                    if (recovered && vessel.isValid()) {
                        notifyPlayer(player, "[Incarnate] This mob was recovered from an interrupted session. Try possessing it again.");
                    } else {
                        notifyPlayer(player, "[Incarnate] This vessel was pending interrupted-session recovery.");
                    }
                    return;
                }

                if (byPlayer.containsKey(playerId) || byVessel.containsKey(vesselId)) {''',
    "recover marked vessel before possession",
)

replace_once(
    '''    public void recoverOrphanVessel(Mob mob) {
        if (byVessel.containsKey(mob.getUniqueId()) || !vesselRecovery.isMarked(mob)) {
            return;
        }
        mob.getScheduler().run(plugin, task -> {
            if (!byVessel.containsKey(mob.getUniqueId()) && vesselRecovery.recoverOrphan(mob, removeOrphanedCreated)) {
                plugin.getLogger().warning("Recovered orphaned Incarnate vessel " + mob.getUniqueId() + " (" + mob.getType() + ").");
            }
        }, null);
    }

    public void recoverAlreadyOnlinePlayers() {''',
    '''    public void recoverOrphanVessel(Mob mob) {
        UUID vesselId = mob.getUniqueId();
        if (byVessel.containsKey(vesselId)) {
            return;
        }

        ScheduledTask recoveryTask = mob.getScheduler().run(plugin, task -> {
            if (byVessel.containsKey(vesselId)) {
                return;
            }
            if (vesselRecovery.recoverOrphan(mob, removeOrphanedCreated)) {
                plugin.getLogger().warning("Recovered orphaned Incarnate vessel " + vesselId + " (" + mob.getType() + ").");
            }
        }, null);
        if (recoveryTask == null) {
            // Keep the durable UUID index: an unloaded/retired entity may load again later.
        }
    }

    public void recoverIndexedVessels() {
        for (UUID vesselId : vesselRecovery.indexedVessels()) {
            Entity entity = Bukkit.getEntity(vesselId);
            if (entity instanceof Mob mob) {
                recoverOrphanVessel(mob);
            }
        }
    }

    public void recoverAlreadyOnlinePlayers() {''',
    "indexed startup vessel recovery",
)

path.write_text(text)
print("PossessionManager recovery-index patch applied successfully")
