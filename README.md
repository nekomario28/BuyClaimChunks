<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued logo" width="256">
</p>

<h1 align="center">💎 BuyClaimChunks Continued</h1>

<p align="center">
  <strong>Buy personal claim capacity with a configurable item currency.</strong>
</p>

<p align="center">
  <a href="README_ja.md">日本語</a> ·
  <a href="https://modrinth.com/mod/buyclaimchunks-continued">Modrinth</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/releases">Releases</a> ·
  <a href="LICENSE">MIT License</a>
</p>

BuyClaimChunks Continued is a server-side economy addon for Minecraft 1.21.1 / NeoForge. Players use `/buyclaim` to spend a configured vanilla or modded item and increase their personal claim capacity.

Version 1.2.0 uses **one universal JAR**. Install exactly one supported claim backend:

- **FTB Chunks**, or
- **Open Parties and Claims (OpenPAC)**.

The same command, configuration, price curve, limits, inventory behavior, and transaction safety are used for both backends.

> [!WARNING]
> Install exactly one backend. If both FTB Chunks and OpenPAC are installed, or neither is installed, the server still starts but `/buyclaim` is disabled to prevent updating the wrong quota.

## Features

- `/buyclaim` purchases one personal extra-claim slot.
- `/buyclaim <amount>` purchases multiple sequentially priced slots.
- Any registered item can be used as currency.
- Fixed or progressive pricing.
- Later curve changes are reconciled from server-recorded lifetime payment totals.
- Price increases never remove claims; the shortfall is carried into the next purchase.
- Price decreases grant compensation capacity supported by previous payments.
- Separate total-cap and per-command limits.
- Payment is counted across the hotbar and normal 36-slot inventory.
- Armor and offhand slots are excluded.
- Capacity and the purchase ledger are updated and verified before payment is consumed.
- Concurrent administrator changes are detected instead of overwritten.
- An unexpected payment failure triggers verified backend and ledger rollback.
- The selected claim mod remains the source of truth for current capacity.

The mod increases **capacity only**. It does not automatically claim the current chunk, sell force-loaded chunks, charge upkeep, or refund unclaims.

## Requirements

| Component | Supported version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Java | 21 |
| BuyClaimChunks Continued | one universal 1.2.0 JAR |
| FTB option | FTB Chunks 2101.1.20 or newer, below 2102, plus its dependencies |
| OpenPAC option | Open Parties and Claims 0.27.6 or newer in the supported 1.21.1 line |

Environment: server required, multiplayer clients optional. For singleplayer, install the mod in the instance because Minecraft runs an integrated server.

## Installation

1. Install Minecraft 1.21.1, NeoForge 21.1.x, and Java 21.
2. Install **exactly one** claim backend:
   - FTB Chunks and its required dependencies, or
   - Open Parties and Claims.
3. Place the single `buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar` in the `mods` directory.
4. Start the server once.
5. Stop the server before editing configuration.
6. Configure the economy and, for OpenPAC, configure the zero-base claim model if every slot should be paid.
7. Restart and test `/buyclaim` with a normal player account.

The original BuyClaimChunks and this continuation use the same `buyclaimchunks` mod ID and cannot be installed together.

## Commands

```text
/buyclaim
/buyclaim <amount>
```

The command is player-only because the transaction charges the player's inventory.

## Default configuration

The generated file is:

```text
config/buyclaimchunks-common.toml
```

Fresh installations use:

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

| Option | Default | Meaning |
|---|---:|---|
| `itemRequired` | `minecraft:diamond` | Registry ID of the payment item. Modded items use `modid:item_name`. |
| `amountRequired` | `4` | Price of the first slot and minimum per-slot price. |
| `priceGrowthFactor` | `3.45` | Strength of progressive price growth. Set to `0` for a fixed price. |
| `priceExponent` | `0.5` | Shape of the curve. `0.5` is square-root growth; `0` also gives a fixed price. |
| `maxExtraClaims` | `100` | Maximum backend-owned personal extra capacity. Administrator-granted extras count toward this cap. |
| `maxPurchaseAmount` | `100` | Maximum amount accepted by one command. |

Existing configuration files are not overwritten. An older installation may still contain `amountRequired = 1`; change it manually or regenerate the file to use the current default curve.

## How to change the settings

1. Stop the Minecraft server completely.
2. Back up `config/buyclaimchunks-common.toml`.
3. Open that file in a text editor.
4. Change only the values after `=` while keeping valid TOML syntax.
5. Save the file.
6. Start the server again.
7. Test `/buyclaim` with a player who has the configured currency.
8. Check `latest.log` if the change is rejected or the item ID is unknown.

Do not edit another instance's config, and do not rely on live reload. Restart the server after changing these values.

### Currency examples

Use emeralds with the default curve:

```toml
itemRequired = "minecraft:emerald"
```

Use a modded coin:

```toml
itemRequired = "examplemod:coin"
```

Charge a fixed 8 emeralds per slot:

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

## Default cost curve

For the one-based extra-capacity number `n`, the per-slot price is:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

With the defaults:

```text
round(4 + 3.45 * (sqrt(n) - 1))
```

| Slot number | Item cost | Cumulative cost through that slot |
|---:|---:|---:|
| 1 | 4 | 4 |
| 2 | 5 | 9 |
| 3 | 7 | 16 |
| 5 | 8 | 31 |
| 10 | 11 | 82 |
| 20 | 16 | 223 |
| 50 | 25 | 850 |
| 100 | 35 | 2,369 |

Bulk purchases sum the next slots individually:

- At 0 paid capacity, `/buyclaim 5` costs `4 + 5 + 7 + 7 + 8 = 31` items.
- At 8 paid capacity, `/buyclaim 3` costs slots 9–11: `11 + 11 + 12 = 34` items.

Pricing position uses the BuyClaimChunks purchase ledger, not administrator-granted capacity. Administrator grants still count toward `maxExtraClaims` because they consume usable backend capacity.

## Changing the curve after players have purchased claims

The server records, per player UUID, the configured currency ID, the number of claims bought through this mod, and the lifetime amount actually consumed.

After a price increase, existing claims are never removed. The next purchase includes the difference between the new cumulative curve and the amount previously paid:

```text
next payment
= new cumulative cost through the paid claims after this purchase
- lifetime consumed amount
```

After a price decrease, previous payments may support additional claims. On the next successful purchase, compensation claims are granted within `maxExtraClaims`, and any remaining credit continues to reduce later prices.

Changing `itemRequired` starts a new baseline because different items have no automatic exchange rate. Changing only the numeric curve values preserves exact debt or credit.

See [`docs/repricing-ledger.md`](docs/repricing-ledger.md) for examples, legacy-world behavior, and transaction details.

## Backend behavior

### FTB Chunks

The purchase updates the player's personal FTB Chunks extra-claim value. It does not increase a party-wide FTB Teams quota. Players still claim land through the normal FTB Chunks controls.

### Open Parties and Claims

The purchase updates OpenPAC's per-player `BONUS_CHUNK_CLAIMS` value.

For an all-paid model, set the effective free capacity to zero in the world's OpenPAC server configuration, normally:

```text
<world>/serverconfig/openpartiesandclaims-server.toml
```

Relevant values:

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

Also ensure no external rank or permission system grants a non-zero claim limit. BuyClaimChunks Continued warns when the effective OpenPAC base limit is not zero, but it does not silently rewrite OpenPAC's configuration.

See [`docs/openpac-setup.md`](docs/openpac-setup.md) for migration and troubleshooting details.

## Transaction safety

A purchase runs in this order:

1. Read current backend capacity and the purchase ledger.
2. Validate the amount, total cap, and overflow limits.
3. Calculate carried price debt or compensation credit and the requested batch.
4. Validate and count the payment item.
5. Update and verify backend capacity using compare-before-write semantics.
6. Compare-and-set the purchase ledger to the quoted result.
7. Consume payment.
8. If payment unexpectedly fails, restore both backend capacity and the ledger.
9. Send success only after capacity, ledger, and payment are confirmed.

A rejected transaction consumes no items.

## Universal-JAR selection rules

| Installed claim mods | Result |
|---|---|
| FTB Chunks only | FTB backend selected automatically |
| OpenPAC only | OpenPAC backend selected automatically |
| Neither | Server starts; purchases disabled |
| Both | Server starts; purchases disabled to avoid ambiguity |

The JAR contains only BuyClaimChunks Continued code and thin API adapters. It does not bundle either claim mod.

## Building and validation

Build the universal JAR:

```shell
./gradlew clean test build -Ptest_backend=none
bash scripts/verify-release-jar.sh
```

Validate with FTB Chunks:

```shell
./gradlew runGameTestServer -Ptest_backend=ftb
bash scripts/run-ftb-restart-integration.sh
```

Validate with OpenPAC:

```shell
./gradlew runGameTestServer -Ptest_backend=openpac
bash scripts/run-openpac-restart-integration.sh
```

The release JAR is generated under `build/libs/`.

## License and attribution

BuyClaimChunks Continued is MIT-licensed and is a maintained fork of the original MIT-licensed BuyClaimChunks by SkyAdri.

FTB Chunks and OpenPAC are separate external downloads and are not redistributed in this JAR. FTB Chunks is All Rights Reserved / visible source; OpenPAC is LGPL-3.0-only. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) and [`docs/license-review.md`](docs/license-review.md).

This project is not affiliated with or endorsed by SkyAdri, Feed The Beast Ltd, Xaero, Mojang, NeoForge, Modrinth, or CurseForge.