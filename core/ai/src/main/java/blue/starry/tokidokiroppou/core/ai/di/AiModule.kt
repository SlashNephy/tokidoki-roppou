package blue.starry.tokidokiroppou.core.ai.di

import android.content.Context
import androidx.room.Room
import blue.starry.tokidokiroppou.core.ai.ArticleExplanationRepository
import blue.starry.tokidokiroppou.core.ai.ArticleExplanationRepositoryImpl
import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheDao
import blue.starry.tokidokiroppou.core.ai.db.ExplanationCacheDatabase
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindsModule {
    @Binds
    @Singleton
    abstract fun bindArticleExplanationRepository(
        impl: ArticleExplanationRepositoryImpl,
    ): ArticleExplanationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AiProvidesModule {
    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.1-flash-lite-preview",
        )

    @Provides
    @Singleton
    fun provideExplanationCacheDatabase(
        @ApplicationContext context: Context,
    ): ExplanationCacheDatabase =
        Room.databaseBuilder(
            context,
            ExplanationCacheDatabase::class.java,
            "explanation_cache.db",
        ).build()

    @Provides
    fun provideExplanationCacheDao(
        database: ExplanationCacheDatabase,
    ): ExplanationCacheDao = database.explanationCacheDao()
}
