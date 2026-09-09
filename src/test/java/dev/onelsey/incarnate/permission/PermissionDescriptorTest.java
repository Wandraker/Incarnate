package dev.onelsey.incarnate.permission;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PermissionDescriptorTest {
    @Test
    void gameplayPermissionsStayDefaultDenyAndReleaseStaysUnrestricted() throws IOException {
        try (InputStream input = PermissionDescriptorTest.class.getResourceAsStream("/plugin.yml")) {
            assertNotNull(input);
            String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            assertDefaultFalse(yaml, "incarnate.use.incarnate");
            assertDefaultFalse(yaml, "incarnate.use.possess");
            assertDefaultFalse(yaml, "incarnate.mob.*");
            assertFalse(yaml.contains("incarnate.use.release"));
        }
    }

    private static void assertDefaultFalse(String yaml, String permission) {
        String quoted = Pattern.quote(permission);
        Pattern pattern = Pattern.compile("(?m)^  " + quoted + ":\\R(?:    .*\\R)*?    default: false$");
        assertTrue(pattern.matcher(yaml).find(), () -> permission + " must remain default: false");
    }
}
