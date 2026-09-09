from pathlib import Path

# Session gets a bounded native Guardian attack window.
p = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java")
text = p.read_text()
old = "    private volatile boolean guardianLaserActive;\n    private volatile long vexChargeUntilTick = Long.MIN_VALUE;"
new = "    private volatile boolean guardianLaserActive;\n    private volatile long guardianLaserDeadlineTick = Long.MIN_VALUE;\n    private volatile long vexChargeUntilTick = Long.MIN_VALUE;"
if text.count(old) != 1:
    raise SystemExit(f"Guardian deadline field: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
old = '''    public boolean guardianLaserActive() { return guardianLaserActive; }
    public void guardianLaserActive(boolean active) { this.guardianLaserActive = active; }

    public void startVexCharge(int ticks) {
'''
new = '''    public boolean guardianLaserActive() { return guardianLaserActive; }

    public void startGuardianLaser(int timeoutTicks) {
        guardianLaserActive = true;
        guardianLaserDeadlineTick = controlTick + Math.max(1, timeoutTicks);
    }

    public boolean guardianLaserExpired() {
        return guardianLaserActive && guardianLaserDeadlineTick != Long.MIN_VALUE && controlTick > guardianLaserDeadlineTick;
    }

    public void clearGuardianLaser() {
        guardianLaserActive = false;
        guardianLaserDeadlineTick = Long.MIN_VALUE;
    }

    public void startVexCharge(int ticks) {
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian session methods: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
p.write_text(text)

# Ability logic lets the vanilla GuardianAttackGoal tick during a tightly bounded attack window.
p = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = p.read_text()
old = '''        if (vessel instanceof Guardian guardian && session.guardianLaserActive()) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            session.guardianLaserActive(false);
        }
'''
new = '''        if (vessel instanceof Guardian guardian && session.guardianLaserActive()) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            guardian.setAware(false);
            session.clearGuardianLaser();
        }
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian abort block: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
start = text.index("    private boolean startGuardianLaser(PossessionSession session, Guardian guardian) {")
end = text.index("    private boolean togglePufferFish", start)
old_block = text[start:end]
new_block = '''    private boolean startGuardianLaser(PossessionSession session, Guardian guardian) {
        if (session.guardianLaserActive()) {
            return false;
        }
        LivingEntity target = findLivingTarget(session, guardian, guardianLaserRange, guardianLaserRaySize);
        if (target == null || !guardian.hasLineOfSight(target)) {
            return false;
        }
        if (!guardian.getWorld().equals(target.getWorld())) {
            return false;
        }
        double distanceSquared = guardian.getLocation().distanceSquared(target.getLocation());
        if (distanceSquared <= 9.0) {
            return melee(session, guardian);
        }

        int timeoutTicks = Math.max(20, guardian.getLaserDuration() + 40);
        int cooldown = Math.max(guardianLaserCooldownTicks, timeoutTicks);
        if (!session.acquirePrimaryCooldown(cooldown)) {
            return false;
        }

        guardian.setLaser(false);
        guardian.setTarget(target);
        guardian.setAware(true);
        guardian.getPathfinder().stopPathfinding();
        guardian.setVelocity(new Vector());
        session.startGuardianLaser(timeoutTicks);
        session.lockMovementControl(timeoutTicks);
        return true;
    }

    private void tickGuardianLaser(PossessionSession session, Guardian guardian) {
        if (!session.guardianLaserActive()) {
            return;
        }

        LivingEntity target = guardian.getTarget();
        if (target == null) {
            guardian.setLaser(false);
            guardian.setAware(false);
            session.clearGuardianLaser();
            return;
        }

        if (session.guardianLaserExpired()
            || !Bukkit.isOwnedByCurrentRegion(target)
            || !target.isValid()
            || target.isDead()
            || !guardian.getWorld().equals(target.getWorld())
            || guardian.getLocation().distanceSquared(target.getLocation()) > guardianLaserRange * guardianLaserRange
            || !guardian.hasLineOfSight(target)) {
            guardian.setLaser(false);
            guardian.setTarget(null);
            guardian.setAware(false);
            session.clearGuardianLaser();
            return;
        }

        guardian.setAware(true);
        guardian.getPathfinder().stopPathfinding();
    }

'''
text = text[:start] + new_block + text[end:]
p.write_text(text)

# Documentation describes the actual native-goal behavior rather than manual attack-time advancement.
p = Path("CHANGELOG.md")
text = p.read_text()
old = "- Added real Guardian / Elder Guardian laser attacks using the Paper Guardian API. The laser charges over native laser ticks and lets the Guardian API apply the actual hit rather than calling manual damage."
new = "- Added real Guardian / Elder Guardian laser attacks. Incarnate temporarily enables awareness only for the bounded laser window so the vanilla GuardianAttackGoal performs its own charge and native magic + attack damage; possession returns the body to unaware immediately afterward."
if text.count(old) != 1:
    raise SystemExit(f"CHANGELOG Guardian line: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
p.write_text(text)

p = Path("README.md")
text = p.read_text()
old = "- Guardian / Elder Guardian: a real charging Guardian laser, advanced tick-by-tick through the Guardian API until native damage is applied;"
new = "- Guardian / Elder Guardian: a real charging Guardian laser; Incarnate temporarily allows the vanilla Guardian attack goal to tick under a locked target/movement window so vanilla Guardian damage is applied;"
if text.count(old) != 1:
    raise SystemExit(f"README Guardian bullet: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
old = "0.1.0 keeps the real Mob AI flag enabled but temporarily sets the body to unaware while it is player-controlled. This is currently the safest public-API way to suppress autonomous pathfinding without making the entity immobile. Paper notes that unaware mobs can also have some unspecified autonomous/environmental behavior disabled, so live gameplay testing remains required before the first public release. Incarnate restores the original awareness state on release and interrupted-session recovery."
new = "0.2.0 keeps the real Mob AI flag enabled but normally sets the body to unaware while it is player-controlled. This is currently the safest public-API way to suppress autonomous pathfinding without making the entity immobile. Paper notes that unaware mobs can also have some unspecified autonomous/environmental behavior disabled, so live gameplay testing remains required before the first public release. A narrowly scoped exception exists for abilities that require their vanilla goal to tick: Guardian laser temporarily enables awareness only for its bounded attack window while movement and target are controlled, then returns the body to unaware. Incarnate restores the original awareness state on release and interrupted-session recovery."
if text.count(old) != 1:
    raise SystemExit(f"README AI note: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
p.write_text(text)

print("Native Guardian laser correction applied")
