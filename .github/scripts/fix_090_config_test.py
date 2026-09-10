from pathlib import Path

path = Path("src/test/java/dev/onelsey/incarnate/config/ConfigMigratorTest.java")
text = path.read_text(encoding="utf-8")
text = text.replace('defaults.set("config-version", 6);', 'defaults.set("config-version", 7);')
text = text.replace('user.set("config-version", 6);', 'user.set("config-version", 7);')
path.write_text(text, encoding="utf-8")
