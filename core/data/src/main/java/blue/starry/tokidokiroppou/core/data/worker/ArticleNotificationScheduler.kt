package blue.starry.tokidokiroppou.core.data.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleNotificationScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun schedule(intervalMinutes: Int) {
        val effectiveInterval = intervalMinutes.toLong().coerceAtLeast(15)

        val workRequest = PeriodicWorkRequestBuilder<ArticleNotificationWorker>(
            effectiveInterval,
            TimeUnit.MINUTES,
        ).build()

        workManager.enqueueUniquePeriodicWork(
            ArticleNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest,
        )

        Timber.d("Scheduled article notification every %d minutes", effectiveInterval)
    }

    fun cancel() {
        workManager.cancelUniqueWork(ArticleNotificationWorker.WORK_NAME)
        Timber.d("Cancelled article notification worker")
    }
}
