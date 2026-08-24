package blue.starry.tokidokiroppou.feature.widget.di

import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetUpdater
import blue.starry.tokidokiroppou.feature.widget.ArticleWidgetUpdaterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {
    @Binds
    @Singleton
    abstract fun bindArticleWidgetUpdater(impl: ArticleWidgetUpdaterImpl): ArticleWidgetUpdater
}
