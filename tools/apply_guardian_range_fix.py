from pathlib import Path

path = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
text = path.read_text()
old = '''            || !target.isValid()
            || target.isDead()
            || !guardian.hasLineOfSight(target)) {
'''
new = '''            || !target.isValid()
            || target.isDead()
            || !guardian.getWorld().equals(target.getWorld())
            || guardian.getLocation().distanceSquared(target.getLocation()) > guardianLaserRange * guardianLaserRange
            || !guardian.hasLineOfSight(target)) {
'''
if text.count(old) != 1:
    raise SystemExit(f"Guardian laser range guard: expected 1 match, found {text.count(old)}")
path.write_text(text.replace(old, new, 1))
print("Guardian laser range guard applied")
