package blue.starry.tokidokiroppou.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import blue.starry.tokidokiroppou.core.data.db.AppDatabase
import blue.starry.tokidokiroppou.core.data.db.ArticleDao
import blue.starry.tokidokiroppou.core.data.db.BookmarkDao
import blue.starry.tokidokiroppou.core.data.db.LawMetadataDao
import blue.starry.tokidokiroppou.core.data.db.StructureHeadingDao
import blue.starry.tokidokiroppou.core.data.repository.ApplicationSettingsRepositoryImpl
import blue.starry.tokidokiroppou.core.data.repository.BookmarkRepositoryImpl
import blue.starry.tokidokiroppou.core.data.repository.LawRepositoryImpl
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.BookmarkRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    @Singleton
    abstract fun bindLawRepository(impl: LawRepositoryImpl): LawRepository

    @Binds
    @Singleton
    abstract fun bindApplicationSettingsRepository(impl: ApplicationSettingsRepositoryImpl): ApplicationSettingsRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS bookmarks (
                    lawCode TEXT NOT NULL,
                    articleNumber TEXT NOT NULL,
                    supplementaryProvisionLabel TEXT NOT NULL DEFAULT '',
                    bookmarkedAt INTEGER NOT NULL,
                    PRIMARY KEY(lawCode, articleNumber, supplementaryProvisionLabel)
                )
                """,
            )
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 構造見出しテーブルの追加
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS structure_headings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    lawCode TEXT NOT NULL,
                    title TEXT NOT NULL,
                    level TEXT NOT NULL,
                    orderIndex INTEGER NOT NULL
                )
                """,
            )
            // lawCode + orderIndex でのクエリを高速化するインデックスを追加
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_structure_headings_lawCode_orderIndex
                ON structure_headings(lawCode, orderIndex)
                """,
            )
            // articles テーブルに orderIndex カラムを追加
            db.execSQL("ALTER TABLE articles ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tokidoki_roppou.db",
        ).addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideArticleDao(database: AppDatabase): ArticleDao {
        return database.articleDao()
    }

    @Provides
    @Singleton
    fun provideLawMetadataDao(database: AppDatabase): LawMetadataDao {
        return database.lawMetadataDao()
    }

    @Provides
    @Singleton
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    @Singleton
    fun provideStructureHeadingDao(database: AppDatabase): StructureHeadingDao {
        return database.structureHeadingDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("settings")
        }
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
