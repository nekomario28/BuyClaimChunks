# BuyClaimChunks Continued

<p align="center">
  <img src="src/main/resources/buyclaimchunks_continued.png" alt="BuyClaimChunks Continued ロゴ" width="256">
</p>

<p align="center">
  <a href="README.md">English</a> |
  <a href="https://github.com/nekomario28/BuyClaimChunks/actions/workflows/build.yml">ビルド状況</a> |
  <a href="LICENSE">MIT License</a>
</p>

[FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge)のプレイヤー個人用追加クレーム枠を、設定可能なアイテム通貨で購入できるMODです。

これは[SkyAdri氏のBuyClaimChunks](https://github.com/SkyAdri-mc/BuyClaimChunks)を独立して継続保守するforkです。元プロジェクトはMIT Licenseで[CurseForge](https://www.curseforge.com/minecraft/mc-mods/buyclaimchunks)に公開されています。本forkは元作者およびFTBによる公式版・公認版ではありません。

## このMODでできること

- `/buyclaim [個数]`で、FTB Chunksの**プレイヤー個人用追加クレーム枠**を1個以上購入できます。
- バニラ・他MODを問わず、登録済みの任意アイテムを通貨にできます。
- 価格は固定にも、現在の個人追加枠総数に応じて段階的に上昇する方式にも設定できます。
- ホットバーを含む通常インベントリ内の複数スタックを合算して支払えます。
- 先にFTB Chunks側の枠追加を実行し、成功した場合にだけ支払いアイテムを消費します。
- 1コマンドで購入できる個数と、個人追加枠の総上限を別々に設定できます。
- 価格計算のオーバーフローを検出し、不正な金額で処理しません。

このMODは、マップ上のチャンクを自動で保護するものではありません。FTB Teamsのパーティー共有上限、強制ロード枠を増やすものでもなく、このMOD独自の購入回数データも保存しません。

## 対応バージョン

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

Minecraft 1.20.1 / Forge 47.4向けの旧版は`forge-1.20.1`ブランチに残されています。コードや設定項目が本ガイドと異なる場合があります。

## 必要なもの

BuyClaimChunks Continuedより先に、次を導入してください。

1. Minecraft 1.21.1向けNeoForge。
2. FTB Chunks。
3. FTB Chunksの必須依存関係であるFTB Library、FTB Teams、Architectury API。
4. サーバープロセス用のJava 21。

コマンドと枠追加処理はサーバー側で動作します。ランチャーやサーバーがクライアントとサーバーのMOD一覧一致を要求する構成では、配布MODパックの両側へ同じJARを入れるのが安全です。

## インストール

1. リポジトリの[Releases](https://github.com/nekomario28/BuyClaimChunks/releases)からMinecraft 1.21.1 / NeoForge版JARをダウンロードします。
2. サーバーを停止します。
3. JARをサーバーの`mods`フォルダーへ入れます。
4. FTB Chunksとその依存MODも`mods`に存在することを確認します。
5. サーバーを一度起動すると、`config/buyclaimchunks-common.toml`が生成されます。
6. 設定を変更する場合はサーバーを停止してから編集します。
7. サーバーを再起動し、通常プレイヤーで`/buyclaim`を試します。

ワールドやMODパックとの互換性を維持するため、元版と同じ`buyclaimchunks`というmod IDを使用しています。**元のBuyClaimChunks JARと同時に導入しないでください。**

## すぐに使う

既定設定では通貨は`minecraft:diamond`で、1枠目はダイヤモンド4個です。

```text
/buyclaim
```

個人用追加クレーム枠を1個購入します。

```text
/buyclaim 5
```

次の5枠を1回の取引で購入します。各枠の価格を個別に計算し、5枠分を合計します。

このコマンドは通常プレイヤーが権限なしで使用できます。ただし、支払い元となるプレイヤーのインベントリが必要なため、サーバーコンソールやコマンドブロックからは実行できません。

## 実際に何が増えるのか

購入に成功すると、FTB Chunksが保持するプレイヤーの**個人用追加クレーム上限**が増えます。現在位置やマップ上のチャンクが自動的に保護されるわけではありません。

枠を購入した後、プレイヤーは通常どおりFTB Chunksのマップ画面やクレーム操作を使ってチャンクを保護します。

現在価格と上限判定には、FTB Chunks側が保持する個人追加枠総数を使用します。そのため、次の点に注意してください。

- 管理者が別途付与した追加枠も、次回購入価格に反映されます。
- `maxExtraClaims`は、このMODで購入した数だけではなく、FTB Chunks上の個人追加枠総数に対する絶対上限です。
- このMODは独自の枠データベースや購入回数を保存しないため、削除・入れ替え時に独自データを移行する必要はありません。

## 支払いの仕組み

支払いアイテムは、ホットバーを含む36枠の通常インベントリから数えます。防具スロットとオフハンドは対象外です。購入前に支払いアイテムをホットバーまたは通常インベントリへ移してください。

同じアイテムの複数スタックは合算されます。FTB Chunks側の枠追加が成功した後、合計金額に達するまで該当スタックから順番にアイテムを減らします。

処理順序は次のとおりです。

1. FTB Chunksから現在の個人追加枠総数を読み取ります。
2. 上限を確認し、複数購入を含む総額を計算します。
3. 設定アイテムが存在するか確認し、プレイヤーの所持数を数えます。
4. FTB Chunksの枠追加コマンドを同期実行します。
5. FTB Chunks側が成功を返した場合にだけ支払いを消費します。

将来のFTB Chunks更新で内部コマンドが変わるなどして枠追加に失敗した場合、購入を中止し、アイテムは消費しません。

## 設定

設定ファイルは次の場所に生成されます。

```text
config/buyclaimchunks-common.toml
```

既定値は次のとおりです。

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
| `itemRequired` | `minecraft:diamond` | 登録済みのアイテムID | 支払いに使うアイテムです。`modid:item_name`形式の他MODアイテムも指定できます。 |
| `amountRequired` | `4` | 1～2,147,483,647 | 基本価格かつ最低価格です。個人追加枠1番目の価格になります。 |
| `priceGrowthFactor` | `3.45` | 0～1,000,000 | 後半の枠がどの程度速く高くなるかを調整します。`0`で固定価格になります。 |
| `priceExponent` | `0.5` | 0～4 | 価格曲線の形を調整します。`0.5`は平方根型です。`0`で固定価格になります。 |
| `maxExtraClaims` | `100` | 1～2,147,483,647 | 購入後に許可するFTB Chunks個人追加枠総数の上限です。 |
| `maxPurchaseAmount` | `100` | 1～10,000 | 1回の`/buyclaim`で指定できる最大個数です。 |

設定編集前にサーバーを停止してください。範囲外の値はNeoForgeの設定検証で拒否される場合があります。存在しない`itemRequired`を指定した場合は購入時に検出され、アイテムは消費されません。

### 通貨を変える例

エメラルドを使用する場合は次のようにします。

```toml
itemRequired = "minecraft:emerald"
```

他MODのアイテムを使用する場合は次のようにします。

```toml
itemRequired = "examplemod:coin"
```

指定したアイテムIDがサーバーのアイテムレジストリに存在する必要があります。

### 固定価格の例

すべての枠をエメラルド8個にする設定です。

```toml
itemRequired = "minecraft:emerald"
amountRequired = 8
priceGrowthFactor = 0.0
priceExponent = 0.5
```

`priceGrowthFactor`または`priceExponent`のどちらかを`0`にすると、計算価格は常に`amountRequired`になります。

## 段階価格

1から数える個人追加枠番号を`n`とすると、1枠の価格は次の式です。

```text
round(amountRequired + priceGrowthFactor * (n ^ priceExponent - 1))
```

計算結果が`amountRequired`を下回ることはありません。

既定値での価格例です。

| 個人追加枠番号 | 必要アイテム数 |
|---:|---:|
| 1 | 4 |
| 2 | 5 |
| 3 | 7 |
| 5 | 8 |
| 10 | 11 |
| 50 | 25 |
| 100 | 35 |

複数購入では、次の枠を順番に1枠ずつ計算して合計します。

- 個人追加枠が0のプレイヤーが`/buyclaim 5`を実行すると、`4 + 5 + 7 + 7 + 8 = 31`個のダイヤモンドが必要です。
- 個人追加枠が8のプレイヤーが`/buyclaim 3`を実行すると、9・10・11番目の価格である`11 + 11 + 12 = 34`個のダイヤモンドが必要です。

## 上限

`maxPurchaseAmount`は1回のコマンドを制限し、`maxExtraClaims`は購入後の個人追加枠総数を制限します。

設定例です。

```toml
maxExtraClaims = 100
maxPurchaseAmount = 10
```

この場合、1回に購入できるのは最大10枠で、FTB Chunks上の個人追加枠総数は100を超えられません。管理者がすでに100枠を付与しているプレイヤーは、このMODで一度も購入していなくても追加購入できません。

## 元版または1.0からの更新

- 両方が`buyclaimchunks`というmod IDを使うため、元のBuyClaimChunks JARを削除してから本forkを導入してください。
- 既存のFTB Chunks個人追加枠値がそのまま正本として使用されます。
- 既存の`buyclaimchunks-common.toml`は維持され、設定値が自動的に新しい既定値へ書き換えられることはありません。
- 旧設定に`amountRequired = 1`が残っている場合、新しい既定価格曲線を使うには`4`へ変更してください。
- またはサーバーを停止し、`config/buyclaimchunks-common.toml`を削除してから起動すると現在の既定値が再生成されます。
- 本番サーバーでは、MOD変更前にワールドと設定をバックアップしてください。

## トラブルシュート

### `/buyclaim`が存在しない

次を確認してください。

- JARがサーバーの`mods`フォルダーにあるか。
- Minecraft 1.21.1 / NeoForge版であり、Forge 1.20.1版を入れていないか。
- FTB Chunksとすべての依存MODが正常に読み込まれているか。
- サーバーログに`buyclaimchunks`のmod ID重複エラーがないか。

### 「This command can only be run by a player!」と表示される

ゲーム内のプレイヤーとして実行してください。コンソールやコマンドブロックには支払い用インベントリがないため購入できません。

### 設定したアイテムが存在しないと表示される

`itemRequired`のスペル、名前空間、アイテムを追加するMODがサーバーに導入されているかを確認してください。

### アイテムを持っているのに不足と表示される

防具スロットやオフハンドから通常インベントリまたはホットバーへ移してください。支払い判定は36枠の通常インベントリだけを対象にします。

### 想定より価格が高い

FTB Chunks上の現在の個人追加枠値を確認してください。管理者から付与された枠も価格上の枠番号に含まれます。

### 「The claim purchase failed. No items were consumed.」と表示される

内部で実行したFTB Chunksの枠追加コマンドが成功を返しませんでした。対応するFTB Chunksバージョンを確認し、サーバーログを調べてください。安全設計により支払いアイテムは残ります。

### 設定変更が反映されない

サーバーを停止し、サーバー側の`config/buyclaimchunks-common.toml`を編集・保存して再起動してください。クライアント側や別インスタンスの設定を編集していないかも確認してください。

## ソースからビルドする

Java 21を使用します。

```shell
./gradlew clean test build
```

JARは次の場所に生成されます。

```text
build/libs/buyclaimchunks-continued-neoforge-1.21.1-1.1.0.jar
```

開発用の専用サーバーは次のコマンドで起動できます。

```shell
./gradlew runServer
```

GitHub Actionsではテスト、JARビルド、クリーンな専用サーバー起動を実行し、Minecraftの`Done`メッセージへ到達することを確認します。ActionsのArtifactは開発確認用の一時成果物であり、利用者向けの永続的な正式Releaseではありません。

## 不具合を報告する

[Issues](https://github.com/nekomario28/BuyClaimChunks/issues)へ、次の情報を添えて報告してください。

- Minecraft、NeoForge、FTB Chunks、BuyClaimChunks Continuedの各バージョン。
- `latest.log`の関係部分またはクラッシュレポート。
- 実行したコマンドと設定値。
- 新しく生成した設定でも再現するか。

アカウントトークン、サーバー管理画面のパスワード、非公開アドレスなどは記載しないでください。

## ライセンスとクレジット

BuyClaimChunks Continuedは[MIT License](LICENSE)で配布されます。

原作はSkyAdri氏によるものです。forkの保守、NeoForge 1.21.1対応、段階価格、安全性改善、テスト、CI、ドキュメント、独立したブランド整備はYuu（`nekomario28`）が担当しています。詳細は[NOTICE](NOTICE)を参照してください。
