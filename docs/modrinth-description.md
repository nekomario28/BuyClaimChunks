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
- Server-side lifetime payment ledger for later numeric price-curve changes.
- Price increases never confiscate existing claims; the unpaid difference is carried into the next purchase.
- Price decreases grant compensation capacity supported by previous payments.
- Separate total-cap and per-command purchase limits.
- Payment is counted across the hotbar and normal inventory.
- Backend capacity and the economic ledger are verified before payment is consumed.
- Concurrent administrator changes are detected instead of overwritten.
- Failed transactions consume no items and attempt to roll back both capacity and ledger state.

FTB Chunks or OpenPAC remains the source of truth for current claim capacity. The BuyClaimChunks ledger stores only the currency ID, capacity bought through this mod, and the lifetime amount actually consumed.

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

## Changing prices later

For the same currency item, a later numeric price change uses:

```text
next payment
= cumulative cost through the paid claims after this purchase under the active curve
- lifetime consumed amount
```

When prices rise, existing claims remain untouched and the shortfall is added to the next purchase. When prices fall, previous payments can grant compensation claims during the next successful purchase. Credit that cannot fit under `maxExtraClaims` continues to reduce later prices.

Changing `itemRequired` starts a new baseline because different items do not have a universal exchange rate. Worlds upgraded from a pre-ledger version start exact tracking from their current backend capacity at the active curve.

## OpenPAC all-paid setup

The OpenPAC backend stores purchases in `BONUS_CHUNK_CLAIMS`. To make every claim slot paid, set OpenPAC's effective base claim limit, party/member bonuses, owner bonus, and permission/rank override to zero. Full settings, repricing details, and migration instructions are in the GitHub documentation.

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
- 後から数値の価格曲線を変更できる、サーバー側の累計支払い台帳を持ちます。
- 値上げ時も既存claimを没収せず、不足額を次回購入へ繰り越します。
- 値下げ時は、過去の支払いで買える差分を補償枠として付与します。
- 追加枠総上限と1コマンド購入上限を別々に設定できます。
- ホットバーを含む通常インベントリから支払いを合算します。
- backend枠と経済台帳を確認してからアイテムを消費します。
- 管理者による同時変更を古い値で上書きしません。
- 失敗した取引ではアイテムを消費せず、枠と台帳のrollbackを試みます。

現在のclaim上限の正本はFTB ChunksまたはOpenPACです。BuyClaimChunksの台帳は、通貨ID、本MODで購入した枠数、実際に消費した累計数だけを保存します。

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

## 後から価格を変更する場合

同じ通貨アイテムの数値設定を変更した場合、次回価格は次で決まります。

```text
次回支払額
= 新しい曲線で今回購入後の有料枠数までに必要な累計額
- これまで実際に消費した累計額
```

値上げ時も既存claimは維持され、差額だけが次回購入へ加わります。値下げ時は、過去の支払いで買える補償枠を次の購入成功時に追加します。`maxExtraClaims`で一度に付与できないcreditは、後続価格を安くする形で残ります。

`itemRequired`を変更した場合は、異なる通貨間に共通の交換比率がないため新しい基準を作ります。台帳導入前のワールドは、現在のbackend枠をその時点の曲線で購入済みだったものとして正確な追跡を開始します。

## OpenPACを全枠有料にする

OpenPAC版では購入分を`BONUS_CHUNK_CLAIMS`へ保存します。全枠を購入制にする場合、OpenPACの有効base上限、party/member bonus、owner bonus、permission/rank overrideを0にしてください。詳細設定、再価格計算、移行手順はGitHubの日本語ガイドにあります。

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
