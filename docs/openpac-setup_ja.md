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

購入分は、`/buyclaim`を実行したプレイヤー自身の次の値へ保存します。

```text
PlayerConfigOptions.BONUS_CHUNK_CLAIMS
```

BuyClaimChunksの経済台帳もプレイヤーUUID単位です。OpenPACのpartyへ加入・脱退しても、購入台帳や`BONUS_CHUNK_CLAIMS`を別プレイヤーへ移動・合算しません。

管理者がOpenPAC側で無料付与したbonus枠は利用可能なbackend総枠には含まれるため`maxExtraClaims`を消費しますが、BuyClaimChunksの購入履歴には加算されず、新しい価格creditも作りません。

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
maxPlayerClaims = 0
maxPlayerClaimsPermission = ""
claimBonusPerPartyMember = 0
claimBonusForPartyOwner = 0
```

5. permission MOD、rank、管理コマンド、他addonが0より大きい上限を付与していないことを確認します。
6. サーバーを起動し、通常プレイヤーで確認します。

BuyClaimChunks ContinuedはOpenPAC設定を勝手に書き換えません。有効base上限が0でなければ警告し、その分は無料枠として残ります。

### `partyOwnedClaims`の選択

`partyOwnedClaims`はBuyClaimChunksが強制する設定ではありません。

```toml
partyOwnedClaims = false
```

なら、通常は各プレイヤーが自分自身のUUIDでclaimします。

```toml
partyOwnedClaims = true
```

ならOpenPACのPARTY claiming modeが使えます。この場合も独立した「party用claim枠」が作られるわけではありません。OpenPACはPARTY modeのclaim所有者UUIDをprimary party ownerのUUIDへ置き換えます。

完全購入制でparty-owned claimsも使いたい場合は、`partyOwnedClaims = true`のまま、`maxPlayerClaims`、`claimBonusPerPartyMember`、`claimBonusForPartyOwner`、permission由来の無料枠を0にしてください。

## OpenPAC partyでの正確な購入・claimモデル

`partyOwnedClaims = true`では、BuyClaimChunksはOpenPACのUUID所有モデルをそのまま尊重します。party独自の購入台帳は作りません。

### 一般メンバー

一般メンバーBが`/buyclaim`を実行すると、増えるのはB自身の`BONUS_CHUNK_CLAIMS`です。

```text
B /buyclaim
→ B UUID のbonusと購入台帳を増加
→ party owner A UUID のbonusは変化しない
```

BがOpenPACのPLAYER modeでclaimすればBの枠を使います。

BがPARTY modeでclaimすれば、OpenPACが所有者をprimary party owner AのUUIDへ変換するため、Aのclaim countとAのfull claim limitを使います。Bが操作したという事実からBの購入枠がpartyへ合算されることはありません。

### party owner

owner Aが`/buyclaim`するとA自身の`BONUS_CHUNK_CLAIMS`が増えます。

A自身のPLAYER claimと、Aまたは他メンバーがPARTY modeで作るclaimは、どちらも技術的にはA UUID名義です。そのため同じclaim count / full claim limitを共有します。

```text
A personal claim      3
B party-mode claim    4
C party-mode claim    2
-----------------------
A UUID claim count    9
```

「A個人用10枠 + party用10枠」のような別プールではありません。

### 加入・脱退

partyへ加入・脱退してもBuyClaimChunksは購入履歴やbonusを動かしません。

```text
Bがpartyへ加入
→ Bの購入台帳はBのまま
→ BのBONUS_CHUNK_CLAIMSもBのまま
→ owner Aの購入枠へ自動合算しない

Bがpartyを脱退
→ Bの購入台帳はBのまま
→ Bのpersonal claimはB名義のまま
→ Bが以前PARTY modeで作ったclaimは最初からowner A名義なのでA名義のまま
```

この方式により、加入・脱退による購入枠の複製や自動返還を起こしません。

### owner移譲

OpenPACのparty owner移譲は、partyのowner mappingを変更します。BuyClaimChunksは移譲時に旧ownerと新ownerの`BONUS_CHUNK_CLAIMS`や購入台帳を自動移動しません。

1.2.0のrelease gateでは、実際のOpenPAC 0.29.3環境で、PARTY claim・PLAYER claim・両プレイヤーのbonus/台帳を作った後に実`openpac-parties transfer ... confirm`を実行し、既存claim所有UUID、party ID、bonus、台帳、移譲後の新規PARTY claimを検証します。

この実挙動が検証を通るまで、owner移譲を「party資産の自動移行」とは扱いません。

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

## 価格変更と購入台帳

BuyClaimChunksはプレイヤーUUIDごとに、通貨ID、購入由来の枠数、実際に消費した累計アイテム数を保存します。

- 同じ通貨で値上げ：既存claimを没収せず不足額を次回購入へ繰り越します。
- 同じ通貨で値下げ：過去の支払いが新価格で追加枠相当なら、後続の成功購入で補償します。
- 通貨変更：異なるアイテム間の交換比率を推測せず、同じ有料枠数で新しい基準を開始します。
- party加入・脱退・owner状態の変化：台帳を別UUIDへ移動しません。

詳しくは[`repricing-ledger_ja.md`](repricing-ledger_ja.md)を参照してください。

## 取引手順

1. 実行プレイヤー自身のOpenPAC bonus枠と購入台帳を読みます。
2. 購入数と総上限を確認します。
3. 値上げ不足額・値下げcredit・連続枠価格を計算します。
4. 通貨と所持数を確認します。
5. 実行プレイヤー自身のbonus絶対値を書き込み、再読込して確認します。
6. 同じUUIDの購入台帳をcompare-and-setで更新します。
7. 支払いを消費します。
8. 確認済みの支払いが想定外に失敗した場合はbonusと台帳の両方を元へ戻します。
9. OpenPAC party contextがある場合は、購入したUUID poolがpersonalのみかowner共有poolかを説明します。

## FTB Chunksからの移行

FTBからOpenPACへの自動枠移行は行いません。バックアップ内に両方のデータが残る状態で自動変換すると、枠を二重付与する危険があるためです。

推奨手順：

1. ワールドと両設定をバックアップします。
2. 各プレイヤーのFTB個人追加枠を記録します。
3. FTB Chunksと不要になった依存を削除します。
4. 同じ統合版BuyClaimChunks JARを残したままOpenPACを導入します。
5. OpenPACを0-baseへ設定します。
6. 購入済み枠を維持する場合は、同じプレイヤーUUIDへ同数のOpenPAC bonus枠を管理者が手動付与します。
7. party-owned claimsを使う場合も、メンバー分をownerへ自動合算しません。
8. 公開再開前に通常プレイヤーでPLAYER/PARTY modeと`/buyclaim`を確認します。

## Release前にCIで確認すること

統合JARのOpenPAC検証では、次を必須にします。

- 単体テストと統合JAR 1本のビルド
- 両adapterを含み、claim MOD本体を同梱していないこと
- OpenPAC NeoForge GameTest
- 実`/buyclaim`による枠`0 -> 1`と支払い`4 -> 0`
- base 0、full limit `0 -> 1`
- 通常終了と別JVM再読込
- `partyOwnedClaims=true`の専用party semantics GameTest
- 一般メンバー購入がownerへ移らないこと
- member PARTY claimがowner UUIDへ入ること
- member PLAYER claimがmember UUIDへ入ること
- 脱退でbonus・台帳・claim所有UUIDを移動しないこと
- owner移譲後のparty ID、claim所有UUID、bonus、台帳、新規PARTY claimを実OpenPACコマンドで検証すること
- dedicated serverの正常起動
- 両backendなし／両方ありでの安全な起動と購入無効化
