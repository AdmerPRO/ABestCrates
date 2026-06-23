# ABestCrates

ABestCrates is a modern, GUI-first crates plugin for Paper servers. It lets server owners create and manage crates, keys, rewards, chances, animations, holograms, and messages directly in-game without manually editing configuration files.

## Features

- Fully configurable crates managed from an in-game GUI
- Physical keys, virtual keys, and placeable crate items
- Per-player virtual key counter below crate holograms
- Full ItemStack metadata support for Nexo, ItemsAdder, Oraxen, ExecutableItems, MMOItems, and similar plugins
- Item bundles (up to 27 items), unlimited commands, or mixed rewards
- GAMBLE crates with weighted rolls and CHOOSE crates with selectable rewards
- Custom preview and opening titles per crate
- Roll/CSGO, Casino, Cosmic, Roulette, Wheel, Wonder, classic, fast, and instant animations
- Progressive Roll slowdown, configurable sounds, and up to 9 animated rewards per opening
- Centered paginated previews with Open and Open All buttons
- Paginated crate/reward menus and drag-or-shift-click reward item editing
- Display-name holograms above placed crates
- Ring, spiral, helix, and Dyson Sphere particle effects
- Custom crate block material
- Crate color presets
- Bulk opening with shift-left click
- Right-click preview and left-click opening
- Protected placed crates against breaking and explosions
- Rare reward broadcasts
- Firework effects
- Milestones, global/per-player reward limits, and dedicated reward logs
- Per-crate cooldown, Vault opening cost, permission, multiple key requirements, and pushback
- Required and blocked permissions per reward
- PlaceholderAPI placeholders
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
- GAMBLE/CHOOSE type
- Particle effect and virtual key display
- Permission, cooldown, Vault cost, and pushback
- Multiple key requirements
- Preview/opening GUI titles
- Milestones
- Placeable crate items

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
- Weight and rarity multiplier
- Up to 27 reward items
- Unlimited commands
- Required/blocked permissions
- Global/per-player limits

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
/abc giveall <crate> <amount> [physical|virtual]
/abc givecrate <player> <crate> <amount>
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
abestcrates.crateitem
```

Players need `abestcrates.use` to open crates.

## Player Interaction

- Right-click a placed crate to preview rewards
- Left-click a placed crate to open it
- Shift-left-click a placed crate to open all matching keys

The **Give Keys To** menu performs the entire physical/virtual key workflow without a command: select the key type, crate, online player, and enter the amount in chat.

## Integrations

- Vault is optional and is required only for crates with an opening cost.
- PlaceholderAPI is optional. Available placeholders are `%abestcrates_virtual_<crate>%`, `%abestcrates_physical_<crate>%`, `%abestcrates_total_<crate>%`, and `%abestcrates_opened_<crate>%`.
- Custom item plugins work through complete Bukkit `ItemStack` metadata cloning and serialization.

## Compatibility

ABestCrates requires Java 21 and Paper 1.21.x.

## License

Copyright (C) 2026 AdmerPRO.

Licensed under the GNU General Public License v3.0 only (`GPL-3.0-only`).
