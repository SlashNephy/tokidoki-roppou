package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import blue.starry.tokidokiroppou.core.data.worker.ArticleWidgetUpdater
import blue.starry.tokidokiroppou.core.domain.model.Article
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleWidgetUpdaterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ArticleWidgetUpdater {

    override suspend fun updateAll(article: Article) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(ArticleWidget::class.java)

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { preferences ->
                preferences[ArticleWidgetStateKeys.LAW_ID] = article.lawId.value
                preferences[ArticleWidgetStateKeys.ARTICLE_NUMBER] = article.articleNumber
                preferences[ArticleWidgetStateKeys.SUPPLEMENTARY_PROVISION_LABEL] =
                    article.supplementaryProvisionLabel.orEmpty()
            }
        }

        ArticleWidget().updateAll(context)
        Timber.d("Updated %d widget(s)", glanceIds.size)
    }

    override suspend fun hasPlacedWidget(): Boolean {
        return GlanceAppWidgetManager(context).getGlanceIds(ArticleWidget::class.java).isNotEmpty()
    }
}
