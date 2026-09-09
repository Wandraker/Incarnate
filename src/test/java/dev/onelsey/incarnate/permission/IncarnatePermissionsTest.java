package dev.onelsey.incarnate.permission;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IncarnatePermissionsTest {
    @Test
    void mobNodeUsesStableLowercaseEntityTypeName() {
        assertEquals("incarnate.mob.skeleton", IncarnatePermissions.mobNode(EntityType.SKELETON));
        assertEquals("incarnate.mob.zombie_villager", IncarnatePermissions.mobNode(EntityType.ZOMBIE_VILLAGER));
    }
}
