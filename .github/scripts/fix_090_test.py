from pathlib import Path

path = Path("src/test/java/dev/onelsey/incarnate/sense/WardenSenseMathTest.java")
text = path.read_text(encoding="utf-8")
old = '            UUID.randomUUID(), 1.0, 2.0, 3.0, "movement", "step", 42L\n'
new = '            UUID.randomUUID(), 1.0, 2.0, 3.0, "movement", "step", null, 42L\n'
if old not in text:
    raise SystemExit("WardenSenseSnapshot regression constructor marker not found")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
