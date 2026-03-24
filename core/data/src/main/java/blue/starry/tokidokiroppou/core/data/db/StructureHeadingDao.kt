package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StructureHeadingDao {
    @Query("SELECT * FROM structure_headings WHERE lawCode = :lawCode ORDER BY orderIndex")
    suspend fun getByLawCode(lawCode: String): List<StructureHeadingEntity>

    @Query("DELETE FROM structure_headings WHERE lawCode = :lawCode")
    suspend fun deleteByLawCode(lawCode: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(headings: List<StructureHeadingEntity>)

    @Query("DELETE FROM structure_headings")
    suspend fun deleteAll()
}
