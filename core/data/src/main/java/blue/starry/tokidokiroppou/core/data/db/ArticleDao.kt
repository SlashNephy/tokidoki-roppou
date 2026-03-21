package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE lawCode = :lawCode")
    suspend fun getByLawCode(lawCode: String): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE lawCode IN (:lawCodes) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByLawCodes(lawCodes: List<String>): ArticleEntity?

    @Query("SELECT * FROM articles WHERE lawCode IN (:lawCodes) AND supplementaryProvisionLabel IS NULL ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomByLawCodesExcludingSupplProvision(lawCodes: List<String>): ArticleEntity?

    @Query("SELECT * FROM articles WHERE lawCode = :lawCode AND articleNumber = :articleNumber AND supplementaryProvisionLabel IS NULL LIMIT 1")
    suspend fun getByLawCodeAndArticleNumber(lawCode: String, articleNumber: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE lawCode = :lawCode AND articleNumber = :articleNumber AND supplementaryProvisionLabel = :supplementaryProvisionLabel LIMIT 1")
    suspend fun getByLawCodeAndArticleNumberAndSupplProvision(lawCode: String, articleNumber: String, supplementaryProvisionLabel: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE lawCode = :lawCode AND articleNumber IN (:articleNumbers)")
    suspend fun getByLawCodeAndArticleNumbers(lawCode: String, articleNumbers: List<String>): List<ArticleEntity>

    @Query("SELECT COUNT(*) FROM articles WHERE lawCode = :lawCode")
    suspend fun countByLawCode(lawCode: String): Int

    @Query("DELETE FROM articles WHERE lawCode = :lawCode")
    suspend fun deleteByLawCode(lawCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun countAll(): Int

    @Query(
        """
        SELECT * FROM articles
        WHERE articleNumber LIKE '%' || :query || '%'
           OR articleTitle LIKE '%' || :query || '%'
           OR articleCaption LIKE '%' || :query || '%'
           OR paragraphsJson LIKE '%' || :query || '%'
        """
    )
    suspend fun search(query: String): List<ArticleEntity>
}
