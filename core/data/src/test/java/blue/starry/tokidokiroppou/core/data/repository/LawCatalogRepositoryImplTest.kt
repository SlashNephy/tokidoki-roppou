package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.api.EGovLawApiClient
import blue.starry.tokidokiroppou.core.data.db.LawDao
import blue.starry.tokidokiroppou.core.data.db.LawEntity
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.data.db.toEntity
import blue.starry.tokidokiroppou.core.domain.model.ApplicationSettings
import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawCategory
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class LawCatalogRepositoryImplTest {
    @Test
    fun observeLawsIncludesPresetsAndSavedLawsWithPresetsFirstAndDeduplicated() = runTest {
        val preset = PresetLaw.CIVIL_CODE.toLaw()
        val savedLaw = createLaw(id = LawId("999AC0000000001"), displayName = "テスト法")
        val duplicatePresetLaw = createLaw(id = preset.id, displayName = "保存済み民法")
        val repository = createRepository(
            lawDao = FakeLawDao(
                initialEntities = listOf(savedLaw.toEntity(), duplicatePresetLaw.toEntity()),
            ),
        )

        val laws = repository.observeLaws().first()

        assertEquals(PresetLaw.all, laws.take(PresetLaw.all.size))
        assertEquals(savedLaw.copy(isAdded = true), laws.last())
        assertEquals(1, laws.count { it.id == preset.id })
    }

    @Test
    fun addLawWithNotificationDisabledUpsertsLawWithoutEnablingNotification() = runTest {
        val lawDao = FakeLawDao()
        val settingsRepository = FakeApplicationSettingsRepository()
        val repository = createRepository(lawDao = lawDao, settingsRepository = settingsRepository)
        val law = createLaw(id = LawId("999AC0000000001"), displayName = "テスト法")

        repository.addLaw(law, enableNotification = false)

        assertEquals(law.copy(isAdded = true), lawDao.getById(law.id.value)?.toDomain())
        assertEquals(emptyList(), settingsRepository.lawEnabledCalls)
    }

    @Test
    fun addLawWithNotificationEnabledUpsertsLawAndEnablesNotification() = runTest {
        val lawDao = FakeLawDao()
        val settingsRepository = FakeApplicationSettingsRepository()
        val repository = createRepository(lawDao = lawDao, settingsRepository = settingsRepository)
        val law = createLaw(id = LawId("999AC0000000001"), displayName = "テスト法")

        repository.addLaw(law, enableNotification = true)

        assertEquals(law.copy(isAdded = true), lawDao.getById(law.id.value)?.toDomain())
        assertEquals(listOf(law.id to true), settingsRepository.lawEnabledCalls)
    }

    @Test
    fun addPresetLawWithNotificationEnabledDoesNotInsertLawAndEnablesNotification() = runTest {
        val lawDao = FakeLawDao()
        val settingsRepository = FakeApplicationSettingsRepository()
        val repository = createRepository(lawDao = lawDao, settingsRepository = settingsRepository)
        val presetLaw = PresetLaw.CIVIL_CODE.toLaw()

        repository.addLaw(presetLaw, enableNotification = true)

        assertNull(lawDao.getById(presetLaw.id.value))
        assertEquals(listOf(presetLaw.id to true), settingsRepository.lawEnabledCalls)
    }

    @Test
    fun removeAddedLawDeletesSavedLawAndDisablesNotification() = runTest {
        val law = createLaw(id = LawId("999AC0000000001"), displayName = "テスト法")
        val lawDao = FakeLawDao(initialEntities = listOf(law.toEntity()))
        val settingsRepository = FakeApplicationSettingsRepository()
        val repository = createRepository(lawDao = lawDao, settingsRepository = settingsRepository)

        repository.removeAddedLaw(law.id)

        assertNull(lawDao.getById(law.id.value))
        assertEquals(listOf(law.id to false), settingsRepository.lawEnabledCalls)
    }

    @Test
    fun removeAddedLawWithPresetLawDoesNotDeleteLawOrDisableNotification() = runTest {
        val presetLaw = PresetLaw.CIVIL_CODE.toLaw()
        val savedPresetLaw = createLaw(id = presetLaw.id, displayName = "保存済み民法")
        val lawDao = FakeLawDao(initialEntities = listOf(savedPresetLaw.toEntity()))
        val settingsRepository = FakeApplicationSettingsRepository()
        val repository = createRepository(lawDao = lawDao, settingsRepository = settingsRepository)

        repository.removeAddedLaw(presetLaw.id)

        assertEquals(savedPresetLaw.copy(isAdded = true), lawDao.getById(presetLaw.id.value)?.toDomain())
        assertEquals(emptyList(), lawDao.deletedLawIds)
        assertEquals(emptyList(), settingsRepository.lawEnabledCalls)
    }

    @Test
    fun getLawReturnsPresetBeforeSavedLaw() = runTest {
        val presetLaw = PresetLaw.CIVIL_CODE.toLaw()
        val savedPresetLaw = createLaw(id = presetLaw.id, displayName = "保存済み民法")
        val lawDao = FakeLawDao(initialEntities = listOf(savedPresetLaw.toEntity()))
        val repository = createRepository(lawDao = lawDao)

        val law = repository.getLaw(presetLaw.id)

        assertEquals(presetLaw, law)
    }

    @Test
    fun getLawReturnsSavedLawWhenLawIsNotPreset() = runTest {
        val savedLaw = createLaw(id = LawId("999AC0000000001"), displayName = "テスト法")
        val lawDao = FakeLawDao(initialEntities = listOf(savedLaw.toEntity()))
        val repository = createRepository(lawDao = lawDao)

        val law = repository.getLaw(savedLaw.id)

        assertEquals(savedLaw.copy(isAdded = true), law)
    }

    private fun createRepository(
        lawDao: FakeLawDao = FakeLawDao(),
        settingsRepository: FakeApplicationSettingsRepository = FakeApplicationSettingsRepository(),
    ): LawCatalogRepositoryImpl {
        return LawCatalogRepositoryImpl(
            apiClient = EGovLawApiClient(HttpClient(OkHttp)),
            lawDao = lawDao,
            settingsRepository = settingsRepository,
        )
    }

    private fun createLaw(
        id: LawId,
        displayName: String,
    ): Law {
        return Law(
            id = id,
            displayName = displayName,
            lawNum = "令和八年法律第一号",
            category = LawCategory.OTHERS,
            isPreset = false,
            isAdded = false,
        )
    }

    private class FakeLawDao(
        initialEntities: List<LawEntity> = emptyList(),
    ) : LawDao {
        private val entities = MutableStateFlow(initialEntities)
        val deletedLawIds = mutableListOf<String>()

        override fun observeAll(): Flow<List<LawEntity>> {
            return entities
        }

        override suspend fun getById(lawId: String): LawEntity? {
            return entities.value.firstOrNull { it.lawId == lawId }
        }

        override suspend fun upsert(entity: LawEntity) {
            entities.value = entities.value
                .filterNot { it.lawId == entity.lawId } + entity
        }

        override suspend fun delete(lawId: String) {
            deletedLawIds += lawId
            entities.value = entities.value.filterNot { it.lawId == lawId }
        }
    }

    private class FakeApplicationSettingsRepository : ApplicationSettingsRepository {
        val lawEnabledCalls = mutableListOf<Pair<LawId, Boolean>>()

        override fun observe(): Flow<ApplicationSettings> {
            return MutableStateFlow(ApplicationSettings())
        }

        override suspend fun get(): ApplicationSettings {
            return ApplicationSettings()
        }

        override suspend fun setNotificationIntervalMinutes(minutes: Int) {
        }

        override suspend fun setNotificationEnabled(enabled: Boolean) {
        }

        override suspend fun setLawEnabled(lawId: LawId, enabled: Boolean) {
            lawEnabledCalls += lawId to enabled
        }

        override suspend fun setUseHalfWidthParentheses(enabled: Boolean) {
        }

        override suspend fun setExcludeSupplementaryProvisions(enabled: Boolean) {
        }

        override suspend fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        }

        override suspend fun setQuietHoursEnabled(enabled: Boolean) {
        }

        override suspend fun setQuietHours(startMinutesOfDay: Int, endMinutesOfDay: Int) {
        }
    }

}
