# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

「ときどき六法」は、一定間隔で日本の法令の条文を通知してくれる Android アプリ。Kotlin / Jetpack Compose / Material 3 で構築されたマルチモジュール構成。

## ビルド・開発コマンド

```bash
# ビルド (Staging Debug)
./gradlew assembleStagingDebug

# ユニットテスト
./gradlew testStagingDebugUnitTest

# Android Lint
./gradlew lintStagingDebug

# 特定モジュールのみビルド
./gradlew :core:domain:assembleStagingDebug

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
├── feature:home                 ← ホーム画面 (条文表示)
│   └── core:domain, core:data, core:ui
├── feature:laws                 ← 法令一覧・検索画面
│   └── core:domain, core:data, core:ui
└── feature:settings             ← 設定画面
    └── core:domain, core:data, core:ui
```

### パッケージルート

`blue.starry.tokidokiroppou`

### レイヤー構造

- **core:domain** — ビジネスモデル (`Article`, `LawCode`, `ApplicationSettings`)、リポジトリインターフェース (`LawRepository`, `ApplicationSettingsRepository`)、テキスト処理ロジック (`TextNormalizer`, `ArticleReferenceExtractor`)
- **core:data** — リポジトリ実装、e-Gov 法令 API クライアント (`EGovLawApiClient`)、Room DB (`AppDatabase`)、WorkManager タスク (通知: `ArticleNotificationWorker`, キャッシュ更新: `CacheRefreshWorker`)、Hilt DI モジュール
- **core:ui** — `ArticleCard`, `SettingSection`, `SettingItem` など共有 Composable
- **feature/*** — 各画面の `Screen` (Composable) + `ViewModel` のペア

### DI

Hilt を使用。DI モジュールは `core:data` の `di/` パッケージに集約:
- `DataProvidesModule` — Room DB, DAO, DataStore, WorkManager
- `DataBindsModule` — リポジトリインターフェースと実装のバインド
- `NetworkModule` — Ktor HttpClient

### データフロー

1. `EGovLawApiClient` が e-Gov 法令 API (`https://laws.e-gov.go.jp/api/2`) からデータ取得
2. `LawJsonParser` が JSON を `Article` モデルに変換
3. `LawRepositoryImpl` が Room DB にキャッシュ (24 時間で自動更新)
4. `CacheRefreshWorker` がバックグラウンドでキャッシュを定期更新
5. `ArticleNotificationWorker` が設定間隔でランダム条文を通知

### ビルドフレーバー

- `staging` — テスト配布用 (Firebase App Distribution)
- `production` — 本番リリース用

### build-logic

`build-logic/convention/` に Gradle Convention Plugin を配置し、モジュール間のビルド設定を共通化:
- `AndroidApplicationConventionPlugin` — compileSdk 36, minSdk 28, targetSdk 35, Java 17
- `AndroidLibraryConventionPlugin` — ライブラリ共通設定
- `ComposeLibraryConventionPlugin` — Compose 有効化
- `HiltConventionPlugin` — Hilt + KSP 自動適用
- `KotlinSerializationConventionPlugin` — Serialization 適用

### ナビゲーション

Jetpack Navigation Compose を使用。各 feature モジュールに `@Serializable` な Route オブジェクトを定義し、`app/App.kt` で NavHost に登録。通知タップ時は `lawCode`/`articleNumber` パラメータ付きで HomeScreen に遷移。

## CI/CD

GitHub Actions で 3 ワークフロー:
- `ci.yml` — assemble, test, android-lint, trivy, codeql (push to main / PR)
- `deploy.yml` — CI 成功後に Staging を Firebase App Distribution へ自動配布
- `release.yml` — GitHub Release 作成時に Production ビルドを配布
