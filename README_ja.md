<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued ロゴ" width="256">
</p>

<h1 align="center">💎 BuyClaimChunks Continued</h1>

<p align="center">
  <strong>設定可能なアイテム通貨で、個人用クレーム枠を購入できるMODです。</strong>
</p>

<p align="center">
  <a href="README.md">English</a> ·
  <a href="https://modrinth.com/mod/buyclaimchunks-continued">Modrinth</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/releases">Releases</a> ·
  <a href="LICENSE">MIT License</a>
</p>

BuyClaimChunks Continuedは、Minecraft 1.21.1／NeoForge向けのサーバー経済アドオンです。プレイヤーは`/buyclaim`を使い、設定されたバニラまたは他MODのアイテムを支払って個人用クレーム上限を増やせます。

1.2.0では、配布JARを**1本に統合**します。次のclaim MODのうち、どちらか一方だけを導入してください。

- **FTB Chunks**
- **Open Parties and Claims（OpenPAC）**

コマンド、設定、価格曲線、上限、インベントリの扱い、安全な取引手順は、どちらのbackendでも同じです。

> [!WARNING]
> FTB ChunksとOpenPACは、必ずどちらか一方だけを導入してください。両方ある場合、または両方ない場合もサーバーは起動しますが、誤った枠を更新しないよう`/buyclaim`は安全に無効化されます。

## 主な機能

- `/buyclaim`で個人用追加枠を1つ購入できます。
- `/buyclaim <個数>`で、連続した価格の複数枠を一括購入できます。
- 登録済みの任意のバニラ・他MODアイテムを通貨にできます。
- 固定価格または段階価格を設定できます。
- 個人用追加枠の総上限と、1コマンドの購入上限を別々に設定できます。
- ホットバーを含む通常36スロットから支払いを合算します。
- 防具スロットとオフハンドは支払いに使いません。
- backend側の枠更新を確認してから支払いを消費します。
- 管理者による同時変更を古い値で上書きしません。
- 確認済みの支払いが想定外に失敗した場合は、枠を元へ戻します。
- 枠の正本はFTB ChunksまたはOpenPACであり、別の購入回数DBは作りません。

このMODが増やすのは**所有可能な枠数**です。現在地を自動claimしたり、強制ロード枠を販売したり、維持費やunclaim返金を追加したりはしません。

## 必要環境

| 構成要素 | 対応バージョン |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Java | 21 |
| BuyClaimChunks Continued | 統合版1.2.0 JAR 1本 |
| FTBを使う場合 | FTB Chunks 2101.1.20以上、2102未満、および必須依存 |
| OpenPACを使う場合 | Open Parties and Claims 0.27.6以上の対応1.21.1系 |

サーバー側は必須、マルチプレイのクライアント側は任意です。シングルプレイでは内蔵サーバーが動くため、通常のインスタンスへ導入します。

## 導入方法

1. Minecraft 1.21.1、NeoForge 21.1.x、Java 21を用意します。
2. claim MODを**どちらか一方だけ**導入します。
   - FTB Chunksとその必須依存、または
   - Open Parties and Claims
3. `buyclaimchunks-continued-neoforge-1.21.1-1.2.0.jar`を`mods`フォルダーへ入れます。
4. サーバーを一度起動します。
5. 設定変更前にサーバーを停止します。
6. 経済設定を変更します。OpenPACで全枠有料にする場合は、無料base枠も0にします。
7. 再起動後、通常プレイヤーで`/buyclaim`を確認します。

元のBuyClaimChunksと本forkは同じ`buyclaimchunks` mod IDを使うため、同時に導入できません。

## コマンド

```text
/buyclaim
/buyclaim <個数>
```

プレイヤーのインベントリから支払うため、サーバーコンソールやコマンドブロックからは実行できません。

## 既定設定

生成される設定ファイル：

```text
config/buyclaimchunks-common.toml
```

新規環境の既定値：

```toml
[general]
itemRequired = "minecraft:diamond"
amountRequired = 4
priceGrowthFactor = 3.45
priceExponent = 0.5
maxExtraClaims = 100
maxPurchaseAmount = 100
```

| 項目 | 既定値 | 意味 |
|---|---:|---|
| `itemRequired` | `minecraft:diamond` | 支払いアイテムの登録ID。他MODなら`modid:item_name`形式です。 |
| `amountRequired` | `4` | 1枠目の価格であり、1枠あたりの最低価格です。 |
| `priceGrowthFactor` | `3.45` | 段階価格の上昇の強さ。`0`で固定価格になります。 |
| `priceExponent` | `0.5` | 曲線の形。`0.5`は平方根型、`0`でも固定価格になります。 |
| `maxExtraClaims` | `100` | backendが保持する個人追加枠総数の上限。管理者付与分も含みます。 |
| `maxPurchaseAmount` | `100` | 1回のコマンドで購入できる最大数です。 |

既存の設定ファイルは自動で上書きされません。旧環境で`amountRequired = 1`のままなら、現在の既定曲線を使うには手動で4へ変更するか、停止中に設定ファイルを削除して再生成してください。

## 設定変更方法

1. Minecraftサーバーを完全に停止します。
2. `config/buyclaimchunks-common.toml`をバックアップします。
3. テキストエディターでそのファイルを開きます。
4. TOML形式を壊さないよう、`=`の右側の値を変更します。
5. 保存します。
6. サーバーを起動し直します。
7. 設定した通貨を持つ通常プレイヤーで`/buyclaim`を試します。
8. 反映されない場合やアイテムIDエラーが出た場合は`latest.log`を確認します。

ライブリロードは前提にしていません。設定変更後は必ず再起動してください。また、別のMinecraftインスタンスの設定ファイルを編集していないか確認してください。

### 通貨の変更例

エメラルドを使い、価格曲線は既定のまま：

```toml
itemRequired = "minecraft:emerald"
```

他MODのコインを使う：

```toml
itemRequired = "examplemod:coin"
```

全枠をエメラルド8個の固定価格にする：

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

## 既定のコスト曲線

1から数える追加枠番号を`n`とすると、1枠の価格は次の式です。

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

既定値では：

```text
round(4 + 3.45 * (sqrt(n) - 1))
```

| 通算枠番号 | その枠の価格 | そこまでの累計 |
|---:|---:|---:|
| 1 | 4 | 4 |
| 2 | 5 | 9 |
| 3 | 7 | 16 |
| 5 | 8 | 31 |
| 10 | 11 | 82 |
| 20 | 16 | 223 |
| 50 | 25 | 850 |
| 100 | 35 | 2,369 |

一括購入では、次に増える各枠の価格を個別に合計します。

- 追加枠0で`/buyclaim 5`：`4 + 5 + 7 + 7 + 8 = 31`個
- 追加枠8で`/buyclaim 3`：9～11枠目の`11 + 11 + 12 = 34`個

価格位置には、管理者が付与した追加枠も含め、選択中backendが現在報告する追加枠総数を使います。

## backendごとの動作

### FTB Chunks

プレイヤー個人のFTB Chunks追加クレーム値を更新します。FTB Teamsのパーティー共有枠は増やしません。土地のclaim自体は通常のFTB Chunks操作から行います。

### Open Parties and Claims

OpenPACのプレイヤー別`BONUS_CHUNK_CLAIMS`を更新します。

すべての枠を購入制にする場合、通常はワールド内の次のファイルを編集します。

```text
<ワールド>/serverconfig/openpartiesandclaims-server.toml
```

関係する設定例：

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

LuckPermsやrank MODなどが別途claim上限を付与していないことも確認してください。本MODは有効なOpenPAC base上限が0でない場合に警告しますが、OpenPAC設定を勝手に書き換えません。

詳しくは[`docs/openpac-setup_ja.md`](docs/openpac-setup_ja.md)を参照してください。

## 安全な取引順序

1. 現在のbackend追加枠を読みます。
2. 購入数、総上限、オーバーフローを検証します。
3. 一括購入の全枠価格を計算します。
4. 通貨アイテムを確認し、所持数を数えます。
5. 枠を再読込し、管理者による同時変更を検出します。
6. backendの枠を更新し、再読込して確認します。
7. 支払いを消費します。
8. 支払いが想定外に失敗した場合は、以前の枠へ戻します。
9. 枠と支払いの両方が確定してから成功を通知します。

枠更新に失敗した場合、アイテムは消費されません。

## 統合JARの選択規則

| 導入済みclaim MOD | 動作 |
|---|---|
| FTB Chunksのみ | FTB backendを自動選択 |
| OpenPACのみ | OpenPAC backendを自動選択 |
| どちらもなし | サーバーは起動、購入機能は無効 |
| 両方あり | 誤更新防止のためサーバーは起動、購入機能は無効 |

統合JARに含まれるのは本プロジェクトの共通処理と薄いadapterだけです。FTB ChunksやOpenPAC本体は同梱しません。

## ビルドと検証

統合JARをビルド：

```shell
./gradlew clean test build -Ptest_backend=none
bash scripts/verify-release-jar.sh
```

FTB Chunks環境を検証：

```shell
./gradlew runGameTestServer -Ptest_backend=ftb
bash scripts/run-ftb-restart-integration.sh
```

OpenPAC環境を検証：

```shell
./gradlew runGameTestServer -Ptest_backend=openpac
bash scripts/run-openpac-restart-integration.sh
```

生成物は`build/libs/`に出力されます。

## ライセンスと帰属

BuyClaimChunks ContinuedはMIT Licenseで公開し、SkyAdri氏によるMIT Licenseの元BuyClaimChunksを継続保守するforkです。

FTB ChunksとOpenPACは別途導入する外部MODであり、本JARには再配布しません。FTB ChunksはAll Rights Reserved／visible source、OpenPACはLGPL-3.0-onlyです。詳細は[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)と[`docs/license-review.md`](docs/license-review.md)に記録しています。

本プロジェクトはSkyAdri氏、Feed The Beast Ltd、Xaero氏、Mojang、NeoForge、Modrinth、CurseForgeによる公式版または公認版ではありません。
