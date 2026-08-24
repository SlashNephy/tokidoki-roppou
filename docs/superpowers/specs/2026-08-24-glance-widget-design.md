# Glance ウィジェット設計

- Issue: [#53](https://github.com/SlashNephy/tokidoki-roppou/issues/53)
- 日付: 2026-08-24

## 目的

ホーム画面のウィジェットにランダムな条文を表示する。通知と同じく「ときどき目に入る」体験をホーム画面上でも提供する。

## 方針

- ウィジェットは**通知とは独立**にランダム抽選を行う。通知が OFF でもウィジェットは動作する。
- 更新間隔は**ウィジェット専用の設定**を持つ。通知の間隔とは独立に選べる。
- 対象法令は通知の `enabledLawIds` を共有する。ウィジェット専用の法令選択は設けない。
- ウィジェット上の操作はリロードと本体タップの 2 つに絞る。コピー・共有・ブックマークはウィジェットの面積を考慮して見送る。

## モジュール構成

新規モジュール `feature:widget` を追加する。既存の convention plugin に乗せる。

```kotlin
plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.compose")
    id("tokidokiroppou.hilt")
}
```

依存は `:core:domain`, `:core:data` のみ。`:core:ui` には依存しない（後述）。

`ArticleWidgetReceiver` と `appwidget-provider` の XML は `feature:widget` 内の `AndroidManifest.xml` / `res/xml` に置き、manifest merger で `app` に統合する。`app` は `feature:widget` に依存を追加するのみ。

### Glance は Compose UI ではない

`androidx.glance.*` は RemoteViews へコンパイルされる別の Composable ツリーであり、`core:ui` の `ArticleCard` などの Material 3 Composable は再利用できない。ウィジェット用の Composable は `feature:widget` 内に新規に書く。

## クラス構成

| クラス | 責務 |
| --- | --- |
| `ArticleWidget : GlanceAppWidget` | 描画のみ。state から条文の identity を読み、DB から解決して表示する |
| `ArticleWidgetReceiver : GlanceAppWidgetReceiver` | ライフサイクル。`onEnabled` / `onUpdate` / `onDisabled` で Worker のスケジュール・即時更新・キャンセルを行う |
| `ArticleWidgetRefresher` | 抽選の判断ロジック。`ArticleWidgetWorker` から Android フレームワーク依存を切り離し、素の JVM ユニットテストで検証できるようにする（`core:data`） |
| `ArticleWidgetWorker : @HiltWorker` | `ArticleWidgetRefresher.refresh()` を呼び、結果を `ListenableWorker.Result` に変換するだけの薄い殻 |
| `ArticleWidgetScheduler` | `PeriodicWorkRequest` の登録・解除。`ArticleNotificationScheduler` と同型 |
| `ArticleWidgetUpdater` | ウィジェットの表示内容を更新するインターフェース。Glance 実装から `core:data` を依存性逆転で切り離す（`core:data`） |
| `ArticleWidgetUpdaterImpl` | `ArticleWidgetUpdater` の実装。Glance state の更新と `ArticleWidget().updateAll(context)` を行う（`feature:widget`） |
| `RefreshArticleAction : ActionCallback` | リロードボタン。`ArticleWidgetScheduler.requestImmediateUpdate()` を呼ぶだけ |
| `ArticleWidgetEntryPoint` | 描画時にリポジトリを取得するための Hilt EntryPoint |

### Hilt の扱い

`GlanceAppWidgetReceiver` および `GlanceAppWidget` には `@AndroidEntryPoint` を付けられない。そのため

- 条文の抽選と永続化は `@HiltWorker` である `ArticleWidgetWorker` に寄せる
- 描画時に必要なリポジトリは `EntryPointAccessors.fromApplication()` で取得する

`ActionCallback` も同様に Hilt 非対応のため、`RefreshArticleAction` は `EntryPointAccessors.fromApplication()` で `ArticleWidgetScheduler` を取得し、`requestImmediateUpdate()` を呼ぶだけに留める。抽選ロジックを二重に持たない。

## 状態の持ち方

Glance の `PreferencesGlanceStateDefinition` に、条文の **identity のみ**を保存する。

- `lawId: String`
- `articleNumber: String`
- `supplementaryProvisionLabel: String?`

本文やタイトルは保存しない。`GlanceAppWidget.provideGlance` は suspend なので、描画のたびに `LawRepository.getArticle()` で解決する。この設計により

- `useHalfWidthParentheses` の設定変更が次の描画で自動的に反映される
- キャッシュ更新後の条文改正が反映される
- state に古いテキストが固着しない

## レイアウト

`Scaffold` + `TitleBar` を使う。

```
┌─────────────────────────────────┐
│ [icon]  日本国憲法           [⟳] │  ← TitleBar
├─────────────────────────────────┤
│ 第九条 戦争の放棄                │  ← 条文名
│                                 │
│ 日本国民は、正義と秩序を基調と    │  ← 本文
│ する国際平和を誠実に希求し、…     │
└─────────────────────────────────┘
```

- `TitleBar` の `startIcon` にアプリアイコン、`title` に法令名（`LawCatalogRepository.getLaw(lawId)?.displayName`）、`actions` にリロードの `CircleIconButton(backgroundColor = null)` を置く
- `TitleBar` の title は `maxLines = 1` かつ `defaultWeight()` で描画されるため、法令名が横幅に入り切らない場合は自動的に末尾が省略される。追加の実装は不要
- 条文名は `article.displayTitle(useHalfWidthParentheses)`、本文は `article.fullText(useHalfWidthParentheses)`
- 条文名は `maxLines = 1` でクランプし、横幅に入り切らない場合は末尾を省略する
- 本文は `LazyColumn` の単一アイテムに入れてスクロールできるようにする。Glance に汎用の縦スクロール修飾子はないため Lazy 系を使う
- 色は `GlanceTheme.colors`（`androidx.glance.GlanceTheme`、`glance-appwidget` 由来）で Dynamic Color に追従する。`glance-material3` には依存しない

#### 本文をスクロールさせることの代償

`LazyColumn` は RemoteViews の `ListView` にコンパイルされ、ListView がタッチを消費する。そのため
**本文タップではアプリを開けない**。実機で検証した結果は次のとおり。

| タップ位置 | アプリが開くか |
| --- | --- |
| 本文（ListView 内） | 開かない |
| 条文名の行（ListView の外） | 開く |
| TitleBar の法令名 | 開く |

遷移導線は TitleBar と条文名の行の 2 箇所で確保する。段落ごとにアイテムを分けても
ListView である点は変わらないため、この代償は回避できない。

また `LazyColumn` の KDoc にあるとおり、`itemId` を明示してもスクロール位置がウィジェット更新を
またいで保たれるのは API 31 以降。minSdk 28 なので、それ未満の端末では更新のたびに先頭へ戻る。

### タップ挙動

TitleBar と条文名の行のタップで `lawCode` / `articleNumber` / `supplementaryProvisionLabel` の extra 付きで `MainActivity` を起動し、HomeScreen にディープリンクする。extra のキーは通知と同じ `ArticleNotificationSender.EXTRA_LAW_CODE` / `EXTRA_ARTICLE_NUMBER` を再利用し、`app` 側の受け口を共通化する。

## 設定

`ApplicationSettings` に次を追加する。

```kotlin
val widgetUpdateIntervalMinutes: Int = 60,
```

設定画面に「ウィジェット」セクションを追加し、更新間隔を選べるようにする。選択肢は既存の `ApplicationSettings.INTERVAL_OPTIONS` を共有する。

設定変更時に `ArticleWidgetScheduler.schedule()` を呼び直す。スケジューラは `ArticleNotificationScheduler` と同型で、`PeriodicWorkRequest`・15 分下限・`ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE` を用いる。

ウィジェットが 1 つも配置されていない間は Worker をスケジュールしない。スケジュールの制御は次の経路から行う。

- `ArticleWidgetReceiver.onEnabled` — 最初の 1 個目のウィジェットが配置されたときに定期更新をスケジュールする
- `ArticleWidgetReceiver.onUpdate` — 2 個目以降のウィジェット配置時に呼ばれる唯一の経路のため、ここで即時更新をリクエストし新規ウィジェットが空表示のまま放置されないようにする（`onEnabled` は最初の 1 個目にしか反応しない）
- `ArticleWidgetReceiver.onDisabled` — 最後のウィジェットが削除されたときに定期更新をキャンセルする
- `TokidokiRoppouApplication.onCreate` — アプリ起動時、ウィジェットが既に配置されていれば定期更新をスケジュールし直す
- `SettingsScreenViewModel.setWidgetUpdateInterval` — 設定画面で更新間隔が変更されたときに再スケジュールする

## エラー処理

| 状況 | 挙動 |
| --- | --- |
| 抽選失敗（キャッシュ未取得など） | state を更新せず前回表示を維持し、Worker は `Result.retry()` を返す |
| 初回で条文が無い | 「条文を読み込めませんでした」とリロードボタンのみを表示する |
| 描画時に条文を解決できない | 同上のフォールバック表示 |

ログは英語、UI 文言は日本語とする（リポジトリの慣習に従う）。

## 依存関係

`gradle/libs.versions.toml` に Renovate が認識できる形で追加する。

```toml
[versions]
glance = "1.2.0-rc01"

[libraries]
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
```

Compose BOM には含まれないため個別に管理する。minSdk 28 は Glance の要件を満たす。

## 検証

- `ApplicationSettingsRepositoryImplTest` に `widgetUpdateIntervalMinutes` の永続化テストを追加する
- `./gradlew test`
- `./gradlew lintStagingDebug`
- 実機にウィジェットを配置し、初期表示・リロード・タップ遷移を録画とスクリーンショットで確認する

## 見送るもの

- ウィジェット上のコピー・共有・ブックマーク操作（面積の制約）
- ウィジェット専用の対象法令設定（通知の設定を共有する）
- 複数サイズごとの作り分け（`Scaffold` の可変高で吸収する）
