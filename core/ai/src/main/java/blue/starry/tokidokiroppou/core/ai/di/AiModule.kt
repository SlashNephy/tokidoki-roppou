package blue.starry.tokidokiroppou.core.ai.di

import blue.starry.tokidokiroppou.core.ai.ArticleExplanationRepository
import blue.starry.tokidokiroppou.core.ai.ArticleExplanationRepositoryImpl
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
            modelName = "gemini-2.5-flash",
            tools = listOf(Tool.googleSearch()),
        )
}
