package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArticleEntity::class, LawMetadataEntity::class, BookmarkEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun lawMetadataDao(): LawMetadataDao
    abstract fun bookmarkDao(): BookmarkDao
}
