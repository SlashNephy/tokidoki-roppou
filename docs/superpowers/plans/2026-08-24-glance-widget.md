# Glance ウィジェット実装プラン

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ホーム画面に配置できる Glance ウィジェットを追加し、ランダムな条文を定期的に表示する。

**Architecture:** 新規モジュール `feature:widget` に Glance の描画を置き、条文の抽選は `@HiltWorker` に寄せる。Glance の state には条文の identity のみを保存し、描画のたびに Room から解決する。更新は WorkManager の `PeriodicWorkRequest` で行う。

**Tech Stack:** Kotlin / Jetpack Glance 1.2.0-rc01 / Hilt / WorkManager / Room / DataStore

**設計ドキュメント:** [docs/superpowers/specs/2026-08-24-glance-widget-design.md](../specs/2026-08-24-glance-widget-design.md)

## Global Constraints

- パッケージルート: `blue.starry.tokidokiroppou`。新規モジュールの namespace は `blue.starry.tokidokiroppou.feature.widget`
- glance のバージョンは `1.2.0-rc01`。`gradle/libs.versions.toml` に Renovate が認識できる形で記述する（バージョンをビルドスクリプトに直書きしない）
- Glance は Compose BOM に含まれないため個別に管理する
- `core:ui` の Material 3 Composable は Glance から再利用できない。`feature:widget` は `:core:ui` に依存しない
- `GlanceAppWidget` / `GlanceAppWidgetReceiver` / `ActionCallback` には `@AndroidEntryPoint` を付けられない。リポジトリの取得は `@HiltWorker` か `EntryPointAccessors` を使う
- コード内のコメントと UI 文言は日本語、ログとエラーメッセージは英語
- コミットメッセージは Conventional Commits 形式。`Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>` を付与する
- ソースコードのパターンを検査するようなテスト、定数をコピーと比較するだけのテストは書かない
- ビルドフレーバーは `local`（既定）/ `staging` / `production`。実機確認は `assembleLocalDebug`、テストは `testStagingDebugUnitTest`、lint は `lintStagingDebug`

---

## File Structure

**新規作成**

| ファイル | 責務 |
| --- | --- |
| `feature/widget/build.gradle.kts` | モジュールのビルド設定 |
| `feature/widget/src/main/AndroidManifest.xml` | receiver の宣言 |
| `feature/widget/src/main/res/xml/article_widget_info.xml` | appwidget-provider のメタデータ |
| `feature/widget/src/main/res/values/strings.xml` | ウィジェット名・説明・UI 文言 |
| `feature/widget/src/main/res/drawable/ic_refresh.xml` | リロードアイコン |
| `.../feature/widget/ArticleWidget.kt` | `GlanceAppWidget`。state を読んで条文を解決し描画する |
| `.../feature/widget/ArticleWidgetContent.kt` | Glance の Composable（Scaffold / TitleBar / 本文 / フォールバック） |
| `.../feature/widget/ArticleWidgetReceiver.kt` | `GlanceAppWidgetReceiver`。onEnabled / onDisabled |
| `.../feature/widget/ArticleWidgetStateKeys.kt` | Glance state のキー定義 |
| `.../feature/widget/ArticleWidgetEntryPoint.kt` | 描画時のリポジトリ取得用 Hilt EntryPoint |
| `.../feature/widget/RefreshArticleAction.kt` | リロードボタンの `ActionCallback` |
| `.../core/data/worker/ArticleWidgetWorker.kt` | 条文の抽選と state 更新 |
| `.../core/data/worker/ArticleWidgetScheduler.kt` | PeriodicWorkRequest の登録・解除 |

`ArticleWidgetWorker` と `ArticleWidgetScheduler` を `core:data` に置く理由は、`feature:widget` から `core:data` への一方向依存を保ちつつ、`app` と `feature:settings` の双方からスケジューラを呼べるようにするため。既存の `ArticleNotificationWorker` / `ArticleNotificationScheduler` と同じ場所・同じ形になる。

Worker から Glance の state を更新するには `core:data` にも glance-appwidget が必要になる。`ArticleWidget` クラス自体は `feature:widget` にあるため、Worker はウィジェットの更新を **`ArticleWidgetUpdater` インターフェース経由**で行い、実装を `feature:widget` に置く（Task 4 で定義）。

**変更**

| ファイル | 変更内容 |
| --- | --- |
| `gradle/libs.versions.toml` | glance の version / library を追加 |
| `settings.gradle.kts` | `include(":feature:widget")` |
| `app/build.gradle.kts` | `implementation(project(":feature:widget"))` |
| `core/data/build.gradle.kts` | glance-appwidget を追加 |
| `.../core/domain/model/ApplicationSettings.kt` | `widgetUpdateIntervalMinutes` を追加 |
| `.../core/domain/repository/ApplicationSettingsRepository.kt` | `setWidgetUpdateIntervalMinutes` を追加 |
| `.../core/data/repository/ApplicationSettingsRepositoryImpl.kt` | 上記の実装と DataStore キー |
| `.../core/data/di/DataModule.kt` | `ArticleWidgetUpdater` のバインド |
| `.../feature/settings/ui/SettingsScreenViewModel.kt` | `setWidgetUpdateInterval` |
| `.../feature/settings/ui/SettingsScreen.kt` | 「ウィジェット」セクション |
| `.../TokidokiRoppouApplication.kt` | 起動時のウィジェット再スケジュール |

---

## Task 1: モジュール雛形と配置可能なウィジェット

ホーム画面にウィジェットを配置でき、固定文言が表示されるところまでを作る。条文の解決はまだ行わない。

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `feature/widget/build.gradle.kts`
- Create: `feature/widget/src/main/AndroidManifest.xml`
- Create: `feature/widget/src/main/res/xml/article_widget_info.xml`
- Create: `feature/widget/src/main/res/values/strings.xml`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidget.kt`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetReceiver.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: なし
- Produces: `class ArticleWidget : GlanceAppWidget`（引数なしコンストラクタ）、`class ArticleWidgetReceiver : GlanceAppWidgetReceiver`

- [ ] **Step 1: `libs.versions.toml` に glance を追加**

`[versions]` セクションの `datastore = "1.2.1"` の次の行に追加:

```toml
glance = "1.2.0-rc01"
```

`[libraries]` セクションの `androidx-datastore-preferences = ...` の次の行に追加:

```toml
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
```

- [ ] **Step 2: `settings.gradle.kts` にモジュールを登録**

`include(":feature:settings")` の次の行に追加:

```kotlin
include(":feature:widget")
```

- [ ] **Step 3: `feature/widget/build.gradle.kts` を作成**

```kotlin
plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.compose")
    id("tokidokiroppou.hilt")
}

android {
    namespace = "blue.starry.tokidokiroppou.feature.widget"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
}
```

- [ ] **Step 4: 文字列リソースを作成**

`feature/widget/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="widget_label">ときどき六法</string>
    <string name="widget_description">ランダムな条文を表示します</string>
    <string name="widget_refresh_content_description">別の条文を表示</string>
    <string name="widget_empty_message">条文を読み込めませんでした</string>
</resources>
```

- [ ] **Step 5: appwidget-provider のメタデータを作成**

`feature/widget/src/main/res/xml/article_widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/widget_description"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minHeight="110dp"
    android:minWidth="180dp"
    android:resizeMode="horizontal|vertical"
    android:targetCellHeight="2"
    android:targetCellWidth="3"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

`@layout/glance_default_loading_layout` は glance-appwidget が提供するレイアウト。`updatePeriodMillis="0"` にして、更新は WorkManager 側に一本化する。

- [ ] **Step 6: `ArticleWidget` を作成（プレースホルダ表示）**

`feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidget.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text

class ArticleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Text(text = "ときどき六法")
            }
        }
    }
}
```

- [ ] **Step 7: `ArticleWidgetReceiver` を作成**

`feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetReceiver.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ArticleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArticleWidget()
}
```

- [ ] **Step 8: Manifest に receiver を宣言**

`feature/widget/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application>
        <receiver
            android:name="blue.starry.tokidokiroppou.feature.widget.ArticleWidgetReceiver"
            android:exported="true"
            android:label="@string/widget_label">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>

            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/article_widget_info" />
        </receiver>
    </application>
</manifest>
```

`android:exported="true"` はシステムから `APPWIDGET_UPDATE` を受け取るために必要。

- [ ] **Step 9: `app` から `feature:widget` に依存**

`app/build.gradle.kts` の `dependencies` ブロック内、`implementation(project(":feature:settings"))` の次の行に追加:

```kotlin
    implementation(project(":feature:widget"))
```

- [ ] **Step 10: ビルドが通ることを確認**

```bash
./gradlew :feature:widget:assembleLocalDebug :app:assembleLocalDebug
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 11: 実機でウィジェットを配置して確認**

```bash
./gradlew :app:installLocalDebug
```

ウィジェットピッカーに「ときどき六法」が現れることを確認し、ホーム画面に配置して「ときどき六法」という文字列が表示されるスクリーンショットを取得する。

```bash
adb shell screencap -p /sdcard/widget-task1.png && adb pull /sdcard/widget-task1.png
```

- [ ] **Step 12: コミット**

```bash
git add gradle/libs.versions.toml settings.gradle.kts app/build.gradle.kts feature/widget
git commit -m "feat(widget): Glance ウィジェットのモジュール雛形を追加

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 2: ウィジェット更新間隔の設定を追加

`ApplicationSettings` に `widgetUpdateIntervalMinutes` を追加し、DataStore に永続化する。

**Files:**
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt`
- Test: `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImplTest.kt`

**Interfaces:**
- Consumes: なし
- Produces:
  - `ApplicationSettings.widgetUpdateIntervalMinutes: Int`（既定値 60）
  - `suspend fun ApplicationSettingsRepository.setWidgetUpdateIntervalMinutes(minutes: Int)`
  - DataStore キー名は `"widget_update_interval"`

- [ ] **Step 1: 失敗するテストを書く**

`ApplicationSettingsRepositoryImplTest.kt` の既存のキー定義群（`excludeSupplementaryProvisionsKey` の宣言の次の行）に追加:

```kotlin
    private val widgetUpdateIntervalKey = intPreferencesKey("widget_update_interval")
```

そしてクラス内の既存テストの後ろに追加:

```kotlin
    @Test
    fun setWidgetUpdateIntervalMinutesPersistsValueAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setWidgetUpdateIntervalMinutes(240)

            assertEquals(240, testEnvironment.dataStore.data.first()[widgetUpdateIntervalKey])
            assertEquals(240, testEnvironment.repository.get().widgetUpdateIntervalMinutes)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getFallsBackToOneHourWhenWidgetUpdateIntervalIsAbsent() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            assertEquals(60, testEnvironment.repository.get().widgetUpdateIntervalMinutes)
        } finally {
            testEnvironment.close()
        }
    }
```

- [ ] **Step 2: テストが失敗することを確認**

```bash
./gradlew :core:data:testStagingDebugUnitTest --tests '*ApplicationSettingsRepositoryImplTest*'
```

期待: コンパイルエラー（`setWidgetUpdateIntervalMinutes` および `widgetUpdateIntervalMinutes` が未解決）

- [ ] **Step 3: `ApplicationSettings` にフィールドを追加**

`ApplicationSettings.kt` の `excludeSupplementaryProvisions: Boolean = false,` の次の行に追加:

```kotlin
    val widgetUpdateIntervalMinutes: Int = 60,
```

- [ ] **Step 4: リポジトリインターフェースにメソッドを追加**

`ApplicationSettingsRepository.kt` の `suspend fun setExcludeSupplementaryProvisions(enabled: Boolean)` の次の行に追加:

```kotlin

    suspend fun setWidgetUpdateIntervalMinutes(minutes: Int)
```

- [ ] **Step 5: 実装を追加**

`ApplicationSettingsRepositoryImpl.kt` の `setExcludeSupplementaryProvisions` の実装の直後に追加:

```kotlin
    override suspend fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_WIDGET_UPDATE_INTERVAL] = minutes
        }
    }
```

`toApplicationSettings()` の `excludeSupplementaryProvisions = ...` の行の次に追加:

```kotlin
            widgetUpdateIntervalMinutes = this[KEY_WIDGET_UPDATE_INTERVAL] ?: 60,
```

`companion object` の末尾に追加:

```kotlin
        private val KEY_WIDGET_UPDATE_INTERVAL = intPreferencesKey("widget_update_interval")
```

- [ ] **Step 6: テストが通ることを確認**

```bash
./gradlew :core:data:testStagingDebugUnitTest --tests '*ApplicationSettingsRepositoryImplTest*'
```

期待: `BUILD SUCCESSFUL`（新規 2 件を含め全件パス）

- [ ] **Step 7: コミット**

```bash
git add core/domain core/data
git commit -m "feat(settings): ウィジェットの更新間隔設定を追加

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 3: 条文の描画

Glance state から条文の identity を読み、Room から解決して TitleBar 付きのレイアウトで表示する。本体タップでアプリに遷移する。state を書き込む経路はまだ無いため、この時点ではフォールバック表示の確認までを行う。

**Files:**
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetStateKeys.kt`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetEntryPoint.kt`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetContent.kt`
- Create: `feature/widget/src/main/res/drawable/ic_refresh.xml`
- Modify: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidget.kt`

**Interfaces:**
- Consumes: `ArticleWidget`（Task 1）、`ApplicationSettings.widgetUpdateIntervalMinutes`（Task 2 — このタスクでは未使用）
- Produces:
  - `object ArticleWidgetStateKeys` — `LAW_ID: Preferences.Key<String>`, `ARTICLE_NUMBER: Preferences.Key<String>`, `SUPPLEMENTARY_PROVISION_LABEL: Preferences.Key<String>`
  - `interface ArticleWidgetEntryPoint` — `lawRepository()`, `lawCatalogRepository()`, `applicationSettingsRepository()`
  - `@Composable fun ArticleWidgetContent(article: Article?, lawDisplayName: String, useHalfWidthParentheses: Boolean, launchAction: Action)`

- [ ] **Step 1: state のキーを定義**

`ArticleWidgetStateKeys.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * ウィジェットが表示中の条文を特定するためのキー。
 * 本文は保存せず、描画のたびに DB から解決する。
 */
object ArticleWidgetStateKeys {
    val LAW_ID = stringPreferencesKey("widget_law_id")
    val ARTICLE_NUMBER = stringPreferencesKey("widget_article_number")
    val SUPPLEMENTARY_PROVISION_LABEL = stringPreferencesKey("widget_supplementary_provision_label")
}
```

- [ ] **Step 2: Hilt EntryPoint を定義**

`ArticleWidgetEntryPoint.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawCatalogRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidget は @AndroidEntryPoint を付けられないため、
 * 描画時のリポジトリ取得は EntryPoint 経由で行う。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ArticleWidgetEntryPoint {
    fun lawRepository(): LawRepository

    fun lawCatalogRepository(): LawCatalogRepository

    fun applicationSettingsRepository(): ApplicationSettingsRepository
}
```

- [ ] **Step 3: リロードアイコンを追加**

`feature/widget/src/main/res/drawable/ic_refresh.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -8,8s3.58,8 8,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z" />
</vector>
```

塗りは白固定にしておき、`CircleIconButton` の `contentColor` でテーマ色に着色する。

- [ ] **Step 4: Glance の Composable を作成**

`ArticleWidgetContent.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.data.R as CoreDataR

/**
 * ウィジェットの本体。
 * article が null のときはフォールバック表示にする。
 */
@Composable
fun ArticleWidgetContent(
    article: Article?,
    lawDisplayName: String,
    useHalfWidthParentheses: Boolean,
    launchAction: Action,
) {
    val context = LocalContext.current

    Scaffold(
        titleBar = {
            TitleBar(
                startIcon = ImageProvider(CoreDataR.drawable.ic_notification),
                title = lawDisplayName,
                actions = {
                    CircleIconButton(
                        imageProvider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = context.getString(R.string.widget_refresh_content_description),
                        backgroundColor = null,
                        contentColor = GlanceTheme.colors.onSurface,
                        onClick = actionRunCallback<RefreshArticleAction>(),
                    )
                },
            )
        },
    ) {
        if (article == null) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.widget_empty_message),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
            }
            return@Scaffold
        }

        Column(modifier = GlanceModifier.fillMaxSize().clickable(launchAction)) {
            Text(
                text = article.displayTitle(useHalfWidthParentheses),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = article.fullText(useHalfWidthParentheses),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 15.sp,
                ),
                modifier = GlanceModifier.padding(top = 6.dp),
            )
        }
    }
}
```

`RefreshArticleAction` は Task 5 で作る。このタスクでは Step 5 で仮実装を置く。

- [ ] **Step 5: `RefreshArticleAction` の仮実装を置く**

`feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/RefreshArticleAction.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import timber.log.Timber

class RefreshArticleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("Widget refresh requested")
    }
}
```

- [ ] **Step 6: `ArticleWidget` を書き換えて条文を解決する**

`ArticleWidget.kt` を全面的に置き換える:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

class ArticleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        )

        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val lawId = preferences[ArticleWidgetStateKeys.LAW_ID]
        val articleNumber = preferences[ArticleWidgetStateKeys.ARTICLE_NUMBER]
        val supplementaryProvisionLabel = preferences[ArticleWidgetStateKeys.SUPPLEMENTARY_PROVISION_LABEL]
            ?.takeIf { it.isNotEmpty() }

        val article = if (lawId != null && articleNumber != null) {
            runCatching {
                entryPoint.lawRepository().getArticle(
                    lawId = LawId(lawId),
                    articleNumber = articleNumber,
                    supplementaryProvisionLabel = supplementaryProvisionLabel,
                )
            }.onFailure { e ->
                Timber.e(e, "Failed to resolve article for widget")
            }.getOrNull()
        } else {
            null
        }

        val lawDisplayName = article?.let {
            runCatching { entryPoint.lawCatalogRepository().getLaw(it.lawId)?.displayName }
                .getOrNull()
                ?: it.lawId.value
        } ?: context.getString(R.string.widget_label)

        val useHalfWidthParentheses = runCatching {
            entryPoint.applicationSettingsRepository().get().useHalfWidthParentheses
        }.getOrDefault(false)

        provideContent {
            GlanceTheme {
                ArticleWidgetContent(
                    article = article,
                    lawDisplayName = lawDisplayName,
                    useHalfWidthParentheses = useHalfWidthParentheses,
                    launchAction = createLaunchAction(context, article),
                )
            }
        }
    }

    /**
     * 通知と同じ extra を付けて MainActivity を起動する。
     * app モジュールを参照できないため、起動 Intent は PackageManager から取得する。
     */
    private fun createLaunchAction(context: Context, article: Article?): Action {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        if (article != null) {
            intent.putExtra(ArticleNotificationSender.EXTRA_LAW_CODE, article.lawId.value)
            intent.putExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER, article.articleNumber)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        return actionStartActivity(intent)
    }
}
```

- [ ] **Step 7: ビルドと lint を確認**

```bash
./gradlew :feature:widget:assembleLocalDebug :app:lintStagingDebug
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 8: 実機でフォールバック表示を確認**

```bash
./gradlew :app:installLocalDebug
```

配置済みウィジェットに TitleBar（アプリアイコン・「ときどき六法」・リロードアイコン）と「条文を読み込めませんでした」が表示されることをスクリーンショットで確認する。

```bash
adb shell screencap -p /sdcard/widget-task3.png && adb pull /sdcard/widget-task3.png
```

- [ ] **Step 9: コミット**

```bash
git add feature/widget
git commit -m "feat(widget): 条文の描画とアプリ遷移を実装

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 4: 抽選 Worker とスケジューラ

条文をランダムに抽選して Glance state に書き込む Worker と、その定期実行を管理するスケジューラを作る。

**Files:**
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetUpdater.kt`
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetWorker.kt`
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetScheduler.kt`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetUpdaterImpl.kt`
- Create: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/di/WidgetModule.kt`
- Modify: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetReceiver.kt`
- Modify: `app/src/main/java/blue/starry/tokidokiroppou/TokidokiRoppouApplication.kt`

**Interfaces:**
- Consumes: `ArticleWidgetStateKeys`（Task 3）、`ApplicationSettings.widgetUpdateIntervalMinutes`（Task 2）
- Produces:
  - `interface ArticleWidgetUpdater` — `suspend fun updateAll(article: Article)`, `suspend fun hasPlacedWidget(): Boolean`
  - `class ArticleWidgetWorker : CoroutineWorker` — `companion object { const val WORK_NAME = "article_widget_update" }`
  - `class ArticleWidgetScheduler` — `fun schedule(intervalMinutes: Int)`, `fun cancel()`, `fun requestImmediateUpdate()`

- [ ] **Step 1: 更新インターフェースを定義**

`core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetUpdater.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.worker

import blue.starry.tokidokiroppou.core.domain.model.Article

/**
 * ウィジェットの表示内容を更新する。
 * Glance の実装は feature:widget にあるため、core:data からはこのインターフェース経由で呼ぶ。
 */
interface ArticleWidgetUpdater {
    /** 全てのウィジェットに条文を反映する */
    suspend fun updateAll(article: Article)

    /** ウィジェットが 1 つ以上配置されているか */
    suspend fun hasPlacedWidget(): Boolean
}
```

- [ ] **Step 2: Worker を作成**

`core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetWorker.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ArticleWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
    private val widgetUpdater: ArticleWidgetUpdater,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("ArticleWidgetWorker started")

        if (!widgetUpdater.hasPlacedWidget()) {
            Timber.d("No widget placed, skipping")
            return Result.success()
        }

        val settings = settingsRepository.get()
        val article = lawRepository.getRandomArticle(
            settings.enabledLawIds,
            settings.excludeSupplementaryProvisions,
        )
        if (article == null) {
            Timber.w("No article found for widget")
            return Result.retry()
        }

        widgetUpdater.updateAll(article)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "article_widget_update"
    }
}
```

条文が取得できないときは state を更新せずに `retry()` を返すため、ウィジェットは前回の表示を維持する。

- [ ] **Step 3: スケジューラを作成**

`core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleWidgetScheduler.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleWidgetScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun schedule(intervalMinutes: Int) {
        val effectiveInterval = intervalMinutes.toLong().coerceAtLeast(15)

        val workRequest = PeriodicWorkRequestBuilder<ArticleWidgetWorker>(
            effectiveInterval,
            TimeUnit.MINUTES,
        ).build()

        workManager.enqueueUniquePeriodicWork(
            ArticleWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest,
        )

        Timber.d("Scheduled widget update every %d minutes", effectiveInterval)
    }

    fun cancel() {
        workManager.cancelUniqueWork(ArticleWidgetWorker.WORK_NAME)
        Timber.d("Cancelled widget update worker")
    }

    /** 即時に 1 回だけ更新する（ウィジェット配置直後やリロードボタン用） */
    fun requestImmediateUpdate() {
        val workRequest = OneTimeWorkRequestBuilder<ArticleWidgetWorker>().build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        Timber.d("Requested immediate widget update")
    }

    private companion object {
        const val IMMEDIATE_WORK_NAME = "article_widget_update_immediate"
    }
}
```

- [ ] **Step 4: `core:data` に glance-appwidget は不要なことを確認**

`core:data` は `ArticleWidgetUpdater` インターフェース越しにしかウィジェットを触らないため、依存の追加は不要。`core/data/build.gradle.kts` は変更しない。

- [ ] **Step 5: `ArticleWidgetUpdater` の実装を作成**

`feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/ArticleWidgetUpdaterImpl.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetUpdater
import blue.starry.tokidokiroppou.core.domain.model.Article
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleWidgetUpdaterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ArticleWidgetUpdater {

    override suspend fun updateAll(article: Article) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(ArticleWidget::class.java)

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { preferences ->
                preferences[ArticleWidgetStateKeys.LAW_ID] = article.lawId.value
                preferences[ArticleWidgetStateKeys.ARTICLE_NUMBER] = article.articleNumber
                preferences[ArticleWidgetStateKeys.SUPPLEMENTARY_PROVISION_LABEL] =
                    article.supplementaryProvisionLabel.orEmpty()
            }
        }

        ArticleWidget().updateAll(context)
        Timber.d("Updated %d widget(s)", glanceIds.size)
    }

    override suspend fun hasPlacedWidget(): Boolean {
        return GlanceAppWidgetManager(context).getGlanceIds(ArticleWidget::class.java).isNotEmpty()
    }
}
```

- [ ] **Step 6: Hilt でバインド**

`feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/di/WidgetModule.kt`:

```kotlin
package blue.starry.tokidokiroppou.feature.widget.di

import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetUpdater
import blue.starry.tokidokiroppou.feature.widget.ArticleWidgetUpdaterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    @Singleton
    abstract fun bindArticleWidgetUpdater(impl: ArticleWidgetUpdaterImpl): ArticleWidgetUpdater
}
```

- [ ] **Step 7: Receiver でライフサイクルを制御**

`ArticleWidgetReceiver.kt` を全面的に置き換える:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

class ArticleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ArticleWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val scheduler = schedulerOf(context)
        val intervalMinutes = runBlocking {
            settingsRepositoryOf(context).get().widgetUpdateIntervalMinutes
        }
        scheduler.schedule(intervalMinutes)
        scheduler.requestImmediateUpdate()
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        schedulerOf(context).cancel()
    }

    private fun schedulerOf(context: Context): ArticleWidgetScheduler =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetSchedulerEntryPoint::class.java,
        ).articleWidgetScheduler()

    private fun settingsRepositoryOf(context: Context) =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        ).applicationSettingsRepository()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ArticleWidgetSchedulerEntryPoint {
        fun articleWidgetScheduler(): ArticleWidgetScheduler
    }
}
```

`onEnabled` は BroadcastReceiver のメインスレッド上で走るため、`runBlocking` は DataStore の 1 回読みに留める。

- [ ] **Step 8: アプリ起動時に再スケジュール**

`TokidokiRoppouApplication.kt` の import に追加:

```kotlin
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
```

`cacheRefreshScheduler` の宣言の次に追加:

```kotlin
    @Inject
    lateinit var widgetScheduler: ArticleWidgetScheduler
```

`onCreate()` の末尾（通知のスケジュール処理の直後）に追加:

```kotlin
        // ウィジェットが配置されている場合のみ定期更新をスケジュールする
        // (未配置なら Worker 側が即座に success を返す)
        widgetScheduler.schedule(settings.widgetUpdateIntervalMinutes)
```

- [ ] **Step 9: ビルドとテストを確認**

```bash
./gradlew :app:assembleLocalDebug :core:data:testStagingDebugUnitTest
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 10: 実機で条文が表示されることを確認**

```bash
./gradlew :app:installLocalDebug
```

ウィジェットを一度削除して再配置し、`onEnabled` → 即時更新の経路で条文が表示されることを確認する。

```bash
adb logcat -d -s TokidokiRoppou:* | grep -i widget | tail -20
adb shell screencap -p /sdcard/widget-task4.png && adb pull /sdcard/widget-task4.png
```

期待: TitleBar に法令名、本体に条文名と本文が表示される。

- [ ] **Step 11: コミット**

```bash
git add core/data feature/widget app/src/main/java/blue/starry/tokidokiroppou/TokidokiRoppouApplication.kt
git commit -m "feat(widget): 条文の定期抽選と更新を実装

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 5: リロードボタン

Task 3 で仮実装した `RefreshArticleAction` を、実際に Worker を起動する実装に置き換える。

**Files:**
- Modify: `feature/widget/src/main/java/blue/starry/tokidokiroppou/feature/widget/RefreshArticleAction.kt`

**Interfaces:**
- Consumes: `ArticleWidgetScheduler.requestImmediateUpdate()`（Task 4）
- Produces: なし

- [ ] **Step 1: `RefreshArticleAction` を実装に置き換える**

`RefreshArticleAction.kt` を全面的に置き換える:

```kotlin
package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber

/**
 * リロードボタン。
 * ActionCallback は Hilt 非対応のため、抽選ロジックは持たず Worker の起動だけを行う。
 */
class RefreshArticleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Timber.d("Widget refresh requested")

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RefreshArticleActionEntryPoint::class.java,
        ).articleWidgetScheduler().requestImmediateUpdate()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RefreshArticleActionEntryPoint {
        fun articleWidgetScheduler(): ArticleWidgetScheduler
    }
}
```

- [ ] **Step 2: ビルドを確認**

```bash
./gradlew :app:assembleLocalDebug
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 3: 実機でリロードとタップ遷移を確認**

```bash
./gradlew :app:installLocalDebug
```

録画しながら次の操作を行う。

```bash
adb shell screenrecord /sdcard/widget-task5.mp4
```

1. リロードアイコンをタップ → 別の条文に変わる
2. 本体をタップ → アプリが起動し、ホーム画面に同じ条文が表示される

```bash
adb pull /sdcard/widget-task5.mp4
```

- [ ] **Step 4: コミット**

```bash
git add feature/widget
git commit -m "feat(widget): リロードボタンを実装

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 6: 設定画面にウィジェットセクションを追加

**Files:**
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreenViewModel.kt`
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreen.kt`

**Interfaces:**
- Consumes: `ApplicationSettingsRepository.setWidgetUpdateIntervalMinutes`（Task 2）、`ArticleWidgetScheduler.schedule`（Task 4）
- Produces: `SettingsScreenViewModel.setWidgetUpdateInterval(minutes: Int)`

- [ ] **Step 1: ViewModel にメソッドを追加**

`SettingsScreenViewModel.kt` の import に追加:

```kotlin
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
```

コンストラクタの `private val scheduler: ArticleNotificationScheduler,` の次の行に追加:

```kotlin
    private val widgetScheduler: ArticleWidgetScheduler,
```

`setExcludeSupplementaryProvisions` の実装の直後に追加:

```kotlin
    fun setWidgetUpdateInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setWidgetUpdateIntervalMinutes(minutes)
            widgetScheduler.schedule(minutes)
        }
    }
```

- [ ] **Step 2: `SettingsScreen` にコールバックを配線**

`SettingsScreen` の `SettingsContent(...)` 呼び出しで、`onExcludeSupplementaryProvisionsChanged = viewModel::setExcludeSupplementaryProvisions,` の次の行に追加:

```kotlin
            onWidgetUpdateIntervalChanged = viewModel::setWidgetUpdateInterval,
```

`SettingsContent` のシグネチャで、`onExcludeSupplementaryProvisionsChanged: (Boolean) -> Unit,` の次の行に追加:

```kotlin
    onWidgetUpdateIntervalChanged: (Int) -> Unit,
```

- [ ] **Step 3: セクションを追加**

`SettingsScreen.kt` の import に追加:

```kotlin
import androidx.compose.material.icons.filled.Widgets
```

「表示」セクションの `item { ... }` ブロックの直後に、新しい `item` を追加:

```kotlin
        item {
            SettingSection(title = "ウィジェット") {
                var showWidgetIntervalDialog by remember { mutableStateOf(false) }
                SettingItem(
                    headline = "更新間隔",
                    supporting = ApplicationSettings.intervalDisplayText(settings.widgetUpdateIntervalMinutes),
                    leadingIcon = Icons.Default.Widgets,
                    onClick = { showWidgetIntervalDialog = true },
                )
                if (showWidgetIntervalDialog) {
                    AlertDialog(
                        onDismissRequest = { showWidgetIntervalDialog = false },
                        title = { Text("ウィジェットの更新間隔") },
                        text = {
                            Column {
                                ApplicationSettings.INTERVAL_OPTIONS.forEach { minutes ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onWidgetUpdateIntervalChanged(minutes)
                                                showWidgetIntervalDialog = false
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RadioButton(
                                            selected = minutes == settings.widgetUpdateIntervalMinutes,
                                            onClick = {
                                                onWidgetUpdateIntervalChanged(minutes)
                                                showWidgetIntervalDialog = false
                                            },
                                        )
                                        Text(
                                            text = ApplicationSettings.intervalDisplayText(minutes),
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                    )
                }
            }
        }
```

- [ ] **Step 4: ビルドを確認**

```bash
./gradlew :app:assembleLocalDebug
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 5: 実機で設定画面を確認**

設定画面に「ウィジェット」セクションが現れ、更新間隔を変更できることをスクリーンショットで確認する。

```bash
adb shell screencap -p /sdcard/widget-task6.png && adb pull /sdcard/widget-task6.png
```

- [ ] **Step 6: コミット**

```bash
git add feature/settings
git commit -m "feat(settings): ウィジェットの更新間隔を設定できるようにした

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>"
```

---

## Task 7: 総合検証と PR

**Files:**
- 変更なし（検証と PR 作成のみ）

**Interfaces:**
- Consumes: Task 1〜6 の全成果物
- Produces: なし

- [ ] **Step 1: 全テストを実行**

```bash
./gradlew testStagingDebugUnitTest
```

期待: `BUILD SUCCESSFUL`

- [ ] **Step 2: lint を実行**

```bash
./gradlew lintStagingDebug
```

期待: `BUILD SUCCESSFUL`。指摘が出た場合は抑制せず、対応方針をユーザーに確認する。

- [ ] **Step 3: 通しの動作を録画**

```bash
adb shell screenrecord /sdcard/widget-final.mp4
```

1. ウィジェットを配置 → 条文が表示される
2. リロード → 別の条文に変わる
3. 本体タップ → アプリのホーム画面に同じ条文が表示される
4. 設定画面 → ウィジェットの更新間隔を変更できる
5. ウィジェットを削除 → Worker が停止する（`adb shell dumpsys jobscheduler` で確認）

```bash
adb pull /sdcard/widget-final.mp4
```

- [ ] **Step 4: PR を作成**

`gh image upload` で録画とスクリーンショットをアップロードし、本文に埋め込む。本文には `Close #53` を含める。

```bash
gh pr create --title "feat(widget): Glance でランダム条文ウィジェットを追加" --assignee SlashNephy
```

- [ ] **Step 5: マージ可否を確認**

```bash
gh pr view --json mergeable,mergeStateStatus
```

コンフリクトしている場合は解消する。

---

## Self-Review

**1. Spec coverage**

| spec の要求 | 対応タスク |
| --- | --- |
| `feature:widget` モジュール新設 | Task 1 |
| 通知と独立したランダム抽選 | Task 4（`ArticleWidgetWorker`） |
| ウィジェット専用の更新間隔設定 | Task 2 / Task 6 |
| 対象法令は `enabledLawIds` を共有 | Task 4（Worker が `settings.enabledLawIds` を渡す） |
| Hilt を Worker と EntryPoint に寄せる | Task 3 / Task 4 / Task 5 |
| state に identity のみ保存 | Task 3（キー定義）/ Task 4（書き込み） |
| Scaffold + TitleBar（法令名・リロード） | Task 3 |
| 条文名と本文の表示 | Task 3 |
| 本体タップでディープリンク | Task 3 |
| 抽選失敗時は前回表示を維持し `retry()` | Task 4 Step 2 |
| 初回で条文が無い場合のフォールバック | Task 3 Step 4 |
| ウィジェット未配置時は Worker をスケジュールしない | Task 4 Step 7（`onDisabled` で cancel）+ Worker 側の早期 return |
| `libs.versions.toml` で glance を管理 | Task 1 Step 1 |
| テスト・lint・実機検証 | Task 2 / Task 7 |

**2. Placeholder scan**

`RefreshArticleAction` は Task 3 で仮実装、Task 5 で本実装に置き換える。これは意図的な段階であり、両方のタスクに完全なコードを記載済み。他に TBD や「適切に処理する」といった記述は無い。

**3. Type consistency**

- `ArticleWidgetStateKeys` のキー名は Task 3（定義）と Task 4 Step 5（書き込み）、Task 3 Step 6（読み出し）で一致
- `ArticleWidgetUpdater` のメソッド名 `updateAll` / `hasPlacedWidget` は Task 4 Step 1・2・5 で一致
- `ArticleWidgetScheduler.requestImmediateUpdate()` は Task 4 Step 3（定義）と Task 4 Step 7・Task 5 Step 1（呼び出し）で一致
- `ArticleWidgetWorker.WORK_NAME` は Task 4 Step 2 で定義、Step 3 で参照
- `widgetUpdateIntervalMinutes` は Task 2・4・6 で一致
- `ArticleWidgetEntryPoint.applicationSettingsRepository()` は Task 3 Step 2 で定義、Task 3 Step 6 と Task 4 Step 7 で参照

**4. 実装時に確認が必要な点**

- `Alignment.Vertical.CenterVertically` / `Alignment.Horizontal.CenterHorizontally` の指定方法は Glance の Column のシグネチャに合わせる（`Alignment.CenterVertically` で足りる場合がある）
- `GlanceTheme.colors.onSurfaceVariant` が glance-material3 の `ColorProviders` に存在することをビルドで確認する
- `ic_notification` の R クラス参照（`blue.starry.tokidokiroppou.core.data.R`）が `implementation` 依存で解決できること
