package blue.starry.tokidokiroppou.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ArticleEntity::class,
        LawMetadataEntity::class,
        BookmarkEntity::class,
        StructureHeadingEntity::class,
        LawEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun lawMetadataDao(): LawMetadataDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun structureHeadingDao(): StructureHeadingDao
    abstract fun lawDao(): LawDao
}
