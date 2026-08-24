package blue.starry.tokidokiroppou.core.data.worker

import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * ウィジェット更新の判断ロジック。
 *
 * [ArticleWidgetWorker] から Android フレームワーク依存 (Context, WorkerParameters,
 * androidx.work.ListenableWorker.Result) を切り離し、素の JVM ユニットテストで検証できるようにする。
 */
@Singleton
class ArticleWidgetRefresher @Inject constructor(
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
    private val widgetUpdater: ArticleWidgetUpdater,
) {
    // 周期更新と即時更新は別々の unique work 名で管理されており WorkManager 側では
    // 同時実行され得るため、ここで直列化して「全ウィジェットは常に同じ条文を表示する」
    // という前提を守る。
    private val mutex = Mutex()

    /** ウィジェットの表示内容を更新すべきか判断し、実行する */
    suspend fun refresh(): Outcome = mutex.withLock {
        if (!widgetUpdater.hasPlacedWidget()) {
            Timber.d("No widget placed, skipping")
            return@withLock Outcome.NoWidgetPlaced
        }

        val settings = settingsRepository.get()
        val article = lawRepository.getRandomArticle(
            settings.enabledLawIds,
            settings.excludeSupplementaryProvisions,
        )
        if (article == null) {
            Timber.w("No article found for widget")
            return@withLock Outcome.ArticleNotFound
        }

        widgetUpdater.updateAll(article)
        Outcome.Updated
    }

    /** [refresh] の結果 */
    sealed interface Outcome {
        /** ウィジェットが 1 つも配置されていないため何もしなかった */
        data object NoWidgetPlaced : Outcome

        /** 条文の抽選に失敗した（state は更新していない） */
        data object ArticleNotFound : Outcome

        /** ウィジェットの表示内容を更新した */
        data object Updated : Outcome
    }
}
