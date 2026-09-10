# Changelog

## 0.8.0 - Body States & Mobility

- Builds directly on the 0.7.1 Dragon/control and Leaf recovery hotfixes.
- Adds reversible Axolotl `playingDead` possession state with an `F` toggle and movement suppression while playing dead.
- Adds Fox body-state capture/recovery for crouching, sleeping, interested, leaping, defending and faceplanted states.
- Adds Fox `Sprint + LMB` pounce and `F` sleep/wake controls with bounded transient state cleanup.
- Adds Panda body-state capture/recovery for rolling, sneezing and on-back states plus a controlled native roll action on `F`.
- Controlled Axolotl play-dead and Fox sleep states are now session-authoritative while possessed, preventing vanilla state drift from silently desynchronizing movement and ability behavior.
- Fox pounce and Panda roll reassert their native transient body flags for the bounded action window, so a server tick cannot prematurely erase the visible/native state while Incarnate still considers the action active.
- Deliberately leaves Panda eating untouched because its setter has additional vanilla state/item preconditions and is not treated as unconditionally reversible.
- Persists the new reversible body-state snapshots through interrupted-session vessel recovery.
- Hardens all spectator-target camera/release paths for Paper/Leaf implementations that signal invalid spectator state with either `IllegalStateException` or `IllegalArgumentException`.
- Advances additive configuration schema to 5 without replacing existing user values.


## 0.7.0

Warden Senses update.

- Added a passive Warden sensory layer driven by real Bukkit/Paper `GenericGameEvent` emissions that are present in Minecraft's `WARDEN_CAN_LISTEN` game-event tag. Incarnate does not poll nearby entities every tick and does not fabricate a player-only radar.
- Possessed Wardens keep normal player vision by default. The sensory layer augments control instead of forcing blindness or Darkness.
- Warden sensory HUD reports a localized event category, direction relative to the controller's current view, and distance. It can react to eligible movement, projectiles, combat, block activity, interactions and other Warden-listenable vibrations, not only players.
- Added configurable Warden sense range, memory duration, self-event suppression and event-kind display. The effective range is clamped by the original game-event broadcast radius.
- Added immutable vessel-position and Warden-sense snapshots so world-event capture does not need to read the live Warden from the Player HUD thread. Active Warden sessions are identified by a vessel type captured at possession start.
- Warden sensing is informational only: it does not mutate anger, disturbance location or Brain memories, so an existing Warden's autonomous state is not damaged merely to provide possession feedback.
- Added pure regression tests for Minecraft-yaw-relative eight-sector direction mapping, event categorization and sense expiry.
- Bumped gameplay config schema to 4; upgrades remain additive/non-destructive.
- This is still a live-gameplay candidate: vibration density and HUD readability should be tuned on a real server, especially around farms/redstone-heavy areas and multiple simultaneous event sources.

## 0.6.0

Native Behavior & State Fidelity update.

- Added an ability-gesture dispatch layer instead of routing every left-click through one hardcoded primary path. `PRIMARY`, `SECONDARY`, `SNEAK_PRIMARY` and `SPRINT_PRIMARY` are now distinct actions, with non-specialized gestures falling back safely to the normal primary ability.
- `Shift + left click` is enabled as a configurable gesture by default. This gives complex mobs room for additional actions without adding extra commands or client mods.
- Added Frog tongue control through the native Paper `Frog#setTongueTarget` API. Frog normal left click remains melee; `Shift + left click` attempts the native tongue action against the aimed living entity. The action is range/region validated, bounded by a timeout and cleaned up if the target disappears.
- Existing Frog tongue target state is captured for normal release. Interrupted-session recovery clears an orphaned tongue target rather than guessing a stale cross-region entity reference.
- Added Shulker secondary shell control on `F`, toggling the real `peek` state. Shulker bullet firing no longer permanently forces the shell open, and the pre-possession peek value is still restored on release/recovery.
- Added Sniffer state fidelity: the native `Sniffer.State` is captured before possession, normalized to `IDLING` while controlled, restored on normal release and persisted through interrupted-session recovery.
- Bumped gameplay config schema to 3 and added non-destructive defaults for gesture input, Frog tongue and Shulker shell control.
- Kept Warden sonic-boom manipulation out of this candidate. Public anger/disturbance APIs do not expose a complete restorable snapshot of the Warden's per-entity anger/Brain state, so Incarnate does not mutate that state merely to fake feature completeness.
- This remains a CI/smoke-test candidate until live gameplay validates Frog tongue behavior with `aware=false`, Shulker shell/bullet feel and gesture input through both mounted and spectator-target camera transports.

## 0.5.0

Complex Bodies foundation.

- Ender Dragon and Shulker are no longer hard-excluded. Both now have dedicated controllers instead of falling through to generic ground movement.
- Added a dedicated Ender Dragon flight controller. While controlled, the dragon is held in native `HOVER` phase to suppress its vanilla podium/portal autopilot, while Incarnate applies 3D input-driven movement and keeps gravity disabled.
- Ender Dragon `phase` and `podium` are captured before possession, restored for existing dragons, and persisted through interrupted-session recovery.
- A created Ender Dragon that is intentionally retained instead of removed is left in safe `HOVER` with its podium moved to its current location, preventing an Incarnate-created body from taking off toward the default podium after release/recovery.
- Added Ender Dragon primary fire using a real `DragonFireball` launched by the dragon body.
- Added a dedicated Shulker surface-crawl controller. Movement advances cell-by-cell along the current attachment surface, fails closed outside the current Folia region, and supports vertical wall crawling with jump/sneak.
- Added Shulker primary fire using a real homing `ShulkerBullet` with the possessed Shulker as projectile source and the aimed living entity as target.
- Shulker `peek` and `attachedFace` state are now captured, restored and persisted through interrupted-session recovery.
- Removed the built-in Dragon/Shulker hard deny and changed the default `excluded-types` list to empty. Server owners can still exclude either mob explicitly.
- Bumped gameplay config schema to 2 and fixed the migration engine so `config-version` actually advances after a migration.
- Schema 1 migration removes the old Dragon/Shulker exclusion only when the list is still exactly the legacy default; customized exclusion lists remain untouched.
- Added English/Russian HUD labels and configuration for Dragon movement, Dragon fireball, Shulker crawl and Shulker bullets.
- This is a CI/smoke-test candidate until live gameplay verifies Dragon movement/camera behavior and Shulker attachment movement on a real client.

## 0.4.0

Mob Identity & Control Update.

- Added a configurable possession actionbar HUD showing the real vessel health plus localized primary/secondary ability labels and live cooldown state.
- Ability cooldowns are now represented by observable session windows instead of write-only timestamps, so the HUD can show READY versus remaining seconds without changing gameplay timing.
- Vessel health/max-health telemetry is sampled on the Mob EntityScheduler and exposed to the Player HUD as a snapshot; the Player-side HUD does not read the live Mob cross-region.
- Primary and secondary capability labels are snapshotted on the vessel-owned thread when possession starts, removing another live Mob read from Player-side camera/HUD messaging.
- Added a native Evoker secondary ability: a forward line of real `EvokerFangs` entities owned by the possessed Evoker, with the real FANGS spell animation/state and configurable count, spacing, delays and cooldown.
- Evoker fang placement fails closed at Folia region ownership boundaries and searches downward for valid solid ground before spawning a fang.
- Spellcaster state is now captured before possession, cleared while controlled, restored on normal release, and persisted through interrupted-session PDC recovery.
- HUD and Evoker options are additive defaults, so existing `config.yml` tuning remains untouched by the existing non-destructive config migration.
- Added English and Russian HUD strings plus localized Evoker fang ability labels; locale parity/MiniMessage tests continue to guard both bundles.
- Added unit coverage for observable cooldown timing, including first-use availability and the cooldown boundary.
- The 0.3.0 mounted free-look camera, controller concealment, default-deny permission model, recovery architecture and native abilities remain intact.
- Goat ram is intentionally not exposed yet: Paper's public ram API mutates Goat brain memories, while Incarnate normally suppresses autonomous AI with `aware=false`; exact pre-possession brain memory restoration is not available through the public API.

## 0.3.0

Free-Look Camera & Mob Polish Update.

- Replaced the default direct spectator-target camera with a mounted free-look camera transport: the hidden spectator Player remains its own camera while riding the real Mob body, allowing normal yaw/pitch input to drive vessel look direction.
- Added configurable camera transport under `camera.mode`, with `MOUNTED` as the default and the previous `SPECTATOR_TARGET` transport available as a compatibility fallback.
- Mounted camera acquisition teleports the hidden controller to the vessel through the Player scheduler, retries region-safe mounting through the vessel EntityScheduler, and fails back or releases safely when mounting cannot complete.
- Added guarded dismount handling so ordinary sneak/dismount input cannot silently detach an active mounted controller.
- Release, quit, death and plugin-disable paths now explicitly detach mounted controllers before restoring Player state or teleporting.
- The control loop now centrally refreshes the last known vessel location every successful tick, improving emergency/recovery exit positioning even for controller families that do not update it themselves.
- Added a native Ravager secondary roar using the real Paper/vanilla roar state (`setRoarTicks(11)`) rather than synthetic damage/effects.
- Ravager attack, stun and roar tick state is preserved across normal release and persisted for interrupted-session recovery.
- Added a centralized Adventure MiniMessage presentation layer with an Incarnate-specific clay/moss/amber theme instead of hardcoded `[Incarnate]` chat strings.
- Added bundled `en_US` and `ru_RU` localizations. `messages.yml` can use a fixed locale or `auto`, which follows the Player client locale when a bundled translation exists and otherwise uses the configured fallback locale.
- Added customizable named theme tokens (`primary`, `secondary`, `accent`, `text`, `muted`, `success`, `warning`, `error`) and a customizable MiniMessage prefix. User overrides in `messages.yml` are layered over bundled locale defaults so future message keys do not require replacing an existing customized file.
- Ability and camera labels are localized as stable message keys instead of being embedded English text.
- Added `config-version: 1` and non-destructive gameplay config migration. Existing `config.yml` values are never replaced by new defaults; only missing keys are added, a pre-migration backup is written under `plugins/Incarnate/backups/`, and the update is saved through a temporary file/atomic move when supported.
- Added locale/theme regression tests that require English and Russian bundles to expose the same message keys and validate bundled MiniMessage templates.
- Existing 0.2.0 native abilities, default-deny permissions, per-mob permissions, concealment and crash recovery remain intact.
- The mounted free-look transport still requires live client validation across representative mobs and server implementations before it is treated as gameplay-final.

## 0.2.0

Native Abilities Update.

- Added real Guardian / Elder Guardian laser attacks. Incarnate temporarily enables awareness only for the bounded laser window so the vanilla GuardianAttackGoal performs its own charge and native magic + attack damage; possession returns the body to unaware immediately afterward.
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
