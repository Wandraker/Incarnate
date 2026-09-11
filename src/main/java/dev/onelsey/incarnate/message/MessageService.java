package dev.onelsey.incarnate.message;

import dev.onelsey.incarnate.IncarnatePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MessageService {
    private static final String DEFAULT_LOCALE = "en_US";
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en_US", "ru_RU");

    private final IncarnatePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, MessageBundle> bundles = new ConcurrentHashMap<>();

    private volatile YamlConfiguration custom;
    private volatile YamlConfiguration settingsDefaults;
    private volatile String configuredLocale;
    private volatile String fallbackLocale;

    public MessageService(IncarnatePlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.isFile()) {
            plugin.saveResource("messages.yml", false);
        }

        this.settingsDefaults = loadResourceYaml("messages.yml");
        this.custom = YamlConfiguration.loadConfiguration(file);
        this.configuredLocale = normalizeConfiguredLocale(custom.getString(
            "locale",
            settingsDefaults.getString("locale", "auto")
        ));
        this.fallbackLocale = normalizeSupportedLocale(custom.getString(
            "fallback-locale",
            settingsDefaults.getString("fallback-locale", DEFAULT_LOCALE)
        ), DEFAULT_LOCALE);
        this.bundles.clear();
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(render(sender, key, Map.of()));
    }

    public void send(CommandSender sender, String key, Map<String, Component> placeholders) {
        sender.sendMessage(render(sender, key, placeholders));
    }

    public Component render(CommandSender sender, String key) {
        return render(sender, key, Map.of());
    }

    public Component render(CommandSender sender, String key, Map<String, Component> placeholders) {
        MessageBundle bundle = bundleFor(resolveLocale(sender));
        String raw = bundle.message(key);
        if (raw == null) {
            plugin.getLogger().warning("Missing Incarnate message key: " + key);
            return Component.text("[Incarnate] Missing message: " + key);
        }

        try {
            TagResolver theme = themeResolver(bundle);
            Component prefix = miniMessage.deserialize(bundle.prefix(), theme);
            TagResolver.Builder resolver = TagResolver.builder()
                .resolver(theme)
                .resolver(TagResolver.resolver("prefix", Tag.selfClosingInserting(prefix)));
            placeholders.forEach((name, component) -> resolver.resolver(Placeholder.component(name, component)));
            return miniMessage.deserialize(raw, resolver.build());
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to render Incarnate message '" + key + "': " + ex.getMessage());
            return Component.text("[Incarnate] " + key);
        }
    }

    public Component abilityLabel(CommandSender sender, String abilityKey) {
        return render(sender, "ability." + abilityKey);
    }

    public Component cameraLabel(CommandSender sender, String cameraKey) {
        String normalized = cameraKey != null && cameraKey.startsWith("camera.")
            ? cameraKey.substring("camera.".length())
            : cameraKey;
        return render(sender, "camera." + normalized);
    }

    public Component plainValue(Object value) {
        return Component.text(String.valueOf(value));
    }

    private MessageBundle bundleFor(String locale) {
        return bundles.computeIfAbsent(locale, key -> {
            YamlConfiguration localeDefaults;
            try {
                localeDefaults = loadResourceYaml("locales/" + key + ".yml");
            } catch (IllegalStateException ex) {
                plugin.getLogger().warning("Missing Incarnate locale " + key + "; using " + fallbackLocale + ".");
                localeDefaults = loadResourceYaml("locales/" + fallbackLocale + ".yml");
            }
            return MessageBundle.load(settingsDefaults, custom, localeDefaults);
        });
    }

    private String resolveLocale(CommandSender sender) {
        if (!configuredLocale.equals("auto")) {
            return configuredLocale;
        }
        if (sender instanceof Player player) {
            Locale locale = player.locale();
            String country = locale.getCountry();
            if (!country.isBlank()) {
                String exact = locale.getLanguage().toLowerCase(Locale.ROOT) + "_" + country.toUpperCase(Locale.ROOT);
                if (SUPPORTED_LOCALES.contains(exact)) {
                    return exact;
                }
            }
            if (locale.getLanguage().equalsIgnoreCase("ru")) {
                return "ru_RU";
            }
            if (locale.getLanguage().equalsIgnoreCase("en")) {
                return "en_US";
            }
        }
        return fallbackLocale;
    }

    private String normalizeConfiguredLocale(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("auto")) {
            return "auto";
        }
        return normalizeSupportedLocale(raw, DEFAULT_LOCALE);
    }

    private String normalizeSupportedLocale(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String normalized = raw.trim().replace('-', '_');
        for (String supported : SUPPORTED_LOCALES) {
            if (supported.equalsIgnoreCase(normalized)) {
                return supported;
            }
        }
        plugin.getLogger().warning("Unsupported Incarnate locale '" + raw + "'; using " + fallback + ".");
        return fallback;
    }

    private YamlConfiguration loadResourceYaml(String path) {
        try (InputStream stream = plugin.getResource(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled resource: " + path);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read bundled resource: " + path, ex);
        }
    }

    private TagResolver themeResolver(MessageBundle bundle) {
        TagResolver.Builder builder = TagResolver.builder();
        bundle.theme().forEach((name, hex) -> {
            if (!name.matches("[a-z0-9_-]+")) {
                return;
            }
            TextColor color = TextColor.fromHexString(hex);
            if (color != null) {
                builder.resolver(TagResolver.resolver(name, Tag.styling(color)));
            }
        });
        return builder.build();
    }
}
