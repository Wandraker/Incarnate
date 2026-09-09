# Changelog

## 0.1.0

First source release of Incarnate.

- True possession of real server Mob entities.
- `/incarnate <mob>`, `/possess`, `/release`, primary and secondary ability fallback commands.
- Left-click primary ability, Swap Offhand secondary ability, and Shift + Swap Offhand release path (normally F and Shift+F).
- Admin-only `/incarnate inspect` with `incarnate.admin.inspect`; controller identity is not exposed visually on the Mob.
- Spectator-target camera protection and Shift latch.
- Controller concealment switched to a hide/show tracking pulse plus cancellable Paper tracking events, avoiding a long-lived Incarnate `hidePlayer` layer.
- Optional per-viewer controller removal from the tab list.
- Folia-oriented split between Player EntityScheduler input sampling and Mob EntityScheduler mutation.
- Ground, flight, aquatic, amphibious, hopping, climbing and cube/slime movement controllers.
- Native `MOVEMENT_SPEED` / `JUMP_STRENGTH` used for ordinary ground bodies.
- Native `FLYING_SPEED` used for flying bodies when present, with configurable clamps and fallback speed.
- Rabbit and dry-land Frog movement changed to hopping behavior; Frog remains aquatic in water.
- Existing Mob AI/awareness/persistence/despawn/aggressive/gravity and special state restored on release.
- Safe restoration of a pre-possession Mob target when the target remains alive and belongs to the same Folia-owned region.
- PDC recovery records for interrupted Player and vessel sessions.
- Durable vessel UUID recovery index so already-loaded bodies can be recovered after restart/reload.
- Player recovery stores world/location/rotation and remains pending if the saved world is temporarily unavailable.
- Orphan created-vessel cleanup on entity load/startup recovery.
- Fixed first-use primary cooldown overflow from development builds.
- Separate secondary ability cooldown channel.
- Real-body projectile abilities for skeleton family, Blaze, Ghast, Wither, Snow Golem, Llama and Breeze.
- Real Creeper fuse control.
- Enderman secondary directional teleport with collision/safe-landing checks.
- Spider / Cave Spider secondary pounce.
- Generic real-Mob melee fallback through `LivingEntity.attack` for bodies without a dedicated primary projectile.
- Damage knockback is preserved instead of being immediately erased by movement input/damping.
- Safer release/recovery state machine prevents a new possession from racing an unfinished previous release.
- Retired Folia scheduler paths defer recovery outside critical retired callbacks.
- Plugin-disable path does not schedule tasks after the JavaPlugin is already disabled.
- Ender Dragon and Shulker explicitly excluded.
- Paper API pinned to `26.2.build.121-stable`, Java 25.
- GitHub CI performs Java 25 compilation and a real Paper 26.2 startup/graceful-shutdown smoke test without publishing the temporary JAR.

## 0.1.0-dev.2

- Added initial real Mob movement, camera guard, Shift+F latch, controller hiding and Skeleton projectile prototype.

## 0.1.0-dev.1

- Initial possession architecture and source project.
