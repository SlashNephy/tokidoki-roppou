package blue.starry.tokidokiroppou.core.data.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleWidgetScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun schedule(intervalMinutes: Int) {
        val effectiveInterval = intervalMinutes.toLong().coerceAtLeast(15)

        val workRequest = PeriodicWorkRequestBuilder<ArticleWidgetWorker>(
            effectiveInterval,
            TimeUnit.MINUTES,
        ).build()

        // UPDATE は間隔変更を反映しつつ既存の周期を維持する。
        // CANCEL_AND_REENQUEUE だと Application#onCreate のたびに周期が
        // 0 から測り直しになり、頻繁にアプリを開くユーザーで自動更新が
        // 慢性的に遅延するため使わない。
        workManager.enqueueUniquePeriodicWork(
            ArticleWidgetWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )

        Timber.d("Scheduled widget update every %d minutes", effectiveInterval)
    }

    fun cancel() {
        workManager.cancelUniqueWork(ArticleWidgetWorker.WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        Timber.d("Cancelled widget update worker")
    }

    /** 即時に 1 回だけ更新する（ウィジェット配置直後やリロードボタン用） */
    fun requestImmediateUpdate() {
        val workRequest = OneTimeWorkRequestBuilder<ArticleWidgetWorker>().build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        Timber.d("Requested immediate widget update")
    }

    private companion object {
        const val IMMEDIATE_WORK_NAME = "article_widget_update_immediate"
    }
}
