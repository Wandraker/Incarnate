# Changelog

## 0.2.0

Native Abilities Update.

- Added real Guardian / Elder Guardian laser attacks using the Paper Guardian API. The laser charges over native laser ticks and lets the Guardian API apply the actual hit rather than calling manual damage.
- Guardian laser targets are validated on the vessel-owned Folia region and the attack cancels safely if the target dies, crosses regions, or leaves line of sight.
- Added PufferFish secondary puff toggle using the real puff state, with normal and crash recovery of the original puff state.
- Added Vex secondary charge using the real charging state plus a 3D physical impulse and movement-control lock; original charging state is recovered.
- Added active-ability ticking on the vessel EntityScheduler, separate from movement-controller failure handling.
- Possession acquisition now tells the controller the actual primary and secondary actions available for that body.
- Guardian pre-possession laser state is restored on normal release when its original target is still safely restorable; interrupted recovery cancels orphaned laser state rather than guessing a target.
- CI smoke tests now derive the plugin version from plugin.yml/JAR metadata instead of hardcoding the previous release number.
- Retains default-deny create/possess/per-mob permissions and unrestricted emergency `/release`.

## 0.1.0

First source release of Incarnate.

- True possession of real server Mob entities.
- `/incarnate <mob>`, `/possess`, `/release`, primary and secondary ability fallback commands.
- Left-click primary ability, Swap Offhand secondary ability, and Shift + Swap Offhand release path (normally F and Shift+F).
- Gameplay access is opt-in: `incarnate.use.incarnate` and `incarnate.use.possess` are `default: false`.
- Added per-mob access nodes in the form `incarnate.mob.<entity_type>` and the explicit `incarnate.mob.*` all-supported-mobs wildcard.
- Per-mob permission checks apply to both created incarnations and possession of existing mobs and are enforced again in the possession core.
- `/incarnate` tab completion only exposes mob forms the player is actually allowed to use.
- `/release` intentionally requires no permission so a permission/rank change cannot trap a player in a vessel or block recovery.
- Admin-only `/incarnate inspect` with `incarnate.admin.inspect`; controller identity is not exposed visually on the Mob.
- Spectator-target camera protection and Shift latch.
- Controller concealment switched to a hide/show tracking pulse plus cancellable Paper tracking events, avoiding a long-lived Incarnate `hidePlayer` layer.
- Optional per-viewer controller removal from the tab list.
- Durable per-viewer TAB ownership recovery so hot reload can restore entries removed by the previous Incarnate instance.
- Folia-oriented split between Player EntityScheduler input sampling and Mob EntityScheduler mutation.
- Entity ray-trace consumers fail closed on current Folia ownership before reading/acting on hit entities.
- Ground, flight, aquatic, amphibious, hopping, climbing and cube/slime movement controllers.
- Native `MOVEMENT_SPEED` / `JUMP_STRENGTH` used for ordinary ground bodies.
- Native `FLYING_SPEED` used for flying bodies when present, with configurable clamps and fallback speed.
- Rabbit and dry-land Frog movement changed to hopping behavior; Frog remains aquatic in water.
- Burst movement uses a short control lock so ability impulses are not overwritten on the next control tick.
- Existing Mob AI/awareness/persistence/despawn/aggressive/gravity and special state restored on release.
- Safe restoration of a pre-possession Mob target when the target remains alive and belongs to the same Folia-owned region; ownership is checked before live target state is read.
- Sittable Mob pose state, Bat awake state, Camel dash state and Creeper ignition/fuse progress are preserved and recovered.
- PDC recovery records for interrupted Player and vessel sessions use commit markers written last to avoid accepting partial snapshots.
- Durable vessel UUID recovery index so already-loaded bodies can be recovered after restart/reload.
- Player recovery stores world/location/rotation and remains pending if the saved world is temporarily unavailable.
- Orphan created-vessel cleanup on entity load/startup recovery.
- Fixed first-use primary cooldown overflow from development builds.
- Separate secondary ability cooldown channel.
- Real-body projectile abilities for skeleton family, Blaze, Ghast, Wither, Snow Golem, Llama and Breeze.
- Native Paper `RangedEntity` attacks for crossbow Pillager/Piglin, trident Drowned, bow Illusioner and Witch when a living target is under the vessel crosshair.
- Real Creeper fuse control.
- Enderman secondary directional teleport with collision/safe-landing checks.
- Spider / Cave Spider secondary pounce with preserved physical impulse.
- Camel secondary dash using the real dashing state plus a physical forward impulse.
- Generic real-Mob melee fallback through `LivingEntity.attack` for bodies without a dedicated primary projectile.
- Damage knockback is preserved instead of being immediately erased by movement input/damping.
- Safer release/recovery state machine prevents a new possession from racing an unfinished previous release.
- Retired Folia scheduler paths defer recovery outside critical retired callbacks.
- Plugin-disable path does not schedule tasks after the JavaPlugin is already disabled.
- Ender Dragon and Shulker explicitly excluded.
- Paper API pinned to `26.2.build.121-stable`, Java 25.
- GitHub CI now gates changes with unit tests, Java 25 compilation and a real Paper 26.2 startup/graceful-shutdown smoke test without publishing the temporary JAR.

## 0.1.0-dev.2

- Added initial real Mob movement, camera guard, Shift+F latch, controller hiding and Skeleton projectile prototype.

## 0.1.0-dev.1

- Initial possession architecture and source project.
