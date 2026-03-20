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

    @Query("SELECT COUNT(*) FROM articles WHERE lawCode = :lawCode")
    suspend fun countByLawCode(lawCode: String): Int

    @Query("DELETE FROM articles WHERE lawCode = :lawCode")
    suspend fun deleteByLawCode(lawCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<ArticleEntity>)

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun countAll(): Int
}
