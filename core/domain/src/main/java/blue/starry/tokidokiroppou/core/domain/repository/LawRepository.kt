package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.model.LawMetadata
import kotlinx.coroutines.flow.Flow

interface LawRepository {
    suspend fun getArticles(lawCode: LawCode): List<Article>

    suspend fun getRandomArticle(lawCodes: Set<LawCode>): Article?

    fun observeLawMetadata(): Flow<Map<LawCode, LawMetadata>>
}
