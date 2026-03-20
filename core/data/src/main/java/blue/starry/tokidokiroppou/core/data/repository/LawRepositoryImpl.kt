package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.api.EGovLawApiClient
import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataEntity
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.data.db.toEntity
import blue.starry.tokidokiroppou.core.data.parser.LawXmlParser
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LawRepositoryImpl @Inject constructor(
    private val apiClient: EGovLawApiClient,
    private val xmlParser: LawXmlParser,
    private val articleDao: ArticleDao,
    private val lawMetadataDao: LawMetadataDao,
) : LawRepository {

    override suspend fun getArticles(lawCode: LawCode): List<Article> {
        val cached = articleDao.getByLawCode(lawCode.name).mapNotNull { it.toDomain() }
        if (cached.isNotEmpty()) {
            return cached
        }

        return fetchAndCache(lawCode)
    }

    override suspend fun getRandomArticle(lawCodes: Set<LawCode>): Article? {
        if (lawCodes.isEmpty()) return null

        val entity = articleDao.getRandomByLawCodes(lawCodes.map { it.name })
        if (entity != null) {
            return entity.toDomain()
        }

        // DB にデータがなければ取得を試みる
        val selectedLawCode = lawCodes.random()
        val articles = fetchAndCache(selectedLawCode)
        return articles.randomOrNull()
    }

    override fun observeLawMetadata(): Flow<Map<LawCode, LawMetadata>> {
        return lawMetadataDao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                val lawCode = runCatching { LawCode.valueOf(entity.lawCode) }.getOrNull()
                    ?: return@mapNotNull null
                lawCode to LawMetadata(
                    lawNum = entity.lawNum,
                    promulgationDate = entity.promulgationDate,
                )
            }.toMap()
        }
    }

    suspend fun getLawCodesNeedingRefresh(): List<LawCode> {
        val threshold = System.currentTimeMillis() - REFRESH_THRESHOLD_MS
        val recentCodes = lawMetadataDao.getRecentlyRefreshedCodes(threshold).toSet()
        return LawCode.entries.filter { it.name !in recentCodes }
    }

    suspend fun refreshLawCode(lawCode: LawCode): Boolean {
        return try {
            val xml = apiClient.getLawData(lawCode.lawId)
            val result = xmlParser.parseLaw(xml, lawCode)
            if (result.articles.isNotEmpty()) {
                articleDao.deleteByLawCode(lawCode.name)
                articleDao.insertAll(result.articles.map { it.toEntity() })
                Timber.d("Cached %d articles from %s", result.articles.size, lawCode.displayName)
            }
            if (result.lawNum != null) {
                lawMetadataDao.upsert(
                    LawMetadataEntity(lawCode.name, result.lawNum, result.promulgationDate)
                )
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh %s", lawCode.displayName)
            false
        }
    }

    suspend fun isCacheAvailable(): Boolean {
        return articleDao.countAll() > 0
    }

    companion object {
        private const val REFRESH_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24時間
    }

    private suspend fun fetchAndCache(lawCode: LawCode): List<Article> {
        return try {
            val xml = apiClient.getLawData(lawCode.lawId)
            val result = xmlParser.parseLaw(xml, lawCode)
            if (result.articles.isNotEmpty()) {
                articleDao.deleteByLawCode(lawCode.name)
                articleDao.insertAll(result.articles.map { it.toEntity() })
                Timber.d("Cached %d articles from %s", result.articles.size, lawCode.displayName)
            }
            if (result.lawNum != null) {
                lawMetadataDao.upsert(
                    LawMetadataEntity(lawCode.name, result.lawNum, result.promulgationDate)
                )
            }
            result.articles
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch articles for %s", lawCode.displayName)
            emptyList()
        }
    }
}
