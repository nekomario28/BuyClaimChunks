# BuyClaimChunks Continued

Buy personal extra claim capacity for
[FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge)
with a configurable item currency.

This is an independently maintained fork of
[SkyAdri's BuyClaimChunks](https://github.com/SkyAdri-mc/BuyClaimChunks).
The original project is available on
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/buyclaimchunks)
under the MIT License. This fork is not affiliated with or endorsed by the
original author or FTB.

## Features

- `/buyclaim [amount]` purchases one or more personal extra-claim slots.
- Any registered vanilla or modded item can be used as currency.
- Progressive prices can increase as a player's purchased quota grows.
- Items are consumed only after FTB Chunks confirms the quota update.
- Purchase size and total personal extra claims have separate limits.
- Price calculations are overflow-safe.

## Supported versions

| Minecraft | Loader | Branch | Java |
|---|---|---|---|
| 1.21.1 | NeoForge 21.1 | `neoforge-1.21.1` | 21 |
| 1.20.1 | Forge 47.4 | `forge-1.20.1` | 17 |

The 1.21.1 build requires FTB Chunks and its required dependencies: FTB
Library, FTB Teams, and Architectury API.

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Install FTB Chunks and its required dependencies.
3. Place the BuyClaimChunks Continued JAR in the server's `mods` directory.
4. Start the server and edit `config/buyclaimchunks-common.toml` if needed.

This fork keeps the original `buyclaimchunks` mod ID for world and modpack
compatibility. It cannot be installed alongside the original BuyClaimChunks.

## Command

```text
/buyclaim
/buyclaim 5
```

The command increases the player's personal FTB Chunks extra-claim allowance.
It does not claim a map chunk automatically and does not add quota to an FTB
Teams party.

## Progressive pricing

The price of personal extra-claim number `n` is:

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

Default values:

```toml
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

With these defaults:

| Personal extra-claim number | Item cost |
|---:|---:|
| 1 | 4 |
| 10 | 11 |
| 50 | 25 |
| 100 | 35 |

A batch purchase sums every individual price in sequence. The claim number is
read from FTB Chunks' personal extra-claim data; no separate purchase counter
is stored by this mod. For example, when a player already has 8 personal extra
claims, `/buyclaim 3` charges the prices of claims 9, 10, and 11. This also
means extra quota granted by an administrator affects the next purchase price.

Set `priceGrowthFactor` or `priceExponent` to `0` for a constant price.

### Upgrading from 1.0

NeoForge preserves existing configuration values. If your old config contains
`amountRequired = 1`, set it to `4` to use the new default curve. Alternatively,
stop the server, delete `buyclaimchunks-common.toml`, and restart to regenerate
the file.

## Building

Use Java 21:

```shell
./gradlew clean test build
```

The JAR is written to `build/libs/`.

## License and credits

BuyClaimChunks Continued is distributed under the [MIT License](LICENSE).
Original work is credited to SkyAdri; fork maintenance and additions are by
Yuu (`nekomario28`). See [NOTICE](NOTICE) for details.

---

# 日本語

BuyClaimChunks Continuedは、アイテムを支払ってFTB Chunksの個人用追加クレーム
上限を購入できる、BuyClaimChunksのメンテナンスforkです。

- 対応環境: Minecraft 1.21.1 / NeoForge 21.1 / Java 21
- コマンド: `/buyclaim [個数]`
- 既定通貨: ダイヤモンド
- 既定価格: 1枠目4個、10枠目11個、50枠目25個、100枠目35個
- FTB Chunks側の追加処理が成功した後だけアイテムを消費
- 価格カーブ、通貨、最大保有数、一度の購入上限をconfigで変更可能

このmodが増やすのはプレイヤー個人の追加クレーム上限です。チャンクを自動で
保護したり、FTB Teamsのパーティー共有上限を増やしたりはしません。
価格計算にはFTB Chunksが保持する個人用追加クレーム数を使い、このmod独自の
購入回数データは保存しません。管理者から付与された追加枠も次回価格に反映
されます。

元のmodと同じ`buyclaimchunks` mod IDを維持しているため、元版との同時導入は
できません。旧configの`amountRequired = 1`は自動変更されないので、新しい
既定カーブを使う場合は`4`へ変更してください。
