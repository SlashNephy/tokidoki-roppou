package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.BookmarkDao
import blue.starry.tokidokiroppou.core.data.db.BookmarkEntity
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawCode
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val articleDao: ArticleDao,
) : BookmarkRepository {

    override fun observeAll(): Flow<List<Article>> {
        return articleDao.observeBookmarkedArticles().map { entities ->
            entities.mapNotNull { it.toDomain() }
        }
    }

    override fun observeIsBookmarked(
        lawCode: LawCode,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(
            lawCode.storedKeys(),
            articleNumber,
            supplementaryProvisionLabel ?: "",
        )
    }

    override suspend fun add(
        lawCode: LawCode,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.insert(
            BookmarkEntity(
                lawCode = lawCode.lawId,
                articleNumber = articleNumber,
                supplementaryProvisionLabel = supplementaryProvisionLabel ?: "",
            ),
        )
    }

    override suspend fun remove(
        lawCode: LawCode,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.delete(
            lawCode.storedKeys(),
            articleNumber,
            supplementaryProvisionLabel ?: "",
        )
    }

    override suspend fun toggle(
        lawCode: LawCode,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.toggle(lawCode.storedKeys(), articleNumber, supplementaryProvisionLabel ?: "")
    }

    private fun LawCode.storedKeys(): List<String> {
        return listOf(lawId, name)
    }
}
