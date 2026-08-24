package blue.starry.tokidokiroppou.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import blue.starry.tokidokiroppou.core.domain.repository.ApplicationSettingsRepository
import blue.starry.tokidokiroppou.core.domain.repository.LawRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ArticleWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lawRepository: LawRepository,
    private val settingsRepository: ApplicationSettingsRepository,
    private val widgetUpdater: ArticleWidgetUpdater,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("ArticleWidgetWorker started")

        if (!widgetUpdater.hasPlacedWidget()) {
            Timber.d("No widget placed, skipping")
            return Result.success()
        }

        val settings = settingsRepository.get()
        val article = lawRepository.getRandomArticle(
            settings.enabledLawIds,
            settings.excludeSupplementaryProvisions,
        )
        if (article == null) {
            Timber.w("No article found for widget")
            return Result.retry()
        }

        widgetUpdater.updateAll(article)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "article_widget_update"
    }
}
