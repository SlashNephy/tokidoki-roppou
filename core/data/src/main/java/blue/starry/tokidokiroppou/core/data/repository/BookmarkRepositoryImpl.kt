package blue.starry.tokidokiroppou.core.data.repository

import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.BookmarkDao
import blue.starry.tokidokiroppou.core.data.db.BookmarkEntity
import blue.starry.tokidokiroppou.core.data.db.toDomain
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import blue.starry.tokidokiroppou.core.domain.model.PresetLaw
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
        lawId: LawId,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(
            lawId.storedKeys(),
            articleNumber,
            supplementaryProvisionLabel ?: "",
        )
    }

    override suspend fun add(
        lawId: LawId,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.insert(
            BookmarkEntity(
                lawCode = lawId.value,
                articleNumber = articleNumber,
                supplementaryProvisionLabel = supplementaryProvisionLabel ?: "",
            ),
        )
    }

    override suspend fun remove(
        lawId: LawId,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.delete(
            lawId.storedKeys(),
            articleNumber,
            supplementaryProvisionLabel ?: "",
        )
    }

    override suspend fun toggle(
        lawId: LawId,
        articleNumber: String,
        supplementaryProvisionLabel: String?,
    ) {
        bookmarkDao.toggle(lawId.storedKeys(), articleNumber, supplementaryProvisionLabel ?: "")
    }

    private fun LawId.storedKeys(): List<String> {
        return listOfNotNull(value, PresetLaw.fromLawId(this)?.legacyCodeName)
    }
}
