package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LawDao {
    @Query("SELECT * FROM laws ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<LawEntity>>

    @Query("SELECT * FROM laws WHERE lawId = :lawId")
    suspend fun getById(lawId: String): LawEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LawEntity)

    @Query("DELETE FROM laws WHERE lawId = :lawId")
    suspend fun delete(lawId: String)
}
