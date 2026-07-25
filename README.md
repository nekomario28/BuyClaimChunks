<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued logo" width="256">
</p>

<h1 align="center">💎 BuyClaimChunks Continued</h1>

<p align="center">
  <strong>Buy personal FTB Chunks extra-claim capacity with a configurable item currency.</strong>
</p>

<p align="center">
  <a href="https://github.com/nekomario28/BuyClaimChunks/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/nekomario28/BuyClaimChunks/build.yml?branch=main&style=flat-square&label=build"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square">
  <img alt="NeoForge 21.1" src="https://img.shields.io/badge/NeoForge-21.1-EF7E25?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square"></a>
</p>

<p align="center">
  <img alt="Server required" src="https://img.shields.io/badge/Server-required-C83A3A?style=flat-square">
  <img alt="Client optional" src="https://img.shields.io/badge/Client-optional-6A7FDB?style=flat-square">
</p>

<p align="center">
  <a href="README_ja.md">日本語</a> ·
  <a href="https://modrinth.com/mod/buyclaimchunks-continued">Modrinth</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/releases">Releases</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/issues">Issues</a> ·
  <a href="LICENSE">License</a>
</p>

---

BuyClaimChunks Continued is a small server-side economy addon for [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge). Players spend a configurable vanilla or modded item to increase their **personal extra-claim allowance** through `/buyclaim`.

It is an independently maintained continuation of [SkyAdri's BuyClaimChunks](https://github.com/SkyAdri-mc/BuyClaimChunks). The original project is MIT-licensed; this fork is not affiliated with or endorsed by the original author or FTB.

## 📌 At a glance

| | |
|---|---|
| **Command** | `/buyclaim [amount]` |
| **Environment** | Server required, client optional; singleplayer supported |
| **Currency** | Any registered vanilla or modded item |
| **Pricing** | Fixed or progressive |
| **Configuration** | `config/buyclaimchunks-common.toml` |
| **Data source** | FTB Chunks personal extra-claim value |

> [!IMPORTANT]
> This mod increases claim **capacity**. It does not automatically claim a map chunk, increase a party-wide allowance, or add force-loaded chunks.

## ✨ Features

- Buy one or multiple personal extra-claim slots with `/buyclaim [amount]`.
- Use any registered item as currency, including modded coins and resources.
- Configure a constant price or a progressive curve based on the current FTB Chunks personal extra-claim total.
- Count matching items across the hotbar and normal 36-slot inventory.
- Update FTB Chunks first and consume payment only after the quota increase succeeds.
- Configure separate limits for one command and the resulting personal extra-claim total.
- Reject invalid item IDs, overflowed totals, and unsafe transactions without consuming payment.
- Reuse FTB Chunks as the source of truth instead of maintaining a second quota database.

## 📦 Supported versions

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

A legacy Minecraft 1.20.1 / Forge 47.4 version remains on the [`forge-1.20.1`](https://github.com/nekomario28/BuyClaimChunks/tree/forge-1.20.1) branch. Its behavior and configuration may differ from this guide.

## 🚀 Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Install FTB Chunks and its required dependencies: FTB Library, FTB Teams, and Architectury API.
3. Download the current JAR from [Modrinth](https://modrinth.com/mod/buyclaimchunks-continued) or [GitHub Releases](https://github.com/nekomario28/BuyClaimChunks/releases).
4. Place the JAR in the server's `mods` directory.
5. Start the server once to generate `config/buyclaimchunks-common.toml`.
6. Stop the server before changing the generated configuration.
7. Restart and test `/buyclaim` with a normal player account.

For singleplayer, place the JAR in the normal instance `mods` directory because the integrated server runs the command logic.

> [!WARNING]
> This fork preserves the original `buyclaimchunks` mod ID. Do not install it together with the original BuyClaimChunks JAR.

## ⚡ Quick start

The default currency is `minecraft:diamond`, and the first extra-claim slot costs 4 diamonds.

```text
/buyclaim
```

Buys one personal extra-claim slot.

```text
/buyclaim 5
```

Buys the next five slots in one transaction. Every slot is priced individually, then the prices are added together.

The command is available to ordinary players. It cannot be run from the server console or a command block because payment requires a player inventory.

## 🔍 What changes after a purchase

A successful transaction increases the player's **personal extra-claim allowance stored by FTB Chunks**. The player still claims land through the normal FTB Chunks map or claim controls.

Because FTB Chunks remains the source of truth:

- Extra quota granted by an administrator affects the next purchase price.
- `maxExtraClaims` limits the total personal extra-claim value, not only slots purchased through this mod.
- Removing the mod does not leave a separate quota database to migrate.

## 🛡️ Transaction safety

Payment items are counted in the hotbar and normal inventory. Armor and offhand slots are not included.

The transaction runs in this order:

1. Read the current personal extra-claim total from FTB Chunks.
2. Validate purchase limits and calculate the complete batch price.
3. Validate the configured item and count the player's inventory.
4. Execute the FTB Chunks quota increase synchronously.
5. Consume payment only when FTB Chunks reports success.

If the quota update fails, the purchase stops and no items are consumed.

## ⚙️ Configuration

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
| `itemRequired` | `minecraft:diamond` | Registered item ID | Payment item, including `modid:item_name`. |
| `amountRequired` | `4` | 1 to 2,147,483,647 | Base and minimum price. |
| `priceGrowthFactor` | `3.45` | 0 to 1,000,000 | Controls how quickly later slots become more expensive. `0` produces a constant price. |
| `priceExponent` | `0.5` | 0 to 4 | Controls the curve shape. `0.5` is a square-root curve; `0` produces a constant price. |
| `maxExtraClaims` | `100` | 1 to 2,147,483,647 | Maximum resulting FTB Chunks personal extra-claim total. |
| `maxPurchaseAmount` | `100` | 1 to 10,000 | Maximum amount accepted by one command. |

Stop the server before editing the file. An unknown item ID is detected when a purchase is attempted, and no payment is taken.

### Currency examples

```toml
# Emeralds
itemRequired = "minecraft:emerald"
```

```toml
# A modded currency
itemRequired = "examplemod:coin"
```

### Constant-price example

Charge 8 emeralds for every slot:

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

Setting either `priceGrowthFactor` or `priceExponent` to `0` keeps the price at `amountRequired`.

## 📈 Progressive pricing

The one-based personal extra-claim number `n` costs:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

The result is never allowed below `amountRequired`.

| Personal extra-claim number | Default item cost |
|---:|---:|
| 1 | 4 |
| 2 | 5 |
| 3 | 7 |
| 5 | 8 |
| 10 | 11 |
| 50 | 25 |
| 100 | 35 |

Batch purchases sum each next slot in sequence:

- From 0 extra claims, `/buyclaim 5` costs `4 + 5 + 7 + 7 + 8 = 31` diamonds.
- From 8 extra claims, `/buyclaim 3` costs the prices of slots 9–11: `11 + 11 + 12 = 34` diamonds.

## 🧩 Compatibility

### Original BuyClaimChunks

The original mod and this continuation cannot be loaded together because both use the `buyclaimchunks` mod ID. Remove the original JAR before installing this one.

### Buying Chunks — FTB Chunks Addon

A static inspection of [`snoopypupserr/buying_chunks_ftbchunks_addon`](https://github.com/snoopypupserr/buying_chunks_ftbchunks_addon) found no direct mod ID, root-command, or configuration-file conflict. The mods serve different purposes:

- **BuyClaimChunks Continued** sells additional personal claim capacity.
- **Buying Chunks** provides a chunk marketplace and can charge a Base Cost when land is claimed.

They are expected to coexist when their shared FTB dependencies use compatible versions. However, enabling Buying Chunks' **Base Cost** means players may pay once for extra capacity and again when actually claiming land. Disable Base Cost when that two-stage economy is not intended.

This is a source-level compatibility assessment, not a guarantee for every future release or modpack combination. Buying Chunks is required on both server and clients, so that requirement applies to a combined installation.

## 🔄 Upgrading from the original mod or 1.0

- Remove the original BuyClaimChunks JAR first.
- Existing FTB Chunks personal extra-claim values remain the source of truth.
- Existing `buyclaimchunks-common.toml` values are preserved.
- Change an old `amountRequired = 1` value to `4` to use the current default curve.
- Alternatively, stop the server, delete the config, and restart to generate current defaults.
- Back up the world and configuration before changing production mods.

## 🛠️ Troubleshooting

<details>
<summary><strong><code>/buyclaim</code> is unknown</strong></summary>

Confirm that the NeoForge 1.21.1 JAR is in the server's `mods` directory, all FTB dependencies loaded successfully, and the log does not report a duplicate `buyclaimchunks` mod ID.
</details>

<details>
<summary><strong>“This command can only be run by a player!”</strong></summary>

Run the command in-game. Consoles and command blocks do not have a player inventory to charge.
</details>

<details>
<summary><strong>The configured item does not exist</strong></summary>

Check the spelling and namespace of `itemRequired`, and confirm that the mod registering the item is installed on the server.
</details>

<details>
<summary><strong>The player owns the item but the mod reports a shortage</strong></summary>

Move the item from armor or offhand slots into the hotbar or normal inventory.
</details>

<details>
<summary><strong>The price is higher than expected</strong></summary>

Check the player's current FTB Chunks personal extra-claim value. Administrator-granted quota is included in the price position.
</details>

<details>
<summary><strong>“The claim purchase failed. No items were consumed.”</strong></summary>

The internal FTB Chunks quota command did not report success. Check the supported FTB Chunks version and server log. Payment remains untouched by design.
</details>

<details>
<summary><strong>Configuration changes are not applied</strong></summary>

Stop the server, edit the server-side `config/buyclaimchunks-common.toml`, save it, and restart. Make sure you did not edit another instance's config.
</details>

## 🧪 Building from source

Use Java 21:

```shell
./gradlew clean test build
```

The JAR is generated at:

```text
build/libs/buyclaimchunks-continued-neoforge-1.21.1-1.1.0.jar
```

Run a development dedicated server with:

```shell
./gradlew runServer
```

GitHub Actions runs tests, builds the JAR, starts a clean dedicated server, and requires it to reach Minecraft's `Done` startup message.

## 🐛 Reporting problems

Open an [issue](https://github.com/nekomario28/BuyClaimChunks/issues) and include:

- Minecraft, NeoForge, FTB Chunks, and BuyClaimChunks Continued versions.
- The relevant section of `latest.log` or the crash report.
- The command and configuration values involved.
- Whether the issue reproduces with a newly generated config.

Do not include account tokens, control-panel passwords, or private server addresses.

## 📄 License and credits

BuyClaimChunks Continued is distributed under the [MIT License](LICENSE) and maintained by **nekomario28**.

The original work is credited to SkyAdri. See [NOTICE](NOTICE) for attribution and fork details.