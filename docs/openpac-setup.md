# OpenPAC setup for the universal JAR

This guide applies to BuyClaimChunks Continued 1.2.0 for Minecraft 1.21.1, NeoForge 21.1.x, and Java 21.

## Installation model

BuyClaimChunks Continued now ships one file:

```text
buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar
```

Install that JAR with **Open Parties and Claims only**. Do not also install FTB Chunks. The universal JAR selects OpenPAC automatically when it is the only supported backend present.

If both backends or neither backend are installed, the server starts but purchases are disabled to prevent ambiguous quota updates.

## OpenPAC value used by purchases

Purchases update the player's OpenPAC bonus claim capacity exposed as:

```text
PlayerConfigOptions.BONUS_CHUNK_CLAIMS
```

No separate BuyClaimChunks quota database is created. Administrator-granted OpenPAC bonus capacity affects the next price and the `maxExtraClaims` check.

## All-paid claim configuration

For every usable claim slot to come from purchases, the effective OpenPAC base capacity and all free-capacity sources must be zero.

1. Stop the server.
2. Back up the world and OpenPAC configuration.
3. Edit the world's server configuration, normally:

```text
<world>/serverconfig/openpartiesandclaims-server.toml
```

4. Set the relevant values:

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

5. Ensure no permission plugin, rank, command, or addon grants a non-zero claim limit.
6. Restart the server and test with a normal player.

BuyClaimChunks Continued does not silently rewrite OpenPAC's configuration. A non-zero effective base limit produces a warning and means some capacity remains free.

## BuyClaimChunks default configuration

File:

```text
config/buyclaimchunks-common.toml
```

Defaults:

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

The one-based slot number `n` costs:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

With the defaults, slot 1 costs 4 diamonds, slot 10 costs 11, slot 50 costs 25, and slot 100 costs 35. Set `priceGrowthFactor` or `priceExponent` to `0` for a fixed price.

To change the economy, stop the server, edit `config/buyclaimchunks-common.toml`, save it, restart, and test `/buyclaim`. Existing configs are retained and are not replaced by new defaults.

## Writable bonus option

`BONUS_CHUNK_CLAIMS` must remain writable for the per-player config. A purchase is rejected without charging items when OpenPAC reports that the option is illegal, unavailable, forced to an incompatible default, changed concurrently, or not persisted.

## Transaction behavior

1. Read the current OpenPAC bonus capacity.
2. Validate amount and total limits.
3. Calculate the full sequential batch price.
4. Validate and count payment.
5. Re-read capacity to detect concurrent changes.
6. Set and verify the absolute new bonus value.
7. Consume payment.
8. Roll back the capacity if validated payment unexpectedly fails.

## Migrating from FTB Chunks

There is no automatic FTB-to-OpenPAC quota migration because automatic conversion can duplicate capacity while both data sets remain in backups.

Recommended process:

1. Back up the world and both configurations.
2. Record each player's FTB personal extra-claim value.
3. Remove FTB Chunks and its unused dependencies.
4. Install OpenPAC while keeping the same universal BuyClaimChunks JAR.
5. Apply the zero-base OpenPAC configuration.
6. Grant equivalent OpenPAC bonus values manually when purchased capacity must be preserved.
7. Verify `/buyclaim` with a normal player before reopening the server.

## CI evidence required for release

The universal-JAR release gate tests OpenPAC independently and requires:

- unit tests and one universal JAR build;
- universal JAR inspection with both thin adapters and no bundled claim mod;
- OpenPAC NeoForge GameTests;
- real `/buyclaim` capacity `0 -> 1` and payment `4 -> 0`;
- zero base and full limit `0 -> 1`;
- normal shutdown and second-JVM reload;
- clean dedicated-server startup;
- safe startup with neither or both backends installed.
