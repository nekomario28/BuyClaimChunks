# BuyClaimChunks Continued 1.2.0

## Universal backend release

- Replaced separate FTB Chunks and OpenPAC artifacts with one universal NeoForge 1.21.1 JAR.
- Automatically selects FTB Chunks when only FTB Chunks is installed.
- Automatically selects Open Parties and Claims when only OpenPAC is installed.
- Safely disables `/buyclaim` when both or neither backend is installed.
- Preserves the same `/buyclaim [amount]` command, configuration file, price curve, limits, and payment behavior for both backends.

## OpenPAC support

- Stores purchased capacity in OpenPAC `BONUS_CHUNK_CLAIMS`.
- Supports an all-paid model with OpenPAC base and free bonuses configured to zero.
- Adds restart-persistence and dedicated-server validation for OpenPAC.

## Safety

- Detects concurrent quota changes before writing.
- Verifies the backend value before consuming payment.
- Attempts a verified rollback if an already validated payment unexpectedly cannot be consumed.
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
