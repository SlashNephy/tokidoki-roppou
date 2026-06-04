package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawContentItem
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import kotlinx.coroutines.flow.Flow

interface LawRepository {
    suspend fun getArticles(lawId: LawId): List<Article>

    /** 構造見出し付きの条文リストを取得する（法令一覧での展開表示用） */
    suspend fun getStructuredContent(lawId: LawId): List<LawContentItem>

    suspend fun getRandomArticle(lawIds: Set<LawId>, excludeSupplementaryProvisions: Boolean = false): Article?

    suspend fun getArticle(lawId: LawId, articleNumber: String, supplementaryProvisionLabel: String? = null): Article?

    suspend fun getRelatedArticles(article: Article): List<Article>

    suspend fun getLawMetadata(lawId: LawId): LawMetadata?

    fun observeLawMetadata(): Flow<Map<LawId, LawMetadata>>

    suspend fun searchArticles(query: String): Map<LawId, List<Article>>
}
