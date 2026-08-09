# BuyClaimChunks Continued 1.2.0

## Universal backend release

- Replaced separate FTB Chunks and OpenPAC artifacts with one universal NeoForge 1.21.1 JAR.
- Automatically selects FTB Chunks when only FTB Chunks is installed.
- Automatically selects Open Parties and Claims when only OpenPAC is installed.
- Safely disables `/buyclaim` when both or neither backend is installed.
- Preserves the same `/buyclaim [amount]` command, configuration file, price curve, limits, and payment behavior for both backends.

## Price changes without claim confiscation

- Added a server-side purchase ledger keyed by player UUID.
- Records the configured currency ID, capacity bought through this mod, and the lifetime amount actually consumed.
- Numeric price increases never remove existing claims. The unpaid cumulative difference is added to the next purchase.
- Numeric price decreases grant compensation capacity supported by previous payments during the next successful purchase.
- Credit that cannot fit under `maxExtraClaims` continues to reduce later purchase prices.
- Changing `itemRequired` starts a new baseline instead of guessing an exchange rate between different items.
- Worlds upgraded from a pre-ledger release preserve their current next-price position and begin exact tracking from that point.

The active calculation is:

```text
next payment
= cumulative cost through the paid claims after this purchase under the current curve
- lifetime consumed amount
```

FTB Chunks or OpenPAC remains the source of truth for current claim capacity. The new ledger records only purchase history needed for safe repricing.

## OpenPAC support

- Stores every purchase on the executing player's own OpenPAC `BONUS_CHUNK_CLAIMS` and player-UUID economic ledger.
- Supports an all-paid model with OpenPAC base and free bonuses configured to zero.
- Resolves party context through OpenPAC's configured **primary party system**, including external primary systems exposed through OpenPAC's public API.
- Does not create a separate BuyClaimChunks party quota and does not sum member purchase ledgers.
- With `partyOwnedClaims=true`, an authorized member's PARTY-mode claim uses the current primary party owner's UUID, count, and limit; the same member's PLAYER-mode claim uses the member's own UUID, count, and limit.
- The party owner's own PLAYER claims and all PARTY-mode claims share one owner-UUID claim count and limit.
- Joining or leaving a party never moves `BONUS_CHUNK_CLAIMS`, purchase-ledger state, or existing claim owner UUIDs.
- Verified OpenPAC 0.29.3 owner transfer behavior: party ID remains stable, but existing claim owner UUIDs, both players' bonus values, and both BuyClaimChunks ledgers remain bound to their original player UUIDs; future PARTY claims use the new owner UUID.
- Adds real-command party semantics, restart-persistence, and dedicated-server validation for OpenPAC.

For OpenPAC's built-in parties, explicitly set `primaryPartySystem = "default"`. If another supported party integration is intentionally primary, keep that integration's registered ID. For an all-paid model, set `maxPlayerClaims`, `claimBonusPerPartyMember`, `claimBonusForPartyOwner`, and permission-derived free capacity to zero.

## Safety

- Detects concurrent quota and ledger changes before committing.
- Verifies backend capacity and compares the ledger snapshot before consuming payment.
- Attempts a verified rollback of both capacity and ledger state if an already validated payment unexpectedly cannot be consumed.
- Keeps OpenPAC purchases bound to the executing player's UUID instead of silently redirecting a member's payment to another player.
- Produces one JAR containing thin API adapters only; FTB Chunks and OpenPAC are not bundled.

## Default configuration

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

Existing configuration files are retained. Stop the server, edit `config/buyclaimchunks-common.toml`, save, and restart to change the economy.

## Requirements

Install exactly one:

- FTB Chunks 2101.1.20 or newer below 2102, plus its dependencies; or
- Open Parties and Claims 0.27.6 or newer in the supported Minecraft 1.21.1 line.

Minecraft 1.21.1, NeoForge 21.1.x, and Java 21 are required.
