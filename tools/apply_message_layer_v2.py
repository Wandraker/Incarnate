from pathlib import Path

source_path = Path("tools/apply_message_layer.py")
source = source_path.read_text()

race_tuple = "    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 1\"),\n    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 2\"),\n    ('                    notifyPlayer(player, \"[Incarnate] Possession race was rejected safely.\");', '                    notifyPlayer(player, \"race-rejected\");', \"race 3\"),\n"
if source.count(race_tuple) != 1:
    raise SystemExit("Could not locate race replacement block")
source = source.replace(race_tuple, "", 1)

marker = "for old, new, label in replacements:\n    text = replace_once(text, old, new, label)\n"
injected = "race_old = '                    notifyPlayer(player, \\\"[Incarnate] Possession race was rejected safely.\\\");'\nrace_new = '                    notifyPlayer(player, \\\"race-rejected\\\");'\nif text.count(race_old) != 3:\n    raise SystemExit(f'race messages: expected 3 matches, got {text.count(race_old)}')\ntext = text.replace(race_old, race_new)\n\n" + marker
if source.count(marker) != 1:
    raise SystemExit("Could not locate replacement loop")
source = source.replace(marker, injected, 1)

exec(compile(source, str(source_path), "exec"))
