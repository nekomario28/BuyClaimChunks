# 統合JAR向けOpenPAC導入ガイド

この文書は、Minecraft 1.21.1、NeoForge 21.1.x、Java 21向けBuyClaimChunks Continued 1.2.0を対象にしています。

## 導入構成

配布ファイルは1本です。

```text
buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar
```

このJARと**Open Parties and Claimsだけ**を導入してください。FTB Chunksは同時に入れません。OpenPACだけが存在すれば、統合JARが自動でOpenPAC backendを選びます。

両backendがある場合、またはどちらもない場合もサーバーは起動しますが、誤った枠を更新しないため購入機能は無効になります。

## 購入で変更するOpenPAC値

購入分は、OpenPACの次のプレイヤー別追加枠へ保存します。

```text
PlayerConfigOptions.BONUS_CHUNK_CLAIMS
```

BuyClaimChunks側に別の購入回数DBは作りません。管理者が付与したOpenPAC bonus枠も、次回価格と`maxExtraClaims`判定に含まれます。

## 全枠を購入制にする設定

利用可能なclaim枠をすべて購入分だけにするには、OpenPACの有効base枠と無料付与元をすべて0にします。

1. サーバーを停止します。
2. ワールドとOpenPAC設定をバックアップします。
3. 通常は次のワールド内設定を編集します。

```text
<ワールド>/serverconfig/openpartiesandclaims-server.toml
```

4. 関係する値を設定します。

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

5. permission MOD、rank、管理コマンド、他addonが0より大きい上限を付与していないことを確認します。
6. サーバーを起動し、通常プレイヤーで確認します。

BuyClaimChunks ContinuedはOpenPAC設定を勝手に書き換えません。有効base上限が0でなければ警告し、その分は無料枠として残ります。

## BuyClaimChunksの既定設定

設定ファイル：

```text
config/buyclaimchunks-common.toml
```

既定値：

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

既定値では、1枠目はダイヤ4個、10枠目は11個、50枠目は25個、100枠目は35個です。`priceGrowthFactor`または`priceExponent`を0にすると固定価格になります。

経済設定を変更するときは、サーバーを停止し、`config/buyclaimchunks-common.toml`を編集して保存し、再起動後に`/buyclaim`を確認します。既存設定は新しい既定値で上書きされません。

## bonus設定の書き換え可否

`BONUS_CHUNK_CLAIMS`はプレイヤー別に書き換え可能である必要があります。OpenPACが不正・利用不可・変更不能・同時変更・保存不一致を返した場合、購入はアイテムを消費せず拒否されます。

## 取引手順

1. 現在のOpenPAC bonus枠を読みます。
2. 購入数と総上限を確認します。
3. 連続する全枠の価格を合計します。
4. 通貨と所持数を確認します。
5. 枠を再読込して同時変更を検出します。
6. 新しい絶対値を書き込み、再読込して確認します。
7. 支払いを消費します。
8. 確認済みの支払いが想定外に失敗した場合は枠を元へ戻します。

## FTB Chunksからの移行

FTBからOpenPACへの自動枠移行は行いません。バックアップ内に両方のデータが残る状態で自動変換すると、枠を二重付与する危険があるためです。

推奨手順：

1. ワールドと両設定をバックアップします。
2. 各プレイヤーのFTB個人追加枠を記録します。
3. FTB Chunksと不要になった依存を削除します。
4. 同じ統合版BuyClaimChunks JARを残したままOpenPACを導入します。
5. OpenPACを0-baseへ設定します。
6. 購入済み枠を維持する場合は、同数のOpenPAC bonus枠を管理者が手動付与します。
7. 公開再開前に通常プレイヤーで`/buyclaim`を確認します。

## Release前にCIで確認すること

統合JARのOpenPAC検証では、次を必須にします。

- 単体テストと統合JAR 1本のビルド
- 両adapterを含み、claim MOD本体を同梱していないこと
- OpenPAC NeoForge GameTest
- 実`/buyclaim`による枠`0 -> 1`と支払い`4 -> 0`
- base 0、full limit `0 -> 1`
- 通常終了と別JVM再読込
- dedicated serverの正常起動
- 両backendなし／両方ありでの安全な起動と購入無効化
