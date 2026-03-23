package blue.starry.tokidokiroppou.core.ai.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExplanationCacheEntity::class],
    version = 1,
)
abstract class ExplanationCacheDatabase : RoomDatabase() {
    abstract fun explanationCacheDao(): ExplanationCacheDao
}
