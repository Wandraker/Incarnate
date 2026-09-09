from pathlib import Path

source_path = Path("tools/apply_message_layer.py")
source = source_path.read_text()

race_block = "    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 1\"),\n    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 2\"),\n    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 3\"),\n"
disappeared_block = "    ('            notifyPlayer(player, \"[Incarnate] The vessel disappeared before possession started.\");', '            notifyPlayer(player, \"vessel-disappeared\");', \"disappeared retired\"),\n    ('            notifyPlayer(player, \"[Incarnate] The vessel disappeared before possession started.\");', '            notifyPlayer(player, \"vessel-disappeared\");', \"disappeared null\"),\n"
for block, name in ((race_block, "race"), (disappeared_block, "disappeared")):
    if source.count(block) != 1:
        raise SystemExit(f"Could not locate {name} replacement block")
    source = source.replace(block, "", 1)

marker = "for old, new, label in replacements:\n    text = replace_once(text, old, new, label)\n"
injected = "race_old = '                    notifyPlayer(player, \\\"[Incarnate] Possession race was rejected safely.\\\");'\nrace_new = '                    notifyPlayer(player, \\\"race-rejected\\\");'\nif text.count(race_old) != 3:\n    raise SystemExit(f'race messages: expected 3 matches, got {text.count(race_old)}')\ntext = text.replace(race_old, race_new)\n\ndisappeared_old = '            notifyPlayer(player, \\\"[Incarnate] The vessel disappeared before possession started.\\\");'\ndisappeared_new = '            notifyPlayer(player, \\\"vessel-disappeared\\\");'\nif text.count(disappeared_old) != 2:\n    raise SystemExit(f'disappeared messages: expected 2 matches, got {text.count(disappeared_old)}')\ntext = text.replace(disappeared_old, disappeared_new)\n\n" + marker
if source.count(marker) != 1:
    raise SystemExit("Could not locate replacement loop")
source = source.replace(marker, injected, 1)

exec(compile(source, str(source_path), "exec"))
