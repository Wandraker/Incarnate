package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;

public final class VesselRecoveryStore {
    private final VesselRecoveryIndex index;
    private final NamespacedKey activeKey;
    private final NamespacedKey originKey;
    private final NamespacedKey awareKey;
    private final NamespacedKey aiKey;
    private final NamespacedKey persistentKey;
    private final NamespacedKey removeFarKey;
    private final NamespacedKey aggressiveKey;
    private final NamespacedKey gravityKey;
    private final NamespacedKey batAwakeKey;
    private final NamespacedKey creeperIgnitedKey;
    private final NamespacedKey creeperFuseTicksKey;

    public VesselRecoveryStore(IncarnatePlugin plugin) {
        this.index = new VesselRecoveryIndex(plugin);
        this.activeKey = new NamespacedKey(plugin, "vessel_recovery_active");
        this.originKey = new NamespacedKey(plugin, "vessel_recovery_origin");
        this.awareKey = new NamespacedKey(plugin, "vessel_recovery_aware");
        this.aiKey = new NamespacedKey(plugin, "vessel_recovery_ai");
        this.persistentKey = new NamespacedKey(plugin, "vessel_recovery_persistent");
        this.removeFarKey = new NamespacedKey(plugin, "vessel_recovery_remove_far");
        this.aggressiveKey = new NamespacedKey(plugin, "vessel_recovery_aggressive");
        this.gravityKey = new NamespacedKey(plugin, "vessel_recovery_gravity");
        this.batAwakeKey = new NamespacedKey(plugin, "vessel_recovery_bat_awake");
        this.creeperIgnitedKey = new NamespacedKey(plugin, "vessel_recovery_creeper_ignited");
        this.creperFuseTicksKey = null;
        this.creeperFuseTicksKey = new NamespacedKey(plugin, "vessel_recovery_creeper_fuse_ticks");
    }

    public void save(Mob mob, PossessionOrigin origin, VesselState state) {
        UUID vesselId = mob.getUniqueId();
        if (!index.mark(vesselId)) {
            throw new IllegalStateException("Could not persist recovery index for vessel " + vesselId);
        }

        try {
            PersistentDataContainer data = mob.getPersistentDataContainer();
            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);
            data.set(originKey, PersistentDataType.STRING, origin.name());
            data.set(awareKey, PersistentDataType.BYTE, bool(state.aware()));
            data.set(aiKey, PersistentDataType.BYTE, bool(state.ai()));
            data.set(persistentKey, PersistentDataType.BYTE, bool(state.persistent()));
            data.set(removeFarKey, PersistentDataType.BYTE, bool(state.removeWhenFarAway()));
            data.set(aggressiveKey, PersistentDataType.BYTE, bool(state.aggressive()));
            data.set(gravityKey, PersistentDataType.BYTE, bool(state.gravity()));
            setNullableBool(data, batAwakeKey, state.batAwake());
            setNullableBool(data, creeperIgnitedKey, state.creeperIgnited());
            if (state.creeperFuseTicks() != null) {
                data.set(creeperFuseTicksKey, PersistentDataType.INTEGER, state.creeperFuseTicks());
            } else {
                data.remove(creeperFuseTicksKey);
            }
        } catch (RuntimeException ex) {
            index.forget(vesselId);
            throw ex;
        }
    }

    public Set<UUID> indexedVessels() {
        return index.snapshot();
    }

    public boolean isMarked(Mob mob) {
        Byte value = mob.getPersistentDataContainer().get(activeKey, PersistentDataType.BYTE);
        return value != null && value != 0;
    }

    public boolean recoverOrphan(Mob mob, boolean removeOrphanedCreated) {
        UUID vesselId = mob.getUniqueId();
        if (!isMarked(mob)) {
            index.forget(vesselId);
            return false;
        }

        PersistentDataContainer data = mob.getPersistentDataContainer();
        String origin = data.get(originKey, PersistentDataType.STRING);
        if (removeOrphanedCreated && PossessionOrigin.CREATED.name().equals(origin)) {
            clear(mob);
            mob.remove();
            return true;
        }

        mob.getPathfinder().stopPathfinding();
        mob.setTarget(null);
        mob.setJumping(false);
        mob.setAI(readBool(data, aiKey, true));
        mob.setAware(readBool(data, awareKey, true));
        mob.setPersistent(readBool(data, persistentKey, false));
        mob.setRemoveWhenFarAway(readBool(data, removeFarKey, true));
        mob.setAggressive(readBool(data, aggressiveKey, false));
        mob.setGravity(readBool(data, gravityKey, mob.hasGravity()));

        if (mob instanceof Bat bat) {
            Boolean awake = readNullableBool(data, batAwakeKey);
            if (awake != null) {
                bat.setAwake(awake);
                bat.setTargetLocation(null);
            }
        }

        if (mob instanceof Creeper creeper) {
            Boolean ignited = readNullableBool(data, creeperIgnitedKey);
            Integer fuseTicks = data.get(creeperFuseTicksKey, PersistentDataType.INTEGER);
            if (ignited != null && fuseTicks != null) {
                VesselState.restoreCreeper(creeper, ignited, fuseTicks);
            }
        }

        clear(mob);
        return true;
    }

    public void clear(Mob mob) {
        UUID vesselId = mob.getUniqueId();
        PersistentDataContainer data = mob.getPersistentDataContainer();
        data.remove(activeKey);
        data.remove(originKey);
        data.remove(awareKey);
        data.remove(aiKey);
        data.remove(persistentKey);
        data.remove(removeFarKey);
        data.remove(aggressiveKey);
        data.remove(gravityKey);
        data.remove(batAwakeKey);
        data.remove(creeperIgnitedKey);
        data.remove(creeperFuseTicksKey);
        index.forget(vesselId);
    }

    private static byte bool(boolean value) {
        return (byte) (value ? 1 : 0);
    }

    private static void setNullableBool(PersistentDataContainer data, NamespacedKey key, Boolean value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.set(key, PersistentDataType.BYTE, bool(value));
        }
    }

    private static boolean readBool(PersistentDataContainer data, NamespacedKey key, boolean fallback) {
        Byte value = data.get(key, PersistentDataType.BYTE);
        return value == null ? fallback : value != 0;
    }

    private static Boolean readNullableBool(PersistentDataContainer data, NamespacedKey key) {
        Byte value = data.get(key, PersistentDataType.BYTE);
        return value == null ? null : value != 0;
    }
}
