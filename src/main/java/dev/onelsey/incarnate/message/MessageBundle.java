package dev.onelsey.incarnate.message;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public record MessageBundle(Map<String, String> theme, String prefix, Map<String, String> messages) {
    public MessageBundle {
        theme = Map.copyOf(theme);
        messages = Map.copyOf(messages);
    }

    public static MessageBundle load(YamlConfiguration settingsDefaults, YamlConfiguration custom, YamlConfiguration localeDefaults) {
        Map<String, String> theme = readStringSection(settingsDefaults, "theme");
        theme.putAll(readStringSection(custom, "theme"));

        Map<String, String> messages = readStringSection(localeDefaults, "messages");
        messages.putAll(readStringSection(custom, "messages"));

        String defaultPrefix = settingsDefaults.getString("prefix", "<bold>INCARNATE</bold>");
        String prefix = custom.getString("prefix", defaultPrefix);
        return new MessageBundle(theme, prefix, messages);
    }

    private static Map<String, String> readStringSection(YamlConfiguration yaml, String path) {
        Map<String, String> result = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            Object value = section.get(key);
            if (value != null) {
                result.put(key, String.valueOf(value));
            }
        }
        return result;
    }

    public String message(String key) {
        return messages.get(key);
    }
}
