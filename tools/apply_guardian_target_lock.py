from pathlib import Path

p = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionSession.java")
text = p.read_text()
old = '''    private volatile boolean guardianLaserActive;
    private volatile long guardianLaserDeadlineTick = Long.MIN_VALUE;
    private volatile long vexChargeUntilTick = Long.MIN_VALUE;
'''
new = '''    private volatile boolean guardianLaserActive;
    private volatile boolean guardianLaserSeenActive;
    private volatile UUID guardianLaserTargetId;
    private volatile long guardianLaserDeadlineTick = Long.MIN_VALUE;
    private volatile long vexChargeUntilTick = Long.MIN_VALUE;
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian target fields: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
old = '''    public void startGuardianLaser(int timeoutTicks) {
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
'''
new = '''    public void startGuardianLaser(UUID targetId, int timeoutTicks) {
        guardianLaserActive = true;
        guardianLaserSeenActive = false;
        guardianLaserTargetId = targetId;
        guardianLaserDeadlineTick = controlTick + Math.max(1, timeoutTicks);
    }

    public UUID guardianLaserTargetId() { return guardianLaserTargetId; }
    public boolean guardianLaserSeenActive() { return guardianLaserSeenActive; }
    public void markGuardianLaserSeenActive() { guardianLaserSeenActive = true; }

    public boolean guardianLaserExpired() {
        return guardianLaserActive && guardianLaserDeadlineTick != Long.MIN_VALUE && controlTick > guardianLaserDeadlineTick;
    }

    public void clearGuardianLaser() {
        guardianLaserActive = false;
        guardianLaserSeenActive = false;
        guardianLaserTargetId = null;
        guardianLaserDeadlineTick = Long.MIN_VALUE;
    }
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian target methods: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
p.write_text(text)

p = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = p.read_text()
old = "        session.startGuardianLaser(timeoutTicks);\n"
new = "        session.startGuardianLaser(target.getUniqueId(), timeoutTicks);\n"
if text.count(old) != 1:
    raise SystemExit(f"Guardian start target: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
old = '''        if (target == null) {
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
'''
new = '''        if (target == null) {
            finishGuardianLaser(session, guardian);
            return;
        }

        if (session.guardianLaserExpired()
            || !Bukkit.isOwnedByCurrentRegion(target)
            || session.guardianLaserTargetId() == null
            || !target.getUniqueId().equals(session.guardianLaserTargetId())
            || !target.isValid()
            || target.isDead()
            || !guardian.getWorld().equals(target.getWorld())
            || guardian.getLocation().distanceSquared(target.getLocation()) > guardianLaserRange * guardianLaserRange
            || !guardian.hasLineOfSight(target)) {
            finishGuardianLaser(session, guardian);
            return;
        }

        if (guardian.hasLaser()) {
            session.markGuardianLaserSeenActive();
        } else if (session.guardianLaserSeenActive()) {
            finishGuardianLaser(session, guardian);
            return;
        }

        guardian.setAware(true);
        guardian.getPathfinder().stopPathfinding();
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian tick state machine: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)
marker = "    private boolean togglePufferFish(PossessionSession session, PufferFish pufferFish) {"
helper = '''    private static void finishGuardianLaser(PossessionSession session, Guardian guardian) {
        guardian.setLaser(false);
        guardian.setTarget(null);
        guardian.setAware(false);
        session.clearGuardianLaser();
    }

'''
if text.count(marker) != 1:
    raise SystemExit(f"Guardian finish helper marker: expected 1 match, found {text.count(marker)}")
text = text.replace(marker, helper + marker, 1)
p.write_text(text)

print("Guardian target lock and completion detection applied")
