package blue.starry.tokidokiroppou.core.data.repository

import androidx.room.withTransaction
import blue.starry.tokidokiroppou.core.data.api.EGovLawApiClient
import blue.starry.tokidokiroppou.core.data.db.AppDatabase
import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.ArticleEntity
import blue.starry.tokidokiroppou.core.data.db.StructureHeadingEntity
import blue.starry.tokidokiroppou.core.data.db.LawMetadataDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataEntity
import blue.starry.tokidokiroppou.core.data.db.StructureHeadingDao
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.data.db.toEntity
import blue.starry.tokidokiroppou.core.data.parser.LawJsonParser
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw
import blue.starry.tokidokiroppou.core.domain.model.extractArticleReferences
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LawRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val apiClient: EGovLawApiClient,
    private val jsonParser: LawJsonParser,
    private val articleDao: ArticleDao,
    private val lawMetadataDao: LawMetadataDao,
    private val structureHeadingDao: StructureHeadingDao,
) : LawRepository {

    override suspend fun getArticles(lawId: LawId): List<Article> {
        val cached = getArticlesByStoredLawIdOrdered(lawId).mapNotNull { it.toDomain() }
        if (cached.isNotEmpty()) {
            return cached
        }

        return fetchAndCache(lawId)
    }

    override suspend fun getStructuredContent(lawId: LawId): List<LawContentItem> {
        // キャッシュ済みの条文と見出しを取得
        val cachedArticles = getArticlesByStoredLawId(lawId)
        val cachedHeadings = getHeadingsByStoredLawId(lawId)

        // v8→v9 移行前のレガシーキャッシュを検出して再取得する
        // レガシーキャッシュ: 条文はあるが見出しがなく、全 orderIndex が初期値(0)のまま
        val isLegacyCache = cachedArticles.isNotEmpty() &&
            cachedHeadings.isEmpty() &&
            cachedArticles.all { it.orderIndex == 0 }

        if (cachedArticles.isEmpty() || isLegacyCache) {
            updateLawDataFromApi(lawId)
            // API 取得後に DB から再読み込み
            return buildStructuredContent(lawId)
        }

        return buildStructuredContent(cachedArticles, cachedHeadings)
    }

    /** DB のエンティティから LawContentItem リストを組み立てる */
    private suspend fun buildStructuredContent(lawId: LawId): List<LawContentItem> {
        val articles = getArticlesByStoredLawId(lawId)
        val headings = getHeadingsByStoredLawId(lawId)
        return buildStructuredContent(articles, headings)
    }

    private fun buildStructuredContent(
        articleEntities: List<ArticleEntity>,
        headingEntities: List<StructureHeadingEntity>,
    ): List<LawContentItem> {
        val articles = articleEntities.mapNotNull { entity ->
            val article = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.ArticleItem(article, entity.orderIndex)
        }
        val headings = headingEntities.mapNotNull { entity ->
            val heading = entity.toDomain() ?: return@mapNotNull null
            LawContentItem.Heading(heading)
        }
        return (articles + headings).sortedBy { it.orderIndex }
    }

    override suspend fun getRandomArticle(lawIds: Set<LawId>, excludeSupplementaryProvisions: Boolean): Article? {
        if (lawIds.isEmpty()) return null

        val codes = lawIds.flatMap { it.storedKeys() }
        val entity = if (excludeSupplementaryProvisions) {
            articleDao.getRandomByLawCodesExcludingSupplProvision(codes)
        } else {
            articleDao.getRandomByLawCodes(codes)
        }
        if (entity != null) {
            return entity.toDomain()
        }

        // DB にデータがなければ取得を試みる
        val selectedLawId = lawIds.random()
        val articles = fetchAndCache(selectedLawId)
        val candidates = if (excludeSupplementaryProvisions) {
            articles.filter { it.supplementaryProvisionLabel == null }
        } else {
            articles
        }
        return candidates.randomOrNull()
    }

    override suspend fun getArticle(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String?): Article? {
        val keys = lawId.storedKeys()
        val entity = if (supplementaryProvisionLabel != null) {
            keys.firstMatchingArticle { key ->
                articleDao.getByLawCodeAndArticleNumberAndSupplProvision(key, articleNumber, supplementaryProvisionLabel)
            }
        } else {
            keys.firstMatchingArticle { key ->
                articleDao.getByLawCodeAndArticleNumber(key, articleNumber)
            }
        }
        return entity?.toDomain()
    }

    override suspend fun getRelatedArticles(article: Article): List<Article> {
        val refs = extractArticleReferences(article)
        if (refs.isEmpty()) return emptyList()
        return getArticlesByStoredLawIdAndArticleNumbers(article.lawId, refs)
            .mapNotNull { it.toDomain() }
    }

    override suspend fun getLawMetadata(lawId: LawId): LawMetadata? {
        val entity = lawId.storedKeys().firstMatchingMetadata { key ->
            lawMetadataDao.getByLawCode(key)
        }
            ?: return null
        return LawMetadata(
            lawNum = entity.lawNum,
            promulgationDate = entity.promulgationDate,
            lastAmendmentDate = entity.lastAmendmentDate,
            lastAmendmentLawNum = entity.lastAmendmentLawNum,
            lastRefreshedAt = entity.lastRefreshedAt,
        )
    }

    override fun observeLawMetadata(): Flow<Map<LawId, LawMetadata>> {
        return lawMetadataDao.observeAll().map { entities ->
            entities.associate { entity ->
                LawId(entity.lawCode) to LawMetadata(
                    lawNum = entity.lawNum,
                    promulgationDate = entity.promulgationDate,
                    lastAmendmentDate = entity.lastAmendmentDate,
                    lastAmendmentLawNum = entity.lastAmendmentLawNum,
                    lastRefreshedAt = entity.lastRefreshedAt,
                )
            }
        }
    }

    override suspend fun searchArticles(query: String): Map<LawId, List<Article>> {
        if (query.isBlank()) return emptyMap()
        return articleDao.search(query)
            .mapNotNull { it.toDomain() }
            .groupBy { it.lawId }
    }

    suspend fun getLawIdsNeedingRefresh(): List<LawId> {
        val threshold = System.currentTimeMillis() - REFRESH_THRESHOLD_MS
        val recentCodes = lawMetadataDao.getRecentlyRefreshedCodes(threshold).toSet()
        return PresetLaw.entries.map { it.id }.filter { lawId ->
            lawId.storedKeys().none { it in recentCodes }
        }
    }

    suspend fun refreshLawId(lawId: LawId): Boolean {
        return updateLawDataFromApi(lawId) != null
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

    /**
     * API からデータを取得し、トランザクション内で DB にキャッシュする共通処理。
     * 成功時は取得した条文リストを返し、失敗時は null を返す。
     */
    private suspend fun updateLawDataFromApi(lawId: LawId): List<Article>? {
        return try {
            val jsonString = apiClient.getLawData(lawId.value)
            val result = jsonParser.parse(jsonString, lawId)
            if (result.articles.isNotEmpty()) {
                // 複数テーブルの更新をトランザクションで保護する
                database.withTransaction {
                    for (key in lawId.storedKeys()) {
                        articleDao.deleteByLawCode(key)
                        structureHeadingDao.deleteByLawCode(key)
                    }
                    articleDao.insertAll(result.articles.map { article ->
                        val key = if (article.supplementaryProvisionLabel != null) {
                            "${article.supplementaryProvisionLabel}:${article.articleNumber}"
                        } else {
                            article.articleNumber
                        }
                        article.toEntity(orderIndex = result.articleOrderIndices[key] ?: 0)
                    })
                    structureHeadingDao.insertAll(result.headings.map { it.toEntity() })
                }
                Timber.d("Cached %d articles and %d headings from %s", result.articles.size, result.headings.size, lawId.displayName())
            }
            val revisionInfo = apiClient.getLawRevisionInfo(lawId.value)
            if (revisionInfo != null) {
                PresetLaw.fromLawId(lawId)?.legacyCodeName?.let { lawMetadataDao.deleteByLawCode(it) }
                lawMetadataDao.upsert(
                    LawMetadataEntity(
                        lawCode = lawId.value,
                        lawNum = revisionInfo.lawNum,
                        promulgationDate = revisionInfo.promulgationDate,
                        lastAmendmentDate = revisionInfo.amendmentDate,
                        lastAmendmentLawNum = revisionInfo.amendmentLawNum,
                    )
                )
            }
            result.articles
        } catch (e: Exception) {
            Timber.e(e, "Failed to update law data for %s", lawId.displayName())
            null
        }
    }

    private suspend fun fetchAndCache(lawId: LawId): List<Article> {
        return updateLawDataFromApi(lawId) ?: emptyList()
    }

    private suspend fun getArticlesByStoredLawId(lawId: LawId): List<ArticleEntity> {
        return lawId.storedKeys().firstNotEmptyOfOrEmpty { key ->
            articleDao.getByLawCode(key)
        }
    }

    private suspend fun getArticlesByStoredLawIdOrdered(lawId: LawId): List<ArticleEntity> {
        return lawId.storedKeys().firstNotEmptyOfOrEmpty { key ->
            articleDao.getByLawCodeOrdered(key)
        }
    }

    private suspend fun getHeadingsByStoredLawId(lawId: LawId): List<StructureHeadingEntity> {
        return lawId.storedKeys().firstNotEmptyOfOrEmpty { key ->
            structureHeadingDao.getByLawCode(key)
        }
    }

    private suspend fun getArticlesByStoredLawIdAndArticleNumbers(
        lawId: LawId,
        articleNumbers: List<String>,
    ): List<ArticleEntity> {
        return lawId.storedKeys().firstNotEmptyOfOrEmpty { key ->
            articleDao.getByLawCodeAndArticleNumbers(key, articleNumbers)
        }
    }

    private fun LawId.storedKeys(): List<String> {
        return listOfNotNull(value, PresetLaw.fromLawId(this)?.legacyCodeName)
    }

    private fun LawId.displayName(): String {
        return PresetLaw.fromLawId(this)?.displayName ?: value
    }

    private suspend fun <T> List<String>.firstNotEmptyOfOrEmpty(block: suspend (String) -> List<T>): List<T> {
        for (key in this) {
            val values = block(key)
            if (values.isNotEmpty()) {
                return values
            }
        }
        return emptyList()
    }

    private suspend fun List<String>.firstMatchingArticle(block: suspend (String) -> ArticleEntity?): ArticleEntity? {
        for (key in this) {
            block(key)?.let { return it }
        }
        return null
    }

    private suspend fun List<String>.firstMatchingMetadata(block: suspend (String) -> LawMetadataEntity?): LawMetadataEntity? {
        for (key in this) {
            block(key)?.let { return it }
        }
        return null
    }
}
