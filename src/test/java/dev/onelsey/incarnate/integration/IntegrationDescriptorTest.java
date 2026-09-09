package dev.onelsey.incarnate.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntegrationDescriptorTest {
    @Test
    void privateTagIntegrationStaysOptionalAndEnabledByDefault() throws IOException {
        String pluginYaml = resource("/plugin.yml");
        String configYaml = resource("/config.yml");

        assertTrue(pluginYaml.contains("softdepend:\n  - Elysium_Private_Tag"));
        assertTrue(configYaml.contains("integrations:\n  elysium-private-tag:\n    enabled: true"));
        assertTrue(configYaml.contains("plugin-name: Elysium_Private_Tag"));
        assertTrue(configYaml.contains("source: Incarnate"));
    }

    private static String resource(String path) throws IOException {
        try (InputStream input = IntegrationDescriptorTest.class.getResourceAsStream(path)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
