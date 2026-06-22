# ABestCrates 1.2.0

Version 1.2.0 expands crate openings with new animations, multi-reward rolls, improved previews, paginated menus, and cleaner configuration files.

## New opening animations

- Added **Roll/CSGO** with fast initial movement and progressive slowdown before stopping on the rewards.
- Added **Casino**, **Cosmic**, **Roulette**, **Wheel**, and **Wonder** opening layouts.
- Opening cycle, slowdown, and finish sounds can now be configured in `config.yml`.

## Multiple rewards

- Crates can now roll from **1 to 9 different rewards per opening**.
- Every selected reward is displayed in the animation and awarded when it finishes.
- `CHOOSE` crates support selecting multiple rewards when multiple rolls are configured.
- Multiple rewards still consume the configured key requirements only once per opening.

## Preview and GUI improvements

- Reward previews are now centered inside the inventory.
- Added **Open this crate** and **Open with all keys** buttons to the preview GUI.
- Added pagination for crate, reward, preview, and key-distribution menus.
- Added light gray stained-glass fillers to empty GUI slots.
- Reward item bundles now support regular drag and drop as well as Shift-clicking items directly from the player inventory.

## Keys and commands

- Multiple key requirements for one crate remain fully supported.
- Added `/abc giveall <crate> <amount> [physical|virtual]` to give keys to every online player.

## Configuration

- Added dedicated configuration files:
  - `config.yml` — global settings, rarities, particles, mass opening, and sounds.
  - `crates.yml` — crate and reward definitions.
  - `keys.yml` — physical key definitions and custom key items.
  - `messages.yml` — plugin messages.
- Existing key definitions stored in the old `crates.yml` format are loaded and migrated on save.
- Existing installations remain compatible with legacy messages stored in `config.yml`.

## Requirements

- Paper 1.21.x
- Java 21
- Vault and PlaceholderAPI remain optional.
