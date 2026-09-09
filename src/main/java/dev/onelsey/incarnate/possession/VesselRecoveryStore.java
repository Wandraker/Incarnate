package dev.onelsey.incarnate.possession;

import dev.onelsey.incarnate.IncarnatePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Mob;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Vex;
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
    private final NamespacedKey sittingKey;
    private final NamespacedKey camelDashingKey;
    private final NamespacedKey batAwakeKey;
    private final NamespacedKey creeperIgnitedKey;
    private final NamespacedKey creeperFuseTicksKey;
    private final NamespacedKey pufferFishPuffStateKey;
    private final NamespacedKey vexChargingKey;
    private final NamespacedKey ravagerAttackTicksKey;
    private final NamespacedKey ravagerStunnedTicksKey;
    private final NamespacedKey ravagerRoarTicksKey;

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
        this.sittingKey = new NamespacedKey(plugin, "vessel_recovery_sitting");
        this.camelDashingKey = new NamespacedKey(plugin, "vessel_recovery_camel_dashing");
        this.batAwakeKey = new NamespacedKey(plugin, "vessel_recovery_bat_awake");
        this.creeperIgnitedKey = new NamespacedKey(plugin, "vessel_recovery_creeper_ignited");
        this.creeperFuseTicksKey = new NamespacedKey(plugin, "vessel_recovery_creeper_fuse_ticks");
        this.pufferFishPuffStateKey = new NamespacedKey(plugin, "vessel_recovery_pufferfish_puff_state");
        this.vexChargingKey = new NamespacedKey(plugin, "vessel_recovery_vex_charging");
        this.ravagerAttackTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_attack_ticks");
        this.ravagerStunnedTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_stunned_ticks");
        this.ravagerRoarTicksKey = new NamespacedKey(plugin, "vessel_recovery_ravager_roar_ticks");
    }

    public void save(Mob mob, PossessionOrigin origin, VesselState state) {
        UUID vesselId = mob.getUniqueId();
        if (!index.mark(vesselId)) {
            throw new IllegalStateException("Could not persist recovery index for vessel " + vesselId);
        }

        try {
            PersistentDataContainer data = mob.getPersistentDataContainer();
            data.remove(activeKey);
            data.set(originKey, PersistentDataType.STRING, origin.name());
            data.set(awareKey, PersistentDataType.BYTE, bool(state.aware()));
            data.set(aiKey, PersistentDataType.BYTE, bool(state.ai()));
            data.set(persistentKey, PersistentDataType.BYTE, bool(state.persistent()));
            data.set(removeFarKey, PersistentDataType.BYTE, bool(state.removeWhenFarAway()));
            data.set(aggressiveKey, PersistentDataType.BYTE, bool(state.aggressive()));
            data.set(gravityKey, PersistentDataType.BYTE, bool(state.gravity()));
            setNullableBool(data, sittingKey, state.sitting());
            setNullableBool(data, camelDashingKey, state.camelDashing());
            setNullableBool(data, batAwakeKey, state.batAwake());
            setNullableBool(data, creeperIgnitedKey, state.creeperIgnited());
            if (state.creeperFuseTicks() != null) {
                data.set(creeperFuseTicksKey, PersistentDataType.INTEGER, state.creeperFuseTicks());
            } else {
                data.remove(creeperFuseTicksKey);
            }
            if (state.pufferFishPuffState() != null) {
                data.set(pufferFishPuffStateKey, PersistentDataType.INTEGER, state.pufferFishPuffState());
            } else {
                data.remove(pufferFishPuffStateKey);
            }
            setNullableBool(data, vexChargingKey, state.vexCharging());
            setNullableInt(data, ravagerAttackTicksKey, state.ravagerAttackTicks());
            setNullableInt(data, ravagerStunnedTicksKey, state.ravagerStunnedTicks());
            setNullableInt(data, ravagerRoarTicksKey, state.ravagerRoarTicks());
            data.set(activeKey, PersistentDataType.BYTE, (byte) 1);
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

        if (mob instanceof Sittable sittable) {
            Boolean sitting = readNullableBool(data, sittingKey);
            if (sitting != null) {
                sittable.setSitting(sitting);
            }
        }
        if (mob instanceof Camel camel) {
            Boolean dashing = readNullableBool(data, camelDashingKey);
            if (dashing != null) {
                camel.setDashing(dashing);
            }
        }
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
        if (mob instanceof PufferFish pufferFish) {
            Integer puffState = data.get(pufferFishPuffStateKey, PersistentDataType.INTEGER);
            if (puffState != null) {
                pufferFish.setPuffState(Math.max(0, Math.min(2, puffState)));
            }
        }
        if (mob instanceof Vex vex) {
            Boolean charging = readNullableBool(data, vexChargingKey);
            if (charging != null) {
                vex.setCharging(charging);
            }
        }
        if (mob instanceof Ravager ravager) {
            Integer attackTicks = data.get(ravagerAttackTicksKey, PersistentDataType.INTEGER);
            Integer stunnedTicks = data.get(ravagerStunnedTicksKey, PersistentDataType.INTEGER);
            Integer roarTicks = data.get(ravagerRoarTicksKey, PersistentDataType.INTEGER);
            if (attackTicks != null) ravager.setAttackTicks(attackTicks);
            if (stunnedTicks != null) ravager.setStunnedTicks(stunnedTicks);
            if (roarTicks != null) ravager.setRoarTicks(roarTicks);
        }
        if (mob instanceof Guardian guardian) {
            guardian.setLaser(false);
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
        data.remove(sittingKey);
        data.remove(camelDashingKey);
        data.remove(batAwakeKey);
        data.remove(creeperIgnitedKey);
        data.remove(creeperFuseTicksKey);
        data.remove(pufferFishPuffStateKey);
        data.remove(vexChargingKey);
        data.remove(ravagerAttackTicksKey);
        data.remove(ravagerStunnedTicksKey);
        data.remove(ravagerRoarTicksKey);
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

    private static void setNullableInt(PersistentDataContainer data, NamespacedKey key, Integer value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.set(key, PersistentDataType.INTEGER, value);
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
