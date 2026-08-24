package blue.starry.tokidokiroppou.feature.widget

import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetScheduler
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawCatalogRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * GlanceAppWidget / GlanceAppWidgetReceiver / ActionCallback は @AndroidEntryPoint を付けられないため、
 * 描画時のリポジトリ取得やスケジューラの取得は EntryPoint 経由で行う。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ArticleWidgetEntryPoint {
    fun lawRepository(): LawRepository

    fun lawCatalogRepository(): LawCatalogRepository

    fun applicationSettingsRepository(): ApplicationSettingsRepository

    fun articleWidgetScheduler(): ArticleWidgetScheduler
}
