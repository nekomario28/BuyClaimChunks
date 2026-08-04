[日本語](#japanese)

<h1 id="english">BuyClaimChunks Continued</h1>

Buy personal claim capacity with a configurable item currency.

BuyClaimChunks Continued is an independently maintained, MIT-licensed continuation of SkyAdri's BuyClaimChunks for Minecraft 1.21.1 and NeoForge. Version 1.2.0 is distributed as **one universal JAR** that works with either FTB Chunks or Open Parties and Claims.

## Install exactly one claim backend

Use the universal JAR with one of these:

- **FTB Chunks** and its required dependencies, or
- **Open Parties and Claims (OpenPAC)**.

Do not install both claim mods. If both or neither are present, the server still starts but `/buyclaim` is disabled to prevent ambiguous quota updates.

## Features

- `/buyclaim [amount]` purchases one or more personal extra-claim slots.
- Same command and configuration for FTB Chunks and OpenPAC.
- Any registered vanilla or modded item can be used as currency.
- Fixed pricing or a configurable progressive cost curve.
- Sequential per-slot pricing for bulk purchases.
- Separate total-cap and per-command purchase limits.
- Payment is counted across the hotbar and normal inventory.
- Capacity is updated and verified before payment is consumed.
- Concurrent administrator changes are detected instead of overwritten.
- Failed backend updates consume no items.
- No separate purchase-count database.

The mod buys claim **capacity**. It does not automatically claim the current chunk, sell force-loaded chunks, charge upkeep, or refund unclaims.

## Default configuration

Generated file:

```text
config/buyclaimchunks-common.toml
```

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

With the defaults, slot 1 costs 4 diamonds, slot 10 costs 11, slot 50 costs 25, and slot 100 costs 35.

To change settings: stop the server, edit `config/buyclaimchunks-common.toml`, save it, restart, and test `/buyclaim`. Existing configs are preserved and are not overwritten with new defaults.

## OpenPAC all-paid setup

The OpenPAC backend stores purchases in `BONUS_CHUNK_CLAIMS`. To make every claim slot paid, set OpenPAC's effective base claim limit, party/member bonuses, owner bonus, and permission/rank override to zero. Full settings and migration instructions are in the GitHub documentation.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Exactly one backend:
  - FTB Chunks 2101.1.20 or newer below 2102, plus its dependencies, or
  - Open Parties and Claims 0.27.6 or newer in the supported 1.21.1 line

**Environment:** server required, multiplayer clients optional. Singleplayer is supported through Minecraft's integrated server.

FTB Chunks may not be available as an official linked Modrinth dependency. Obtain it and its dependencies from an official FTB or CurseForge distribution channel. OpenPAC is listed as an optional Modrinth dependency for this universal file.

## License and attribution

BuyClaimChunks Continued is MIT-licensed. Original work by SkyAdri; continued maintenance and additions by nekomario28.

FTB Chunks and OpenPAC are external downloads and are not bundled. FTB Chunks is All Rights Reserved / visible source. OpenPAC is LGPL-3.0-only. The distributed JAR includes `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md`.

This project is not affiliated with or endorsed by SkyAdri, Feed The Beast Ltd, Xaero, Mojang, NeoForge, Modrinth, or CurseForge.

---

[English](#english)

<h1 id="japanese">日本語</h1>

設定可能なアイテム通貨で、個人用クレーム枠を購入できるMODです。

BuyClaimChunks Continuedは、SkyAdri氏のBuyClaimChunksをMinecraft 1.21.1／NeoForge向けに独立して継続保守するMIT LicenseのMODです。1.2.0は、FTB ChunksまたはOpen Parties and Claimsのどちらでも使える**統合JAR 1本**として配布します。

## claim MODは必ずどちらか一方

統合JARと、次のどちらか一方を導入してください。

- **FTB Chunks**とその必須依存
- **Open Parties and Claims（OpenPAC）**

両方を同時に導入しないでください。両方ある場合、またはどちらもない場合もサーバーは起動しますが、誤った枠を更新しないよう`/buyclaim`は無効になります。

## 機能

- `/buyclaim [個数]`で個人用追加claim枠を購入できます。
- FTB ChunksとOpenPACで同じコマンド・設定を使います。
- 任意のバニラ・他MODアイテムを通貨にできます。
- 固定価格または設定可能な段階価格にできます。
- 一括購入では各枠の連続価格を合計します。
- 追加枠総上限と1コマンド購入上限を別々に設定できます。
- ホットバーを含む通常インベントリから支払いを合算します。
- backendの枠更新を確認してからアイテムを消費します。
- 管理者による同時変更を古い値で上書きしません。
- backend更新に失敗した場合、アイテムは消費されません。
- 独自の購入回数DBは作りません。

購入するのはclaimの**所有可能枠**です。現在地の自動claim、強制ロード枠販売、維持費、unclaim返金は追加しません。

## 既定設定

生成されるファイル：

```text
config/buyclaimchunks-common.toml
```

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

1から数える枠番号`n`の価格：

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

既定値では、1枠目はダイヤ4個、10枠目は11個、50枠目は25個、100枠目は35個です。

設定変更は、サーバー停止後に`config/buyclaimchunks-common.toml`を編集・保存し、再起動して`/buyclaim`を確認します。既存設定は新しい既定値で上書きされません。

## OpenPACを全枠有料にする

OpenPAC版では購入分を`BONUS_CHUNK_CLAIMS`へ保存します。全枠を購入制にする場合、OpenPACの有効base上限、party/member bonus、owner bonus、permission/rank overrideを0にしてください。詳細設定と移行手順はGitHubの日本語ガイドにあります。

## 必要環境

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- 次のbackendをどちらか一方：
  - FTB Chunks 2101.1.20以上2102未満と必須依存、または
  - Open Parties and Claims 0.27.6以上の対応1.21.1系

**環境：** サーバー必須、マルチプレイのクライアント側は任意です。シングルプレイでは内蔵サーバーがあるため通常どおり導入します。

FTB Chunks本体をModrinthの公式依存として紐づけられない場合は、公式FTBまたはCurseForge配布から入手してください。OpenPACは、この統合ファイルのoptional dependencyとしてModrinthに登録します。

## ライセンスと帰属

BuyClaimChunks ContinuedはMIT Licenseです。元実装はSkyAdri氏、継続保守と追加実装はnekomario28です。

FTB ChunksとOpenPAC本体は同梱しません。FTB ChunksはAll Rights Reserved／visible source、OpenPACはLGPL-3.0-onlyです。配布JARには`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md`を含めます。

本プロジェクトはSkyAdri氏、Feed The Beast Ltd、Xaero氏、Mojang、NeoForge、Modrinth、CurseForgeによる公式版・公認版ではありません。
