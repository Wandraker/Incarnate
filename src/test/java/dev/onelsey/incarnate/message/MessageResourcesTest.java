package dev.onelsey.incarnate.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageResourcesTest {
    private static final Set<String> THEME_KEYS = Set.of(
        "primary", "secondary", "accent", "text", "muted", "success", "warning", "error"
    );
    private static final Set<String> PLACEHOLDERS = Set.of(
        "entity", "distance", "mob", "vessel", "controller", "controller_uuid", "origin",
        "reason", "camera", "primary_action", "secondary_action",
        "sense_kind", "sense_direction", "sense_distance"
    );

    @Test
    void localesExposeTheSameMessageKeys() {
        YamlConfiguration english = load("src/main/resources/locales/en_US.yml");
        YamlConfiguration russian = load("src/main/resources/locales/ru_RU.yml");
        assertEquals(messageKeys(english), messageKeys(russian));
    }

    @Test
    void themeAndConfigSchemasArePresent() {
        YamlConfiguration messages = load("src/main/resources/messages.yml");
        ConfigurationSection theme = messages.getConfigurationSection("theme");
        assertNotNull(theme);
        assertTrue(theme.getKeys(false).containsAll(THEME_KEYS));
        assertEquals(1, messages.getInt("config-version"));

        YamlConfiguration config = load("src/main/resources/config.yml");
        assertEquals(11, config.getInt("config-version"));
        assertTrue(!config.contains("camera.mounted-client-game-mode"));
        assertEquals("DIRECT_ENTITY", config.getString("camera.mode"));
        assertEquals(8, config.getInt("camera.attach-retries"));
        assertTrue(!config.getBoolean("camera.fallback-to-spectator-target"));
        assertTrue(config.getBoolean("vision.warden.enabled"));
        assertEquals(12.0, config.getDouble("vision.warden.entity-hard-limit"));
        assertTrue(config.getInt("vision.warden.evaluation-interval-ticks") >= 1);
    }

    @Test
    void wardenAndGhastAbilityPresentationIsBundled() {
        for (String locale : Set.of("en_US", "ru_RU")) {
            YamlConfiguration localized = load("src/main/resources/locales/" + locale + ".yml");
            assertNotNull(localized.getString("messages.ability.ghast-fireball"));
            assertNotNull(localized.getString("messages.ability.sonic-boom"));
            assertNotNull(localized.getString("messages.hud.no-target"));
        }
    }

    @Test
    void bundledMiniMessageTemplatesParse() {
        YamlConfiguration settings = load("src/main/resources/messages.yml");
        for (String locale : Set.of("en_US", "ru_RU")) {
            YamlConfiguration localized = load("src/main/resources/locales/" + locale + ".yml");
            MessageBundle bundle = MessageBundle.load(settings, new YamlConfiguration(), localized);
            TagResolver theme = themeResolver(bundle);
            MiniMessage mini = MiniMessage.miniMessage();
            Component prefix = mini.deserialize(bundle.prefix(), theme);

            TagResolver.Builder resolver = TagResolver.builder()
                .resolver(theme)
                .resolver(TagResolver.resolver("prefix", Tag.selfClosingInserting(prefix)));
            for (String placeholder : PLACEHOLDERS) {
                resolver.resolver(Placeholder.component(placeholder, Component.text("value")));
            }
            TagResolver built = resolver.build();

            bundle.messages().forEach((key, raw) ->
                assertDoesNotThrow(() -> mini.deserialize(raw, built), locale + ": " + key)
            );
        }
    }

    private static Set<String> messageKeys(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection("messages");
        assertNotNull(section);
        Set<String> keys = new HashSet<>();
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static TagResolver themeResolver(MessageBundle bundle) {
        TagResolver.Builder builder = TagResolver.builder();
        bundle.theme().forEach((name, hex) -> {
            TextColor color = TextColor.fromHexString(hex);
            if (color != null) {
                builder.resolver(TagResolver.resolver(name, Tag.styling(color)));
            }
        });
        return builder.build();
    }

    private static YamlConfiguration load(String path) {
        return YamlConfiguration.loadConfiguration(new File(path));
    }
}
