# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

「ときどき六法」は、一定間隔で日本の法令の条文を通知してくれる Android アプリ。通知のほか、ホーム画面ウィジェットでの条文表示、法令一覧・検索、条文のブックマーク (コレクション)、AI による条文の解説を備える。Kotlin / Jetpack Compose / Material 3 で構築されたマルチモジュール構成。

## ビルド・開発コマンド

```bash
# ビルド (Staging Debug)
./gradlew assembleStagingDebug

# ユニットテスト (全モジュール)
# フレーバーは app にしかないため、testStagingDebugUnitTest では 1 件も実行されない
./gradlew test

# Android Lint
./gradlew lintStagingDebug

# 特定モジュールのみビルド
# ライブラリモジュールにはフレーバーがないため、タスク名にフレーバーは付かない
./gradlew :core:domain:assembleDebug

# Staging Release ビルド (署名付き)
./gradlew assembleStagingRelease
```

**注意**: `app/google-services.json` は Git 管理外。ビルド前に Firebase コンソールから取得して配置する必要がある。

## アーキテクチャ

### モジュール構成と依存関係

```
app                              ← エントリーポイント (Activity, Navigation, Theme)
├── core:domain                  ← ドメイン層 (モデル, リポジトリIF, テキスト処理)
├── core:data                    ← データ層 (API, DB, DI, Worker)
│   └── core:domain
├── core:ui                      ← 共有 UI コンポーネント
│   └── core:domain
├── core:ai                      ← 条文の AI 解説 (Firebase AI / Gemini)
│   └── core:domain, core:ui
├── feature:home                 ← ホーム画面 (条文表示)
│   └── core:domain, core:data, core:ui, core:ai
├── feature:laws                 ← 法令一覧・検索画面
│   └── core:domain, core:data, core:ui
├── feature:collection           ← コレクション画面 (ブックマークした条文の一覧)
│   └── core:domain, core:data, core:ui, core:ai
├── feature:settings             ← 設定画面
│   └── core:domain, core:data, core:ui
└── feature:widget               ← ホーム画面ウィジェット (Glance)
    └── core:domain, core:data
```

`feature:widget` は Glance (RemoteViews) で描画するため、Compose UI 前提の `core:ui` には依存しない。

### パッケージルート

`blue.starry.tokidokiroppou`

### レイヤー構造

- **core:domain** — ビジネスモデル (`Article`, `LawCode`, `ApplicationSettings`)、リポジトリインターフェース (`LawRepository`, `LawCatalogRepository`, `ApplicationSettingsRepository`, `BookmarkRepository`)、テキスト処理ロジック (`TextNormalizer`, `ArticleReferenceExtractor`)
- **core:data** — リポジトリ実装、e-Gov 法令 API クライアント (`EGovLawApiClient`)、Room DB (`AppDatabase`)、WorkManager タスク (通知: `ArticleNotificationWorker`, キャッシュ更新: `CacheRefreshWorker`, ウィジェット更新: `ArticleWidgetWorker`)、Hilt DI モジュール
- **core:ui** — `ArticleCard`, `ArticleExplanationSheet`, `SettingSection`, `SettingItem` など共有 Composable
- **core:ai** — Firebase AI (Gemini) による条文の解説生成 (`ArticleExplanationRepository`, `ArticleExplanationViewModel`)。生成結果は専用の Room DB (`ExplanationCacheDatabase`) にキャッシュする
- **feature/*** — 各画面の `Screen` (Composable) + `ViewModel` のペア
- **feature:widget** — Glance ウィジェット (`ArticleWidget`)。条文の抽選は `core:data` の `ArticleWidgetWorker` が行い、ウィジェットの更新は `ArticleWidgetUpdater` インターフェース経由で `core:data` から呼ばれる (依存性逆転)

### DI

Hilt を使用。DI モジュールは各モジュールの `di/` パッケージに置く:
- `core:data` の `DataModule` — Room DB, DAO, DataStore, WorkManager の提供と、リポジトリインターフェースと実装のバインド
- `core:data` の `NetworkModule` — Ktor HttpClient
- `core:ai` の `AiBindsModule` / `AiProvidesModule` — 解説リポジトリのバインドと `GenerativeModel` の提供
- `feature:widget` の `WidgetModule` — `ArticleWidgetUpdater` の実装バインド (`core:data` からの依存性逆転を成立させる)

### データフロー

1. `EGovLawApiClient` が e-Gov 法令 API (`https://laws.e-gov.go.jp/api/2`) からデータ取得
2. `LawJsonParser` が JSON を `Article` モデルに変換
3. `LawRepositoryImpl` が Room DB にキャッシュ (24 時間で自動更新)
4. `CacheRefreshWorker` がバックグラウンドでキャッシュを定期更新
5. `ArticleNotificationWorker` が設定間隔でランダム条文を通知
6. `ArticleWidgetWorker` が設定間隔とリロード操作でランダム条文を抽選し、ウィジェットへ反映

### ビルドフレーバー

`environment` ディメンションに 3 つ。フレーバーは `app` にのみ定義されており、ライブラリモジュールには存在しない。

- `local` — ローカル開発用 (`applicationId` に `.local` サフィックス、`isDefault`)
- `staging` — テスト配布用 (`.staging` サフィックス、Firebase App Distribution)
- `production` — 本番リリース用 (サフィックスなし)

`local` と `staging` は `applicationId` が異なる別アプリとして端末にインストールされる。実機検証で APK を入れ替えるときは、検証対象のフレーバーを取り違えないよう注意する。

### build-logic

`build-logic/convention/` に Gradle Convention Plugin を配置し、モジュール間のビルド設定を共通化:
- `AndroidApplicationConventionPlugin` — compileSdk 37, minSdk 28, targetSdk 35, Java 17
- `AndroidLibraryConventionPlugin` — ライブラリ共通設定
- `ComposeLibraryConventionPlugin` — Compose 有効化
- `HiltConventionPlugin` — Hilt + KSP 自動適用
- `KotlinSerializationConventionPlugin` — Serialization 適用

### ナビゲーション

Navigation 3 (`androidx.navigation3`) を使用。各 feature モジュールに `@Serializable` かつ `NavKey` を実装した Route を定義し (`HomeRoute`, `LawsRoute`, `CollectionRoute`。`SettingsRoute` のみ `app/App.kt`)、`app/App.kt` の `NavDisplay` に `entry<Route> { ... }` で登録する。バックスタックは `rememberNavBackStack` が保持し、遷移は `backStack.add(...)` で行う。通知やウィジェットのタップ時は `lawCode`/`articleNumber`/`supplementaryProvisionLabel` を Intent extra で受け取り、それらを持つ `HomeRoute` を開始 Route にする。

## CI/CD

GitHub Actions で 3 ワークフロー:
- `ci.yml` — assemble, test, android-lint, trivy, codeql (push to main / PR)
- `deploy.yml` — CI 成功後に Staging を Firebase App Distribution へ自動配布
- `release.yml` — GitHub Release 作成時に Production ビルドを配布
