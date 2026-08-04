# OpenPAC backend 導入ガイド

この文書は、Minecraft 1.21.1、NeoForge 21.1.x、Java 21向けのBuyClaimChunks Continued Open Parties and Claims版を対象にしています。

## backendは必ずどちらか一方だけ導入する

次の2つは同時導入できません。

```text
buyclaimchunks-continued-ftb-neoforge-1.21.1-1.2.0.jar
buyclaimchunks-continued-openpac-neoforge-1.21.1-1.2.0.jar
```

両方ともmod IDは`buyclaimchunks`で、同じ設定ファイルを使用します。必ず片方だけを導入してください。

OpenPAC版は、Open Parties and Claims 0.27.6以上かつ0.28未満の互換版を必要とします。OpenPAC版にFTB Chunksは不要です。

## FTB版と同じ機能

OpenPAC版でも、プレイヤーから見える機能はFTB版と同じです。

- `/buyclaim`で個人用追加claim枠を1個購入します。
- `/buyclaim <個数>`で複数枠を一括購入します。
- 通貨アイテムと段階価格の設定は同じです。
- 1回の購入上限と追加枠総上限も同じです。
- 支払いはホットバーを含む通常36スロットを対象とし、防具とオフハンドは含みません。
- 枠の更新と再確認に成功した後だけ支払いを消費します。
- 枠更新に失敗した場合はアイテムを消費しません。
- 管理者から付与されたbonus枠も、次の価格と`maxExtraClaims`判定に含まれます。
- BuyClaimChunks独自の購入回数や上限データベースは保存しません。
- 購入するのはclaim可能枠であり、現在地のチャンクを自動claimするわけではありません。

OpenPAC版の正本は次のプレイヤー設定です。

```text
playerConfig.claims.bonusChunkClaims
```

API上では`PlayerConfigOptions.BONUS_CHUNK_CLAIMS`です。

## 全claim枠を購入制にする設定

すべての利用可能claim枠を購入制にするには、OpenPACの有効base枠と、無料枠を生む設定をすべて0にします。

サーバーを停止して、ワールドまたはサーバー側の`openpartiesandclaims-server.toml`を編集します。関係する設定は次のとおりです。

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

さらに、permissionプラグイン、rank、管理コマンド、他のアドオンからbase claim枠が付与されていないことを確認してください。

BuyClaimChunksはOpenPACの設定を勝手に書き換えません。有効base枠が0でなくても購入自体は動作しますが、その分は無料枠になり、サーバーログへ警告を記録します。

## bonus設定を書き換え可能にする

OpenPACの`BONUS_CHUNK_CLAIMS`は、プレイヤー設定として許可され、サーバー既定値に強制固定されていない必要があります。

次の場合、購入を拒否し、支払いは消費しません。

- bonus設定が不正または利用不能である。
- 直接設定できない項目として扱われている。
- 取引処理中に管理者などが現在値を変更した。
- 書込み後に要求値が保存されていない。
- OpenPACが初期化されていない、または予期せず失敗した。

## BuyClaimChunks側の設定

OpenPAC版でも既存の設定ファイルを使用します。

```text
config/buyclaimchunks-common.toml
```

設定項目と既定値は変わりません。

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

1から数える追加枠番号を`n`とすると、1枠の価格は次の式です。

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

`priceGrowthFactor`または`priceExponent`を`0`にすると固定価格になります。

## 取引の安全性

購入は次の順番で行います。

1. 現在のOpenPAC bonus枠を読みます。
2. 1回の購入上限と追加枠総上限を確認します。
3. 一括購入の総額を計算します。
4. 通貨アイテムの存在と所持数を確認します。
5. bonus枠を再読込し、同時変更がないか確認します。
6. 新しい絶対値を書き込み、再読込して一致を確認します。
7. 支払いを消費します。
8. 検証済みの支払いが想定外に失敗した場合、以前のbonus値への自動復元を試み、結果をログへ残します。

## FTB Chunksから移行する場合

FTBからOpenPACへの自動枠移行は行いません。ワールドバックアップ内に両方のclaimデータが残っている状態で自動移行すると、枠が重複する危険があるためです。

安全な移行手順は次のとおりです。

1. ワールドと両方のclaim設定をバックアップします。
2. 各プレイヤーのFTB個人追加枠を記録します。
3. バックアップ完了後にFTB版JARとFTB Chunksを外します。
4. OpenPACとOpenPAC版JARを導入します。
5. base枠0の設定を適用します。
6. 購入済み枠を引き継ぐ場合、同数のOpenPAC bonus枠を管理者が手動で付与します。
7. サーバー公開前に、OPではない通常プレイヤーで`/buyclaim`を確認します。

## CIで要求する検証

OpenPAC版のRelease候補には次を要求します。

- Java 21での単体テストとビルド。
- FTB backendクラスが混入していないことを確認するJAR検査。
- NeoForge GameTest。
- 実際の`/buyclaim`によるbonus枠`0 -> 1`。
- ダイヤモンド4個から0個への支払い。
- base枠0と、計算上のfull枠1。
- 通常終了後、別JVMでbonus枠1を再読込できること。
- 専用サーバーがMinecraftの`Done`まで正常起動すること。
