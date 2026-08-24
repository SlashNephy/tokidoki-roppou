package blue.starry.tokidokiroppou.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class ArticleWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refresher: ArticleWidgetRefresher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("ArticleWidgetWorker started")

        return when (refresher.refresh()) {
            ArticleWidgetRefresher.Outcome.NoWidgetPlaced -> Result.success()
            ArticleWidgetRefresher.Outcome.ArticleNotFound -> Result.retry()
            ArticleWidgetRefresher.Outcome.Updated -> Result.success()
        }
    }

    companion object {
        const val WORK_NAME = "article_widget_update"
    }
}
