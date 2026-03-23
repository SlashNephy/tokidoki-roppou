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
import com.google.firebase.ai.type.Tool
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
    private const val MODEL_NAME = "gemini-2.5-flash"

    @Provides
    @Singleton
    @Grounded
    fun provideGroundedModel(): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            tools = listOf(Tool.googleSearch()),
        )

    @Provides
    @Singleton
    @Plain
    fun providePlainModel(): GenerativeModel =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
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
