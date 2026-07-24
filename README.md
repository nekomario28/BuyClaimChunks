# BuyClaimChunks Continued

<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued logo" width="256">
</p>

<p align="center">
  <a href="README_ja.md">日本語</a> |
  <a href="https://github.com/nekomario28/BuyClaimChunks/actions/workflows/build.yml">Build status</a> |
  <a href="LICENSE">MIT License</a>
</p>

Buy personal extra-claim capacity for [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge) with a configurable item currency.

This is an independently maintained fork of [SkyAdri's BuyClaimChunks](https://github.com/SkyAdri-mc/BuyClaimChunks). The original project is available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/buyclaimchunks) under the MIT License. This fork is not affiliated with or endorsed by the original author or FTB.

## What this mod does

- `/buyclaim [amount]` buys one or more **personal FTB Chunks extra-claim slots**.
- Any registered vanilla or modded item can be used as currency.
- Prices can be constant or progressively increase with the player's current personal extra-claim total.
- Multiple stacks in the normal inventory and hotbar are counted together.
- FTB Chunks is updated first; payment is consumed only after the quota update succeeds.
- Per-command purchase size and total personal extra claims have separate configurable limits.
- Price calculations detect overflow instead of charging an invalid amount.

This mod **does not** automatically claim a map chunk, increase the FTB Teams party allowance, sell force-loaded chunks, or store a separate purchase counter.

## Supported versions

The `main` branch and this guide cover Minecraft 1.21.1.

| Component | Supported / tested version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1; tested with 21.1.242 |
| Java | 21 |
| FTB Chunks | 2101.1.20 up to, but not including, 2102 |
| FTB Teams | tested with 2101.1.9 |
| FTB Library | tested with 2101.1.30 |
| Architectury API | tested with 13.0.8 |

A legacy Minecraft 1.20.1 / Forge 47.4 version is kept on the `forge-1.20.1` branch. Its code and configuration may differ from this guide.

## Requirements

Install the following before BuyClaimChunks Continued:

1. NeoForge for Minecraft 1.21.1.
2. FTB Chunks.
3. FTB Chunks' required dependencies: FTB Library, FTB Teams, and Architectury API.
4. Java 21 for the server process.

The command and quota update run on the server. For distributed modpacks, including the same JAR on both server and clients is the least-surprise setup when a launcher or server enforces matching mod lists.

## Installation

1. Download the Minecraft 1.21.1 NeoForge JAR from the repository's [Releases](https://github.com/nekomario28/BuyClaimChunks/releases) page.
2. Stop the server.
3. Place the JAR in the server's `mods` directory.
4. Confirm that FTB Chunks and all of its dependencies are also in `mods`.
5. Start the server once. NeoForge creates `config/buyclaimchunks-common.toml`.
6. Stop the server before editing the generated configuration.
7. Start the server and test `/buyclaim` with a normal player account.

This fork keeps the original `buyclaimchunks` mod ID for world and modpack compatibility. **Do not install it alongside the original BuyClaimChunks JAR.**

## Quick start

With the default configuration, the currency is `minecraft:diamond` and the first extra-claim slot costs 4 diamonds.

```text
/buyclaim
```

Buys one personal extra-claim slot.

```text
/buyclaim 5
```

Buys the next five personal extra-claim slots in one transaction. Each slot is priced individually and the five prices are added together.

The command is available to ordinary players and must be run by a player. Running it from the server console or a command block fails because there is no player inventory to charge.

## Exactly what changes

A successful purchase increases the player's FTB Chunks **personal extra-claim allowance**. It does not select or claim a chunk for the player.

After buying capacity, the player still claims chunks through the normal FTB Chunks map or claim controls.

The current price and maximum check use the personal extra-claim count stored by FTB Chunks. Therefore:

- Extra quota granted by an administrator affects the next purchase price.
- `maxExtraClaims` is an absolute ceiling for the FTB Chunks personal extra-claim total, not only the number bought through this mod.
- Removing or changing this mod does not create a separate quota database to migrate.

## Payment behavior

The configured item is counted in the player's normal 36-slot inventory, including the hotbar. Armor slots and the offhand slot are not included; move payment items into the hotbar or main inventory before purchasing.

Matching stacks are added together. After FTB Chunks confirms the quota update, items are removed from matching inventory stacks until the full cost has been paid.

The transaction order is:

1. Read the current personal extra-claim total from FTB Chunks.
2. Validate limits and calculate the full batch price.
3. Validate the configured item and count the player's inventory.
4. Execute the FTB Chunks quota command synchronously.
5. Consume payment only if the FTB Chunks command reports success.

If the FTB Chunks command fails or changes incompatibly in a future release, the purchase stops and no items are consumed.

## Configuration

Configuration file:

```text
config/buyclaimchunks-common.toml
```

Default values:

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

| Option | Default | Allowed values | Meaning |
|---|---:|---|---|
| `itemRequired` | `minecraft:diamond` | A registered item ID | Item used as payment, including modded items such as `modid:item_name`. |
| `amountRequired` | `4` | 1 to 2,147,483,647 | Base price and minimum price; the cost of personal extra-claim number 1. |
| `priceGrowthFactor` | `3.45` | 0 to 1,000,000 | Controls how quickly later slots become more expensive. Set to `0` for a constant price. |
| `priceExponent` | `0.5` | 0 to 4 | Controls the curve shape. `0.5` is a square-root curve. Set to `0` for a constant price. |
| `maxExtraClaims` | `100` | 1 to 2,147,483,647 | Maximum FTB Chunks personal extra-claim total allowed before purchases are rejected. |
| `maxPurchaseAmount` | `100` | 1 to 10,000 | Maximum amount accepted by one `/buyclaim` command. |

Stop the server before editing the file. Invalid values may be rejected by NeoForge configuration validation. An unknown `itemRequired` ID is detected when a player attempts a purchase and no payment is taken.

### Currency examples

Use emeralds:

```toml
itemRequired = "minecraft:emerald"
```

Use a modded item:

```toml
itemRequired = "examplemod:coin"
```

The item ID must exist in the server's item registry.

### Constant-price example

Charge 8 emeralds for every slot:

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

Setting either `priceGrowthFactor` or `priceExponent` to `0` makes the calculated price constant at `amountRequired`.

## Progressive pricing

The one-based personal extra-claim number `n` costs:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

The result is never allowed below `amountRequired`.

With the defaults:

| Personal extra-claim number | Item cost |
|---:|---:|
| 1 | 4 |
| 2 | 5 |
| 3 | 7 |
| 5 | 8 |
| 10 | 11 |
| 50 | 25 |
| 100 | 35 |

A batch purchase sums every next slot in order. For example:

- A player with 0 personal extra claims pays `4 + 5 + 7 + 7 + 8 = 31` diamonds for `/buyclaim 5`.
- A player with 8 personal extra claims pays the prices of slots 9, 10, and 11: `11 + 11 + 12 = 34` diamonds for `/buyclaim 3`.

## Limits

`maxPurchaseAmount` limits one command. `maxExtraClaims` limits the resulting personal extra-claim total.

Example:

```toml
maxExtraClaims = 100
maxPurchaseAmount = 10
```

A player may buy at most 10 slots at once and may not exceed 100 personal extra claims in FTB Chunks. If an administrator has already granted that player 100 extra claims, further purchases are rejected even if none were bought through this mod.

## Upgrading from the original mod or 1.0

- Remove the original BuyClaimChunks JAR before installing this fork because both use the `buyclaimchunks` mod ID.
- Existing FTB Chunks personal extra-claim values remain the source of truth.
- Existing `buyclaimchunks-common.toml` values are preserved and are not silently replaced.
- If an older config contains `amountRequired = 1`, change it to `4` to use the new default curve.
- Alternatively, stop the server, delete `config/buyclaimchunks-common.toml`, and restart to generate current defaults.
- Back up the server and configuration before changing production mods.

## Troubleshooting

### `/buyclaim` is unknown

Confirm that:

- The JAR is in the server's `mods` directory.
- It is the NeoForge 1.21.1 build, not the Forge 1.20.1 build.
- FTB Chunks and all dependencies loaded successfully.
- The server log does not show a duplicate `buyclaimchunks` mod ID.

### “This command can only be run by a player!”

Run the command in-game. The console and command blocks do not have a player inventory and cannot purchase slots.

### “The configured item ... does not exist”

Check `itemRequired` for spelling, namespace, and whether the mod that registers the item is installed on the server.

### The player owns the item but the mod says there is not enough

Move the item from armor or offhand slots into the normal inventory or hotbar. Only the 36 normal inventory slots are counted.

### The price is higher than expected

Check the player's current FTB Chunks personal extra-claim value. Administrator-granted quota is included in the price position.

### “The claim purchase failed. No items were consumed.”

The internal FTB Chunks quota command did not report success. Confirm the supported FTB Chunks version and inspect the server log. The payment remains untouched by design.

### Config changes appear to do nothing

Stop the server, edit `config/buyclaimchunks-common.toml`, save it, and restart. Also confirm that you edited the server's config rather than a client or unrelated instance.

## Building from source

Use Java 21:

```shell
./gradlew clean test build
```

The JAR is written to:

```text
build/libs/buyclaimchunks-continued-neoforge-1.21.1-1.1.0.jar
```

Run a dedicated-server development instance with:

```shell
./gradlew runServer
```

The GitHub Actions workflow performs tests, builds the JAR, starts a clean dedicated server, and requires it to reach the Minecraft `Done` startup message. Workflow artifacts are temporary development outputs, not permanent end-user releases.

## Reporting problems

Open an [issue](https://github.com/nekomario28/BuyClaimChunks/issues) and include:

- Minecraft, NeoForge, FTB Chunks, and BuyClaimChunks Continued versions.
- The relevant section of `latest.log` or the crash report.
- The command used and configuration values.
- Whether the problem occurs with a newly generated config.

Do not include account tokens, server control-panel passwords, or private addresses.

## License and credits

BuyClaimChunks Continued is distributed under the [MIT License](LICENSE).

Original work is credited to SkyAdri. Fork maintenance, NeoForge 1.21.1 support, progressive pricing, safety changes, tests, CI, documentation, and independent branding are maintained by Yuu (`nekomario28`). See [NOTICE](NOTICE) for details.
