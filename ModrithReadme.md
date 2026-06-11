# ABestCrates

ABestCrates is a modern, GUI-first crates plugin for Paper servers. It lets server owners create and manage crates, keys, rewards, chances, animations, holograms, and messages directly in-game without manually editing configuration files.

## Features

- Fully configurable crates managed from an in-game GUI
- Physical keys with custom item support
- Virtual keys stored per player
- Item rewards, command rewards, or mixed rewards
- Real chance and display chance support
- Reward preview GUI
- Classic, fast, and instant opening animations
- Display-name holograms above placed crates
- Custom crate block material
- Crate color presets
- Bulk opening with shift-left click
- Right-click preview and left-click opening
- Protected placed crates against breaking and explosions
- Rare reward broadcasts
- Firework effects
- Custom messages
- Tab completion for commands, players, crates, and key amounts

## Crate Editing

Administrators can configure crates from the GUI:

- Display name
- Technical crate name
- Crate color
- Block type
- Hologram lines
- Rewards
- Key item
- No-key message
- Opening animation
- Reward preview

Items can be assigned by holding them in hand, clicking the target slot, or dragging an item into the GUI. Custom key items copy the original item attributes while replacing only the key name.

## Rewards

Each reward can have:

- Custom display item
- Dropped item
- Commands
- Real chance
- Display chance
- Rarity text
- Broadcast toggle
- Firework toggle

This allows public-facing odds to be shown separately from the real internal roll chance.

## Commands

Main command:

```text
/abestcrates
```

Aliases:

```text
/abc
/acrates
```

Admin commands:

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

## Permissions

```text
abestcrates.admin
abestcrates.use
abestcrates.open
abestcrates.create
abestcrates.givekey
abestcrates.reload
```

Players need `abestcrates.use` to open crates.

## Player Interaction

- Right-click a placed crate to preview rewards
- Left-click a placed crate to open it
- Shift-left-click a placed crate to open all matching keys

Bulk opening checks inventory space before consuming keys.

## Compatibility

ABestCrates is designed for modern Paper servers and uses the current Paper API.

## License

All Rights Reserved.
