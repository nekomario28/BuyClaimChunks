# OpenPAC setup for the universal JAR

This guide applies to BuyClaimChunks Continued 1.2.0 for Minecraft 1.21.1, NeoForge 21.1.x, and Java 21.

## Installation model

BuyClaimChunks Continued ships one file:

```text
buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar
```

Install that JAR with **Open Parties and Claims only**. Do not also install FTB Chunks. The universal JAR selects OpenPAC automatically when it is the only supported backend present.

If both backends or neither backend are installed, the server starts but purchases are disabled to prevent ambiguous quota updates.

## OpenPAC value used by purchases

A purchase always updates the executing player's own OpenPAC bonus claim capacity:

```text
PlayerConfigOptions.BONUS_CHUNK_CLAIMS
```

The BuyClaimChunks economic ledger is also keyed by player UUID. Joining or leaving an OpenPAC party does not merge, move, or refund another player's purchase ledger or `BONUS_CHUNK_CLAIMS`.

Administrator-granted backend bonus capacity still occupies real backend capacity and counts toward `maxExtraClaims`, but it is not added to BuyClaimChunks purchase history and does not create new pricing credit.

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
maxPlayerClaims = 0
maxPlayerClaimsPermission = ""
claimBonusPerPartyMember = 0
claimBonusForPartyOwner = 0
```

5. Ensure no permission plugin, rank, command, or addon grants a non-zero free claim limit.
6. Restart the server and test with a normal player.

BuyClaimChunks Continued does not silently rewrite OpenPAC's configuration. A non-zero effective base limit produces a warning and means some capacity remains free.

### Choosing `partyOwnedClaims`

BuyClaimChunks does not force this option.

```toml
partyOwnedClaims = false
```

keeps the ordinary player-owned model.

```toml
partyOwnedClaims = true
```

enables OpenPAC's PARTY claiming mode. This does **not** create a separate party quota. OpenPAC resolves PARTY-mode claims to the primary party owner's player UUID.

For an all-paid server that intentionally uses party-owned claims, keep `partyOwnedClaims = true` while leaving `maxPlayerClaims`, `claimBonusPerPartyMember`, `claimBonusForPartyOwner`, and permission-derived free capacity at zero.

## Exact OpenPAC party purchase and claim model

BuyClaimChunks follows OpenPAC's UUID ownership model and does not create its own party account.

### Non-owner member

If member B runs `/buyclaim`, only B's own `BONUS_CHUNK_CLAIMS` and B's own purchase ledger increase.

```text
B /buyclaim
-> B UUID bonus + ledger increase
-> party owner A UUID bonus is unchanged
```

If B claims in PLAYER mode, B uses B's own claim count and limit.

If B claims in PARTY mode, OpenPAC resolves the claim owner to primary party owner A. The claim therefore uses A's claim count and A's full claim limit. B's previously purchased personal capacity is not summed into A's party-owner pool.

### Party owner

If owner A runs `/buyclaim`, A's own `BONUS_CHUNK_CLAIMS` increases.

A's PLAYER-mode claims and all authorized members' PARTY-mode claims are technically owned by A's UUID, so they share the same claim count and full claim limit.

```text
A PLAYER claim        3
B PARTY-mode claim    4
C PARTY-mode claim    2
-----------------------
A UUID claim count    9
```

There is no separate “10 personal + 10 party” pool for the owner.

### Joining and leaving

Party membership changes do not move BuyClaimChunks economic state.

```text
B joins party
-> B ledger stays B's
-> B BONUS_CHUNK_CLAIMS stays B's
-> nothing is automatically added to owner A

B leaves party
-> B ledger stays B's
-> B PLAYER claims stay owned by B
-> claims B created in PARTY mode were already owned by A and stay owned by A
```

This avoids capacity duplication or automatic refunds during membership churn.

### Owner transfer

OpenPAC can transfer party ownership. BuyClaimChunks does not automatically move the old owner's or new owner's `BONUS_CHUNK_CLAIMS` or purchase ledger during that operation.

The 1.2.0 release gate runs the real OpenPAC 0.29.3 transfer command after creating PLAYER claims, PARTY claims, purchases, and separate ledgers for both players. It then verifies the party ID, existing claim owner UUIDs, both bonus values, both ledgers, and the owner UUID used by a new PARTY-mode claim.

Until that runtime probe passes, BuyClaimChunks does not describe owner transfer as an automatic migration of party assets.

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

The one-based paid slot number `n` costs:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

With the defaults, slot 1 costs 4 diamonds, slot 10 costs 11, slot 50 costs 25, and slot 100 costs 35. Set `priceGrowthFactor` or `priceExponent` to `0` for a fixed price.

To change the economy, stop the server, edit `config/buyclaimchunks-common.toml`, save it, restart, and test `/buyclaim`. Existing configs are retained and are not replaced by new defaults.

## Writable bonus option

`BONUS_CHUNK_CLAIMS` must remain writable for the per-player config. A purchase is rejected without charging items when OpenPAC reports that the option is illegal, unavailable, forced to an incompatible default, changed concurrently, or not persisted.

## Repricing and the purchase ledger

BuyClaimChunks stores the currency ID, BuyClaimChunks-paid capacity, and lifetime amount actually consumed per player UUID.

- same-currency price increase: existing claims are not confiscated; the shortfall is carried into a later purchase;
- same-currency price decrease: previous payments can support compensation capacity on a later successful purchase;
- currency change: no exchange rate is guessed, so a same-paid-capacity baseline is created for the new item;
- party join, leave, or owner-state change: the ledger is not transferred to another UUID.

See [`repricing-ledger.md`](repricing-ledger.md) for exact examples.

## Transaction behavior

1. Read the executing player's OpenPAC bonus and per-player ledger snapshot.
2. Validate amount and total limits.
3. Calculate carried debt, compensation credit, and sequential slot price.
4. Validate and count payment.
5. Compare-before-write and verify the executing player's absolute bonus value.
6. Compare-and-set the same UUID's purchase ledger.
7. Consume payment.
8. Roll back both bonus and ledger if validated payment unexpectedly fails.
9. When relevant, explain whether that OpenPAC UUID pool is personal-only or is also the current primary-party owner pool.

## Migrating from FTB Chunks

There is no automatic FTB-to-OpenPAC quota migration because automatic conversion can duplicate capacity while both data sets remain in backups.

Recommended process:

1. Back up the world and both configurations.
2. Record each player's FTB personal extra-claim value.
3. Remove FTB Chunks and its unused dependencies.
4. Install OpenPAC while keeping the same universal BuyClaimChunks JAR.
5. Apply the zero-base OpenPAC configuration.
6. Grant equivalent OpenPAC bonus values to the same player UUIDs manually when purchased capacity must be preserved.
7. Do not automatically merge member values into the party owner when enabling party-owned claims.
8. Verify PLAYER mode, PARTY mode, and `/buyclaim` with normal players before reopening the server.

## CI evidence required for release

The universal-JAR release gate requires:

- unit tests and one universal JAR build;
- universal JAR inspection with both thin adapters and no bundled claim mod;
- OpenPAC NeoForge GameTests;
- real `/buyclaim` capacity `0 -> 1` and payment `4 -> 0`;
- zero base and full limit `0 -> 1`;
- normal shutdown and second-JVM reload;
- a dedicated `partyOwnedClaims=true` semantics GameTest;
- non-owner member purchases remaining on the member UUID;
- member PARTY-mode claims using the owner UUID;
- member PLAYER-mode claims using the member UUID;
- member departure not moving bonuses, ledgers, or existing claim ownership;
- real owner transfer with party ID, claim UUIDs, both bonuses, both ledgers, and subsequent PARTY claim verified;
- clean dedicated-server startup;
- safe startup and no-charge rejection with neither or both backends installed.
