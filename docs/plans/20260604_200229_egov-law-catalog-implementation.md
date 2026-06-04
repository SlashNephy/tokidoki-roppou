# e-Gov Law Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Treat currently bundled laws as presets and let users add searchable e-Gov laws without losing existing cached articles, bookmarks, or notification settings.

**Architecture:** Move the canonical in-app law identifier from `LawCode.name` to e-Gov `lawId`, while keeping preset laws as seed metadata and legacy conversion input. Add a persisted user law catalog, migrate existing Room/DataStore values through a compatibility map, and expose separate add flows for laws list and settings.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, DataStore Preferences, Ktor, kotlinx.serialization, WorkManager, Gradle.

---

## File Map

- Create `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/LawId.kt`: value class for e-Gov law IDs.
- Create `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/Law.kt`: domain law catalog item.
- Create `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/PresetLaw.kt`: current built-in laws plus legacy enum-name conversion.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/LawCode.kt`: keep only as migration shim or replace usages after `PresetLaw` lands.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/Article.kt`: store `lawId: LawId` instead of `lawCode: LawCode`.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/StructureHeading.kt`: store `lawId: LawId`.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt`: expose `enabledLawIds: Set<LawId>`.
- Create `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/LawCatalogRepository.kt`: observe presets plus saved laws, search e-Gov catalog, add/remove laws.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/LawRepository.kt`: use `LawId`.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt`: use `LawId`.
- Modify `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/BookmarkRepository.kt`: use `LawId`.
- Create `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntity.kt`: persisted user-added laws.
- Create `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawDao.kt`: DAO for user-added laws.
- Create `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntityMapper.kt`: DB/domain mapping.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/AppDatabase.kt`: add `LawEntity`, `LawDao`, version 10.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt`: add migration 9->10, provide `LawDao`, bind `LawCatalogRepository`.
- Modify DB entity/mapper/DAO files under `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/`: keep SQL column name `lawCode` initially, but pass `lawId.value`.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/api/EGovLawApiClient.kt`: add law search/list APIs.
- Create `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawCatalogRepositoryImpl.kt`: merge presets and user-added laws, call e-Gov search, add laws with notification behavior.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt`: legacy DataStore conversion and `LawId` settings.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawRepositoryImpl.kt`: operate on `LawId`, resolve display names through catalog when logging/UI data needs it.
- Modify `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/parser/LawJsonParser.kt`: parse with `LawId`.
- Modify notification and worker files under `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/`: pass `LawId` through intents and cache refresh.
- Modify feature screens/view models in `feature/laws`, `feature/settings`, `feature/home`, and `feature/collection`: use `Law`/`LawId`, add e-Gov add flows.
- Modify `app/src/main/java/blue/starry/tokidokiroppou/App.kt`: route article navigation by `lawId` string.
- Modify `gradle/libs.versions.toml` and module `build.gradle.kts` files if test dependencies are needed.

## Task 1: Add LawId, Law, and PresetLaw Domain Models

**Files:**
- Create: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/LawId.kt`
- Create: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/Law.kt`
- Create: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/PresetLaw.kt`
- Test: `core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/PresetLawTest.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `core/domain/build.gradle.kts`

- [ ] **Step 1: Add test dependencies**

Add these libraries to `gradle/libs.versions.toml`:

```toml
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
```

Add this dependency to `core/domain/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test)
}
```

- [ ] **Step 2: Write the failing preset conversion test**

Create `core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/PresetLawTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresetLawTest {
    @Test
    fun `fromLegacyCodeName converts existing LawCode enum names to law IDs`() {
        assertEquals(LawId("129AC0000000089"), PresetLaw.fromLegacyCodeName("CIVIL_CODE")?.id)
        assertEquals(LawId("140AC0000000045"), PresetLaw.fromLegacyCodeName("PENAL_CODE")?.id)
    }

    @Test
    fun `fromLegacyCodeName returns null for unknown names`() {
        assertNull(PresetLaw.fromLegacyCodeName("UNKNOWN_LAW"))
    }

    @Test
    fun `default notification laws are the preset roppou laws`() {
        assertEquals(
            setOf(
                LawId("321CONSTITUTION"),
                LawId("129AC0000000089"),
                LawId("132AC0000000048"),
                LawId("140AC0000000045"),
                LawId("408AC0000000109"),
                LawId("323AC0000000131"),
            ),
            PresetLaw.defaultNotificationLawIds,
        )
    }
}
```

- [ ] **Step 3: Run the test and verify it fails**

Run:

```bash
./gradlew :core:domain:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.domain.model.PresetLawTest'
```

Expected: FAIL because `LawId` and `PresetLaw` do not exist.

- [ ] **Step 4: Add the minimal domain models**

Create `LawId.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class LawId(val value: String) {
    init {
        require(value.isNotBlank()) { "lawId must not be blank" }
    }

    override fun toString(): String = value
}
```

Create `Law.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Law(
    val id: LawId,
    val displayName: String,
    val lawNum: String? = null,
    val category: LawCategory = LawCategory.OTHERS,
    val isPreset: Boolean = false,
    val isAdded: Boolean = false,
)
```

Create `PresetLaw.kt` by moving the current `LawCode` entries into `PresetLaw` with `legacyCodeName`, `id`, `displayName`, and `category`. Include:

```kotlin
companion object {
    val all: List<Law> = entries.map {
        Law(
            id = it.id,
            displayName = it.displayName,
            category = it.category,
            isPreset = true,
            isAdded = true,
        )
    }

    val defaultNotificationLawIds: Set<LawId> = entries
        .filter { it.category == LawCategory.ROPPOU }
        .map { it.id }
        .toSet()

    fun fromLegacyCodeName(name: String): PresetLaw? {
        return entries.firstOrNull { it.legacyCodeName == name }
    }

    fun fromLawId(id: LawId): PresetLaw? {
        return entries.firstOrNull { it.id == id }
    }
}
```

- [ ] **Step 5: Run the test and verify it passes**

Run:

```bash
./gradlew :core:domain:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.domain.model.PresetLawTest'
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml core/domain/build.gradle.kts core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/LawId.kt core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/Law.kt core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/PresetLaw.kt core/domain/src/test/java/blue/starry/tokidokiroppou/core/domain/model/PresetLawTest.kt
git commit -m "feat: 法令識別子とプリセット法令モデルを追加"
```

## Task 2: Migrate Settings From LawCode to LawId

**Files:**
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt`
- Test: `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImplTest.kt`
- Modify: `core/data/build.gradle.kts`

- [ ] **Step 1: Add data module test dependencies**

Add to `core/data/build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":core:domain"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.work.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Write the failing DataStore migration test**

Create `ApplicationSettingsRepositoryImplTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationSettingsRepositoryImplTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Test
    fun `get converts legacy enabled law code names to law IDs`() = scope.runTest {
        val dataStore = newDataStore()
        dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey("enabled_law_codes")] = setOf("CIVIL_CODE", "PENAL_CODE")
        }

        val repository = ApplicationSettingsRepositoryImpl(dataStore)

        assertEquals(
            setOf(LawId("129AC0000000089"), LawId("140AC0000000045")),
            repository.get().enabledLawIds,
        )
        assertEquals(
            setOf("129AC0000000089", "140AC0000000045"),
            dataStore.data.first()[stringSetPreferencesKey("enabled_law_codes")],
        )
    }

    @Test
    fun `get keeps unknown law IDs instead of deleting them`() = scope.runTest {
        val dataStore = newDataStore()
        dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey("enabled_law_codes")] = setOf("999AC0000000001")
        }

        val repository = ApplicationSettingsRepositoryImpl(dataStore)

        assertEquals(setOf(LawId("999AC0000000001")), repository.get().enabledLawIds)
    }

    private fun newDataStore(): DataStore<Preferences> {
        val file = File.createTempFile("settings", ".preferences_pb").apply { delete() }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }
}
```

- [ ] **Step 3: Run the test and verify it fails**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.repository.ApplicationSettingsRepositoryImplTest'
```

Expected: FAIL because `enabledLawIds` and `LawId` settings APIs are not implemented.

- [ ] **Step 4: Implement settings migration**

Change `ApplicationSettings` to:

```kotlin
data class ApplicationSettings(
    val notificationIntervalMinutes: Int = 60,
    val enabledLawIds: Set<LawId> = PresetLaw.defaultNotificationLawIds,
    val isNotificationEnabled: Boolean = true,
    val useHalfWidthParentheses: Boolean = false,
    val excludeSupplementaryProvisions: Boolean = false,
)
```

Change `ApplicationSettingsRepository` method:

```kotlin
suspend fun setLawEnabled(lawId: LawId, enabled: Boolean)
```

In `ApplicationSettingsRepositoryImpl`, convert each stored string with:

```kotlin
private fun normalizeLawIdValue(value: String): String {
    return PresetLaw.fromLegacyCodeName(value)?.id?.value ?: value
}
```

In `get()` and `observe()`, call a private `migrateEnabledLawIdsIfNeeded()` that rewrites `enabled_law_codes` only when normalized values differ from stored values.

- [ ] **Step 5: Run the settings test and verify it passes**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.repository.ApplicationSettingsRepositoryImplTest'
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/ApplicationSettings.kt core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/ApplicationSettingsRepository.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImpl.kt core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/ApplicationSettingsRepositoryImplTest.kt core/data/build.gradle.kts
git commit -m "feat: 通知対象設定を lawId に移行"
```

## Task 3: Add User Law Catalog Storage and Room Migration

**Files:**
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntity.kt`
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawDao.kt`
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntityMapper.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/AppDatabase.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt`
- Test: `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/db/Migration9To10Test.kt`

- [ ] **Step 1: Write the failing migration test**

Create `Migration9To10Test.kt` with a helper that creates a version 9 schema, inserts legacy enum names into all four existing tables, runs `DataProvidesModule.MIGRATION_9_10`, and asserts the values are law IDs.

Use this core assertion:

```kotlin
assertEquals("129AC0000000089", querySingleString(db, "SELECT lawCode FROM articles"))
assertEquals("129AC0000000089", querySingleString(db, "SELECT lawCode FROM bookmarks"))
assertEquals("129AC0000000089", querySingleString(db, "SELECT lawCode FROM law_metadata"))
assertEquals("129AC0000000089", querySingleString(db, "SELECT lawCode FROM structure_headings"))
```

- [ ] **Step 2: Run the migration test and verify it fails**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.db.Migration9To10Test'
```

Expected: FAIL because `MIGRATION_9_10` and `laws` table do not exist.

- [ ] **Step 3: Add laws table and DAO**

Create `LawEntity.kt`:

```kotlin
@Entity(tableName = "laws")
data class LawEntity(
    @PrimaryKey val lawId: String,
    val displayName: String,
    val lawNum: String?,
    val category: String,
    val addedAt: Long = System.currentTimeMillis(),
)
```

Create `LawDao.kt` with:

```kotlin
@Dao
interface LawDao {
    @Query("SELECT * FROM laws ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<LawEntity>>

    @Query("SELECT * FROM laws WHERE lawId = :lawId")
    suspend fun getById(lawId: String): LawEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LawEntity)

    @Query("DELETE FROM laws WHERE lawId = :lawId")
    suspend fun delete(lawId: String)
}
```

Add `LawEntity` and `lawDao()` to `AppDatabase`, bump version to 10.

- [ ] **Step 4: Add migration 9 to 10**

In `DataProvidesModule`, add an internal `MIGRATION_9_10` and include it in `addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)`.

The migration must:

```sql
CREATE TABLE IF NOT EXISTS laws (
    lawId TEXT NOT NULL PRIMARY KEY,
    displayName TEXT NOT NULL,
    lawNum TEXT,
    category TEXT NOT NULL,
    addedAt INTEGER NOT NULL
)
```

Then update each legacy enum name in `articles`, `bookmarks`, `law_metadata`, and `structure_headings` using the complete `PresetLaw` legacy map. Unknown values stay unchanged.

- [ ] **Step 5: Run the migration test and verify it passes**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.db.Migration9To10Test'
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntity.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawDao.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/LawEntityMapper.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/AppDatabase.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt core/data/src/test/java/blue/starry/tokidokiroppou/core/data/db/Migration9To10Test.kt
git commit -m "feat: 追加法令テーブルと DB マイグレーションを追加"
```

## Task 4: Convert Repository and Entity Mapping to LawId

**Files:**
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/Article.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/StructureHeading.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model/LawContentItem.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/LawRepository.kt`
- Modify: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/BookmarkRepository.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/ArticleEntityMapper.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db/StructureHeadingEntity.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawRepositoryImpl.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/BookmarkRepositoryImpl.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/parser/LawJsonParser.kt`

- [ ] **Step 1: Write mapper and parser tests**

Create `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/db/ArticleEntityMapperTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.db

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleEntityMapperTest {
    @Test
    fun `article entity mapping preserves dynamic law ID`() {
        val article = Article(
            lawId = LawId("999AC0000000001"),
            articleNumber = "1",
            articleTitle = "第一条",
            articleCaption = "目的",
            paragraphs = listOf(Article.Paragraph(number = 1, text = "この法律は、テストを目的とする。")),
        )

        val entity = article.toEntity(orderIndex = 7)
        val restored = entity.toDomain()

        assertEquals(LawId("999AC0000000001"), restored?.lawId)
        assertEquals(7, entity.orderIndex)
    }
}
```

Create `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/parser/LawJsonParserTest.kt`:

```kotlin
package blue.starry.tokidokiroppou.core.data.parser

import blue.starry.tokidokiroppou.core.domain.model.LawId
import kotlin.test.Test
import kotlin.test.assertEquals

class LawJsonParserTest {
    @Test
    fun `parse assigns the supplied law ID to articles`() {
        val json = """
            {
              "law_full_text": {
                "tag": "Law",
                "children": [
                  {
                    "tag": "Article",
                    "attr": { "Num": "1" },
                    "children": [
                      { "tag": "ArticleTitle", "children": ["第一条"] },
                      {
                        "tag": "Paragraph",
                        "children": [
                          {
                            "tag": "ParagraphSentence",
                            "children": [
                              { "tag": "Sentence", "children": ["この法律は、テストを目的とする。"] }
                            ]
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val result = LawJsonParser().parse(json, LawId("999AC0000000001"))

        assertEquals(LawId("999AC0000000001"), result.articles.single().lawId)
    }
}
```

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests '*ArticleEntityMapperTest' --tests '*LawJsonParserTest'
```

Expected: FAIL because the domain model still requires `LawCode`.

- [ ] **Step 3: Change domain and repository signatures to LawId**

Replace `Article.lawCode` with `Article.lawId`. Replace `StructureHeading.lawCode` with `StructureHeading.lawId`. Update `LawRepository`, `BookmarkRepository`, and parser signatures from `LawCode` to `LawId`.

Keep SQL column names unchanged for this task:

```kotlin
ArticleEntity(
    lawCode = lawId.value,
    articleNumber = articleNumber,
    articleTitle = articleTitle,
    articleCaption = articleCaption,
    paragraphsJson = paragraphsJson,
    supplementaryProvisionLabel = supplementaryProvisionLabel,
    orderIndex = orderIndex,
)
```

Map back with:

```kotlin
lawId = LawId(lawCode)
```

- [ ] **Step 4: Update data repositories**

In `LawRepositoryImpl`, replace all `lawCode.name` with `lawId.value`, and all `lawCode.lawId` with `lawId.value`. Change logging to use the raw law ID until catalog display names are available:

```kotlin
Timber.e(e, "Failed to update law data for %s", lawId.value)
```

- [ ] **Step 5: Run module checks**

Run:

```bash
./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/model core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository core/data/src/main/java/blue/starry/tokidokiroppou/core/data/db core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository core/data/src/main/java/blue/starry/tokidokiroppou/core/data/parser core/data/src/test
git commit -m "refactor: 条文データ参照を lawId に移行"
```

## Task 5: Implement Law Catalog Repository and e-Gov Search

**Files:**
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/api/EGovLawApiClient.kt`
- Create: `core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/LawCatalogRepository.kt`
- Create: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawCatalogRepositoryImpl.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt`
- Test: `core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/LawCatalogRepositoryImplTest.kt`

- [ ] **Step 1: Write repository behavior tests**

Create `LawCatalogRepositoryImplTest.kt` with these three tests:

```kotlin
@Test
fun `observeLaws includes presets and saved laws`() = runTest {
    val savedLaw = Law(
        id = LawId("999AC0000000001"),
        displayName = "テスト法",
        lawNum = "令和九年法律第一号",
        category = LawCategory.OTHERS,
        isAdded = true,
    )
    lawDao.upsert(savedLaw.toEntity())

    val laws = repository.observeLaws().first()

    assertEquals(true, laws.any { it.id == LawId("129AC0000000089") && it.isPreset })
    assertEquals(true, laws.any { it.id == LawId("999AC0000000001") && it.displayName == "テスト法" })
}

@Test
fun `addLaw from laws tab does not enable notifications`() = runTest {
    val law = Law(id = LawId("999AC0000000001"), displayName = "テスト法", isAdded = true)

    repository.addLaw(law, enableNotification = false)

    assertEquals(false, settingsRepository.get().enabledLawIds.contains(LawId("999AC0000000001")))
}

@Test
fun `addLaw from settings enables notifications`() = runTest {
    val law = Law(id = LawId("999AC0000000001"), displayName = "テスト法", isAdded = true)

    repository.addLaw(law, enableNotification = true)

    assertEquals(true, settingsRepository.get().enabledLawIds.contains(LawId("999AC0000000001")))
}
```

Use small fake implementations for `lawDao`, `settingsRepository`, and `apiClient` if an in-memory Room database makes the test too slow. The fakes must store values in memory and expose the same public methods used by `LawCatalogRepositoryImpl`.

- [ ] **Step 2: Run tests and verify they fail**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.repository.LawCatalogRepositoryImplTest'
```

Expected: FAIL because `LawCatalogRepositoryImpl` does not exist.

- [ ] **Step 3: Add repository interface**

Create:

```kotlin
interface LawCatalogRepository {
    fun observeLaws(): Flow<List<Law>>
    suspend fun searchEGovLaws(query: String): List<Law>
    suspend fun addLaw(law: Law, enableNotification: Boolean)
    suspend fun removeAddedLaw(lawId: LawId)
    suspend fun getLaw(lawId: LawId): Law?
}
```

- [ ] **Step 4: Add e-Gov search DTO parsing**

In `EGovLawApiClient`, add `searchLaws(query: String): List<Law>` that calls `/keyword?keyword=$encodedQuery` first. Encode the query with Ktor URL parameters rather than string concatenation. If e-Gov returns a shape where the result list is nested, keep the method return type stable and adapt only the parser inside the client.

- [ ] **Step 5: Implement repository**

`observeLaws()` must emit `PresetLaw.all + lawDao.observeAll().map { it.toDomain() }`, de-duplicated by `LawId` with presets first. `addLaw(law, enableNotification)` must upsert non-preset laws and call `settingsRepository.setLawEnabled(law.id, true)` only when `enableNotification` is true.

- [ ] **Step 6: Run tests and commit**

Run:

```bash
./gradlew :core:data:testDebugUnitTest --tests 'blue.starry.tokidokiroppou.core.data.repository.LawCatalogRepositoryImplTest'
```

Expected: PASS.

Commit:

```bash
git add core/domain/src/main/java/blue/starry/tokidokiroppou/core/domain/repository/LawCatalogRepository.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/api/EGovLawApiClient.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawCatalogRepositoryImpl.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/di/DataModule.kt core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/LawCatalogRepositoryImplTest.kt
git commit -m "feat: e-Gov 法令カタログ検索と追加処理を実装"
```

## Task 6: Update Workers, Navigation, and Notifications

**Files:**
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/ArticleNotificationWorker.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker/CacheRefreshWorker.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/notification/ArticleNotificationSender.kt`
- Modify: `core/data/src/main/java/blue/starry/tokidokiroppou/core/data/notification/BookmarkActionReceiver.kt`
- Modify: `app/src/main/java/blue/starry/tokidokiroppou/App.kt`
- Modify: `feature/home/src/main/java/blue/starry/tokidokiroppou/feature/home/ui/HomeScreenViewModel.kt`
- Modify: `feature/collection/src/main/java/blue/starry/tokidokiroppou/feature/collection/ui/CollectionScreen*.kt`

- [ ] **Step 1: Run compile to reveal LawCode call sites**

Run:

```bash
./gradlew :app:compileStagingDebugKotlin
```

Expected: FAIL with remaining `LawCode` usage errors.

- [ ] **Step 2: Update call sites to pass law ID strings**

Notification extras should continue using a string key, but the value is now `article.lawId.value`. `HomeScreenViewModel.ArticleNavigationTarget.lawCode` should be renamed to `lawId`, and `loadSpecificArticle()` should use `LawId(lawIdValue)`.

- [ ] **Step 3: Update display names**

Where notifications or cards need a law display name, resolve it through `LawCatalogRepository.getLaw(article.lawId)?.displayName ?: article.lawId.value`.

- [ ] **Step 4: Run compile and commit**

Run:

```bash
./gradlew :app:compileStagingDebugKotlin
```

Expected: PASS.

Commit:

```bash
git add app/src/main/java/blue/starry/tokidokiroppou/App.kt core/data/src/main/java/blue/starry/tokidokiroppou/core/data/worker core/data/src/main/java/blue/starry/tokidokiroppou/core/data/notification feature/home/src/main/java/blue/starry/tokidokiroppou/feature/home/ui feature/collection/src/main/java/blue/starry/tokidokiroppou/feature/collection/ui
git commit -m "refactor: 通知と画面遷移を lawId に対応"
```

## Task 7: Add Laws Tab e-Gov Search and Non-Notification Add Flow

**Files:**
- Modify: `feature/laws/src/main/java/blue/starry/tokidokiroppou/feature/laws/ui/LawsScreenViewModel.kt`
- Modify: `feature/laws/src/main/java/blue/starry/tokidokiroppou/feature/laws/ui/LawsScreen.kt`
- Modify: `feature/laws/src/main/java/blue/starry/tokidokiroppou/feature/laws/ui/LawsRoute.kt`

- [ ] **Step 1: Add UI state for catalog search**

Add state to the ViewModel:

```kotlin
val catalogSearchResults: StateFlow<List<Law>>
val isCatalogSearching: StateFlow<Boolean>
val catalogSearchError: StateFlow<String?>
fun searchCatalog(query: String)
fun addLawForBrowsing(law: Law)
```

`addLawForBrowsing(law)` must call `lawCatalogRepository.addLaw(law, enableNotification = false)`.

- [ ] **Step 2: Update screen**

Add a mode control near the existing search field with two options: cached article search and e-Gov law search. In e-Gov law search mode, show law results with an add icon button for laws not already added.

- [ ] **Step 3: Verify compile**

Run:

```bash
./gradlew :feature:laws:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add feature/laws/src/main/java/blue/starry/tokidokiroppou/feature/laws/ui
git commit -m "feat: 法令一覧から e-Gov 法令を追加可能にする"
```

## Task 8: Add Settings Add Flow With Notification Enabled

**Files:**
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreenViewModel.kt`
- Modify: `feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui/SettingsScreen.kt`

- [ ] **Step 1: Add ViewModel methods**

Add:

```kotlin
fun searchCatalog(query: String)
fun addLawForNotifications(law: Law)
```

`addLawForNotifications(law)` must call `lawCatalogRepository.addLaw(law, enableNotification = true)`.

- [ ] **Step 2: Update Settings UI**

Show preset laws and added laws in separate sections. Add a "法令を追加" action in settings that opens a dialog or sheet with an e-Gov search field and result list. Added from this dialog must immediately appear checked.

- [ ] **Step 3: Verify compile**

Run:

```bash
./gradlew :feature:settings:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add feature/settings/src/main/java/blue/starry/tokidokiroppou/feature/settings/ui
git commit -m "feat: 設定画面から通知対象法令を追加可能にする"
```

## Task 9: Final Verification and PR

**Files:**
- Modify only files needed for compile/test fixes.

- [ ] **Step 1: Run focused checks**

Run:

```bash
./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:laws:compileDebugKotlin :feature:settings:compileDebugKotlin :app:compileStagingDebugKotlin
```

Expected: PASS.

- [ ] **Step 2: Run full check**

Run:

```bash
./gradlew testStagingDebugUnitTest lintStagingDebug assembleStagingDebug
```

Expected: PASS. If Firebase files are missing and block assemble, record the exact missing file and keep the focused compile/test evidence.

- [ ] **Step 3: Inspect diff**

Run:

```bash
git status --short
git diff --stat origin/main..HEAD
git diff --check origin/main..HEAD
```

Expected: only intended tracked files are changed; `gradle/gradle-daemon-jvm.properties` remains untracked and uncommitted unless the user explicitly says otherwise.

- [ ] **Step 4: Commit final fixes if any**

If verification required fixes:

Run `git status --short`, stage only the files changed by the verification fix, then commit:

```bash
git status --short
git add core/data/src/main/java/blue/starry/tokidokiroppou/core/data/repository/LawRepositoryImpl.kt core/data/src/test/java/blue/starry/tokidokiroppou/core/data/repository/LawRepositoryImplTest.kt
git commit -m "fix: e-Gov 法令追加フローの検証指摘を修正"
```

If the fix is in different files, replace the two `git add` paths with the exact files shown by `git status --short` for that fix only.

- [ ] **Step 5: Push and create PR**

```bash
git push -u origin feature/egov-law-catalog
gh pr create --title "e-Gov 法令カタログから法令を追加できるようにする" --body "$(cat <<'EOF'
## 概要

- 既存の対応法令をプリセットとして扱い、e-Gov 法令カタログから法令を追加できるようにします
- 既存の通知対象、ブックマーク、キャッシュを lawId にマイグレーションします
- 法令一覧タブからの追加は通知 OFF、設定画面からの追加は通知 ON として扱います

## 確認

- [ ] ./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:laws:compileDebugKotlin :feature:settings:compileDebugKotlin :app:compileStagingDebugKotlin
- [ ] ./gradlew testStagingDebugUnitTest lintStagingDebug assembleStagingDebug
EOF
)"
```

Expected: PR is created in Japanese and is not draft unless verification has unresolved blockers.
