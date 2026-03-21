package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import kotlinx.coroutines.flow.Flow

interface LawRepository {
    suspend fun getArticles(lawCode: LawCode): List<Article>

    suspend fun getRandomArticle(lawCodes: Set<LawCode>, excludeSupplementaryProvisions: Boolean = false): Article?

    suspend fun getArticle(lawCode: LawCode, articleNumber: String, supplementaryProvisionLabel: String? = null): Article?

    suspend fun getRelatedArticles(article: Article): List<Article>

    suspend fun getLawMetadata(lawCode: LawCode): LawMetadata?

    fun observeLawMetadata(): Flow<Map<LawCode, LawMetadata>>

    suspend fun searchArticles(query: String): Map<LawCode, List<Article>>
}
