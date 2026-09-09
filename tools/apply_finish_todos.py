from pathlib import Path

ability_path = Path("src/main/java/dev/onelsey/incarnate/ability/AbilityRegistry.java")
ability = ability_path.read_text()

old = "import org.bukkit.FluidCollisionMode;\n"
new = "import org.bukkit.Bukkit;\nimport org.bukkit.FluidCollisionMode;\n"
if ability.count(old) != 1:
    raise SystemExit(f"AbilityRegistry Bukkit import: expected 1 match, found {ability.count(old)}")
ability = ability.replace(old, new, 1)

old = '''            entity -> entity instanceof LivingEntity
                && !entity.getUniqueId().equals(vessel.getUniqueId())
                && (!(entity instanceof Player player) || !player.getUniqueId().equals(session.playerId()))
'''
new = '''            entity -> Bukkit.isOwnedByCurrentRegion(entity)
                && entity instanceof LivingEntity
                && !entity.getUniqueId().equals(vessel.getUniqueId())
                && (!(entity instanceof Player player) || !player.getUniqueId().equals(session.playerId()))
'''
if ability.count(old) != 1:
    raise SystemExit(f"AbilityRegistry owned target filter: expected 1 match, found {ability.count(old)}")
ability = ability.replace(old, new, 1)
ability_path.write_text(ability)

manager_path = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
manager = manager_path.read_text()
start = manager.index("    public void shutdown() {")
block = manager[start:]
old = "            visibility.forget(session.playerId());\n"
new = "            visibility.detachForDisable(session.playerId());\n"
if block.count(old) != 1:
    raise SystemExit(f"shutdown visibility detach: expected 1 match, found {block.count(old)}")
block = block.replace(old, new, 1)
manager = manager[:start] + block
manager_path.write_text(manager)

print("Finished pending Folia target and reload visibility fixes")
