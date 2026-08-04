# OpenPAC backend setup

This guide applies to the Open Parties and Claims build of BuyClaimChunks Continued for Minecraft 1.21.1, NeoForge 21.1.x, and Java 21.

## Choose exactly one backend

Install one of these mutually exclusive artifacts:

```text
buyclaimchunks-continued-ftb-neoforge-1.21.1-1.2.0.jar
buyclaimchunks-continued-openpac-neoforge-1.21.1-1.2.0.jar
```

Both artifacts use mod ID `buyclaimchunks` and the same config file. They must not be installed together.

The OpenPAC artifact requires Open Parties and Claims 0.27.6 or a compatible release below 0.28. FTB Chunks is not required by the OpenPAC artifact.

## Feature parity

The OpenPAC build intentionally keeps the FTB build's player-visible contract:

- `/buyclaim` buys one personal extra claim slot.
- `/buyclaim <amount>` buys a batch in one transaction.
- The same item currency and progressive pricing options are used.
- The same per-command and total-extra limits are used.
- Payment is counted across the normal 36-slot inventory, including the hotbar and excluding armor and offhand.
- Capacity is updated and verified before payment is consumed.
- A rejected capacity update consumes no payment.
- Administrator-granted bonus capacity affects the next price and `maxExtraClaims` check.
- No separate purchase counter or quota database is stored by BuyClaimChunks.
- The command buys capacity only; it does not automatically claim a map chunk.

For OpenPAC, the source of truth is:

```text
playerConfig.claims.bonusChunkClaims
```

which is exposed through `PlayerConfigOptions.BONUS_CHUNK_CLAIMS`.

## All-paid claim configuration

To make every usable claim slot purchasable, the effective OpenPAC base capacity and every source of free capacity must be zero.

Stop the server and edit the world/server copy of `openpartiesandclaims-server.toml`. The relevant settings are:

```toml
[serverConfig]
permissionSystem = ""

[serverConfig.claims]
enabled = true
partyOwnedClaims = false
maxPlayerClaims = 0
maxPlayerClaimsPermission = ""
claimBonusPerPartyMember = 0
claimBonusForPartyOwner = 0
```

Also ensure that no permission plugin, rank, command, or other addon grants a non-zero base claim limit.

BuyClaimChunks does not silently rewrite OpenPAC configuration. If a player's effective base capacity is non-zero, purchases still work, but some claim capacity remains free and the server log records a warning.

## Bonus option must be writable

The OpenPAC player option `BONUS_CHUNK_CLAIMS` must be allowed and not forced to a server default that prevents per-player changes.

A purchase is rejected without payment when OpenPAC reports any of these conditions:

- the option is illegal or unavailable;
- the option is not directly configurable;
- the current value changed during the transaction;
- the requested value was not persisted after the write;
- OpenPAC was not initialized or failed unexpectedly.

## BuyClaimChunks configuration

The OpenPAC artifact uses the existing file:

```text
config/buyclaimchunks-common.toml
```

The keys and defaults are unchanged:

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

The price of one-based extra slot number `n` is:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

Set `priceGrowthFactor` or `priceExponent` to `0` for a constant price.

## Transaction safety

The purchase order is:

1. Read the current OpenPAC bonus capacity.
2. Validate purchase and total limits.
3. Calculate the full batch price.
4. Validate and count the payment item.
5. Re-read the capacity to detect concurrent changes.
6. Write the absolute new bonus value and re-read it for verification.
7. Consume payment.
8. If payment unexpectedly fails after the verified grant, attempt to restore the previous bonus value and log the result.

## Moving from FTB Chunks

There is no automatic FTB-to-OpenPAC quota migration. Automatic migration could duplicate capacity while an administrator still has both claim data sets in a world backup.

A safe migration is:

1. Back up the world and both claim configurations.
2. Record every player's FTB personal extra-claim value.
3. Remove the FTB backend JAR and FTB Chunks only after the backup is complete.
4. Install OpenPAC and the OpenPAC backend JAR.
5. Apply the zero-base configuration.
6. Grant equivalent OpenPAC bonus values manually when preserving purchased capacity is desired.
7. Test `/buyclaim` with a normal non-operator player before reopening the server.

## Validation performed by CI

The OpenPAC release gate requires:

- Java 21 unit tests and build;
- backend-specific JAR inspection with no FTB backend classes;
- NeoForge GameTests;
- real `/buyclaim` purchase from bonus `0` to `1`;
- payment from four diamonds to zero;
- base limit `0` and derived full limit `1`;
- normal server shutdown and second-JVM bonus reload;
- clean dedicated-server startup to Minecraft's `Done` message.
