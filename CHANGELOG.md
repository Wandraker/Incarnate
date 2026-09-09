# Changelog

## 0.1.0

First source release of Incarnate.

- True possession of real server Mob entities.
- `/incarnate <mob>`, `/possess`, `/release`, and primary ability fallback command.
- Shift + Swap Offhand release path, normally Shift+F.
- Spectator-target camera protection and Shift latch.
- Player controller hidden from world tracking and optionally from tab list.
- Folia-oriented split between Player EntityScheduler input sampling and Mob EntityScheduler mutation.
- Ground, flight, aquatic, amphibious, climbing and cube/slime movement controllers.
- Native entity movement attributes used for ordinary ground bodies.
- Existing Mob AI/awareness/persistence/despawn/aggressive state restored on release.
- PDC recovery records for interrupted Player and vessel sessions.
- Orphan created-vessel cleanup on entity load.
- Fixed first-use primary cooldown overflow from development builds.
- Real-body projectile abilities for skeleton family, Blaze, Ghast, Wither, Snow Golem, Llama and Breeze.
- Real Creeper fuse control.
- Generic real-Mob melee fallback through `LivingEntity.attack` for bodies without a dedicated primary projectile.
- Ender Dragon and Shulker explicitly excluded.
- Paper API pinned to 26.2.build.121-stable, Java 25.

## 0.1.0-dev.2

- Added initial real Mob movement, camera guard, Shift+F latch, controller hiding and Skeleton projectile prototype.

## 0.1.0-dev.1

- Initial possession architecture and source project.
