# 夜間の通知抑止 (Quiet Hours) 実装プラン

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ユーザーが指定した時間帯 (既定 23:00〜7:00) は条文通知を送らないようにし、就寝中に通知が蓄積するのを防ぐ。

**Architecture:** 時刻判定は `core:domain` の純粋な値オブジェクト `QuietHours` に閉じ込め、JVM ユニットテストで検証する。`core:data` は DataStore への永続化と、`ArticleNotificationWorker` 冒頭での早期リターンのみを担う。`feature:settings` は Material3 `TimePicker` で開始・終了時刻を編集する。ウィジェットとスケジューラは変更しない。

**Tech Stack:** Kotlin / Jetpack Compose (Material 3) / Hilt / WorkManager / DataStore Preferences / kotlin-test

**設計書:** `docs/superpowers/specs/2026-08-31-quiet-hours-design.md`
**Issue:** https://github.com/SlashNephy/tokidoki-roppou/issues/109

## Global Constraints

- パッケージルート: `blue.starry.tokidokiroppou`
- 時刻は「その日の 0:00 からの経過分」(`minutesOfDay`, 0..1439) の `Int` で表現する。`LocalTime` や文字列では保存しない
- 抑止判定は半開区間。開始時刻ちょうどは抑止し、終了時刻ちょうどは抑止しない
- 日跨ぎ (`start > end`) を必ず扱う
- `start == end` は空区間とし、抑止しない
- デフォルト値は「有効・23:00〜7:00」。既存ユーザーにもキー未設定のまま適用される
- ウィジェット (`ArticleWidgetWorker`) と `ArticleNotificationScheduler` は変更しない
- 抑止時の Worker の戻り値は `Result.success()`。`failure()` / `retry()` は使わない
- コード内コメントとコミットメッセージは日本語、ログとエラーメッセージは英語
- コミットメッセージは Conventional Commits 形式で、末尾に `Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>` を付ける
- ブランチは `feat/quiet-hours` (設計書のコミット済み)

## File Structure

| ファイル | 責務 |
| --- | --- |
| `core/domain/src/main/java/.../core/domain/model/QuietHours.kt` (新規) | 抑止時間帯の値オブジェクト。区間判定と表示フォーマット |
| `core/domain/src/test/java/.../core/domain/model/QuietHoursTest.kt` (新規) | 区間判定・境界・日跨ぎ・空区間のテスト |
| `core/domain/src/main/java/.../core/domain/model/ApplicationSettings.kt` (変更) | 設定値の保持と、有効/無効を含めた抑止判定 |
| `core/domain/src/test/java/.../core/domain/model/ApplicationSettingsQuietHoursTest.kt` (新規) | 有効/無効と区間判定の組み合わせのテスト |
| `core/domain/src/main/java/.../core/domain/repository/ApplicationSettingsRepository.kt` (変更) | 書き込み API の追加 |
| `core/data/src/main/java/.../core/data/repository/ApplicationSettingsRepositoryImpl.kt` (変更) | DataStore への永続化と範囲外値のフォールバック |
| `core/data/src/test/java/.../core/data/repository/ApplicationSettingsRepositoryImplTest.kt` (変更) | 永続化・デフォルト値・フォールバックのテスト |
| `core/data/src/main/java/.../core/data/di/DataModule.kt` (変更) | `java.time.Clock` の提供 |
| `core/data/src/main/java/.../core/data/worker/ArticleNotificationWorker.kt` (変更) | 抑止時間帯での早期リターン |
| `feature/settings/src/main/java/.../feature/settings/ui/SettingsScreenViewModel.kt` (変更) | 設定値の更新 |
| `feature/settings/src/main/java/.../feature/settings/ui/SettingsScreen.kt` (変更) | トグルと TimePicker ダイアログ |

---

### Task 1: QuietHours 値オブジェクト

**Files:**
- Create: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/QuietHours.kt`
- Test: `core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/QuietHoursTest.kt`

**Interfaces:**
- Consumes: なし
- Produces:
  - `data class QuietHours(val startMinutesOfDay: Int, val endMinutesOfDay: Int)`
  - `fun QuietHours.contains(minutesOfDay: Int): Boolean`
  - `QuietHours.Companion.DEFAULT: QuietHours` (23:00〜7:00)
  - `QuietHours.Companion.MINUTES_PER_DAY: Int` (1440)
  - `QuietHours.Companion.isValidMinutesOfDay(minutesOfDay: Int): Boolean`
  - `QuietHours.Companion.formatMinutesOfDay(minutesOfDay: Int): String` (`"23:00"` 形式)

- [ ] **Step 1: 失敗するテストを書く**

`core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/QuietHoursTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuietHoursTest {
    @Test
    fun containsReturnsTrueInsideSameDayRange() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertTrue(quietHours.contains(14 * 60))
    }

    @Test
    fun containsReturnsFalseOutsideSameDayRange() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertFalse(quietHours.contains(12 * 60 + 59))
        assertFalse(quietHours.contains(16 * 60))
    }

    @Test
    fun containsIncludesStartAndExcludesEnd() {
        val quietHours = QuietHours(startMinutesOfDay = 13 * 60, endMinutesOfDay = 15 * 60)

        assertTrue(quietHours.contains(13 * 60))
        assertFalse(quietHours.contains(15 * 60))
    }

    @Test
    fun containsHandlesRangeCrossingMidnight() {
        val quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60)

        assertTrue(quietHours.contains(23 * 60))
        assertTrue(quietHours.contains(23 * 60 + 30))
        assertTrue(quietHours.contains(0))
        assertTrue(quietHours.contains(6 * 60 + 59))
        assertFalse(quietHours.contains(7 * 60))
        assertFalse(quietHours.contains(12 * 60))
        assertFalse(quietHours.contains(22 * 60 + 59))
    }

    @Test
    fun containsReturnsFalseWhenStartEqualsEnd() {
        val quietHours = QuietHours(startMinutesOfDay = 9 * 60, endMinutesOfDay = 9 * 60)

        assertFalse(quietHours.contains(9 * 60))
        assertFalse(quietHours.contains(0))
        assertFalse(quietHours.contains(23 * 60 + 59))
    }

    @Test
    fun isValidMinutesOfDayAcceptsOnlyValuesWithinOneDay() {
        assertTrue(QuietHours.isValidMinutesOfDay(0))
        assertTrue(QuietHours.isValidMinutesOfDay(1439))
        assertFalse(QuietHours.isValidMinutesOfDay(-1))
        assertFalse(QuietHours.isValidMinutesOfDay(1440))
    }

    @Test
    fun formatMinutesOfDayPadsHourAndMinuteToTwoDigits() {
        assertEquals("00:00", QuietHours.formatMinutesOfDay(0))
        assertEquals("07:05", QuietHours.formatMinutesOfDay(7 * 60 + 5))
        assertEquals("23:00", QuietHours.formatMinutesOfDay(23 * 60))
        assertEquals("23:59", QuietHours.formatMinutesOfDay(1439))
    }
}
```

- [ ] **Step 2: テストが失敗することを確認する**

```bash
./gradlew :core:domain:testDebugUnitTest --tests "*QuietHoursTest*"
```

Expected: コンパイルエラー (`Unresolved reference: QuietHours`)

- [ ] **Step 3: 最小の実装を書く**

`core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/QuietHours.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

/**
 * 通知を抑止する時間帯。
 *
 * 時刻はその日の 0:00 からの経過分 (0..1439) で表す。
 */
data class QuietHours(
    val startMinutesOfDay: Int,
    val endMinutesOfDay: Int,
) {
    /**
     * [minutesOfDay] が抑止対象かを返す。
     *
     * 開始時刻ちょうどは抑止し、終了時刻ちょうどは抑止しない (半開区間)。
     * 開始と終了が同値のときは空区間とみなし、常に false を返す。通知が丸一日止まる事故を避けるため。
     */
    fun contains(minutesOfDay: Int): Boolean {
        return when {
            startMinutesOfDay == endMinutesOfDay -> false
            startMinutesOfDay < endMinutesOfDay -> minutesOfDay >= startMinutesOfDay && minutesOfDay < endMinutesOfDay
            // 日をまたぐ区間
            else -> minutesOfDay >= startMinutesOfDay || minutesOfDay < endMinutesOfDay
        }
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

        val DEFAULT = QuietHours(
            startMinutesOfDay = 23 * 60,
            endMinutesOfDay = 7 * 60,
        )

        fun isValidMinutesOfDay(minutesOfDay: Int): Boolean {
            return minutesOfDay in 0 until MINUTES_PER_DAY
        }

        fun formatMinutesOfDay(minutesOfDay: Int): String {
            val hour = minutesOfDay / 60
            val minute = minutesOfDay % 60
            return "%02d:%02d".format(hour, minute)
        }
    }
}
```

- [ ] **Step 4: テストが通ることを確認する**

```bash
./gradlew :core:domain:testDebugUnitTest --tests "*QuietHoursTest*"
```

Expected: PASS (7 tests)

- [ ] **Step 5: コミット**

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/QuietHours.kt core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/QuietHoursTest.kt
git commit -m "$(cat <<'EOF'
feat(domain): 通知を抑止する時間帯を表す QuietHours を追加

Refs #109

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: ApplicationSettings への統合

**Files:**
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt`
- Test: `core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettingsQuietHoursTest.kt`

**Interfaces:**
- Consumes: `QuietHours`, `QuietHours.DEFAULT`, `QuietHours.contains(Int)` (Task 1)
- Produces:
  - `ApplicationSettings.isQuietHoursEnabled: Boolean` (既定 `true`)
  - `ApplicationSettings.quietHours: QuietHours` (既定 `QuietHours.DEFAULT`)
  - `fun ApplicationSettings.shouldSuppressNotificationAt(minutesOfDay: Int): Boolean`

`ApplicationSettings` はコンストラクタ引数がすべて既定値を持つ `data class` なので、末尾に 2 つ追加しても既存の呼び出し (テストの `ApplicationSettings()` を含む) は壊れない。

- [ ] **Step 1: 失敗するテストを書く**

`core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettingsQuietHoursTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationSettingsQuietHoursTest {
    @Test
    fun shouldSuppressNotificationAtReturnsTrueInsideQuietHours() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = true,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertTrue(settings.shouldSuppressNotificationAt(3 * 60))
    }

    @Test
    fun shouldSuppressNotificationAtReturnsFalseOutsideQuietHours() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = true,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertFalse(settings.shouldSuppressNotificationAt(12 * 60))
    }

    @Test
    fun shouldSuppressNotificationAtReturnsFalseWhenQuietHoursAreDisabled() {
        val settings = ApplicationSettings(
            isQuietHoursEnabled = false,
            quietHours = QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
        )

        assertFalse(settings.shouldSuppressNotificationAt(3 * 60))
    }
}
```

- [ ] **Step 2: テストが失敗することを確認する**

```bash
./gradlew :core:domain:testDebugUnitTest --tests "*ApplicationSettingsQuietHoursTest*"
```

Expected: コンパイルエラー (`Cannot find a parameter with this name: isQuietHoursEnabled`)

- [ ] **Step 3: 最小の実装を書く**

`ApplicationSettings.kt` のコンストラクタ末尾 (`widgetUpdateIntervalMinutes` の下) に 2 行追加する:

```kotlin
    val widgetUpdateIntervalMinutes: Int = 60,
    val isQuietHoursEnabled: Boolean = true,
    val quietHours: QuietHours = QuietHours.DEFAULT,
) {
```

`enabledLawCodes` プロパティの下に判定を追加する:

```kotlin
    /** [minutesOfDay] (その日の 0:00 からの経過分) が通知を抑止すべき時刻かを返す */
    fun shouldSuppressNotificationAt(minutesOfDay: Int): Boolean {
        return isQuietHoursEnabled && quietHours.contains(minutesOfDay)
    }
```

- [ ] **Step 4: テストが通ることを確認する**

```bash
./gradlew :core:domain:testDebugUnitTest
```

Expected: PASS (Task 1 のテストと既存の `PresetLawTest` / `TextNormalizerTest` を含めて全件成功)

- [ ] **Step 5: コミット**

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettingsQuietHoursTest.kt
git commit -m "$(cat <<'EOF'
feat(domain): ApplicationSettings に夜間の通知抑止設定を追加

Refs #109

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: DataStore への永続化

**Files:**
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt`
- Test: `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `ApplicationSettings.isQuietHoursEnabled`, `ApplicationSettings.quietHours`, `QuietHours`, `QuietHours.DEFAULT`, `QuietHours.isValidMinutesOfDay(Int)` (Task 1, 2)
- Produces:
  - `suspend fun ApplicationSettingsRepository.setQuietHoursEnabled(enabled: Boolean)`
  - `suspend fun ApplicationSettingsRepository.setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int)`
  - DataStore キー: `quiet_hours_enabled` (Boolean), `quiet_hours_start_minutes` (Int), `quiet_hours_end_minutes` (Int)

開始と終了は片方だけ変えると一時的に不整合な状態になりうるため、1 つのメソッドでまとめて書き込む。

- [ ] **Step 1: 失敗するテストを書く**

`ApplicationSettingsRepositoryImplTest.kt` のキー定義 (`widgetUpdateIntervalKey` の下) に追加する:

```kotlin
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartMinutesKey = intPreferencesKey("quiet_hours_start_minutes")
    private val quietHoursEndMinutesKey = intPreferencesKey("quiet_hours_end_minutes")
```

`getFallsBackToOneHourWhenWidgetUpdateIntervalIsAbsent` の下にテストを追加する。ファイル先頭の import に
`blue.starry.tokidokiroppou.core.domain.model.QuietHours` と `kotlin.test.assertFalse` / `kotlin.test.assertTrue` を追加すること:

```kotlin
    @Test
    fun getFallsBackToEnabledQuietHoursFrom23To7WhenKeysAreAbsent() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            val settings = testEnvironment.repository.get()

            assertTrue(settings.isQuietHoursEnabled)
            assertEquals(QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60), settings.quietHours)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setQuietHoursEnabledPersistsValueAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setQuietHoursEnabled(false)

            assertEquals(false, testEnvironment.dataStore.data.first()[quietHoursEnabledKey])
            assertFalse(testEnvironment.repository.get().isQuietHoursEnabled)
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun setQuietHoursPersistsBothEndpointsAndIsReadBack() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.repository.setQuietHours(startMinutesOfDay = 22 * 60 + 30, endMinutesOfDay = 6 * 60)

            val preferences = testEnvironment.dataStore.data.first()
            assertEquals(22 * 60 + 30, preferences[quietHoursStartMinutesKey])
            assertEquals(6 * 60, preferences[quietHoursEndMinutesKey])
            assertEquals(
                QuietHours(startMinutesOfDay = 22 * 60 + 30, endMinutesOfDay = 6 * 60),
                testEnvironment.repository.get().quietHours,
            )
        } finally {
            testEnvironment.close()
        }
    }

    @Test
    fun getFallsBackToDefaultQuietHoursWhenStoredMinutesAreOutOfRange() = runTest {
        val testEnvironment = createTestEnvironment()
        try {
            testEnvironment.dataStore.edit { preferences ->
                preferences[quietHoursStartMinutesKey] = -1
                preferences[quietHoursEndMinutesKey] = 1440
            }

            assertEquals(
                QuietHours(startMinutesOfDay = 23 * 60, endMinutesOfDay = 7 * 60),
                testEnvironment.repository.get().quietHours,
            )
        } finally {
            testEnvironment.close()
        }
    }
```

- [ ] **Step 2: テストが失敗することを確認する**

```bash
./gradlew :core:data:testDebugUnitTest --tests "*ApplicationSettingsRepositoryImplTest*"
```

Expected: コンパイルエラー (`Unresolved reference: setQuietHoursEnabled`)

- [ ] **Step 3: 最小の実装を書く**

`ApplicationSettingsRepository.kt` の `setWidgetUpdateIntervalMinutes` の下に追加する:

```kotlin
    suspend fun setQuietHoursEnabled(enabled: Boolean)

    suspend fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int)
```

`ApplicationSettingsRepositoryImpl.kt` の `import` に `blue.starry.tokidokiroppou.core.domain.model.QuietHours` を追加し、
`setWidgetUpdateIntervalMinutes` の下にメソッドを追加する:

```kotlin
    override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_ENABLED] = enabled
        }
    }

    override suspend fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int) {
        // 片方だけ書き込むと一時的に不整合な区間になるため、まとめて書き込む
        dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_START_MINUTES] = startMinutesOfDay
            preferences[KEY_QUIET_HOURS_END_MINUTES] = endMinutesOfDay
        }
    }
```

`toApplicationSettings()` の `return ApplicationSettings(...)` に 2 行追加する:

```kotlin
            widgetUpdateIntervalMinutes = this[KEY_WIDGET_UPDATE_INTERVAL] ?: 60,
            isQuietHoursEnabled = this[KEY_QUIET_HOURS_ENABLED] ?: true,
            quietHours = readQuietHours(),
        )
```

`toApplicationSettings()` の直後にヘルパーを追加する:

```kotlin
    // 手動での書き換えや将来の仕様変更で範囲外の値が入っていた場合に備え、既定値へフォールバックする
    private fun Preferences.readQuietHours(): QuietHours {
        val start = this[KEY_QUIET_HOURS_START_MINUTES] ?: QuietHours.DEFAULT.startMinutesOfDay
        val end = this[KEY_QUIET_HOURS_END_MINUTES] ?: QuietHours.DEFAULT.endMinutesOfDay
        if (!QuietHours.isValidMinutesOfDay(start) || !QuietHours.isValidMinutesOfDay(end)) {
            Timber.w("Stored quiet hours are out of range (start=%d, end=%d), falling back to default", start, end)
            return QuietHours.DEFAULT
        }

        return QuietHours(startMinutesOfDay = start, endMinutesOfDay = end)
    }
```

`companion object` にキーを追加する:

```kotlin
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_HOURS_START_MINUTES = intPreferencesKey("quiet_hours_start_minutes")
        private val KEY_QUIET_HOURS_END_MINUTES = intPreferencesKey("quiet_hours_end_minutes")
```

- [ ] **Step 4: テストが通ることを確認する**

```bash
./gradlew :core:data:testDebugUnitTest --tests "*ApplicationSettingsRepositoryImplTest*"
```

Expected: PASS (既存 6 件 + 追加 4 件)

- [ ] **Step 5: コミット**

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImplTest.kt
git commit -m "$(cat <<'EOF'
feat(data): 夜間の通知抑止設定を DataStore に永続化する

Refs #109

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Worker での抑止

**Files:**
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleNotificationWorker.kt`

**Interfaces:**
- Consumes: `ApplicationSettings.shouldSuppressNotificationAt(Int)` (Task 2)
- Produces: Hilt で `java.time.Clock` (端末のデフォルトタイムゾーン) を注入できるようになる

判定ロジック自体は Task 1・2 のユニットテストで網羅済みのため、ここでは新規のユニットテストを追加しない。`ArticleNotificationWorker` は `Context` / `WorkerParameters` / `ListenableWorker.Result` に依存しており、素の JVM テストからは組み立てられない (この repo には Robolectric も `androidx.work:work-testing` も入っていない)。呼び出しの結線は Task 6 の実機検証で確認する。

- [ ] **Step 1: Clock を提供する**

`DataModule.kt` の import に `java.time.Clock` を追加し、`DataProvidesModule` の `provideWorkManager` の下に追加する:

```kotlin
    @Provides
    @Singleton
    fun provideClock(): Clock {
        return Clock.systemDefaultZone()
    }
```

- [ ] **Step 2: Worker に抑止判定を入れる**

`ArticleNotificationWorker.kt` の import に以下を追加する:

```kotlin
import java.time.Clock
import java.time.LocalTime
```

コンストラクタに `Clock` を追加する:

```kotlin
    private val notificationSender: ArticleNotificationSender,
    private val clock: Clock,
) : CoroutineWorker(appContext, workerParams) {
```

`isNotificationEnabled` チェックの直後 (条文の抽選より前) に判定を挿入する:

```kotlin
        if (!settings.isNotificationEnabled) {
            Timber.d("Notifications disabled, skipping")
            return Result.success()
        }

        // 抑止時間帯では条文を抽選せずに終了する。次の周期を待ち、埋め合わせの通知は行わない
        if (settings.shouldSuppressNotificationAt(currentMinutesOfDay())) {
            Timber.d("In quiet hours, skipping")
            return Result.success()
        }
```

`doWork()` の下にヘルパーを追加する:

```kotlin
    private fun currentMinutesOfDay(): Int {
        val now = LocalTime.now(clock)
        return now.hour * 60 + now.minute
    }
```

- [ ] **Step 3: ビルドが通ることを確認する**

```bash
./gradlew :core:data:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 全ユニットテストが通ることを確認する**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 5: コミット**

```bash
git add core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleNotificationWorker.kt
git commit -m "$(cat <<'EOF'
feat(data): 抑止時間帯では条文通知を送らないようにする

Refs #109

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: 設定 UI

**Files:**
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreenViewModel.kt`
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreen.kt`

**Interfaces:**
- Consumes: `ApplicationSettingsRepository.setQuietHoursEnabled(Boolean)`, `ApplicationSettingsRepository.setQuietHours(Int, Int)` (Task 3), `QuietHours.formatMinutesOfDay(Int)` (Task 1)
- Produces:
  - `SettingsScreenViewModel.setQuietHoursEnabled(enabled: Boolean)`
  - `SettingsScreenViewModel.setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int)`

この repo には Compose UI テストが存在しないため、UI は実機で検証する (Task 6)。

- [ ] **Step 1: ViewModel にメソッドを追加する**

`SettingsScreenViewModel.kt` の `setWidgetUpdateInterval` の下に追加する。抑止は Worker 実行時に判定されるためスケジュールの取り直しは不要:

```kotlin
    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setQuietHoursEnabled(enabled)
        }
    }

    fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int) {
        viewModelScope.launch {
            settingsRepository.setQuietHours(startMinutesOfDay, endMinutesOfDay)
        }
    }
```

- [ ] **Step 2: Screen に設定項目を追加する**

`SettingsScreen.kt` のファイル先頭の opt-in に `ExperimentalMaterial3Api` を追加する:

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
```

import に以下を追加する:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.filled.Bedtime
import blue.starry.tokidokiroppou.core.domain.model.QuietHours
```

`SettingsScreen` composable の `SettingsContent(...)` 呼び出しに引数を 2 つ追加する:

```kotlin
            onWidgetUpdateIntervalChanged = viewModel::setWidgetUpdateInterval,
            onQuietHoursEnabledChanged = viewModel::setQuietHoursEnabled,
            onQuietHoursChanged = viewModel::setQuietHours,
            onClearCacheAndRefresh = viewModel::clearCacheAndRefresh,
```

`SettingsContent` のシグネチャに対応する引数を追加する:

```kotlin
    onWidgetUpdateIntervalChanged: (Int) -> Unit,
    onQuietHoursEnabledChanged: (Boolean) -> Unit,
    onQuietHoursChanged: (Int, Int) -> Unit,
    onClearCacheAndRefresh: () -> Unit,
```

「通知」セクションの通知間隔ダイアログの下、「附則を除外する」の上に追加する:

```kotlin
                SettingItem(
                    headline = "夜間は通知しない",
                    supporting = if (settings.isQuietHoursEnabled) {
                        "${QuietHours.formatMinutesOfDay(settings.quietHours.startMinutesOfDay)} 〜 " +
                            QuietHours.formatMinutesOfDay(settings.quietHours.endMinutesOfDay)
                    } else {
                        "指定した時間帯は条文を通知しません"
                    },
                    leadingIcon = Icons.Default.Bedtime,
                    trailing = {
                        Switch(
                            checked = settings.isQuietHoursEnabled,
                            onCheckedChange = onQuietHoursEnabledChanged,
                        )
                    },
                    onClick = {
                        onQuietHoursEnabledChanged(!settings.isQuietHoursEnabled)
                    },
                )

                if (settings.isQuietHoursEnabled) {
                    var showQuietHoursStartDialog by remember { mutableStateOf(false) }
                    var showQuietHoursEndDialog by remember { mutableStateOf(false) }

                    SettingItem(
                        headline = "開始時刻",
                        supporting = QuietHours.formatMinutesOfDay(settings.quietHours.startMinutesOfDay),
                        leadingIcon = Icons.Default.Schedule,
                        onClick = { showQuietHoursStartDialog = true },
                    )
                    if (showQuietHoursStartDialog) {
                        TimePickerDialog(
                            title = "通知を停止する時刻",
                            initialMinutesOfDay = settings.quietHours.startMinutesOfDay,
                            // 開始と終了が同値だと空区間になり抑止されなくなるため、同値の保存を禁止する
                            forbiddenMinutesOfDay = settings.quietHours.endMinutesOfDay,
                            onConfirm = { minutesOfDay ->
                                onQuietHoursChanged(minutesOfDay, settings.quietHours.endMinutesOfDay)
                            },
                            onDismiss = { showQuietHoursStartDialog = false },
                        )
                    }

                    SettingItem(
                        headline = "終了時刻",
                        supporting = QuietHours.formatMinutesOfDay(settings.quietHours.endMinutesOfDay),
                        leadingIcon = Icons.Default.Schedule,
                        onClick = { showQuietHoursEndDialog = true },
                    )
                    if (showQuietHoursEndDialog) {
                        TimePickerDialog(
                            title = "通知を再開する時刻",
                            initialMinutesOfDay = settings.quietHours.endMinutesOfDay,
                            forbiddenMinutesOfDay = settings.quietHours.startMinutesOfDay,
                            onConfirm = { minutesOfDay ->
                                onQuietHoursChanged(settings.quietHours.startMinutesOfDay, minutesOfDay)
                            },
                            onDismiss = { showQuietHoursEndDialog = false },
                        )
                    }
                }
```

`IntervalPickerDialog` の下に新しい composable を追加する:

```kotlin
@Composable
private fun TimePickerDialog(
    title: String,
    initialMinutesOfDay: Int,
    forbiddenMinutesOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialMinutesOfDay / 60,
        initialMinute = initialMinutesOfDay % 60,
        is24Hour = true,
    )
    val selectedMinutesOfDay = timePickerState.hour * 60 + timePickerState.minute

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TimePicker(state = timePickerState)
                if (selectedMinutesOfDay == forbiddenMinutesOfDay) {
                    Text(
                        text = "開始時刻と終了時刻は同じにできません",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedMinutesOfDay)
                    onDismiss()
                },
                enabled = selectedMinutesOfDay != forbiddenMinutesOfDay,
            ) {
                Text("決定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        },
    )
}
```

- [ ] **Step 3: ビルドが通ることを確認する**

```bash
./gradlew assembleStagingDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Lint を通す**

```bash
./gradlew lintStagingDebug
```

Expected: BUILD SUCCESSFUL。指摘が出た場合は設定変更やコメントでの抑制をせず、ユーザーに対応方針を確認する

- [ ] **Step 5: コミット**

```bash
git add feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreenViewModel.kt feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreen.kt
git commit -m "$(cat <<'EOF'
feat(settings): 夜間の通知抑止を設定画面から変更できるようにする

Refs #109

Co-Authored-By: Claude Fable 6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: 検証と PR

**Files:** なし (検証のみ)

**Interfaces:**
- Consumes: Task 1〜5 のすべて
- Produces: PR

- [ ] **Step 1: 全体のユニットテストと Lint**

```bash
./gradlew test lintStagingDebug
```

Expected: BUILD SUCCESSFUL。出力をログとして控え、PR に添付する

- [ ] **Step 2: 実機に staging debug をインストールする**

`app/google-services.json` が Git 管理外のため、未配置ならビルド前に Firebase コンソールから取得して配置する。`local` フレーバーと `applicationId` が異なる別アプリなので、対象を取り違えないこと。

```bash
./gradlew installStagingDebug
```

Expected: INSTALL SUCCEEDED

- [ ] **Step 3: 設定画面のスクリーンショットを撮る (after)**

mobile-mcp があればそれを使い、なければ adb を直接使う (実機が接続されているため `-s` でシリアルを指定する)。設定 → 通知セクションを開き、以下を撮る。

1. 「夜間は通知しない」が ON、supporting が `23:00 〜 07:00` になっている状態
2. 「開始時刻」をタップして TimePicker ダイアログが開いた状態
3. 開始時刻を終了時刻と同値にしたときに「決定」が無効化され、エラー文言が出ている状態
4. トグルを OFF にして「開始時刻」「終了時刻」が消えた状態

before との比較のため、この変更を含まないビルド (`git stash` または `main` ビルド) の通知セクションのスクリーンショットも撮る。

- [ ] **Step 4: 抑止が効くことを実機で確認する**

1. 通知を有効にし、通知間隔を 15 分にする
2. 抑止時間帯を「現在時刻を含む」範囲に設定する (例: 現在が 14:20 なら 14:00〜15:00)
3. 通知 Worker を即時実行し、通知が出ないことを確認する

```bash
adb -s <SERIAL> shell logcat -c
adb -s <SERIAL> shell cmd jobscheduler run -f blue.starry.tokidokiroppou.staging <JOB_ID>
adb -s <SERIAL> shell logcat -d -s ArticleNotificationWorker:*
```

Expected: `In quiet hours, skipping` が出力され、通知は表示されない

アプリは debug ビルドで `Timber.DebugTree()` を plant しており、タグは呼び出し元のクラス名 (`ArticleNotificationWorker`) になる。タグで絞って何も出ない場合は `adb -s <SERIAL> shell logcat -d | grep -i "quiet hours"` で確認する

`<JOB_ID>` は次のコマンドで `article_notification` の周期ワークに対応するものを特定する:

```bash
adb -s <SERIAL> shell dumpsys jobscheduler | grep -A 5 "blue.starry.tokidokiroppou.staging"
```

4. 抑止時間帯を現在時刻を含まない範囲に戻し、同じ手順で通知が表示されることを確認する (抑止が過剰でないことの確認)

Expected: 条文の通知が表示される

- [ ] **Step 5: PR を作成する**

```bash
git push -u origin feat/quiet-hours
```

PR 本文には以下を含める。

- 目的 (Issue #109 の解決) と `Close #109`
- 判定規則の表 (半開区間・日跨ぎ・空区間)
- DND 連動を採らなかった理由 (通知ポリシーアクセスが必要で、かつ DND では通知の蓄積自体は防げない)
- `./gradlew test lintStagingDebug` の出力
- before / after のスクリーンショット。添付には `github-image-upload` スキル (`gh image upload`) を使う
- Step 4 の logcat 出力

PR 作成後にユーザーを Assign し、マージ可否を確認してコンフリクトがあれば解消する。未検証の項目が残っている場合は Draft PR にする。

## Self-Review

**Spec coverage:**

| 設計書の項目 | 対応タスク |
| --- | --- |
| `QuietHours` と判定規則 (半開区間・日跨ぎ・空区間) | Task 1 |
| `ApplicationSettings` へのフィールド追加とデフォルト値 | Task 2 |
| DataStore キー 3 つとリポジトリ API | Task 3 |
| 範囲外値のフォールバック | Task 3 (`readQuietHours`) |
| Worker での早期リターンと `Result.success()` | Task 4 |
| `java.time.Clock` の注入 | Task 4 |
| 設定 UI (トグル + TimePicker、同値の保存を弾く) | Task 5 |
| スコープ外 (ウィジェット・通知 ID・埋め合わせ通知) | どのタスクでも触れない |
| テスト (`QuietHoursTest`, リポジトリのテスト) | Task 1, 2, 3 |
| 検証 (`test` / `lint` / 実機) | Task 6 |

**Type consistency:** `QuietHours(startMinutesOfDay, endMinutesOfDay)` / `contains(minutesOfDay)` / `formatMinutesOfDay` / `isValidMinutesOfDay` / `DEFAULT` / `shouldSuppressNotificationAt(minutesOfDay)` / `setQuietHoursEnabled(enabled)` / `setQuietHours(startMinutesOfDay, endMinutesOfDay)` は全タスクで一致している。

**既知の制約:** `ArticleNotificationWorker` 自体のユニットテストは追加しない (Task 4 に理由を記載)。判定ロジックは Task 1・2 で網羅し、結線は Task 6 の実機検証で確認する。
