package blue.starry.tokidokiroppou.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import blue.starry.tokidokiroppou.core.data.notification.ArticleNotificationSender
import blue.starry.tokidokiroppou.core.domain.model.Article
import blue.starry.tokidokiroppou.core.domain.model.LawId
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

class ArticleWidget : GlanceAppWidget() {

    /**
     * [provideGlance] はセッションが生きている間 1 度しか評価されない
     * (`GlanceAppWidget.update` / `updateAll` は実行中の `provideGlance` を再開しない)。
     * そのため state と設定の読み出しは必ず [provideContent] の内側で行い、
     * `currentState` と Flow の購読を通じて更新が届くようにする。
     *
     * ここで事前に解決しているのは、初回描画で空表示が一瞬挟まらないようにするための
     * 初期値だけで、以降の更新はコンポジション内の [LaunchedEffect] が担う。
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ArticleWidgetEntryPoint::class.java,
        )

        val initialPreferences = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val initialArticleState = resolveArticleState(context, entryPoint, initialPreferences)
        val initialUseHalfWidthParentheses = try {
            entryPoint.applicationSettingsRepository().get().useHalfWidthParentheses
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to read settings for widget")
            false
        }

        provideContent {
            val preferences = currentState<Preferences>()
            var articleState by remember { mutableStateOf(initialArticleState) }
            LaunchedEffect(preferences) {
                articleState = resolveArticleState(context, entryPoint, preferences)
            }

            val useHalfWidthParentheses by remember {
                entryPoint.applicationSettingsRepository()
                    .observe()
                    .map { it.useHalfWidthParentheses }
                    .distinctUntilChanged()
            }.collectAsState(initialUseHalfWidthParentheses)

            GlanceTheme {
                ArticleWidgetContent(
                    article = articleState.article,
                    lawDisplayName = articleState.lawDisplayName,
                    useHalfWidthParentheses = useHalfWidthParentheses,
                    launchAction = createLaunchAction(context, articleState.article),
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
            intent.putExtra(
                ArticleNotificationSender.EXTRA_SUPPLEMENTARY_PROVISION_LABEL,
                article.supplementaryProvisionLabel ?: "",
            )
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        return actionStartActivity(intent)
    }

    /** ウィジェットが表示する条文と法令名。両者がちぐはぐにならないよう一括で差し替える */
    private data class ArticleState(
        val article: Article?,
        val lawDisplayName: String,
    )

    private companion object {
        suspend fun resolveArticleState(
            context: Context,
            entryPoint: ArticleWidgetEntryPoint,
            preferences: Preferences,
        ): ArticleState {
            val lawId = preferences[ArticleWidgetStateKeys.LAW_ID]
            val articleNumber = preferences[ArticleWidgetStateKeys.ARTICLE_NUMBER]
            val supplementaryProvisionLabel =
                preferences[ArticleWidgetStateKeys.SUPPLEMENTARY_PROVISION_LABEL]
                    ?.takeIf { it.isNotEmpty() }

            // CancellationException を握り潰すと、キャンセル済みの LaunchedEffect が
            // フォールバック状態を書き戻してしまうため、必ず再スローする。
            val article = if (lawId != null && articleNumber != null) {
                try {
                    entryPoint.lawRepository().getArticle(
                        lawId = LawId(lawId),
                        articleNumber = articleNumber,
                        supplementaryProvisionLabel = supplementaryProvisionLabel,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to resolve article for widget")
                    null
                }
            } else {
                null
            }

            val lawDisplayName = article?.let {
                val displayName = try {
                    entryPoint.lawCatalogRepository().getLaw(it.lawId)?.displayName
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to resolve law name for widget")
                    null
                }
                displayName ?: it.lawId.value
            } ?: context.getString(R.string.widget_label)

            return ArticleState(article, lawDisplayName)
        }
    }
}
