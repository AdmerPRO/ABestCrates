# Changelog

All notable changes to the ABestCrates project are documented in this file.

## [Unreleased] - 2026-06-21

### Added

- Roll/CSGO animation with fast initial movement and progressive slowdown.
- Casino, Cosmic, Roulette, Wheel, and Wonder opening layouts.
- Configurable 1-9 distinct rewards per opening, including multi-selection for CHOOSE crates.
- Centered paginated reward previews with Open and Open All buttons.
- Pagination for crate, reward, and key-distribution menus.
- Light gray stained-glass fillers across menus.
- Shift-click and drag/drop item editing for reward item bundles.
- `/abc giveall <crate> <amount> [physical|virtual]` for all online players.
- Configurable cycle, slowdown, and finish sounds.
- Dedicated `crates.yml`, `keys.yml`, and `messages.yml` resources with legacy loading compatibility.

## [1.1.0] - 2026-06-20

### Added

- Two crate modes:
  - `GAMBLE` — rolls a reward using weights and rarity multipliers.
  - `CHOOSE` — allows the player to select a reward without gambling.
- Crate particle effects: `RING`, `SPIRAL`, `HELIX`, and `DYSON_SPHERE`.
- A personalized virtual key counter displayed below each crate hologram.
- Global and per-crate options for enabling the virtual key display.
- Placeable crate items that are automatically linked to their crate definition.
- The `/abc givecrate <player> <crate> <amount>` command.
- The `abestcrates.crateitem` permission.
- A command-free key distribution workflow in the GUI:
  - select a physical or virtual key,
  - select a crate,
  - select a player,
  - enter the amount in chat.
- Multiple key requirements for a single crate.
- Vault economy costs for opening crates.
- Per-player and per-crate opening cooldowns.
- Permission requirements for individual crates.
- Optional pushback for players who do not meet crate requirements.
- Milestones that grant bonus rewards for consecutive openings of the same crate.
- Global and per-player reward limits.
- Required and blocked permissions for individual rewards.
- An automatically scalable reward weight system.
- Configurable rarity-based weight multipliers in `config.yml`.
- Item rewards containing up to 27 items with a dedicated GUI editor.
- Unlimited commands assigned to a reward.
- Dedicated reward roll logging in `reward-rolls.log`.
- Opening counts, cooldowns, limits, and player streaks stored in `player-data.yml`.
- Custom preview and opening GUI titles for every crate.
- A PlaceholderAPI expansion with the following placeholders:
  - `%abestcrates_virtual_<crate>%`
  - `%abestcrates_physical_<crate>%`
  - `%abestcrates_total_<crate>%`
  - `%abestcrates_opened_<crate>%`

### Expanded

- The crate GUI editor with crate modes, effects, key counters, costs, cooldowns, permissions, pushback, key requirements, milestones, and GUI titles.
- The reward GUI editor with rarity, weight, commands, item bundles, permissions, broadcasts, fireworks, and limits.
- The reward system with functional item, command, and mixed reward types.
- Mass Open with multiple key requirements, reward limits, economy costs, and rarity weights.
- The key system with safe splitting of large physical key amounts into multiple stacks.
- Crate ID renaming to also update key requirements, crate items, and keys held by online players.
- Custom item support by preserving complete Bukkit `ItemStack` metadata, including data from Nexo, ItemsAdder, Oraxen, ExecutableItems, and MMOItems.
- Holograms with owner-only virtual key information.
- Crate block protection and automatic visual refreshing after configuration changes.

### Integrations

- Added optional Vault integration for crate opening costs.
- Added optional PlaceholderAPI integration with version `2.11.6` as a `provided` dependency.
- Added soft dependencies for Vault, PlaceholderAPI, Nexo, ItemsAdder, Oraxen, ExecutableItems, and MMOItems.

### Configuration

- Added the `virtual-key-display` section.
- Added the `particles` section.
- Added the `mass-open.max-at-once` safety limit.
- Added configurable rarity multipliers.
- Added messages for costs, cooldowns, milestones, CHOOSE crates, permissions, and crate items.
- Preserved compatibility with the previous `crates.yml` format; existing single-item rewards are automatically loaded by the new reward system.

### Documentation

- Expanded `README.md` and `ModrithReadme.md` with the new features, integrations, commands, placeholders, and data files.
- Bumped the project and output JAR version from `1.0.0` to `1.1.0`.

### Verification

- Built the project using `mvn -q -DskipTests package`.
- Generated the `ABestCrates-1.1.0.jar` artifact.
