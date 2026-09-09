# Incarnate

Incarnate is a true mob incarnation / possession plugin for Minecraft Java 26.2+.

It does not disguise a Player as a mob. The controlled body is a real server-side Mob with its own UUID, hitbox, health, equipment, damage, fire state, knockback and vanilla client animations. The Player becomes a hidden controller while the real Mob body remains authoritative.

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
- `/incarnate inspect` - admin-only inspection of a controlled body without exposing its controller visually.
- Left click - primary attack / ability.
- `F` - secondary ability with default keybinds. This uses Swap Offhand internally.
- `Shift + F` - quick release.

## Permissions

Mob control is opt-in. Ordinary players receive no Incarnate gameplay access by default.

Creating a body requires both:

- `incarnate.use.incarnate`
- `incarnate.mob.<entity_type>`

Possessing an existing mob requires both:

- `incarnate.use.possess`
- `incarnate.mob.<entity_type>`

Examples:

- `incarnate.mob.skeleton`
- `incarnate.mob.creeper`
- `incarnate.mob.enderman`
- `incarnate.mob.zombie_villager`

`incarnate.mob.*` grants all currently supported mob types. Explicitly excluded types such as Ender Dragon and Shulker remain unavailable even with the wildcard.

This permission model is suitable for ranks or donor perks. Per-mob checks are enforced in both commands and the possession core, and `/incarnate` tab completion only exposes forms the player can actually use.

`/release` intentionally has no required permission so a removed rank or donor permission cannot trap somebody inside a vessel.

`incarnate.admin.inspect` remains separate and `default: op`.

## What 0.3.0 implements

### Real bodies

Existing possession keeps the same Mob UUID and preserves its equipment/state. Created incarnation spawns a real Mob and removes it on release by default.

Ender Dragon and Shulker are deliberately excluded.

### Free-look camera

0.3.0 changes the default camera transport.

Instead of directly attaching the client camera to the Mob through `setSpectatorTarget`, the hidden spectator Player is moved to the vessel and mounted on the real Mob while remaining its own camera. This is designed to preserve normal mouse/touch yaw and pitch input while the Mob body follows that view direction.

The camera configuration is:

```yaml
camera:
  mode: MOUNTED
  mount-retries: 8
  fallback-to-spectator-target: true
```

`MOUNTED` is the new default. `SPECTATOR_TARGET` remains available as a compatibility mode/fallback. Mounted acquisition uses Player and Mob EntitySchedulers and retries region-safe attachment before falling back or releasing safely.

Dismount input is guarded during possession, and release/quit/death/plugin-disable paths explicitly detach the hidden Player before state restoration or recovery teleportation.

The mounted camera is a major architecture change and still requires live client testing across representative mob types before being considered gameplay-final.

### Controller privacy

The hidden Player is not copied into the Mob name. Incarnate blocks Paper entity tracking for concealed controllers and, by default, temporarily removes them from each viewer's tab list.

World concealment uses a short hide/show tracking pulse plus `PlayerTrackEntityEvent` rather than retaining a long-lived plugin-scoped `hidePlayer` layer. Per-viewer TAB ownership is persisted so interrupted reloads can restore entries Incarnate removed.

### Folia-oriented ownership model

Player input is sampled on the Player EntityScheduler. Vessel mutation runs on the Mob EntityScheduler. Cross-region control uses snapshots rather than live Player reads from the vessel thread.

Entity ray-trace consumers fail closed on Folia ownership before acting on a hit entity. Interrupted vessel recovery uses a durable UUID index so already-loaded bodies can also recover after restart/reload.

### Movement families

Dedicated controllers exist for:

- ground mobs;
- flying mobs;
- aquatic mobs;
- amphibious mobs;
- Rabbit and Frog hopping;
- Spider / Cave Spider climbing;
- Slime, Magma Cube and Sulfur Cube hopping.

Ground bodies use real `MOVEMENT_SPEED` and `JUMP_STRENGTH` where available. Flying bodies use real `FLYING_SPEED` where available. Unknown/future Mob types fall back to ground movement.

Burst abilities use a short movement-control lock so normal WASD processing does not immediately erase their physical impulse.

The control loop also centrally records the latest vessel position every successful tick for safer emergency/recovery exits.

### Primary abilities

The Mob body is the actual projectile/attack source.

Implemented:

- Skeleton / Stray / Bogged / Parched - Arrow with a bow;
- Blaze - Small Fireball;
- Ghast - Large Fireball;
- Wither - Wither Skull;
- Snow Golem - Snowball;
- Llama / Trader Llama - Llama Spit;
- Breeze - Breeze Wind Charge;
- Guardian / Elder Guardian - real charging Guardian laser using a bounded vanilla Guardian attack-goal window;
- Creeper - real fuse toggle;
- Pillager / Piglin - native ranged attack with crossbow;
- Drowned - native ranged attack with trident;
- Illusioner - native ranged attack with bow;
- Witch - native ranged attack through the Mob implementation.

Other mobs use a real Mob melee attack when a living target is under the vessel crosshair.

### Secondary abilities

Secondary abilities have a separate cooldown channel.

Implemented:

- Enderman - directional teleport with collision and safe-landing checks;
- Spider / Cave Spider - physical forward pounce;
- Camel - real dashing state plus physical dash impulse;
- PufferFish - real puff-state toggle;
- Vex - real charging state plus 3D charge impulse;
- Ravager - native roar using the real Ravager roar state and vanilla/Paper damage behavior.

More mob-specific abilities can be added without changing the possession core.

## Recovery

Before taking over an existing Mob, Incarnate stores its original AI/awareness/persistence/despawn/aggressive/gravity and relevant special state in PDC. Interrupted sessions can recover marked vessels when they load again. Created orphan vessels are removed by default.

Special-state restoration includes sitting state, Bat awake state, Camel dash, Creeper ignition/fuse progress, Guardian laser handling, PufferFish puff state, Vex charging state and Ravager attack/stun/roar ticks.

On normal release, a pre-possession combat target is restored only when it is still alive and safely owned by the same Folia region.

The Player's original game mode, flight permission/state, fly speed, world, location and rotation are stored for interrupted recovery. Recovery remains pending if the saved world is temporarily unavailable.

### AI suppression note

0.3.0 keeps the real Mob AI flag enabled but normally sets a controlled body to unaware. This is currently the safest public-API approach to suppress autonomous pathfinding without making the entity immobile, but Paper notes that unaware mobs can also lose some autonomous/environmental behavior.

Abilities that genuinely need vanilla AI are handled narrowly. Guardian laser temporarily enables awareness only for its bounded attack window with a locked target/movement policy, then returns the body to unaware.

The original awareness state is restored on release and interrupted-session recovery.

## Validation

GitHub Actions runs unit tests and compiles Incarnate with Java 25 against Paper API `26.2.build.121-stable`, then starts a real Paper 26.2 build 121 server and performs a graceful startup/shutdown smoke test.

Validated CI builds also expose the built JAR as a workflow artifact after the Paper smoke test passes.

Live gameplay testing remains the final gate for camera feel, mounted-camera behavior and individual Mob mechanics.

## Build

Install JDK 25 and Gradle, then run:

```text
gradle build
```

## License

Incarnate is proprietary software distributed under the **Incarnate All Rights Reserved License v1.0**.

In short:

- official releases may be downloaded and used, including on monetized Minecraft servers;
- exact, complete and unmodified official release packages may be shared free of charge;
- modified builds, rebranding, derivative versions and redistributed forks are not permitted without prior written permission from Onelsey;
- all rights not explicitly granted remain reserved.

See [`LICENSE`](LICENSE) for the complete terms.
