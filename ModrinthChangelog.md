# ABestCrates 1.2.2

Version 1.2.2 adds admin maintenance tools, crate templates, config versioning, and release polish for the Paper 1.21.x branch.

## Added

- Admin commands and GUI tools for resetting player cooldowns, player open stats, player reward limit counters, and global reward limit counters.
- Admin Tools statistics GUI for viewing a player's crate history, players who opened a selected crate, player totals, and the most-opened crates.
- Crate duplication with `/abc duplicate <crate> <newName>`.
- Crate template export/import with `/abc export <crate> [file]` and `/abc import <file> [newName]`.
- Template files stored under `plugins/ABestCrates/templates`.
- `config-version: 2` markers for generated configuration files.
- bStats integration wiring, controlled by `metrics.enabled` and `metrics.plugin-id` in `config.yml`.
- Insert Nickname lookup for player statistics and Give Keys GUI workflows.
- Additional unit coverage for crate IDs, safe value clamping, reward settings, and zero-chance reward rolls.

## Changed

- Chat prompts, command help, protected-crate, admin, and template messages now live in `messages.yml`.
- Player open data now stores last-known player names for easier offline stat lookup.
- The old unused `OpeningService` implementation is now a small runtime contract used by `AdvancedOpeningService`.
- The Modrinth README file was renamed from `ModrithReadme.md` to `ModrinthReadme.md`.

## Fixed

- Modern Paper serialized `ItemStack` fields in crate files are normalized before loading to avoid `Material cannot be null` startup errors.
- Existing bundled config files are no longer recopied on startup, removing noisy already-exists warnings.

## Requirements

- Java 21.
- Paper 1.21.x.
- Vault and PlaceholderAPI remain optional.
