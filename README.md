# ABestCrates

ABestCrates is a Paper plugin for configurable Minecraft crates managed mostly through in-game GUI menus.

## Features

- GUI-based crate creation and editing
- Physical placed crates with protected blocks and holograms
- Physical keys and virtual keys
- Custom key item templates with copied item meta/custom model data
- Item rewards and command rewards
- Real chance and display chance per reward
- Reward preview by right-clicking a crate
- Opening by left-clicking a crate
- Bulk opening with Shift + left-click
- Basic opening animations: Classic, Fast, Instant
- Placed crate removal with `/abc deletecrate`

## Requirements

- Java 21
- Paper 1.21.x
- Maven for building from source

## Build

From the repository root:

```bash
mvn -f ABestCrates/pom.xml -q -DskipTests package
```

The release jar is generated at:

```text
ABestCrates/target/ABestCrates-1.0.0.jar
```

## Installation

1. Put `ABestCrates-1.0.0.jar` in your server `plugins` folder.
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
```

## Runtime Data

The plugin stores runtime data in:

```text
plugins/ABestCrates/crates.yml
plugins/ABestCrates/virtual-keys.yml
plugins/ABestCrates/locations.yml
```

## License

All Rights Reserved. See `LICENSE`.
