package blue.starry.tokidokiroppou.core.ai.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExplanationCacheDao {
    @Query("""
        SELECT * FROM explanation_cache
        WHERE lawCode = :lawCode
          AND articleNumber = :articleNumber
          AND supplementaryProvisionLabel = :supplementaryProvisionLabel
          AND modelName = :modelName
          AND createdAt > :minTimestamp
        LIMIT 1
    """)
    suspend fun get(
        lawCode: String,
        articleNumber: String,
        supplementaryProvisionLabel: String,
        modelName: String,
        minTimestamp: Long,
    ): ExplanationCacheEntity?

    @Upsert
    suspend fun upsert(entity: ExplanationCacheEntity)

    @Query("DELETE FROM explanation_cache WHERE createdAt <= :minTimestamp")
    suspend fun deleteExpired(minTimestamp: Long)
}
