package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LawMetadataDao {
    @Query("SELECT * FROM law_metadata")
    fun observeAll(): Flow<List<LawMetadataEntity>>

    @Query("SELECT lawNum FROM law_metadata WHERE lawCode = :lawCode")
    suspend fun getLawNum(lawCode: String): String?

    @Query("SELECT lawCode FROM law_metadata WHERE lastRefreshedAt > :threshold")
    suspend fun getRecentlyRefreshedCodes(threshold: Long): List<String>

    @Query("DELETE FROM law_metadata")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LawMetadataEntity)
}
