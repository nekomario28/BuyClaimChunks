<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued ロゴ" width="256">
</p>

<h1 align="center">💎 BuyClaimChunks Continued</h1>

<p align="center">
  <strong>設定可能なアイテム通貨で、FTB Chunksの個人用追加クレーム枠を購入できるMODです。</strong>
</p>

<p align="center">
  <a href="https://github.com/nekomario28/BuyClaimChunks/actions/workflows/build.yml"><img alt="ビルド" src="https://img.shields.io/github/actions/workflow/status/nekomario28/BuyClaimChunks/build.yml?branch=main&style=flat-square&label=build"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square">
  <img alt="NeoForge 21.1" src="https://img.shields.io/badge/NeoForge-21.1-EF7E25?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/License-MIT-blue?style=flat-square"></a>
</p>

<p align="center">
  <img alt="サーバー必須" src="https://img.shields.io/badge/Server-required-C83A3A?style=flat-square">
  <img alt="クライアント任意" src="https://img.shields.io/badge/Client-optional-6A7FDB?style=flat-square">
</p>

<p align="center">
  <a href="README.md">English</a> ·
  <a href="https://modrinth.com/mod/buyclaimchunks-continued">Modrinth</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/releases">Releases</a> ·
  <a href="https://github.com/nekomario28/BuyClaimChunks/issues">Issues</a> ·
  <a href="LICENSE">License</a>
</p>

---

BuyClaimChunks Continuedは、[FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge)向けの小規模なサーバー経済アドオンです。プレイヤーは`/buyclaim`を使い、設定されたバニラまたは他MODのアイテムを支払って、**個人用追加クレーム上限**を増やせます。

これは[SkyAdri氏のBuyClaimChunks](https://github.com/SkyAdri-mc/BuyClaimChunks)を独立して継続保守するforkです。元プロジェクトはMIT Licenseで公開されており、本forkは元作者およびFTBによる公式版・公認版ではありません。

## 📌 ひと目で分かる概要

| | |
|---|---|
| **コマンド** | `/buyclaim [個数]` |
| **動作環境** | サーバー必須、クライアント任意、シングルプレイ対応 |
| **通貨** | 登録済みの任意のバニラ・他MODアイテム |
| **価格方式** | 固定価格または段階価格 |
| **設定ファイル** | `config/buyclaimchunks-common.toml` |
| **枠データの正本** | FTB Chunksの個人用追加枠値 |

> [!IMPORTANT]
> このMODが増やすのはクレームの**所有可能枠**です。マップ上のチャンクを自動で保護したり、パーティー共有上限や強制ロード枠を増やしたりするものではありません。

## ✨ 主な機能

- `/buyclaim [個数]`で個人用追加クレーム枠を1個以上まとめて購入できます。
- バニラ・他MODを問わず、登録済みの任意アイテムを通貨にできます。
- 固定価格と、現在のFTB Chunks個人追加枠総数に応じた段階価格を選べます。
- ホットバーを含む36枠の通常インベントリ内にある複数スタックを合算します。
- 先にFTB Chunks側の枠追加を行い、成功した場合にだけ支払いを消費します。
- 1コマンドの購入上限と、購入後の個人追加枠総上限を別々に設定できます。
- 不正なアイテムID、価格オーバーフロー、危険な取引を支払いなしで拒否します。
- 独自の枠データベースを作らず、FTB Chunksを正本として使用します。

## 📦 対応バージョン

`main`ブランチと本ガイドはMinecraft 1.21.1版を対象にしています。

| 構成要素 | 対応・検証バージョン |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1、検証環境は21.1.242 |
| Java | 21 |
| FTB Chunks | 2101.1.20以上、2102未満 |
| FTB Teams | 2101.1.9で検証 |
| FTB Library | 2101.1.30で検証 |
| Architectury API | 13.0.8で検証 |

Minecraft 1.20.1 / Forge 47.4向けの旧版は[`forge-1.20.1`](https://github.com/nekomario28/BuyClaimChunks/tree/forge-1.20.1)ブランチに残されています。動作や設定項目が本ガイドと異なる場合があります。

## 🚀 インストール

1. Minecraft 1.21.1向けNeoForgeを導入します。
2. FTB Chunksと、その必須依存関係であるFTB Library、FTB Teams、Architectury APIを導入します。
3. [Modrinth](https://modrinth.com/mod/buyclaimchunks-continued)または[GitHub Releases](https://github.com/nekomario28/BuyClaimChunks/releases)から現在のJARを取得します。
4. JARをサーバーの`mods`フォルダーへ入れます。
5. サーバーを一度起動し、`config/buyclaimchunks-common.toml`を生成します。
6. 設定を変更する場合はサーバーを停止してから編集します。
7. 再起動後、通常プレイヤーで`/buyclaim`を試します。

シングルプレイでは内蔵サーバーがコマンド処理を行うため、通常のインスタンスの`mods`フォルダーへJARを入れてください。

> [!WARNING]
> 本forkは元版と同じ`buyclaimchunks`というmod IDを維持しています。元のBuyClaimChunks JARと同時に導入しないでください。

## ⚡ すぐに使う

既定の通貨は`minecraft:diamond`で、1枠目はダイヤモンド4個です。

```text
/buyclaim
```

個人用追加クレーム枠を1個購入します。

```text
/buyclaim 5
```

次の5枠を1回の取引で購入します。各枠を個別に計算し、5枠分の価格を合計します。

このコマンドは通常プレイヤーが権限なしで使用できます。支払い元となるプレイヤーのインベントリが必要なため、サーバーコンソールやコマンドブロックからは実行できません。

## 🔍 購入後に実際に変わるもの

購入に成功すると、FTB Chunksが保持するプレイヤーの**個人用追加クレーム上限**が増えます。土地の保護自体は、購入後に通常のFTB Chunksマップやクレーム操作から行います。

FTB Chunksを正本として使用するため、次の動作になります。

- 管理者が別途付与した追加枠も、次回購入価格に反映されます。
- `maxExtraClaims`は、このMODで購入した数だけではなく、FTB Chunks上の個人追加枠総数を制限します。
- このMODを削除しても、別の枠データベースを移行する必要はありません。

## 🛡️ 取引の安全設計

支払いアイテムは、ホットバーを含む通常インベントリから数えます。防具スロットとオフハンドは対象外です。

処理は次の順序で行われます。

1. FTB Chunksから現在の個人追加枠総数を読み取ります。
2. 上限を確認し、複数購入を含む総額を計算します。
3. 設定アイテムが存在するか確認し、プレイヤーの所持数を数えます。
4. FTB Chunksの枠追加処理を同期実行します。
5. FTB Chunks側が成功を返した場合にだけ支払いを消費します。

枠追加に失敗した場合、購入は中止され、アイテムは消費されません。

## ⚙️ 設定

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

| 項目 | 既定値 | 設定可能範囲 | 意味 |
|---|---:|---|---|
| `itemRequired` | `minecraft:diamond` | 登録済みのアイテムID | 支払いアイテムです。`modid:item_name`形式の他MODアイテムも使用できます。 |
| `amountRequired` | `4` | 1～2,147,483,647 | 基本価格かつ最低価格です。 |
| `priceGrowthFactor` | `3.45` | 0～1,000,000 | 後半の枠がどの程度速く高くなるかを調整します。`0`で固定価格になります。 |
| `priceExponent` | `0.5` | 0～4 | 価格曲線の形を調整します。`0.5`は平方根型、`0`は固定価格です。 |
| `maxExtraClaims` | `100` | 1～2,147,483,647 | 購入後に許可するFTB Chunks個人追加枠総数の上限です。 |
| `maxPurchaseAmount` | `100` | 1～10,000 | 1回のコマンドで指定できる最大個数です。 |

設定編集前にサーバーを停止してください。存在しないアイテムIDは購入時に検出され、支払いは消費されません。

### 通貨設定の例

```toml
# エメラルド
itemRequired = "minecraft:emerald"
```

```toml
# 他MODの通貨
itemRequired = "examplemod:coin"
```

### 固定価格の例

すべての枠をエメラルド8個にする設定です。

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

`priceGrowthFactor`または`priceExponent`のどちらかを`0`にすると、価格は常に`amountRequired`になります。

## 📈 段階価格

1から数える個人追加枠番号を`n`とすると、1枠の価格は次の式です。

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

計算結果が`amountRequired`を下回ることはありません。

| 個人追加枠番号 | 既定の必要アイテム数 |
|---:|---:|
| 1 | 4 |
| 2 | 5 |
| 3 | 7 |
| 5 | 8 |
| 10 | 11 |
| 50 | 25 |
| 100 | 35 |

複数購入では、次の枠を順番に計算して合計します。

- 個人追加枠が0の状態で`/buyclaim 5`を実行すると、`4 + 5 + 7 + 7 + 8 = 31`個のダイヤモンドが必要です。
- 個人追加枠が8の状態で`/buyclaim 3`を実行すると、9～11番目の価格である`11 + 11 + 12 = 34`個が必要です。

## 🧩 互換性

### 元のBuyClaimChunks

元版と本forkはどちらも`buyclaimchunks`というmod IDを使用するため、同時に読み込めません。本forkを導入する前に元版のJARを削除してください。

### Buying Chunks — FTB Chunks Addon

[`snoopypupserr/buying_chunks_ftbchunks_addon`](https://github.com/snoopypupserr/buying_chunks_ftbchunks_addon)を静的検査した範囲では、mod ID、ルートコマンド、設定ファイルの直接衝突は確認されていません。両MODの役割は異なります。

- **BuyClaimChunks Continued**：プレイヤー個人の所有可能枠を販売します。
- **Buying Chunks**：チャンク市場を提供し、土地をクレームするときにBase Costを課すことができます。

共通するFTB依存MODを互換範囲のバージョンへ揃えれば、併用できる可能性が高い構成です。ただし、Buying Chunksの**Base Cost**を有効にすると、追加枠の購入時と実際の土地クレーム時にそれぞれ支払う二段階経済になります。この動作を意図しない場合はBase Costを無効にしてください。

これはソース上の互換性評価であり、将来のすべてのリリースやMODパック構成を保証するものではありません。Buying Chunksはサーバーと全クライアントへの導入が必要なため、併用時はその要件が適用されます。

## 🔄 元版または1.0からの更新

- 元のBuyClaimChunks JARを先に削除してください。
- 既存のFTB Chunks個人追加枠値がそのまま正本として使われます。
- 既存の`buyclaimchunks-common.toml`は維持されます。
- 旧設定の`amountRequired = 1`を`4`へ変更すると、現在の既定価格曲線を利用できます。
- またはサーバーを停止し、設定ファイルを削除してから起動すると現在の既定値が再生成されます。
- 本番サーバーでは、MOD変更前にワールドと設定をバックアップしてください。

## 🛠️ トラブルシュート

<details>
<summary><strong><code>/buyclaim</code>が存在しない</strong></summary>

Minecraft 1.21.1 / NeoForge版JARがサーバーの`mods`にあるか、FTB依存MODが正常に読み込まれたか、ログに`buyclaimchunks`の重複エラーがないか確認してください。
</details>

<details>
<summary><strong>「This command can only be run by a player!」と表示される</strong></summary>

ゲーム内のプレイヤーとして実行してください。コンソールやコマンドブロックには支払い元のインベントリがありません。
</details>

<details>
<summary><strong>設定したアイテムが存在しないと表示される</strong></summary>

`itemRequired`のスペルと名前空間、アイテムを追加するMODがサーバーに導入されているかを確認してください。
</details>

<details>
<summary><strong>アイテムを持っているのに不足と表示される</strong></summary>

防具スロットやオフハンドから、ホットバーまたは通常インベントリへ移してください。
</details>

<details>
<summary><strong>想定より価格が高い</strong></summary>

FTB Chunks上の現在の個人追加枠値を確認してください。管理者から付与された枠も価格上の枠番号に含まれます。
</details>

<details>
<summary><strong>「The claim purchase failed. No items were consumed.」と表示される</strong></summary>

内部のFTB Chunks枠追加処理が成功しませんでした。対応バージョンとサーバーログを確認してください。安全設計により支払いは残ります。
</details>

<details>
<summary><strong>設定変更が反映されない</strong></summary>

サーバーを停止し、サーバー側の`config/buyclaimchunks-common.toml`を編集・保存して再起動してください。別インスタンスの設定を編集していないかも確認してください。
</details>

## 🧪 ソースからビルドする

Java 21を使用します。単体テストとリリースJARのビルドを実行します。

```shell
./gradlew clean test build
```

NeoForge GameTestサーバーを実行します。

```shell
./gradlew runGameTestServer
```

生成されたJARの内容とメタデータを検証します。

```shell
bash scripts/verify-release-jar.sh
```

JARは次の場所に生成されます。

```text
build/libs/buyclaimchunks-continued-neoforge-1.21.1-1.1.1.jar
```

開発用の専用サーバーは次のコマンドで起動できます。

```shell
./gradlew runServer
```

GitHub Actionsでは単体テスト、完成JAR検査、NeoForge GameTest、クリーンな専用サーバー起動を実行し、Minecraftの`Done`メッセージへ到達することを確認します。プロジェクトのバージョンと一致するタグでは、検証後にGitHub ReleaseとSHA-256チェックサムも公開します。

## 🐛 不具合を報告する

[Issues](https://github.com/nekomario28/BuyClaimChunks/issues)へ、次の情報を添えて報告してください。

- Minecraft、NeoForge、FTB Chunks、BuyClaimChunks Continuedの各バージョン。
- `latest.log`の関係部分またはクラッシュレポート。
- 実行したコマンドと設定値。
- 新しく生成した設定でも再現するか。

アカウントトークン、サーバー管理画面のパスワード、非公開アドレスなどは記載しないでください。

## 📄 ライセンスとクレジット

BuyClaimChunks Continuedは[MIT License](LICENSE)で配布され、**nekomario28**が保守しています。

原作はSkyAdri氏によるものです。帰属表示とforkの詳細は[NOTICE](NOTICE)を参照してください。