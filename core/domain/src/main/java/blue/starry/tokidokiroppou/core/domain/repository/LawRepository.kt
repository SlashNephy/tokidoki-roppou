package blue.starry.tokidokiroppou.core.domain.repository

import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode

interface LawRepository {
    suspend fun getArticles(lawCode: LawCode): List<Article>

    suspend fun getRandomArticle(lawCodes: Set<LawCode>): Article?
}
