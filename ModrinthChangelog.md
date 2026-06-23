# ABestCrates 1.2.1

Version 1.2.1 is a stability release for the Paper 1.21.x branch.

## Fixed

- Rewards with `0.0` real or effective chance can no longer be selected when the random roll lands exactly on the lower boundary.
- Virtual key displays now refresh after crate renames migrate stored virtual key balances.
- Empty virtual-key entries are removed after the last key is consumed, removed, or loaded from disk.
- Virtual-key rename/removal now avoids unnecessary disk saves when no balances changed.
- Vault economy reconnects now clear stale hooks when Vault is unavailable, disabled, or incompatible.
- PlaceholderAPI expansion requests safely ignore empty placeholder parameters.
- Virtual key display refreshes now guard against teleport events without a destination.
- Reward lookup and removal now handle null IDs safely, and blank reward IDs are normalized to a safe fallback.
- README and Modrinth README now reference the `1.2.1` artifact and complete permission list.

## Requirements

- Java 21.
- Paper 1.21.x.
- Vault and PlaceholderAPI remain optional.
