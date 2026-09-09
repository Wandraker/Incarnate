from pathlib import Path

path = Path("src/main/java/dev/onelsey/incarnate/possession/PossessionManager.java")
text = path.read_text()

start = text.index("    public void releaseOnControllerDeath(Player player) {")
end = text.index("    public void onPlayerRespawn(Player player) {", start)
block = text[start:end]
old = "        visibility.forget(session.playerId());\n"
if block.count(old) != 1:
    raise SystemExit(f"controller death visibility: expected 1 match, found {block.count(old)}")
block = block.replace(old, "", 1)
text = text[:start] + block + text[end:]

old = '''        if (!playerRecovery.hasRecovery(player)) {
            restoringPlayers.remove(playerId);
            recoveryInFlight.remove(playerId);
            return;
        }
'''
new = '''        if (!playerRecovery.hasRecovery(player)) {
            restoringPlayers.remove(playerId);
            recoveryInFlight.remove(playerId);
            if (visibility.isConcealed(playerId)) {
                visibility.reveal(playerId, player);
            }
            return;
        }
'''
if text.count(old) != 1:
    raise SystemExit(f"no-marker recovery: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)

old = "            if (player.isOnline() && Bukkit.isOwnedByCurrentRegion(player)) {"
new = "            if (Bukkit.isOwnedByCurrentRegion(player) && player.isOnline()) {"
if text.count(old) != 1:
    raise SystemExit(f"shutdown player ownership: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)

old = "            if (vessel.isValid() && Bukkit.isOwnedByCurrentRegion(vessel)) {"
new = "            if (Bukkit.isOwnedByCurrentRegion(vessel) && vessel.isValid()) {"
if text.count(old) != 1:
    raise SystemExit(f"shutdown vessel ownership: expected 1 match, found {text.count(old)}")
text = text.replace(old, new, 1)

path.write_text(text)
print("PossessionManager safety patch applied")
