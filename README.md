# ABestCrates

ABestCrates is a Paper plugin for configurable Minecraft crates managed mostly through in-game GUI menus.

## Features

- GUI-based crate creation and editing
- Physical placed crates with protected blocks and holograms
- Physical keys, virtual keys, and placeable crate items
- Personal virtual key counters below crate holograms
- GAMBLE and selectable CHOOSE crates
- Ring, spiral, helix, and Dyson Sphere particle effects
- Custom key item templates with copied item meta/custom model data
- Item bundles (up to 27 items) and unlimited command rewards
- Real chance and display chance per reward
- Reward preview by right-clicking a crate
- Opening by left-clicking a crate
- Bulk opening with Shift + left-click
- Opening animations: Roll/CSGO with progressive slowdown, Casino, Cosmic, Roulette, Wheel, Wonder, Classic, Fast, and Instant
- Multiple distinct reward rolls per opening (1-9), animated and awarded together
- Centered, paginated reward previews with Open and Open All controls
- Paginated crate and reward management menus
- Configurable cycle, slowdown, and finish sounds
- Milestones, cooldowns, Vault costs, permissions, multiple key requirements, pushback, reward limits, and logs
- PlaceholderAPI expansion and custom item metadata support
- Command-free **Give Keys To** GUI workflow
- Placed crate removal with `/abc deletecrate`

## Requirements

- Paper 1.20.1-1.20.6
- Java 17 for Paper 1.20.1-1.20.4
- Java 21 for Paper 1.20.5-1.20.6 (the plugin remains compiled for Java 17)
- Maven for building from source
- Optional: Vault and PlaceholderAPI

## Build

From the repository root:

```bash
mvn -f ABestCrates/pom.xml -q -DskipTests package
```

The release jar is generated at:

```text
ABestCrates/target/ABestCrates-1.1.0.jar
```

## Installation

1. Put `ABestCrates-1.1.0.jar` in your server `plugins` folder.
2. Start or restart the Paper server.
3. Use `/abc gui` to open the main GUI.

## Commands

```text
/abc gui
/abc reload
/abc create <name>
/abc delete <name>
/abc deletecrate
/abc spawncrate <name>
/abc edit <name>
/abc givekey <player> <crate> <amount>
/abc giveall <crate> <amount> [physical|virtual]
/abc givecrate <player> <crate> <amount>
/abc addkeys <player> <crate> <amount>
/abc removekeys <player> <crate> <amount>
/abc forceopen <player> <crate>
```

Aliases:

```text
/abestcrates
/acrates
/abc
```

## Permissions

```text
abestcrates.admin
abestcrates.use
abestcrates.open
abestcrates.create
abestcrates.givekey
abestcrates.reload
abestcrates.crateitem
```

## Runtime Data

The plugin stores runtime data in:

```text
plugins/ABestCrates/crates.yml
plugins/ABestCrates/keys.yml
plugins/ABestCrates/messages.yml
plugins/ABestCrates/virtual-keys.yml
plugins/ABestCrates/locations.yml
plugins/ABestCrates/player-data.yml
plugins/ABestCrates/reward-rolls.log
```

PlaceholderAPI exposes `%abestcrates_virtual_<crate>%`, `%abestcrates_physical_<crate>%`, `%abestcrates_total_<crate>%`, and `%abestcrates_opened_<crate>%`.

## License

Copyright (C) 2026 AdmerPRO.

Licensed under the GNU General Public License v3.0 only (`GPL-3.0-only`). See `LICENSE`.
