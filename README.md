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

`incarnate.mob.*` grants all currently supported mob types. Server owners can still deny any supported type through `excluded-types`.

This permission model is suitable for ranks or donor perks. Per-mob checks are enforced in both commands and the possession core, and `/incarnate` tab completion only exposes forms the player can actually use.

`/release` intentionally has no required permission so a removed rank or donor permission cannot trap somebody inside a vessel.

`incarnate.admin.inspect` remains separate and `default: op`.

## Messages, theme and localization

Incarnate uses Adventure MiniMessage for player-facing output instead of hardcoded legacy color strings.

`plugins/Incarnate/messages.yml` controls the presentation layer without touching gameplay settings. The default Incarnate theme uses clay, moss, amber, warm parchment and terracotta tones and exposes named tags:

- `<primary>`
- `<secondary>`
- `<accent>`
- `<text>`
- `<muted>`
- `<success>`
- `<warning>`
- `<error>`

The prefix is also a normal MiniMessage value and can be fully replaced by the server owner.

Bundled message locales currently include `en_US` and `ru_RU`. With `locale: auto`, Incarnate follows the player's client locale when a bundled translation exists and otherwise uses `fallback-locale`.

The user `messages.yml` is an override layer rather than a copied full language pack. Bundled locale defaults remain inside the plugin JAR, so new messages introduced by later Incarnate versions become available automatically without replacing existing server theme/prefix/message overrides.

## Safe configuration upgrades

Gameplay configuration is versioned separately with `config-version`.

When an existing `plugins/Incarnate/config.yml` is opened by a newer compatible Incarnate build:

- existing values are kept exactly as the server configured them;
- only settings missing from the old file are copied from the new bundled defaults;
- before the first migration write, the original file is backed up under `plugins/Incarnate/backups/`;
- the migrated file is written through a temporary file and atomic replace when the filesystem supports it;
- a configuration from a newer unsupported schema is rejected rather than silently downgraded.

This means tuned movement speeds, cooldowns, camera choices, release behavior, excluded mobs and other existing settings are not reset merely because a later release adds new options.

## What 0.6.0 implements

### Real bodies

Existing possession keeps the same Mob UUID and preserves its equipment/state. Created incarnation spawns a real Mob and removes it on release by default.

Ender Dragon and Shulker now use dedicated complex-body controllers and are enabled by default. They can still be disabled through `excluded-types`.

### Free-look camera

The default camera transport remains the mounted free-look architecture introduced in 0.3.0.

Instead of directly attaching the client camera to the Mob through `setSpectatorTarget`, the hidden spectator Player is moved to the vessel and mounted on the real Mob while remaining its own camera. This is designed to preserve normal mouse/touch yaw and pitch input while the Mob body follows that view direction.

```yaml
camera:
  mode: MOUNTED
  mount-retries: 8
  fallback-to-spectator-target: true
```

`SPECTATOR_TARGET` remains available as a compatibility fallback. Mounted acquisition uses Player and Mob EntitySchedulers and retries region-safe attachment before falling back or releasing safely.

Dismount input is guarded during possession, and release/quit/death/plugin-disable paths explicitly detach the hidden Player before state restoration or recovery teleportation.

The mounted camera still requires live client testing across representative mob sizes and server implementations before being considered gameplay-final.

### Possession HUD

The localized actionbar HUD remains active while possession is active. By default it shows:

- the real Mob body's current and maximum health;
- the localized left-click ability;
- the localized `F` ability;
- whether each ability is ready or how much cooldown remains.

```yaml
hud:
  actionbar:
    enabled: true
    interval-ticks: 4
    show-health: true
    show-abilities: true
```

Vessel health and capability information is snapshotted from the Mob-owned thread. The Player-side HUD consumes those snapshots instead of reading the live Mob cross-region. Cooldowns are now observable session state, so the UI does not need to guess whether an ability is ready.

### Controller privacy

The hidden Player is not copied into the Mob name. Incarnate blocks Paper entity tracking for concealed controllers and, by default, temporarily removes them from each viewer's tab list.

World concealment uses a short hide/show tracking pulse plus `PlayerTrackEntityEvent` rather than retaining a long-lived plugin-scoped `hidePlayer` layer. Per-viewer TAB ownership is persisted so interrupted reloads can restore entries Incarnate removed.

### Folia-oriented ownership model

Player input and HUD delivery run on the Player EntityScheduler. Vessel mutation, capability detection and vessel telemetry run on the Mob EntityScheduler. Cross-region control uses snapshots rather than live Player/Mob reads from the opposite region thread.

Entity ray-trace consumers fail closed on Folia ownership before acting on a hit entity. Interrupted vessel recovery uses a durable UUID index so already-loaded bodies can also recover after restart/reload.

### Ability gestures

0.6.0 introduces a gesture dispatch layer so Incarnate is no longer limited internally to exactly one left-click action and one `F` action.

Current gesture types are:

- normal primary (`left click`);
- secondary (`F` by default);
- sneak-primary (`Shift + left click`);
- sprint-primary (`Sprint + left click`).

Gesture modifiers are configurable:

```yaml
input:
  gestures:
    sneak-primary: true
    sprint-primary: true
```

A mob only consumes a special gesture when it has a dedicated native action for it; otherwise the gesture falls back to the ordinary primary action. This keeps existing mobs compatible while giving complex bodies additional control slots. True attack-button hold/release semantics are deliberately not emulated from repeated arm-swing packets because Paper does not expose a reliable held-left-click state.

Frog is the first vessel using the extra channel: normal left click remains melee while `Shift + left click` uses its native tongue target API.

### Movement families

Dedicated controllers exist for:

- ground mobs;
- flying mobs;
- aquatic mobs;
- amphibious mobs;
- Rabbit and Frog hopping;
- Spider / Cave Spider climbing;
- Slime, Magma Cube and Sulfur Cube hopping;
- Ender Dragon 3D flight under a forced native `HOVER` phase;
- Shulker attachment-surface crawling.

Ground bodies use real `MOVEMENT_SPEED` and `JUMP_STRENGTH` where available. Ordinary flying bodies use real `FLYING_SPEED` where available. Ender Dragon and Shulker bypass those generic families because their vanilla movement models require dedicated control. Unknown/future Mob types still fall back to ground movement.

Burst abilities use a short movement-control lock so normal WASD processing does not immediately erase their physical impulse.

The control loop centrally records the latest vessel position every successful tick for safer emergency/recovery exits.

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
- Ender Dragon - real `DragonFireball`;
- Shulker - real homing `ShulkerBullet` targeting the aimed living entity;
- Frog - normal melee plus native tongue targeting on `Shift + left click`;
- Creeper - real fuse toggle;
- Pillager / Piglin - native ranged attack with crossbow;
- Drowned - native ranged attack with trident;
- Illusioner - native ranged attack with bow;
- Witch - native ranged attack through the Mob implementation.

Other mobs use a real Mob melee attack when a living target is under the vessel crosshair.

### Complex bodies

Ender Dragon is controlled differently from ordinary flying mobs. Incarnate forces the native `HOVER` phase during possession so the vanilla dragon phase controller cannot steer it back toward a portal/podium while the player is in control. The original phase and podium are restored for an existing dragon. If a created dragon is explicitly configured to remain after release, Incarnate leaves it in `HOVER` at its current position rather than reactivating a default podium flight path.

Shulker remains an attached body rather than becoming a generic walker. WASD moves it cell-by-cell along its current support surface; on wall attachments, jump/sneak can crawl vertically. Candidate cells and support blocks must belong to the current Folia region and be physically usable before the move is accepted.

Both complex-body implementations remain subject to live gameplay validation, especially camera feel and Shulker surface transitions.

### Secondary abilities

Secondary abilities have their own cooldown channel.

Implemented:

- Enderman - directional teleport with collision and safe-landing checks;
- Spider / Cave Spider - physical forward pounce;
- Camel - real dashing state plus physical dash impulse;
- PufferFish - real puff-state toggle;
- Vex - real charging state plus 3D charge impulse;
- Ravager - native roar using the real Ravager roar state and vanilla/Paper damage behavior;
- Evoker - a forward line of real `EvokerFangs` entities owned by the Evoker, with the real FANGS spell state and staggered attack delays;
- Shulker - real shell open/close through its native `peek` state.

Evoker fang placement checks current Folia region ownership and only creates fangs where a valid ground location can be found. Count, spacing, attack-delay step, cast window and cooldown are configurable.

More mob-specific abilities can be added without changing the possession core.

### Why Goat ram is not exposed yet

Paper provides a Goat ram API, but its implementation writes directly into Goat Brain RAM memories and activates the RAM activity. Incarnate normally controls mobs with real AI enabled but `aware=false`, which suppresses normal AI ticking. The public API does not expose the exact previous RAM brain memory state needed to restore an arbitrary existing Goat after possession.

Incarnate therefore does not currently expose Goat ram rather than leaving a possessed existing mob with silently modified brain state after `/release`.

### Warden note

Incarnate does not make a possessed Warden player literally blind by default. A future immersive sensory mode can represent vibrations or disturbances through HUD/audio cues without making the body impractical to control.

The current Paper API exposes Warden anger and disturbance operations, but not a complete enumerable snapshot of every per-entity anger/Brain entry. For that reason 0.6.0 does not force native sonic-boom AI by mutating anger and then pretending the previous state can be restored exactly.

## Recovery

Before taking over an existing Mob, Incarnate stores its original AI/awareness/persistence/despawn/aggressive/gravity and relevant special state in PDC. Interrupted sessions can recover marked vessels when they load again. Created orphan vessels are removed by default.

Special-state restoration includes sitting state, Bat awake state, Camel dash, Creeper ignition/fuse progress, Guardian laser handling, PufferFish puff state, Vex charging state, Ravager attack/stun/roar ticks, the current Spellcaster spell state, Ender Dragon phase/podium, Shulker peek/attachment face, Frog tongue target for normal release, and Sniffer state. Interrupted Frog recovery clears a stale tongue target instead of trying to resurrect an unsafe entity reference.

On normal release, a pre-possession combat target is restored only when it is still alive and safely owned by the same Folia region.

The Player's original game mode, flight permission/state, fly speed, world, location and rotation are stored for interrupted recovery. Recovery remains pending if the saved world is temporarily unavailable.

### AI suppression note

Incarnate keeps the real Mob AI flag enabled but normally sets a controlled body to unaware. This is currently the safest public-API approach to suppress autonomous pathfinding without making the entity immobile, but Paper notes that unaware mobs can also lose some autonomous/environmental behavior.

Abilities that genuinely need vanilla AI are handled narrowly. Guardian laser temporarily enables awareness only for its bounded attack window with a locked target/movement policy, then returns the body to unaware. Evoker fangs do not require enabling autonomous AI; Incarnate creates the real fang entities directly while using the Evoker's native spell state for presentation.

The original awareness state is restored on release and interrupted-session recovery.

## Validation

GitHub Actions runs unit tests and compiles Incarnate with Java 25 against Paper API `26.2.build.121-stable`, then starts a real Paper 26.2 build 121 server and performs a graceful startup/shutdown smoke test.

The test suite validates bundled locale key parity, MiniMessage parsing, non-destructive config migration and observable cooldown timing.

Validated CI builds expose the built JAR as a workflow artifact after the Paper smoke test passes.

Live gameplay testing remains the final gate for mounted-camera feel, actionbar readability and individual Mob mechanics.

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
