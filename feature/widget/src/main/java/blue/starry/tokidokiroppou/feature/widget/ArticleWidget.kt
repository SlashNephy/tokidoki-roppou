package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

class ArticleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        )

        val preferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val lawId = preferences[ArticleWidgetStateKeys.LAW_ID]
        val articleNumber = preferences[ArticleWidgetStateKeys.ARTICLE_NUMBER]
        val supplementaryProvisionLabel = preferences[ArticleWidgetStateKeys.SUPPLEMENTARY_PROVISION_LABEL]
            ?.takeIf { it.isNotEmpty() }

        val article = if (lawId != null && articleNumber != null) {
            runCatching {
                entryPoint.lawRepository().getArticle(
                    lawId = LawId(lawId),
                    articleNumber = articleNumber,
                    supplementaryProvisionLabel = supplementaryProvisionLabel,
                )
            }.onFailure { e ->
                Timber.e(e, "Failed to resolve article for widget")
            }.getOrNull()
        } else {
            null
        }

        val lawDisplayName = article?.let {
            runCatching { entryPoint.lawCatalogRepository().getLaw(it.lawId)?.displayName }
                .getOrNull()
                ?: it.lawId.value
        } ?: context.getString(R.string.widget_label)

        val useHalfWidthParentheses = runCatching {
            entryPoint.applicationSettingsRepository().get().useHalfWidthParentheses
        }.getOrDefault(false)

        provideContent {
            GlanceTheme {
                ArticleWidgetContent(
                    article = article,
                    lawDisplayName = lawDisplayName,
                    useHalfWidthParentheses = useHalfWidthParentheses,
                    launchAction = createLaunchAction(context, article),
                )
            }
        }
    }

    /**
     * 通知と同じ extra を付けて MainActivity を起動する。
     * app モジュールを参照できないため、起動 Intent は PackageManager から取得する。
     */
    private fun createLaunchAction(context: Context, article: Article?): Action {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        if (article != null) {
            intent.putExtra(ArticleNotificationSender.EXTRA_LAW_CODE, article.lawId.value)
            intent.putExtra(ArticleNotificationSender.EXTRA_ARTICLE_NUMBER, article.articleNumber)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        return actionStartActivity(intent)
    }
}
