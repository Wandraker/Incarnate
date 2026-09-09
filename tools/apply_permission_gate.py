from pathlib import Path

path = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = path.read_text()

old = "import dev.onelsey.incarnate.movement.VesselController;\n"
new = "import dev.onelsey.incarnate.movement.VesselController;\nimport dev.onelsey.incarnate.permission.IncarnatePermissions;\n"
if text.count(old) != 1:
    raise SystemExit(f"permission import: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)

old = '''    public void begin(Player player, Mob vessel, PossessionOrigin origin) {
        UUID playerId = player.getUniqueId();
        UUID vesselId = vessel.getUniqueId();

        if (!player.isOnline() || player.isDead()) {
'''
new = '''    public void begin(Player player, Mob vessel, PossessionOrigin origin) {
        UUID playerId = player.getUniqueId();
        UUID vesselId = vessel.getUniqueId();
        EntityType vesselType = vessel.getType();

        if (origin == PossessionOrigin.CREATED && !player.hasPermission(IncarnatePermissions.CREATE)) {
            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have permission to create vessels.");
            return;
        }
        if (origin == PossessionOrigin.EXISTING && !player.hasPermission(IncarnatePermissions.POSSESS)) {
            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have permission to possess existing mobs.");
            return;
        }
        if (!IncarnatePermissions.canUseMob(player, vesselType)) {
            rejectBeforeStart(player, vessel, origin, "[Incarnate] You do not have access to the " + vesselType + " vessel.");
            return;
        }
        if (!player.isOnline() || player.isDead()) {
'''
if text.count(old) != 1:
    raise SystemExit(f"begin permission gate: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)

path.write_text(text)
print("Central possession permission gate applied")
