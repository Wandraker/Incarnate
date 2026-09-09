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

## Commands

- `/incarnate <mob>` - create a real Mob body and enter it.
- `/possess` - possess the real Mob currently aimed at within the configured distance.
- `/release` - leave the current body.
- `/incarnate primary` - fallback primary-ability trigger for testing.
- `Shift + F` - normal quick release with default keybinds. Technically this is Sneak + Swap Offhand, so a player who rebinds Swap Offhand uses their rebound key.

## What 0.1.0 implements

### Real bodies

Existing possession preserves the same Mob UUID and its equipment/state. Created incarnation spawns a real Mob and removes it on release by default.

Ender Dragon and Shulker are deliberately excluded in 0.1.0.

### Controller privacy

The hidden Player is not copied into the Mob name. Incarnate hides the controller from other players and, by default, temporarily unlists it from their tab list. A Paper tracking event is also blocked for concealed controllers so a re-track does not make the hidden Player appear again.

### Folia-safe ownership model

Player input is sampled on the Player EntityScheduler. Vessel mutation runs on the Mob EntityScheduler. Cross-region control uses snapshots rather than reading live Player state from the vessel thread.

### Movement families

0.1.0 has dedicated controllers for:

- ground mobs;
- flying mobs;
- aquatic mobs;
- amphibious mobs;
- spiders/cave spiders with wall climbing;
- Slime, Magma Cube and Sulfur Cube with hopping movement.

Unknown/future Mob types fall back to ground movement rather than packet disguise logic.

### Primary abilities

The body is the projectile source.

Implemented in 0.1.0:

- Skeleton / Stray / Bogged / Parched: Arrow when a bow is held;
- Blaze: Small Fireball;
- Ghast: Large Fireball;
- Wither: Wither Skull;
- Snow Golem: Snowball;
- Llama / Trader Llama: Llama Spit;
- Breeze: Breeze Wind Charge;
- Creeper: primary toggles the real creeper fuse.

Other mobs fall back to a real melee attack when a living target is under the vessel crosshair. Damage, held equipment and knockback are evaluated by the Mob/Paper attack path, not by the hidden Player.

More mob-specific abilities are planned after runtime testing proves the camera/input foundation.

## Recovery

Before taking over an existing Mob, Incarnate stores its original AI/awareness/persistence/despawn/aggressive state in PDC. Interrupted sessions can restore marked vessels when they load again. Created orphan vessels are removed by default.

The Player's original game mode, flight permission, flight state and fly speed are also stored for interrupted-session recovery.

## Validation

GitHub Actions compiles Incarnate with Java 25 against Paper API `26.2.build.121-stable` and runs a startup smoke test on an actual Paper 26.2 build 121 server. Live gameplay testing on Paper/Purpur/Leaf/Folia remains the final validation gate for camera/input behavior and mob-specific mechanics.

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
