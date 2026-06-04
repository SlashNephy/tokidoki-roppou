package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.api.EGovLawApiClient
import blue.starry.tokidokiroppou.core.data.db.LawDao
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.data.db.toEntity
import blue.starry.tokidokiroppou.core.domain.model.Law
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawCatalogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LawCatalogRepositoryImpl @Inject constructor(
    private val apiClient: EGovLawApiClient,
    private val lawDao: LawDao,
    private val settingsRepository: ApplicationSettingsRepository,
) : LawCatalogRepository {
    override fun observeLaws(): Flow<List<Law>> {
        return lawDao.observeAll().map { savedLaws ->
            (PresetLaw.all + savedLaws.map { it.toDomain() }).distinctBy { it.id }
        }
    }

    override suspend fun searchEGovLaws(query: String): List<Law> {
        return apiClient.searchLaws(query)
    }

    override suspend fun addLaw(law: Law, enableNotification: Boolean) {
        if (!law.isPreset && PresetLaw.fromLawId(law.id) == null) {
            lawDao.upsert(law.toEntity())
        }

        if (enableNotification) {
            settingsRepository.setLawEnabled(law.id, enabled = true)
        }
    }

    override suspend fun removeAddedLaw(lawId: LawId) {
        if (PresetLaw.fromLawId(lawId) != null) {
            return
        }

        lawDao.delete(lawId.value)
        settingsRepository.setLawEnabled(lawId, enabled = false)
    }

    override suspend fun getLaw(lawId: LawId): Law? {
        return PresetLaw.fromLawId(lawId)?.toLaw()
            ?: lawDao.getById(lawId.value)?.toDomain()
    }
}
