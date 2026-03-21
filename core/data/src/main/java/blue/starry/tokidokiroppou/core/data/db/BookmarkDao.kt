package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY bookmarkedAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) > 0 FROM bookmarks WHERE lawCode = :lawCode AND articleNumber = :articleNumber AND supplementaryProvisionLabel = :supplementaryProvisionLabel")
    fun observeIsBookmarked(lawCode: String, articleNumber: String, supplementaryProvisionLabel: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE lawCode = :lawCode AND articleNumber = :articleNumber AND supplementaryProvisionLabel = :supplementaryProvisionLabel")
    suspend fun delete(lawCode: String, articleNumber: String, supplementaryProvisionLabel: String)

    @Query("SELECT COUNT(*) > 0 FROM bookmarks WHERE lawCode = :lawCode AND articleNumber = :articleNumber AND supplementaryProvisionLabel = :supplementaryProvisionLabel")
    suspend fun isBookmarked(lawCode: String, articleNumber: String, supplementaryProvisionLabel: String): Boolean
}
