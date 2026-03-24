package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.api.EGovLawApiClient
import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataEntity
import blue.starry.tokidokiroppou.core.data.db.StructureHeadingDao
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.data.db.toEntity
import blue.starry.tokidokiroppou.core.data.parser.LawJsonParser
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.extractArticleReferences
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LawRepositoryImpl @Inject constructor(
    private val apiClient: EGovLawApiClient,
    private val jsonParser: LawJsonParser,
    private val articleDao: ArticleDao,
    private val lawMetadataDao: LawMetadataDao,
    private val structureHeadingDao: StructureHeadingDao,
) : LawRepository {

    override suspend fun getArticles(lawCode: LawCode): List<Article> {
        val cached = articleDao.getByLawCode(lawCode.name).mapNotNull { it.toDomain() }
        if (cached.isNotEmpty()) {
            return cached
        }

        return fetchAndCache(lawCode)
    }

    override suspend fun getStructuredContent(lawCode: LawCode): List<LawContentItem> {
        // キャッシュ済みの条文と見出しを取得
        val cachedArticles = articleDao.getByLawCode(lawCode.name).mapNotNull { entity ->
            val article = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.ArticleItem(article, entity.orderIndex)
        }
        val cachedHeadings = structureHeadingDao.getByLawCode(lawCode.name).mapNotNull { entity ->
            val heading = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.Heading(heading)
        }

        if (cachedArticles.isNotEmpty()) {
            return (cachedArticles + cachedHeadings).sortedBy { it.orderIndex }
        }

        // キャッシュがなければ API から取得してキャッシュ
        fetchAndCache(lawCode)

        // キャッシュ後に再取得
        val articles = articleDao.getByLawCode(lawCode.name).mapNotNull { entity ->
            val article = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.ArticleItem(article, entity.orderIndex)
        }
        val headings = structureHeadingDao.getByLawCode(lawCode.name).mapNotNull { entity ->
            val heading = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.Heading(heading)
        }
        return (articles + headings).sortedBy { it.orderIndex }
    }

    override suspend fun getRandomArticle(lawCodes: Set<LawCode>, excludeSupplementaryProvisions: Boolean): Article? {
        if (lawCodes.isEmpty()) return null

        val codes = lawCodes.map { it.name }
        val entity = if (excludeSupplementaryProvisions) {
            articleDao.getRandomByLawCodesExcludingSupplProvision(codes)
        } else {
            articleDao.getRandomByLawCodes(codes)
        }
        if (entity != null) {
            return entity.toDomain()
        }

        // DB にデータがなければ取得を試みる
        val selectedLawCode = lawCodes.random()
        val articles = fetchAndCache(selectedLawCode)
        val candidates = if (excludeSupplementaryProvisions) {
            articles.filter { it.supplementaryProvisionLabel == null }
        } else {
            articles
        }
        return candidates.randomOrNull()
    }

    override suspend fun getArticle(lawCode: LawCode, articleNumber: String, supplementaryProvisionLabel: String?): Article? {
        val entity = if (supplementaryProvisionLabel != null) {
            articleDao.getByLawCodeAndArticleNumberAndSupplProvision(lawCode.name, articleNumber, supplementaryProvisionLabel)
        } else {
            articleDao.getByLawCodeAndArticleNumber(lawCode.name, articleNumber)
        }
        return entity?.toDomain()
    }

    override suspend fun getRelatedArticles(article: Article): List<Article> {
        val refs = extractArticleReferences(article)
        if (refs.isEmpty()) return emptyList()
        return articleDao.getByLawCodeAndArticleNumbers(article.lawCode.name, refs)
            .mapNotNull { it.toDomain() }
    }

    override suspend fun getLawMetadata(lawCode: LawCode): LawMetadata? {
        val entity = lawMetadataDao.getByLawCode(lawCode.name) ?: return null
        return LawMetadata(
            lawNum = entity.lawNum,
            promulgationDate = entity.promulgationDate,
            lastAmendmentDate = entity.lastAmendmentDate,
            lastAmendmentLawNum = entity.lastAmendmentLawNum,
            lastRefreshedAt = entity.lastRefreshedAt,
        )
    }

    override fun observeLawMetadata(): Flow<Map<LawCode, LawMetadata>> {
        return lawMetadataDao.observeAll().map { entities ->
            entities.mapNotNull { entity ->
                val lawCode = runCatching { LawCode.valueOf(entity.lawCode) }.getOrNull()
                    ?: return@mapNotNull null
                lawCode to LawMetadata(
                    lawNum = entity.lawNum,
                    promulgationDate = entity.promulgationDate,
                    lastAmendmentDate = entity.lastAmendmentDate,
                    lastAmendmentLawNum = entity.lastAmendmentLawNum,
                    lastRefreshedAt = entity.lastRefreshedAt,
                )
            }.toMap()
        }
    }

    override suspend fun searchArticles(query: String): Map<LawCode, List<Article>> {
        if (query.isBlank()) return emptyMap()
        return articleDao.search(query)
            .mapNotNull { it.toDomain() }
            .groupBy { it.lawCode }
    }

    suspend fun getLawCodesNeedingRefresh(): List<LawCode> {
        val threshold = System.currentTimeMillis() - REFRESH_THRESHOLD_MS
        val recentCodes = lawMetadataDao.getRecentlyRefreshedCodes(threshold).toSet()
        return LawCode.entries.filter { it.name !in recentCodes }
    }

    suspend fun refreshLawCode(lawCode: LawCode): Boolean {
        return try {
            val jsonString = apiClient.getLawData(lawCode.lawId)
            val result = jsonParser.parse(jsonString, lawCode)
            if (result.articles.isNotEmpty()) {
                articleDao.deleteByLawCode(lawCode.name)
                structureHeadingDao.deleteByLawCode(lawCode.name)
                articleDao.insertAll(result.articles.map { article ->
                    val key = if (article.supplementaryProvisionLabel != null) {
                        "${article.supplementaryProvisionLabel}:${article.articleNumber}"
                    } else {
                        article.articleNumber
                    }
                    article.toEntity(orderIndex = result.articleOrderIndices[key] ?: 0)
                })
                structureHeadingDao.insertAll(result.headings.map { it.toEntity() })
                Timber.d("Cached %d articles and %d headings from %s", result.articles.size, result.headings.size, lawCode.displayName)
            }
            val revisionInfo = apiClient.getLawRevisionInfo(lawCode.lawId)
            if (revisionInfo != null) {
                lawMetadataDao.upsert(
                    LawMetadataEntity(
                        lawCode = lawCode.name,
                        lawNum = revisionInfo.lawNum,
                        promulgationDate = revisionInfo.promulgationDate,
                        lastAmendmentDate = revisionInfo.amendmentDate,
                        lastAmendmentLawNum = revisionInfo.amendmentLawNum,
                    )
                )
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh %s", lawCode.displayName)
            false
        }
    }

    suspend fun clearCache() {
        articleDao.deleteAll()
        structureHeadingDao.deleteAll()
        lawMetadataDao.deleteAll()
        Timber.d("Cleared all cached articles, headings and metadata")
    }

    suspend fun isCacheAvailable(): Boolean {
        return articleDao.countAll() > 0
    }

    companion object {
        private const val REFRESH_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24時間
    }

    private suspend fun fetchAndCache(lawCode: LawCode): List<Article> {
        return try {
            val jsonString = apiClient.getLawData(lawCode.lawId)
            val result = jsonParser.parse(jsonString, lawCode)
            if (result.articles.isNotEmpty()) {
                articleDao.deleteByLawCode(lawCode.name)
                structureHeadingDao.deleteByLawCode(lawCode.name)
                articleDao.insertAll(result.articles.map { article ->
                    val key = if (article.supplementaryProvisionLabel != null) {
                        "${article.supplementaryProvisionLabel}:${article.articleNumber}"
                    } else {
                        article.articleNumber
                    }
                    article.toEntity(orderIndex = result.articleOrderIndices[key] ?: 0)
                })
                structureHeadingDao.insertAll(result.headings.map { it.toEntity() })
                Timber.d("Cached %d articles and %d headings from %s", result.articles.size, result.headings.size, lawCode.displayName)
            }
            val revisionInfo = apiClient.getLawRevisionInfo(lawCode.lawId)
            if (revisionInfo != null) {
                lawMetadataDao.upsert(
                    LawMetadataEntity(
                        lawCode = lawCode.name,
                        lawNum = revisionInfo.lawNum,
                        promulgationDate = revisionInfo.promulgationDate,
                        lastAmendmentDate = revisionInfo.amendmentDate,
                        lastAmendmentLawNum = revisionInfo.amendmentLawNum,
                    )
                )
            }
            result.articles
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch articles for %s", lawCode.displayName)
            emptyList()
        }
    }
}
