# Incarnate

Incarnate is a true mob incarnation / possession plugin for Minecraft Java 26.2+.

It does not disguise a Player as a mob. The controlled body is a real server-side Mob with its own UUID, hitbox, health, equipment, damage, fire state, knockback and vanilla client animations. The Player becomes a hidden controller and spectates the vessel.

Author: Onelsey

## Target

- Minecraft Java 26.2+
- Java 25+
- Paper
- Purpur
- Leaf
- Folia
- Spigot is not an official target

The build is pinned to Paper API `26.2.build.121-stable`.

## Commands and controls

- `/incarnate <mob>` - create a real Mob body and enter it.
- `/possess` - possess the real Mob currently aimed at within the configured distance.
- `/release` - leave the current body or retry a pending recovery.
- `/incarnate primary` - fallback primary-ability trigger for testing.
- `/incarnate secondary` - fallback secondary-ability trigger for testing.
- `/incarnate inspect` - admin-only inspection of the controlled body currently aimed at. It reports the controller, vessel UUID and whether the body was created or possessed without exposing that information visually above the Mob.
- Left click - primary attack / ability.
- `F` - secondary ability with default keybinds. Technically this is Swap Offhand, so rebinding Swap Offhand also rebinds this control.
- `Shift + F` - quick release. Technically this is Sneak + Swap Offhand.

## Permissions

Mob control is opt-in. Ordinary players receive no Incarnate gameplay access by default, and the gameplay permissions below use `default: false`.

Starting a created incarnation requires both:

- `incarnate.use.incarnate`
- `incarnate.mob.<entity_type>`

Possessing an existing mob requires both:

- `incarnate.use.possess`
- `incarnate.mob.<entity_type>`

Examples of per-mob permissions:

- `incarnate.mob.skeleton`
- `incarnate.mob.creeper`
- `incarnate.mob.enderman`
- `incarnate.mob.zombie_villager`

`incarnate.mob.*` grants access to every mob type supported by the current Incarnate build. Explicitly excluded types such as Ender Dragon and Shulker remain unavailable even with the wildcard.

This makes the permission model suitable for server ranks or donor perks. For example, a rank can receive `incarnate.use.incarnate` plus only `incarnate.mob.skeleton`, while a higher rank can receive `incarnate.mob.*`. Existing-mob possession can be sold or granted separately through `incarnate.use.possess`.

The per-mob check is enforced in the possession core as well as the commands, so a future alternate command/API path cannot bypass it accidentally. Mob tab completion is also filtered: players only see forms they are allowed to use.

`/release` intentionally has no required permission. If a rank or donor permission is removed while a player is already controlling a body, they must still be able to leave it and complete recovery safely.

`incarnate.admin.inspect` is separate and remains `default: op`.

## What 0.2.0 implements

### Real bodies

Existing possession preserves the same Mob UUID and its equipment/state. Created incarnation spawns a real Mob and removes it on release by default.

Ender Dragon and Shulker are deliberately excluded in 0.2.0.

### Controller privacy

The hidden Player is not copied into the Mob name. Incarnate blocks Paper entity tracking for concealed controllers and, by default, temporarily removes them from each viewer's tab list.

World concealment uses a short hide/show tracking pulse rather than keeping a long-lived plugin-scoped `hidePlayer` layer. The pulse forces the old Player tracker pairing to be removed, then the `PlayerTrackEntityEvent` guard prevents the controller from being spawned again while possession is active.

Per-viewer tab-list ownership is persisted separately so an interrupted plugin reload can restore entries that the previous Incarnate instance removed instead of leaving a player absent from TAB until reconnect.

### Elysium Private Tag integration

Incarnate has an optional integration with `Elysium_Private_Tag` V4.1+.

The integration does not manipulate Private Tag `TextDisplay` entities directly. It uses the source-aware public methods exposed by Elysium Private Tag:

- `hideTag(Player, String)`
- `showTag(Player, String)`

Incarnate uses the source name `Incarnate`. When possession becomes active, the player's overhead tag is suppressed before the controller enters spectator mode. The suppression stays active through release teleport, death/respawn recovery and interrupted-session recovery, then only Incarnate's own suppression source is removed after recovery succeeds.

This means another system can hide the same tag at the same time without Incarnate accidentally making it visible. For example, a death-system suppression remains in effect even when Incarnate releases its own source.

The integration is a soft dependency and uses reflection rather than a compile-time dependency. Incarnate works normally when Elysium Private Tag is not installed or is disabled. On Incarnate startup, stale `Incarnate` suppression is cleared for already-online players so a hot plugin reload cannot leave their tag hidden permanently. A full server crash is also safe because Elysium Private Tag suppression state is in memory and Incarnate independently recovers the Player state on startup.

Default configuration:

```yaml
integrations:
  elysium-private-tag:
    enabled: true
    plugin-name: Elysium_Private_Tag
    source: Incarnate
```

### Folia-safe ownership model

Player input is sampled on the Player EntityScheduler. Vessel mutation runs on the Mob EntityScheduler. Cross-region control uses snapshots rather than reading live Player state from the vessel thread.

Entity ray-trace consumers fail closed on Folia ownership before reading or acting on a hit entity. Interrupted vessel recovery uses a durable UUID index so already-loaded Mob bodies can also be recovered after plugin/server restart instead of depending only on a future entity-load event.

### Movement families

0.2.0 has dedicated controllers for:

- ground mobs;
- flying mobs;
- aquatic mobs;
- amphibious mobs;
- Rabbit and Frog hopping behavior;
- spiders/cave spiders with wall climbing;
- Slime, Magma Cube and Sulfur Cube with hopping movement.

Ordinary ground bodies use their real `MOVEMENT_SPEED` and `JUMP_STRENGTH` attributes where available. Flying bodies use their real `FLYING_SPEED` attribute where available, with configurable safety clamps and a fallback speed for entities without that attribute.

Burst movement abilities use a short movement-control lock so the normal WASD controller does not immediately overwrite the physical impulse on the next server tick.

Unknown/future Mob types fall back to ground movement rather than packet disguise logic.

### Primary abilities

The body is the projectile source.

Implemented in 0.2.0:

- Skeleton / Stray / Bogged / Parched: Arrow when a bow is held;
- Blaze: Small Fireball;
- Ghast: Large Fireball;
- Wither: Wither Skull;
- Snow Golem: Snowball;
- Llama / Trader Llama: Llama Spit;
- Breeze: Breeze Wind Charge;
- Guardian / Elder Guardian: a real charging Guardian laser; Incarnate temporarily allows the vanilla Guardian attack goal to tick under a locked target/movement window so vanilla Guardian damage is applied;
- Creeper: primary toggles the real creeper fuse;
- Pillager / Piglin: native Paper ranged attack when holding a crossbow;
- Drowned: native ranged attack when holding a trident;
- Illusioner: native ranged attack when holding a bow;
- Witch: native ranged attack through the Mob's own ranged attack implementation.

Other mobs fall back to a real melee attack when a living target is under the vessel crosshair. Damage, held equipment and knockback are evaluated by the Mob/Paper attack path, not by the hidden Player.

### Secondary abilities

Secondary abilities use a separate cooldown channel from primary attacks.

Implemented in 0.2.0:

- Enderman: directional teleport with collision ray tracing and a safe landing search;
- Spider / Cave Spider: forward pounce from the real body while grounded;
- Camel: real dashing state plus a physical forward dash impulse;
- PufferFish: toggles the real puff state;
- Vex: native charging state plus a 3D physical charge impulse.

More secondary abilities can be added per Mob without changing the possession core.

## Recovery

Before taking over an existing Mob, Incarnate stores its original AI/awareness/persistence/despawn/aggressive/gravity and special-state data in PDC. Interrupted sessions can restore marked vessels when they load again. Created orphan vessels are removed by default.

Special-state restoration currently includes sitting state, Bat awake/hanging state, Camel dashing state, Creeper ignition/fuse progress, PufferFish puff state and Vex charging state. Normal release also restores a Guardian's pre-possession laser state when its original target is still safely restorable; crash recovery safely cancels an orphaned laser because its old target cannot be persisted Folia-safely.

On a normal release, Incarnate also restores the Mob's pre-possession combat target when that target is still alive and safely owned by the same Folia region. It deliberately does not perform a cross-region target restore.

The Player's original game mode, flight permission, flight state, fly speed, world, location and rotation are stored for interrupted-session recovery. Recovery remains pending instead of deleting its marker if the saved world is temporarily unavailable.

### AI suppression note

0.2.0 keeps the real Mob AI flag enabled but normally sets the body to unaware while it is player-controlled. This is currently the safest public-API way to suppress autonomous pathfinding without making the entity immobile. Paper notes that unaware mobs can also have some unspecified autonomous/environmental behavior disabled, so live gameplay testing remains required before the first public release. A narrowly scoped exception exists for abilities that require their vanilla goal to tick: Guardian laser temporarily enables awareness only for its bounded attack window while movement and target are controlled, then returns the body to unaware. Incarnate restores the original awareness state on release and interrupted-session recovery.

## Validation

GitHub Actions runs unit tests and compiles Incarnate with Java 25 against Paper API `26.2.build.121-stable`, then runs an actual Paper 26.2 build 121 startup/shutdown smoke test. The smoke test waits for full server startup, sends the normal `stop` command and verifies that Incarnate both enables and disables without plugin exceptions.

Live gameplay testing on Paper/Purpur/Leaf/Folia remains the final validation gate for camera/input feel and individual Mob mechanics.

## Build

Install JDK 25 and Gradle, then run:

```text
gradle build
```

Release source archives intentionally contain no plugin JAR or compiled `.class` files. CI may build a temporary JAR for validation but does not publish that validation artifact.

## License

Incarnate is proprietary software distributed under the **Incarnate All Rights Reserved License v1.0**.

In short:

- official releases may be downloaded and used, including on monetized Minecraft servers;
- exact, complete and unmodified official release packages may be shared free of charge;
- modified builds, rebranding, derivative versions and redistributed forks are not permitted without prior written permission from Onelsey;
- all rights not explicitly granted remain reserved.

See [`LICENSE`](LICENSE) for the complete terms.
