# ABestCrates 1.2.1

Version 1.2.1 is a stability release focused on routine bug fixes and safer edge-case handling.

## Fixed

- Rewards with `0.0` real or effective chance can no longer be selected when the random roll lands exactly on the lower boundary.
- Virtual key displays now refresh after crate renames migrate stored virtual key balances.
- Empty virtual-key entries are removed after the last key is consumed or removed.
- Vault economy reconnects now clear stale hooks before checking for an available provider.
- PlaceholderAPI expansion requests safely ignore empty or null placeholder parameters.
- Virtual key display refreshes now guard against teleport events without a destination.
- Reward lookup and removal now handle null IDs safely, and blank reward IDs are normalized to a safe fallback.

## Version

- Project version bumped to `1.2.1`.

## Requirements

- Paper 1.20.1-1.20.6.
- Java 17 for Paper 1.20.1-1.20.4.
- Java 21 for Paper 1.20.5-1.20.6.
- Vault and PlaceholderAPI remain optional.
